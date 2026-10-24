package com.ajayinnovations.argus.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeduperTest {

    @Test
    fun `duplicate messages are skipped`() {
        val deduper = Deduper()
        assertTrue(deduper.isNew("Your BVN is blocked"))
        assertFalse(deduper.isNew("Your BVN is blocked"))
    }

    @Test
    fun `duplicates ignore case and surrounding space`() {
        val deduper = Deduper()
        assertTrue(deduper.isNew("Your BVN is blocked"))
        assertFalse(deduper.isNew("  your bvn is BLOCKED \n"))
    }

    @Test
    fun `different messages are new`() {
        val deduper = Deduper()
        assertTrue(deduper.isNew("one message here"))
        assertTrue(deduper.isNew("another message here"))
    }

    @Test
    fun `oldest messages are forgotten past capacity`() {
        val deduper = Deduper(capacity = 2)
        deduper.isNew("a message")
        deduper.isNew("b message")
        deduper.isNew("c message")
        assertTrue(deduper.isNew("a message"))
        assertFalse(deduper.isNew("c message"))
    }
}
