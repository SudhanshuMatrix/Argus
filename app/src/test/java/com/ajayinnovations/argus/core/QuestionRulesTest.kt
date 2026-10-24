package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Test

class QuestionRulesTest {

    /** Answers from a 7-char Y/N string, e.g. "NYNNNNN". */
    private fun a(yn: String): Answers {
        require(yn.length == 7)
        val b = yn.map { it == 'Y' }
        return Answers(b[0], b[1], b[2], b[3], b[4], b[5], b[6])
    }

    private fun decide(yn: String, flags: List<String> = emptyList()) = QuestionRules.decide(a(yn), flags)
    private val link = listOf("link")
    private val phone = listOf("phone number")

    @Test
    fun `asks for a code is always SCAM`() {
        assertEquals(QuestionRules.Decision(Level.SCAM, ReasonCode.ASKS_FOR_CODE), decide("NYNNNNN"))
    }

    @Test
    fun `account problem plus any action is SCAM`() {
        assertEquals(ReasonCode.ACCOUNT_PROBLEM, decide("YNNNYNN").reason)         // + money
        assertEquals(ReasonCode.ACCOUNT_PROBLEM, decide("NNYNYNN").reason)         // + asks to click/call
        assertEquals(ReasonCode.ACCOUNT_PROBLEM, decide("NNNNYNN", link).reason)   // + link found by regex
        assertEquals(ReasonCode.ACCOUNT_PROBLEM, decide("NNNNYNN", phone).reason)  // + phone found by regex
    }

    @Test
    fun `account problem alone asks for nothing`() {
        assertEquals(Level.NONE, decide("NNNNYNN").level)
    }

    @Test
    fun `prize, job or loan plus money or link is SCAM`() {
        assertEquals(ReasonCode.TOO_GOOD_OFFER, decide("YNNNNYN").reason)
        assertEquals(ReasonCode.TOO_GOOD_OFFER, decide("NNNNNYN", link).reason)
        assertEquals(ReasonCode.TOO_GOOD_OFFER, decide("NNYNNYN").reason)
    }

    @Test
    fun `official plus money or link is SCAM`() {
        assertEquals(ReasonCode.FAKE_OFFICIAL, decide("YNNYNNN").reason)
        assertEquals(ReasonCode.FAKE_OFFICIAL, decide("NNNYNNN", link).reason)
    }

    @Test
    fun `official asking only to call is not SCAM`() {
        // "Your bank: call us on 0700..." with nothing else: a call alone is not money, a code or a link
        assertEquals(Level.NONE, decide("NNYYNNN").level)
    }

    @Test
    fun `money or a link with no other sign is CAREFUL`() {
        assertEquals(QuestionRules.Decision(Level.CAREFUL, ReasonCode.ASKS_FOR_MONEY_OR_LINK), decide("YNNNNNN"))
        assertEquals(ReasonCode.ASKS_FOR_MONEY_OR_LINK, decide("NNNNNNN", link).reason)
    }

    @Test
    fun `pressure plus a call is CAREFUL`() {
        assertEquals(QuestionRules.Decision(Level.CAREFUL, ReasonCode.PRESSURE), decide("NNYNNNY"))
        assertEquals(ReasonCode.PRESSURE, decide("NNNNNNY", phone).reason)
    }

    @Test
    fun `pressure alone is NONE`() {
        assertEquals(Level.NONE, decide("NNNNNNY").level)
    }

    @Test
    fun `asks for nothing is NONE`() {
        assertEquals(QuestionRules.Decision(Level.NONE, null), decide("NNNNNNN"))
        assertEquals(Level.NONE, decide("NNNYNYN").level)   // official + promise, but asks nothing
    }

    @Test
    fun `earlier rules win`() {
        assertEquals(ReasonCode.ASKS_FOR_CODE, decide("YYYYYYY", link).reason)
        assertEquals(ReasonCode.ACCOUNT_PROBLEM, decide("YNYYYYY").reason)
    }
}
