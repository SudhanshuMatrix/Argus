package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConcreteSignsTest {

    @Test
    fun `finds links and phone numbers through RedFlags`() {
        assertTrue(Sign.LINK in ConcreteSigns.find("Update at bit.ly/xyz123"))
        assertTrue(Sign.PHONE in ConcreteSigns.find("Call 0803 456 7890"))
    }

    @Test
    fun `code request needs a secret and a hand-over verb`() {
        assertTrue(Sign.CODE_REQUEST in ConcreteSigns.find("Please forward the code to me"))
        assertTrue(Sign.CODE_REQUEST in ConcreteSigns.find("confirm your card number and PIN"))
        assertFalse(Sign.CODE_REQUEST in ConcreteSigns.find("Do not share this code with anyone"))
        assertFalse(Sign.CODE_REQUEST in ConcreteSigns.find("Your PIN was changed"))
    }

    @Test
    fun `money request needs a pay verb and an amount`() {
        assertTrue(Sign.MONEY_REQUEST in ConcreteSigns.find("Send N40,000 to this account"))
        assertTrue(Sign.MONEY_REQUEST in ConcreteSigns.find("pay a processing fee"))
        assertTrue(Sign.MONEY_REQUEST in ConcreteSigns.find("abeg send 5k"))
        assertFalse(Sign.MONEY_REQUEST in ConcreteSigns.find("Tomatoes are now N8,000 per basket"))
        assertFalse(Sign.MONEY_REQUEST in ConcreteSigns.find("Debit Alert Amt: NGN5,000.00 POS Purchase"))
    }

    @Test
    fun `urgency, official claims and new numbers`() {
        assertTrue(Sign.URGENCY in ConcreteSigns.find("I need it urgently"))
        assertTrue(Sign.OFFICIAL_CLAIM in ConcreteSigns.find("Dear Customer, your account"))
        assertFalse(Sign.OFFICIAL_CLAIM in ConcreteSigns.find("send it to Blessing, Zentra Bank"))
        assertTrue(Sign.NEW_NUMBER in ConcreteSigns.find("I am using my friend's line"))
        assertTrue(Sign.NEW_NUMBER in ConcreteSigns.find("this is my new number"))
    }

    @Test
    fun `guard keeps SCAM only with a concrete sign`() {
        assertEquals(ReasonCode.ASKS_FOR_CODE, ConcreteSigns.guardScam(setOf(Sign.CODE_REQUEST)).reason)
        assertEquals(ReasonCode.MONEY_UNDER_PRESSURE, ConcreteSigns.guardScam(setOf(Sign.MONEY_REQUEST, Sign.NEW_NUMBER)).reason)
        assertEquals(ReasonCode.FAKE_OFFICIAL, ConcreteSigns.guardScam(setOf(Sign.MONEY_REQUEST, Sign.OFFICIAL_CLAIM)).reason)
        assertEquals(ReasonCode.MONEY_UNDER_PRESSURE, ConcreteSigns.guardScam(setOf(Sign.MONEY_REQUEST, Sign.URGENCY)).reason)
        assertEquals(ReasonCode.LINK_OR_NUMBER, ConcreteSigns.guardScam(setOf(Sign.LINK)).reason)
        assertEquals(Level.SCAM, ConcreteSigns.guardScam(setOf(Sign.PHONE)).level)
    }

    @Test
    fun `guard downgrades SCAM without a concrete sign`() {
        assertEquals(QuestionRules.Decision(Level.CAREFUL, ReasonCode.ASKS_FOR_MONEY_OR_LINK), ConcreteSigns.guardScam(setOf(Sign.MONEY_REQUEST)))
        assertEquals(QuestionRules.Decision(Level.NONE, null), ConcreteSigns.guardScam(setOf(Sign.URGENCY)))
        assertEquals(QuestionRules.Decision(Level.NONE, null), ConcreteSigns.guardScam(emptySet()))
    }
}
