package com.ajayinnovations.argus.alert

import com.ajayinnovations.argus.core.Alert
import com.ajayinnovations.argus.core.Level
import kotlin.random.Random

/**
 * Recent alerts (verdict, sender, preview), in memory only, so Intents carry just an ID and
 * never message-derived text. Never logged or written to disk.
 * After the process dies the IDs are gone and WarningActivity shows the generic warning.
 * IDs start at a random number so a stale notification from an earlier process can't open
 * a newer verdict. Pure Kotlin.
 */
class VerdictStore(
    private val capacity: Int = DEFAULT_CAPACITY,
    firstId: Int = Random.nextInt(1, Int.MAX_VALUE / 2),
) {
    private var nextId = firstId
    /** SCAM alerts she hasn't tapped "I understand" on yet, oldest first. */
    private val pending = LinkedHashSet<Int>()
    private val verdicts = object : LinkedHashMap<Int, Alert>(16, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Alert>) = size > capacity
    }

    /** Stores [alert] and returns its ID (always positive). */
    @Synchronized
    fun put(alert: Alert): Int {
        val id = nextId
        nextId = if (nextId == Int.MAX_VALUE) 1 else nextId + 1
        verdicts[id] = alert
        if (alert.verdict.level == Level.SCAM) pending += id
        pending.retainAll(verdicts.keys)
        return id
    }

    @Synchronized
    fun get(id: Int): Alert? = verdicts[id]

    /** "I understand" on this alert. */
    @Synchronized
    fun acknowledge(id: Int) {
        pending -= id
    }

    /** IDs of SCAM alerts not yet acknowledged, oldest first (only those still in memory). */
    @Synchronized
    fun pendingScams(): List<Int> = pending.filter { it in verdicts }

    companion object {
        const val DEFAULT_CAPACITY = 20
    }
}
