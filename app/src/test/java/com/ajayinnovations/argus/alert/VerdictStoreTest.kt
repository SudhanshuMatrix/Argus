package com.ajayinnovations.argus.alert

import com.ajayinnovations.argus.core.Alert
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.core.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerdictStoreTest {

    @Test
    fun `returns what was stored by ID`() {
        val store = VerdictStore()
        val scam = Alert(Verdict(Level.SCAM), sender = "Tunde", preview = "Send me the OTP")
        val careful = Alert(Verdict(Level.CAREFUL))
        val a = store.put(scam)
        val b = store.put(careful)
        assertNotEquals(a, b)
        assertEquals(scam, store.get(a))
        assertEquals(careful, store.get(b))
    }

    @Test
    fun `unknown ID gives null`() = assertNull(VerdictStore().get(-1))

    @Test
    fun `oldest verdicts are forgotten`() {
        val store = VerdictStore(capacity = 2, firstId = 1)
        val first = store.put(Alert(Verdict(Level.SCAM)))
        store.put(Alert(Verdict(Level.OTP)))
        store.put(Alert(Verdict(Level.CAREFUL)))
        assertNull(store.get(first))
    }

    @Test
    fun `IDs stay positive when the counter wraps`() {
        val store = VerdictStore(firstId = Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, store.put(Alert(Verdict(Level.SCAM))))
        assertTrue(store.put(Alert(Verdict(Level.SCAM))) > 0)
    }

    @Test
    fun `scam warnings stay pending until acknowledged, oldest first`() {
        val store = VerdictStore()
        val first = store.put(Alert(Verdict(Level.SCAM)))
        store.put(Alert(Verdict(Level.CAREFUL)))   // only scams wait for "I understand"
        val second = store.put(Alert(Verdict(Level.SCAM)))
        assertEquals(listOf(first, second), store.pendingScams())
        store.acknowledge(second)
        assertEquals(listOf(first), store.pendingScams())
        store.acknowledge(first)
        assertEquals(emptyList<Int>(), store.pendingScams())
    }

    @Test
    fun `alerts pushed out of memory are no longer pending`() {
        val store = VerdictStore(capacity = 2)
        val old = store.put(Alert(Verdict(Level.SCAM)))
        store.put(Alert(Verdict(Level.SCAM)))
        store.put(Alert(Verdict(Level.SCAM)))
        assertFalse(old in store.pendingScams())
        assertEquals(2, store.pendingScams().size)
    }
}
