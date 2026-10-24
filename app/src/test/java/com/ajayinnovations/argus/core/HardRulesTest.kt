package com.ajayinnovations.argus.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HardRulesTest {

    private val bvnBlocked = "Dear customer, your BVN is blocked, click bit.ly/x1abc to update today or your account will be closed"

    @Test
    fun `the BVN blocked link is a hard SCAM`() = assertEquals(HardRules.Rule.LINK_ACCOUNT, HardRules.match(bvnBlocked))

    @Test
    fun `a link plus each account word`() {
        listOf("BVN", "NIN", "blocked", "restricted", "suspended", "verify", "update your account").forEach {
            assertEquals(it, HardRules.Rule.LINK_ACCOUNT, HardRules.match("Notice: $it here https://x.co/a now"))
        }
    }

    @Test
    fun `account words without a link are not a hard rule`() = assertNull(HardRules.match("Your BVN is blocked, visit any branch"))

    @Test
    fun `a link without account words is not a hard rule`() =
        assertNull(HardRules.match("See the wedding photos here https://photos.app.goo.gl/abc"))

    @Test
    fun `requests to hand over a secret`() {
        listOf(
            "Please send me the OTP you just received",
            "Share your BVN with our agent",
            "Kindly forward the 6-digit code sent to your phone",
            "Give me your ATM PIN so I can help",
            "Tell me your password",
            "Abeg send the code wey dem send you",
        ).forEach { assertEquals(it, HardRules.Rule.CODE_REQUEST, HardRules.match(it)) }
    }

    @Test
    fun `do not share warnings are not requests`() {
        listOf(
            "Your OTP is 482913. Do not share this code with anyone.",
            "Never give your PIN to anybody, not even bank staff.",
            "Don't send your password to anyone.",
        ).forEach { assertNull(it, HardRules.match(it)) }
    }

    @Test
    fun `the checker decides at once without calling the model`() = runBlocking {
        val model = FakeModel.answering("NONE")
        val result = ScamChecker(model).check(bvnBlocked, "SMS", Language.ENGLISH)
        assertEquals(Level.SCAM, result.verdict.level)
        assertEquals(DecidedBy.HARD_RULE, result.decidedBy)
        assertEquals(ReasonCode.ACCOUNT_PROBLEM, result.verdict.reasonCode)
        assertEquals(0, model.calls)
    }

    @Test
    fun `a code request is SCAM even if the model would say NONE`() = runBlocking {
        val result = ScamChecker(FakeModel.answering("NONE")).check("Mummy please send me the OTP the bank sent you now", "WhatsApp", Language.ENGLISH)
        assertEquals(Level.SCAM, result.verdict.level)
        assertEquals(ReasonCode.ASKS_FOR_CODE, result.verdict.reasonCode)
    }

    @Test
    fun `a genuine OTP stays OTP`() = runBlocking {
        val result = ScamChecker(FakeModel.answering("SCAM")).check("Your OTP is 482913. Do not share this code with anyone.", "SMS", Language.ENGLISH)
        assertEquals(Level.OTP, result.verdict.level)
    }

    @Test
    fun `money from a new number`() {
        listOf(
            "Mummy this is Tunde, I am using my friend line. Please send 50,000 naira urgently",
            "Hello ma, it's me, this is my new line. Kindly transfer N20,000 for my school fees",
            "Daddy my phone fell in water, I'm on my friend's phone. Send 15k for transport",
            "I lost my phone, this is my new number. Please send money, N10,000",
        ).forEach { assertEquals(it, HardRules.Rule.MONEY_NEW_NUMBER, HardRules.match(it)) }
    }

    @Test
    fun `a new number alone, or money alone, is not a hard rule`() {
        assertNull(HardRules.match("Hi mummy, this is my new number, save it. See you on Sunday"))
        assertNull(HardRules.match("Please send the 5,000 naira contribution to the treasurer by Friday"))
    }

    @Test
    fun `prize plus a fee or payment`() {
        listOf(
            "Congratulations! You have won 2,000,000 naira in the MTN promo. Pay 5,000 processing fee to claim.",
            "You have won a new fridge! Send N3,500 delivery charge to receive it today",
            "MTN PROMO: your number won a prize. Transfer the redemption fee of N2,000 to 0123456789",
            "Congrats o! Congratulations, you don win prize. Pay small charge make we send am",
        ).forEach { assertEquals(it, HardRules.Rule.PRIZE_FEE, HardRules.match(it)) }
    }

    @Test
    fun `congratulations or a promo without a fee is not a hard rule`() {
        assertNull(HardRules.match("Congratulations on your new baby! God bless the family"))
        assertNull(HardRules.match("Shoprite promo this weekend: 20% off rice and oil"))
    }

    @Test
    fun `no normal sample hits a hard rule`() {
        Samples.normals.forEach { assertNull(it.name, HardRules.match(it.text)) }
    }

    @Test
    fun `most scam samples hit a hard rule`() {
        val hits = Samples.scams.filter { HardRules.match(it.text) != null }.map { it.name }
        println("Hard-rule scams: $hits")
        assertTrue("only ${hits.size}: $hits", hits.size >= 4)
    }
}
