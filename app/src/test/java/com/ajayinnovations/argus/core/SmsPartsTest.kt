package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsPartsTest {

    @Test
    fun `parts from one sender are joined in order`() {
        val parts = listOf(
            SmsParts.Part("0801", "Dear customer, your BVN "),
            SmsParts.Part("0801", "is blocked. Click "),
            SmsParts.Part("0801", "bit.ly/xyz123"),
        )
        assertEquals(listOf("0801" to "Dear customer, your BVN is blocked. Click bit.ly/xyz123"), SmsParts.join(parts, "?"))
    }

    @Test
    fun `different senders stay separate`() {
        val parts = listOf(SmsParts.Part("A", "one"), SmsParts.Part("B", "two"), SmsParts.Part("A", " more"))
        assertEquals(listOf("A" to "one more", "B" to "two"), SmsParts.join(parts, "?"))
    }

    @Test
    fun `missing sender and empty bodies`() {
        assertEquals(listOf("?" to "hello"), SmsParts.join(listOf(SmsParts.Part(null, "hello"), SmsParts.Part("", null)), "?"))
        assertEquals(emptyList<Pair<String, String>>(), SmsParts.join(listOf(SmsParts.Part("A", null)), "?"))
    }
}
