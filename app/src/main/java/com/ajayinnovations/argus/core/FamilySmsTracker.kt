package com.ajayinnovations.argus.core

/** What the send reports say about one family SMS. */
enum class FamilySmsOutcome { SENT, FAILED }

/**
 * Combines the per-part send reports of each family SMS (a long text goes out in parts).
 * FAILED as soon as any part fails, SENT once every part succeeded, each reported once.
 * Never retries. In memory; pure Kotlin.
 */
class FamilySmsTracker {

    private class Batch(val parts: Int) {
        var succeeded = 0
        var done = false
    }

    private val batches = HashMap<Int, Batch>()
    private var nextId = 1

    /** Call before sending; returns the batch ID to put in each part's report. */
    @Synchronized
    fun start(parts: Int): Int {
        require(parts > 0)
        val id = nextId++
        batches[id] = Batch(parts)
        // Finished batches are kept so late reports of their other parts are ignored; oldest dropped
        if (batches.size > MAX_BATCHES) batches.keys.minOrNull()?.let(batches::remove)
        return id
    }

    /**
     * One part's report. Returns the outcome once it is known, otherwise null. A failure for an
     * unknown batch (the process restarted while sending) still counts as FAILED.
     */
    @Synchronized
    fun onPartResult(batchId: Int, ok: Boolean): FamilySmsOutcome? {
        val batch = batches[batchId] ?: return if (ok) null else FamilySmsOutcome.FAILED
        if (batch.done) return null
        if (!ok) {
            batch.done = true
            return FamilySmsOutcome.FAILED
        }
        batch.succeeded++
        if (batch.succeeded < batch.parts) return null
        batch.done = true
        return FamilySmsOutcome.SENT
    }

    private companion object {
        const val MAX_BATCHES = 20
    }
}
