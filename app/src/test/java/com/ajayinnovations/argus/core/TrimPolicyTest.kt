package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrimPolicyTest {

    @Test
    fun `moderate pressure or worse unloads`() {
        listOf(TrimPolicy.RUNNING_MODERATE, TrimPolicy.RUNNING_LOW, TrimPolicy.RUNNING_CRITICAL, TrimPolicy.MODERATE, TrimPolicy.COMPLETE)
            .forEach { assertTrue("level $it", TrimPolicy.shouldUnload(it)) }
    }

    @Test
    fun `leaving the app or becoming cached keeps the model`() {
        assertFalse(TrimPolicy.shouldUnload(TrimPolicy.UI_HIDDEN))
        assertFalse(TrimPolicy.shouldUnload(TrimPolicy.BACKGROUND))
    }

    @Test
    fun `values match Android's ComponentCallbacks2`() {
        // Mirrored by hand so core has no Android import; these never change
        assertEquals(listOf(5, 10, 15, 20, 40, 60, 80), listOf(
            TrimPolicy.RUNNING_MODERATE, TrimPolicy.RUNNING_LOW, TrimPolicy.RUNNING_CRITICAL,
            TrimPolicy.UI_HIDDEN, TrimPolicy.BACKGROUND, TrimPolicy.MODERATE, TrimPolicy.COMPLETE,
        ))
    }
}
