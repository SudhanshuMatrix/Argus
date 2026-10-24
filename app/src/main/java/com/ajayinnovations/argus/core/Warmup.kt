package com.ajayinnovations.argus.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Loads the model and runs one tiny check about [delayMillis] after the listener connects
 * (after boot or a restart), so the first real message doesn't pay for reading the weights
 * from storage. Never at the moment of boot, when the phone is busiest. Pure Kotlin.
 */
class Warmup(
    private val scope: CoroutineScope,
    /** Setup finished and the model file is ready (never triggers the copy). */
    private val isReady: () -> Boolean,
    private val isLoaded: () -> Boolean,
    private val model: TextGenerator,
    private val delayMillis: Long = DEFAULT_DELAY_MILLIS,
) {
    private var job: Job? = null

    /** Restarts the countdown; a reconnect within the delay doesn't warm twice. */
    @Synchronized
    fun schedule() {
        job?.cancel()
        job = scope.launch {
            delay(delayMillis)
            if (!isReady() || isLoaded()) return@launch
            try {
                model.generate(SYSTEM, PROMPT, MAX_OUTPUT_TOKENS)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nothing lost: the next real message loads it again
            }
        }
    }

    @Synchronized
    fun cancel() {
        job?.cancel()
        job = null
    }

    companion object {
        const val DEFAULT_DELAY_MILLIS = 2 * 60 * 1000L
        private const val SYSTEM = "Reply with one word."
        private const val PROMPT = "Say OK."
        private const val MAX_OUTPUT_TOKENS = 4
    }
}
