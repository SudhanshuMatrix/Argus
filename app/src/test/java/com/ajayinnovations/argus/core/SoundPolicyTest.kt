package com.ajayinnovations.argus.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundPolicyTest {

    private val scam = requireNotNull(AlertPlan.forLevel(Level.SCAM))
    private val careful = requireNotNull(AlertPlan.forLevel(Level.CAREFUL))

    @Test
    fun `speaks when the ringer is on`() = assertTrue(SoundPolicy.shouldSpeak(Ringer.NORMAL, speakOnSilent = false))

    @Test
    fun `stays quiet on silent and vibrate by default`() {
        assertFalse(SoundPolicy.shouldSpeak(Ringer.SILENT, speakOnSilent = false))
        assertFalse(SoundPolicy.shouldSpeak(Ringer.VIBRATE, speakOnSilent = false))
    }

    @Test
    fun `the setting speaks even on silent`() {
        assertTrue(SoundPolicy.shouldSpeak(Ringer.SILENT, speakOnSilent = true))
        assertTrue(SoundPolicy.shouldSpeak(Ringer.VIBRATE, speakOnSilent = true))
    }

    @Test
    fun `hear it again always plays`() = assertTrue(SoundPolicy.shouldSpeak(Ringer.SILENT, speakOnSilent = false, userAsked = true))

    @Test
    fun `scam vibrates directly on silent, where the channel can't`() {
        assertTrue(SoundPolicy.vibrateDirectly(scam, Ringer.SILENT, notificationPosted = true))
        assertFalse(SoundPolicy.vibrateDirectly(scam, Ringer.NORMAL, notificationPosted = true))
        assertFalse(SoundPolicy.vibrateDirectly(scam, Ringer.VIBRATE, notificationPosted = true))
    }

    @Test
    fun `scam vibrates directly when the notification failed`() =
        assertTrue(SoundPolicy.vibrateDirectly(scam, Ringer.NORMAL, notificationPosted = false))

    @Test
    fun `careful never vibrates directly`() {
        assertFalse(SoundPolicy.vibrateDirectly(careful, Ringer.SILENT, notificationPosted = false))
    }
}
