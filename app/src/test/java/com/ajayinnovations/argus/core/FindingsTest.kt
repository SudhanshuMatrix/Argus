package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindingsTest {

    @Test
    fun `link and blocked account`() {
        val found = Findings.find("Dear customer, your account is blocked. Click bit.ly/x1abc to fix it")
        assertEquals(listOf(Finding.Link, Finding.AccountProblem(AccountTrouble.BLOCKED)), found.take(2))
    }

    @Test
    fun `asks for the BVN comes first`() {
        val found = Findings.find("Share your BVN with our agent at www.cbn-help.com")
        assertEquals(Finding.AsksForSecret(Secret.BVN), found.first())
        assertTrue(Finding.Link in found)
    }

    @Test
    fun `OTP request names the OTP`() =
        assertEquals(Finding.AsksForSecret(Secret.OTP), Findings.find("Send me the OTP you received").first())

    @Test
    fun `relative on a new number asking for money`() {
        val found = Findings.find("Mummy this is Tunde, I am using my friend line. Please send 50,000 naira urgently")
        assertTrue(Finding.NewNumber in found)
        assertTrue(Finding.MoneyRequest in found)
        assertTrue(found.indexOf(Finding.NewNumber) < found.indexOf(Finding.MoneyRequest))
    }

    @Test
    fun `prize`() = assertTrue(Finding.Prize in Findings.find("Congratulations! You have won 2,000,000 naira in the promo"))

    @Test
    fun `a family greeting finds nothing`() =
        assertTrue(Findings.find("Good morning mummy, we will visit this Sunday after church").isEmpty())

    @Test
    fun `do not share is not a request`() =
        assertTrue(Findings.find("Your OTP is 482913. Do not share this code with anyone.").none { it is Finding.AsksForSecret })
}
