package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagePreviewTest {

    @Test
    fun `short text is kept and flattened`() = assertEquals("Hello mummy, how are you?", MessagePreview.of("Hello mummy,\n\n  how are you?  "))

    @Test
    fun `long text is cut at a word with an ellipsis`() {
        val preview = MessagePreview.of("word ".repeat(60))
        assertTrue(preview.endsWith("word…"))
        assertTrue(preview.length <= MessagePreview.DEFAULT_MAX + 1)
    }

    @Test
    fun `one long word is cut hard`() = assertEquals("a".repeat(120) + "…", MessagePreview.of("a".repeat(300)))

    @Test
    fun `the full message keeps line breaks and is capped without splitting an emoji`() {
        assertEquals("Line one\nLine two", MessagePreview.full("  Line one\nLine two  "))
        val long = MessagePreview.full("a".repeat(MessagePreview.FULL_MAX - 1) + "\uD83D\uDE00".repeat(10))
        assertEquals("a".repeat(MessagePreview.FULL_MAX - 1) + "…", long)
    }

    @Test
    fun `a cut never splits an emoji`() {
        // 119 letters then emoji with no spaces: the hard cut at 120 falls inside the first emoji
        val preview = MessagePreview.of("a".repeat(119) + "\uD83D\uDE00".repeat(50))
        assertEquals("a".repeat(119) + "…", preview)
    }

    @Test
    fun `non-Latin text is previewed`() {
        val preview = MessagePreview.of("Привет мама, это мой новый номер. ".repeat(10))
        assertTrue(preview.startsWith("Привет мама"))
        assertTrue(preview.endsWith("…"))
    }

    @Test
    fun `a link near the end is kept, up to the hard limit`() {
        val text = "Congratulations! You have won 2,000,000 naira in the MTN promo. Pay N5,000 processing fee to claim. Click www.mtn-promo-claim.com now"
        val preview = MessagePreview.keepingMarks(text, Findings.find(text))
        assertTrue(preview, preview.contains("www.mtn-promo-claim.com"))
        assertTrue(MessagePreview.keepingMarks("x ".repeat(300) + "bit.ly/abc", emptyList()).length <= MessagePreview.HARD_MAX + 1)
    }
}
