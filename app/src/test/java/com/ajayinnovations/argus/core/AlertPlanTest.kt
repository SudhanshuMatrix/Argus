package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertPlanTest {

    @Test
    fun `NONE does nothing`() = assertNull(AlertPlan.forLevel(Level.NONE))

    @Test
    fun `SCAM is loud, full screen, always vibrates and may text the family`() {
        val plan = requireNotNull(AlertPlan.forLevel(Level.SCAM))
        assertEquals(AlertChannel.SCAM, plan.channel)
        assertTrue(plan.fullScreen)
        assertTrue(plan.alwaysVibrate)
        assertTrue(plan.familySms)
    }

    @Test
    fun `CAREFUL and OTP are quiet notifications only`() {
        listOf(Level.CAREFUL, Level.OTP).forEach { level ->
            val plan = requireNotNull(AlertPlan.forLevel(level))
            assertEquals(AlertChannel.CAREFUL, plan.channel)
            assertFalse(plan.fullScreen)
            assertFalse(plan.alwaysVibrate)
            assertFalse(plan.familySms)
        }
    }
}
