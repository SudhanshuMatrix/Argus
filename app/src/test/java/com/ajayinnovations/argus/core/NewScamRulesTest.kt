package com.ajayinnovations.argus.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The three rules added after the first holdout run (Oct 4), with normal messages that must stay quiet. */
class NewScamRulesTest {

    private fun check(text: String, model: TextGenerator = FakeModel.answering("NONE")) =
        runBlocking { ScamChecker(model).check(text, "SMS", Language.ENGLISH) }

    // Rule 1: reply with / enter / type / send the code

    @Test
    fun `reply with the code is a code request and a hard rule`() {
        assertEquals(HardRules.Rule.CODE_REQUEST, HardRules.match("Your Airtel line is due for upgrade. Kindly reply with the 4 digit code sent to you."))
        assertEquals(HardRules.Rule.CODE_REQUEST, HardRules.match("Abeg reply this message with the PIN wey dem send you"))
        assertEquals(HardRules.Rule.CODE_REQUEST, HardRules.match("Please send me the code that just came to your phone"))
    }

    @Test
    fun `enter or type the code counts as a code request for the guard`() {
        assertTrue(Sign.CODE_REQUEST in ConcreteSigns.find("Go to the page and type the code we sent you to receive your reward"))
        assertTrue(Sign.CODE_REQUEST in ConcreteSigns.find("Enter the OTP on the form so we can release your money"))
        val result = check("Go to the page and type the code we sent you to receive your reward", FakeModel.answering("SCAM"))
        assertEquals(Level.SCAM, result.verdict.level)
    }

    @Test
    fun `real codes that say enter it in the app stay an OTP reminder`() {
        val real = "Your verification code is 552901. Enter it in the app to log in. Do not share this code with anyone."
        assertEquals(Level.OTP, check(real).verdict.level)
    }

    @Test
    fun `do not reply with the code is a warning, not a request`() {
        assertNull(HardRules.match("Never reply to anyone with this code, even if they say they are from the bank. Code 4812"))
    }

    @Test
    fun `an estate gate code is not a loud alarm`() {
        assertNotEquals(Level.SCAM, check("Visitor pass for Saturday: enter code 4421 at the estate gate, the guard will let you in.").verdict.level)
    }

    // Rule 2: investment or scheme promising a return

    @Test
    fun `promised returns are an investment sign`() {
        listOf(
            "Put 20k in our pool and withdraw 60k by Friday",
            "Join us and double your money in two weeks",
            "Guaranteed profit every month, no risk at all",
            "Our plan pays 15% weekly profit to every member",
            "I invested $200 in the trading group and got $900 back",
        ).forEach { assertTrue(it, Sign.INVESTMENT_RETURN in ConcreteSigns.find(it)) }
    }

    @Test
    fun `promised return with a money request is a hard-rule scam`() {
        val text = "Invest N30,000 with our forex manager this week and receive N95,000 next Monday. Send to the account below."
        assertEquals(HardRules.Rule.INVESTMENT_MONEY, HardRules.match(text))
        assertEquals(Level.SCAM, check(text).verdict.level)
        assertEquals(DecidedBy.HARD_RULE, check(text).decidedBy)
    }

    @Test
    fun `promised return alone is never silent`() {
        val text = "Good evening ma, my cousin's crypto group helps members double your money in 10 days. Should I add you?"
        val result = check(text)   // model says NONE
        assertEquals(Level.CAREFUL, result.verdict.level)
        assertEquals(ReasonCode.TOO_GOOD_OFFER, result.verdict.reasonCode)
    }

    @Test
    fun `ordinary savings, prices and bank interest stay quiet`() {
        listOf(
            "I put the 50k in the cooperative account like you said. The meeting is next Sunday.",
            "Tomatoes are 18k a basket now, I bought half and got change of 2k.",
            "Your fixed deposit of N500,000 has matured. Interest of N12,500 has been credited.",
        ).forEach {
            assertFalse(it, Sign.INVESTMENT_RETURN in ConcreteSigns.find(it))
            assertEquals(it, Level.NONE, check(it).verdict.level)
        }
    }

    // Rule 3: call this number about account, card or fraud

    @Test
    fun `call a number about account trouble is at least CAREFUL`() {
        val text = "We noticed suspicious logins on your profile. Call 08097771234 now to stop it."
        assertTrue(Sign.CALL_ABOUT_ACCOUNT in ConcreteSigns.find(text))
        assertEquals(Level.CAREFUL, check(text).verdict.level)
        // Even when the model fails completely
        assertEquals(Level.CAREFUL, check(text, FakeModel.failing()).verdict.level)
    }

    @Test
    fun `call about account trouble from a claimed bank or customer care is a hard-rule scam`() {
        val text = "Dear valued customer, your ATM card has been blocked due to fraud. Call our customer care on 07012345678 to reactivate."
        assertEquals(HardRules.Rule.CALL_OFFICIAL, HardRules.match(text))
        assertEquals(Level.SCAM, check(text).verdict.level)
        val pidgin = "Na fraud desk be dis. Your account don block. Call 09123456780 sharp sharp"
        assertEquals(HardRules.Rule.CALL_OFFICIAL, HardRules.match(pidgin))
    }

    @Test
    fun `a real bank message asking you to call the number on your card stays quiet`() {
        listOf(
            "Dear Customer, a debit of N5,000 occurred on your account. If you did not authorise it, call the number on the back of your card.",
            "Acct 0123***89 credited with NGN20,000. For enquiries call 07001234567. Thank you for banking with us.",
            "Dear customer, your new debit card is ready for pickup at our Ikeja branch. Call 08031112222 if you have questions.",
        ).forEach {
            assertNull(it, HardRules.match(it))
            assertFalse(it, Sign.CALL_ABOUT_ACCOUNT in ConcreteSigns.find(it))
            assertNotEquals(it, Level.SCAM, check(it).verdict.level)
        }
    }

    @Test
    fun `the original 10 normal samples still don't get a rule alarm`() {
        Samples.normals.forEach { sample ->
            val signs = ConcreteSigns.find(sample.text)
            assertFalse(sample.name, Sign.INVESTMENT_RETURN in signs)
            assertFalse(sample.name, Sign.CALL_ABOUT_ACCOUNT in signs)
        }
    }
}
