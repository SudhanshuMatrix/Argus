package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightsTest {

    private fun marked(text: String) = Highlights.find(text).map { text.substring(it.start, it.end) to it.kind }

    @Test
    fun `BVN blocked with a link`() {
        val marks = marked("Dear customer, your BVN is blocked. Click bit.ly/x1abc to update today.")
        assertTrue(("blocked" to MarkKind.ACCOUNT) in marks)
        assertTrue(("bit.ly/x1abc" to MarkKind.LINK) in marks)
    }

    @Test
    fun `secret words are marked only when they are asked for`() {
        assertTrue(("OTP" to MarkKind.SECRET) in marked("Please send me the OTP you received"))
        assertTrue(marked("Your OTP is 482913. Do not share it.").none { it.second == MarkKind.SECRET })
    }

    @Test
    fun `money and new number`() {
        val marks = marked("Mummy I am using my friend line. Send N50,000 or 20k urgently")
        assertTrue(("friend line" to MarkKind.NEW_NUMBER) in marks)
        assertTrue(("N50,000" to MarkKind.MONEY) in marks)
        assertTrue(("20k" to MarkKind.MONEY) in marks)
    }

    @Test
    fun `prize and phone`() {
        val marks = marked("Congratulations! Call 0803 123 4567 to claim your prize")
        assertTrue(("Congratulations" to MarkKind.PRIZE) in marks)
        assertTrue(("0803 123 4567" to MarkKind.PHONE) in marks)
        assertTrue(("prize" to MarkKind.PRIZE) in marks)
    }

    @Test
    fun `marks never overlap and stay in order`() {
        val text = "Visit www.verify-bvn-blocked.com now, pay ₦5,000"
        val marks = Highlights.find(text)
        marks.zipWithNext().forEach { (a, b) -> assertTrue(a.end <= b.start) }
        assertEquals(MarkKind.LINK, marks.first().kind)
        assertEquals("www.verify-bvn-blocked.com", text.substring(marks.first().start, marks.first().end))
    }

    @Test
    fun `a family greeting has no marks`() = assertTrue(Highlights.find("Good morning mummy, see you on Sunday").isEmpty())
}
