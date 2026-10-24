package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FamilySmsTrackerTest {

    private val tracker = FamilySmsTracker()

    @Test
    fun `one part sent`() {
        val id = tracker.start(1)
        assertEquals(FamilySmsOutcome.SENT, tracker.onPartResult(id, ok = true))
    }

    @Test
    fun `sent only when every part is sent`() {
        val id = tracker.start(2)
        assertNull(tracker.onPartResult(id, ok = true))
        assertEquals(FamilySmsOutcome.SENT, tracker.onPartResult(id, ok = true))
    }

    @Test
    fun `any failed part fails it, reported once`() {
        val id = tracker.start(3)
        assertNull(tracker.onPartResult(id, ok = true))
        assertEquals(FamilySmsOutcome.FAILED, tracker.onPartResult(id, ok = false))
        assertNull(tracker.onPartResult(id, ok = false))
    }

    @Test
    fun `a failure for an unknown batch still counts`() {
        assertEquals(FamilySmsOutcome.FAILED, tracker.onPartResult(99, ok = false))
        assertNull(tracker.onPartResult(99, ok = true))
    }

    @Test
    fun `batches are independent`() {
        val a = tracker.start(1)
        val b = tracker.start(1)
        assertEquals(FamilySmsOutcome.FAILED, tracker.onPartResult(b, ok = false))
        assertEquals(FamilySmsOutcome.SENT, tracker.onPartResult(a, ok = true))
    }
}
