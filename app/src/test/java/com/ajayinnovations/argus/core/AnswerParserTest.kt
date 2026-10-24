package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnswerParserTest {

    @Test
    fun `exact format`() {
        assertEquals("YNNYYNY", AnswerParser.parse("1Y 2N 3N 4Y 5Y 6N 7Y")?.compact())
    }

    @Test
    fun `no spaces, lowercase, words and punctuation`() {
        assertEquals("YNNYYNY", AnswerParser.parse("1y2n3n4y5y6n7y")?.compact())
        assertEquals("YNNYYNY", AnswerParser.parse("1. Yes\n2. No\n3) no\n4: YES\n5 - yes\n6=n\n7 y")?.compact())
    }

    @Test
    fun `template echoed with question marks`() {
        assertEquals("NYNNNNY", AnswerParser.parse("1? N 2? Y 3? N 4? N 5? N 6? N 7? Y")?.compact())
        assertEquals("NYNNNNY", AnswerParser.parse("1?N 2?Y 3?N 4?N 5?N 6?N 7?Y")?.compact())
    }

    @Test
    fun `text around the answers and code fences`() {
        assertEquals("NNNNNNN", AnswerParser.parse("```\nAnswers: 1N 2N 3N 4N 5N 6N 7N\n```\nDone.")?.compact())
    }

    @Test
    fun `the first answer to a question counts`() {
        assertEquals("YNNNNNN", AnswerParser.parse("1Y 2N 3N 4N 5N 6N 7N 1N")?.compact())
    }

    @Test
    fun `missing answers give null`() {
        assertNull(AnswerParser.parse("1Y 2N 3N 4Y 5Y 6N"))
        assertNull(AnswerParser.parse(""))
        assertNull(AnswerParser.parse("This message is a scam."))
        assertNull(AnswerParser.parse("""{"level":"SCAM"}"""))
    }

    @Test
    fun `digits inside numbers or words are not answers`() {
        // "N5,000" and "21yes" must not be read as question 5 or 1
        assertNull(AnswerParser.parse("Send N5,000 now, 21yes 31no"))
        assertNull(AnswerParser.parse("1Yellow 2Nope 3N 4N 5N 6N 7N"))
    }
}
