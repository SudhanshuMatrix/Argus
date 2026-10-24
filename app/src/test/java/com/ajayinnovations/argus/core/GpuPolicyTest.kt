package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpuPolicyTest {

    @Test
    fun `CPU is the default`() {
        assertFalse(GpuPolicy.shouldUseGpu(optedIn = false, disabled = false))
    }

    @Test
    fun `GPU only when opted in and not disabled`() {
        assertTrue(GpuPolicy.shouldUseGpu(optedIn = true, disabled = false))
        assertFalse(GpuPolicy.shouldUseGpu(optedIn = true, disabled = true))
    }

    @Test
    fun `a clean start keeps the count`() {
        assertEquals(1, GpuPolicy.deathsAfterStart(callWasInProgress = false, deathsInARow = 1))
    }

    @Test
    fun `one death does not disable the GPU`() {
        val deaths = GpuPolicy.deathsAfterStart(callWasInProgress = true, deathsInARow = 0)
        assertEquals(1, deaths)
        assertFalse(GpuPolicy.shouldDisable(deaths))
    }

    @Test
    fun `two deaths in a row disable the GPU`() {
        val deaths = GpuPolicy.deathsAfterStart(callWasInProgress = true, deathsInARow = 1)
        assertTrue(GpuPolicy.shouldDisable(deaths))
    }
}
