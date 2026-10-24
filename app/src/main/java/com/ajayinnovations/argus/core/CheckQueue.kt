package com.ajayinnovations.argus.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class SourceType(val label: String) { SMS("SMS"), WHATSAPP("WhatsApp") }

/**
 * One incoming message. Lives only in memory; [sender] is used only inside the prompt and on the
 * warning screen. [chatKey] says which chat it came from (a WhatsApp notification key, or the SMS
 * sender); only its hash leaves this class.
 */
class IncomingMessage(
    val type: SourceType,
    val text: String,
    val sender: String,
    val chatKey: String = "${type.label}:$sender",
) {
    /** For the prompt only, e.g. "SMS from 0801…". Never logged. */
    val sourceLabel: String get() = "${type.label} from $sender"

    companion object {
        /** When SMS or WhatsApp gives no sender. Not shown to the parent. */
        const val UNKNOWN_SENDER = "someone"
    }
}

/** The most recent check, for the debug screen. No text. */
data class LastCheck(val type: SourceType, val level: Level, val decidedBy: DecidedBy, val millis: Long)

/** Counts since the process started. No text. */
data class IngestStats(
    val received: Int = 0,
    /** Same text seen recently (WhatsApp re-posts, SMS parts). */
    val duplicates: Int = 0,
    /** Setup hasn't finished or the model isn't ready: never triggers the model copy. */
    val notReady: Int = 0,
    /** Obviously harmless (very short, emoji only). */
    val skippedByFilter: Int = 0,
    /** Went through the checker (rules and/or model). */
    val checked: Int = 0,
    /** Anything unexpected while checking; the queue keeps going. */
    val failed: Int = 0,
    /** Pushed out because the queue was full. */
    val dropped: Int = 0,
    /** Results by level, for messages that reached the checker (skip filter included). */
    val levels: Map<Level, Int> = emptyMap(),
    val last: LastCheck? = null,
) {
    val skipped: Int get() = duplicates + notReady + skippedByFilter + dropped
}

/**
 * One consumer checks messages one at a time, in memory only. When full, the oldest waiting
 * message is dropped. Messages the rules decide alone (skip filter, OTP, hard rules) never
 * queue: they are handled as they arrive, so a hard-rule scam in a busy group alerts at once.
 * Nothing here can crash the app: every failure is counted and skipped.
 */
class CheckQueue(
    private val checker: ScamChecker,
    private val deduper: Deduper,
    /** True once setup has finished and the model file is ready. */
    private val isReady: () -> Boolean,
    private val language: () -> Language,
    /** Called after each check, on the queue's thread. Must not log, store or send text or sender. */
    private val onChecked: (IncomingMessage, CheckResult, millis: Long) -> Unit,
    private val clock: () -> Long,
    private val scope: CoroutineScope,
    capacity: Int = DEFAULT_CAPACITY,
    /** Longest a queued check may take; then the rules alone decide (the model may be stuck). */
    private val checkTimeoutMillis: Long = CHECK_TIMEOUT_MILLIS,
    /** Told the class name of anything that went wrong (for the log). Never text. */
    private val onError: (String) -> Unit = {},
) {
    private class Item(val message: IncomingMessage, val onDone: () -> Unit)

    private val _stats = MutableStateFlow(IngestStats())
    val stats: StateFlow<IngestStats> = _stats.asStateFlow()

    private val channel = Channel<Item>(
        capacity = capacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = { item ->
            _stats.update { it.copy(dropped = it.dropped + 1) }
            item.onDone.safely()
        },
    )

    init {
        scope.launch {
            for (item in channel) {
                try {
                    process(item.message)
                } catch (e: CancellationException) {
                    // Only the queue shutting down may end this loop. A stray cancellation from
                    // inside a check must not stop every later message from being checked.
                    if (!currentCoroutineContext().isActive) throw e
                    failed(e)
                } catch (t: Throwable) {
                    failed(t)
                } finally {
                    item.onDone.safely()
                }
            }
        }
    }

    /**
     * Queues a message. Returns false if it was a duplicate (then [onDone] runs at once).
     * [onDone] runs exactly once, after the check or when the message is dropped.
     */
    fun submit(message: IncomingMessage, onDone: () -> Unit = {}): Boolean {
        _stats.update { it.copy(received = it.received + 1) }
        if (!deduper.isNew(message.text)) {
            _stats.update { it.copy(duplicates = it.duplicates + 1) }
            onDone.safely()
            return false
        }
        // Decided by the rules alone: no need to wait behind model checks
        val start = clock()
        val fast = try {
            if (isReady()) checker.checkWithoutModel(message.text) else null
        } catch (t: Throwable) {
            null   // the queue will try again, and count it if it fails there
        }
        if (fast != null) {
            val millis = clock() - start
            scope.launch {
                try {
                    record(message, fast, millis)
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    _stats.update { it.copy(failed = it.failed + 1) }
                } finally {
                    onDone.safely()
                }
            }
            return true
        }
        channel.trySend(Item(message, onDone))
        return true
    }

    private suspend fun process(message: IncomingMessage) {
        if (!isReady()) {
            _stats.update { it.copy(notReady = it.notReady + 1) }
            return
        }
        val start = clock()
        val result = withTimeoutOrNull(checkTimeoutMillis) {
            checker.check(message.text, message.sourceLabel, language())
        } ?: checker.checkRulesOnly(message.text).also { onError.safely("CheckTimeout") }
        record(message, result, clock() - start)
    }

    private fun failed(t: Throwable) {
        _stats.update { it.copy(failed = it.failed + 1) }
        onError.safely(t.javaClass.simpleName)
    }

    private fun ((String) -> Unit).safely(name: String) {
        try {
            this(name)
        } catch (t: Throwable) {
            // Logging must never stop the queue
        }
    }

    private fun record(message: IncomingMessage, result: CheckResult, millis: Long) {
        val level = result.verdict.level
        _stats.update {
            val counted = if (result.decidedBy == DecidedBy.SKIP_FILTER) it.copy(skippedByFilter = it.skippedByFilter + 1)
            else it.copy(checked = it.checked + 1)
            counted.copy(
                levels = counted.levels + (level to (counted.levels[level] ?: 0) + 1),
                last = LastCheck(message.type, level, result.decidedBy, millis),
            )
        }
        onChecked(message, result, millis)
    }

    private fun (() -> Unit).safely() {
        try {
            this()
        } catch (t: Throwable) {
            // A broken callback must never stop the queue
        }
    }

    companion object {
        const val DEFAULT_CAPACITY = 32
        /** A cold start after a reboot measured 43 s on the Pixel 8 (load + first inference). */
        const val CHECK_TIMEOUT_MILLIS = 90_000L
    }
}
