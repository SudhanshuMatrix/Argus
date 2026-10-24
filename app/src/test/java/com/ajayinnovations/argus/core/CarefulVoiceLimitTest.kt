package com.ajayinnovations.argus.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarefulVoiceLimitTest {

    private val minute = 60_000L

    @Test
    fun `one spoken be-careful per chat every 10 minutes`() {
        val limit = CarefulVoiceLimit()
        assertTrue(limit.allow(Level.CAREFUL, chatId = 1, now = 0))
        assertFalse(limit.allow(Level.CAREFUL, chatId = 1, now = 9 * minute))
        assertTrue(limit.allow(Level.CAREFUL, chatId = 1, now = 10 * minute))
    }

    @Test
    fun `other chats are not limited`() {
        val limit = CarefulVoiceLimit()
        assertTrue(limit.allow(Level.CAREFUL, chatId = 1, now = 0))
        assertTrue(limit.allow(Level.CAREFUL, chatId = 2, now = 1))
    }

    @Test
    fun `scams and OTPs always speak, even right after a be-careful`() {
        val limit = CarefulVoiceLimit()
        assertTrue(limit.allow(Level.CAREFUL, chatId = 1, now = 0))
        assertTrue(limit.allow(Level.SCAM, chatId = 1, now = 1))
        assertTrue(limit.allow(Level.SCAM, chatId = 1, now = 2))
        assertTrue(limit.allow(Level.OTP, chatId = 1, now = 3))
    }

    @Test
    fun `manual checks and test alerts have no chat and always speak`() {
        val limit = CarefulVoiceLimit()
        assertTrue(limit.allow(Level.CAREFUL, chatId = null, now = 0))
        assertTrue(limit.allow(Level.CAREFUL, chatId = null, now = 1))
    }

    @Test
    fun `a burst of 29 group cautions speaks once`() {
        val limit = CarefulVoiceLimit()
        val spoken = (0 until 29).count { limit.allow(Level.CAREFUL, chatId = 42, now = it * 8_000L) }
        assertTrue(spoken == 1)
    }
}
