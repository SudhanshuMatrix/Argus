package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedFlagsTest {

    @Test
    fun `keywords use word boundaries`() {
        assertFalse("pin" in RedFlags.find("I went shopping for spinach"))
        assertFalse("nin" in RedFlags.find("Good morning, dinner is ready"))
        assertFalse("loan" in RedFlags.find("The loans officer phoned"))
        assertTrue("pin" in RedFlags.find("Send your PIN now"))
    }

    @Test
    fun `keywords ignore case`() {
        assertTrue("bvn" in RedFlags.find("Your BVN is BLOCKED"))
        assertTrue("blocked" in RedFlags.find("Your BVN is BLOCKED"))
    }

    @Test
    fun `multi-word phrases match`() {
        assertTrue("you have won" in RedFlags.find("Hello, you have won a car"))
        assertTrue("customer care" in RedFlags.find("Call Customer Care today"))
    }

    @Test
    fun `links are found`() {
        listOf("click https://x.co", "go to www.example.com", "bit.ly/xyz123", "wa.me/2348011112222", "tinyurl.com/abc")
            .forEach { assertTrue(it, "link" in RedFlags.find(it)) }
    }

    @Test
    fun `Nigerian phone numbers are found with or without spaces`() {
        listOf("0801 234 5678", "08012345678", "+2348012345678", "0905-123-4567")
            .forEach { assertTrue(it, "phone number" in RedFlags.find("call $it")) }
    }

    @Test
    fun `account numbers and prices are not phone numbers`() {
        assertFalse("phone number" in RedFlags.find("Send N5,000 to 2087654321"))
        assertFalse("phone number" in RedFlags.find("Bal: NGN84,250.50"))
    }

    @Test
    fun `a normal message has no flags`() {
        assertEquals(emptyList<String>(), RedFlags.find("Mummy good morning. I will come on Saturday."))
    }
}
