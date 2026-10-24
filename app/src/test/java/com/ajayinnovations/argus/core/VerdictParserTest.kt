package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerdictParserTest {

    @Test
    fun `clean JSON`() {
        assertEquals(
            Verdict(Level.SCAM, "Asks for BVN.", "Do not reply."),
            VerdictParser.parse("""{"level":"SCAM","reason":"Asks for BVN.","advice":"Do not reply."}"""),
        )
    }

    @Test
    fun `JSON in code fences`() {
        val raw = "```json\n{\"level\": \"CAREFUL\", \"reason\": \"Unknown link.\", \"advice\": \"Ask family.\"}\n```"
        assertEquals(Verdict(Level.CAREFUL, "Unknown link.", "Ask family."), VerdictParser.parse(raw))
    }

    @Test
    fun `text before and after the JSON`() {
        val raw = "Sure! Here is my answer:\n{\"level\":\"NONE\",\"reason\":\"Family chat.\",\"advice\":\"Nothing to do.\"}\nHope this helps {smile}"
        assertEquals(Verdict(Level.NONE, "Family chat.", "Nothing to do."), VerdictParser.parse(raw))
    }

    @Test
    fun `missing reason and advice`() {
        assertEquals(Verdict(Level.SCAM), VerdictParser.parse("""{"level":"SCAM"}"""))
    }

    @Test
    fun `lowercase and padded levels`() {
        assertEquals(Level.SCAM, VerdictParser.parse("""{"level":"scam"}""")?.level)
        assertEquals(Level.CAREFUL, VerdictParser.parse("""{"level":" Careful "}""")?.level)
    }

    @Test
    fun `model OTP becomes CAREFUL`() {
        assertEquals(Level.CAREFUL, VerdictParser.parse("""{"level":"OTP","reason":"A code.","advice":"Keep it."}""")?.level)
    }

    @Test
    fun `lenient JSON with single quotes and unquoted keys`() {
        assertEquals(Level.SCAM, VerdictParser.parse("{level: 'SCAM', reason: 'Fake prize.'}")?.level)
    }

    @Test
    fun `braces inside strings do not confuse it`() {
        assertEquals("Says {urgent}.", VerdictParser.parse("""{"level":"CAREFUL","reason":"Says {urgent}."}""")?.reason)
    }

    @Test
    fun `skips a broken object and uses the next one`() {
        assertEquals(Level.SCAM, VerdictParser.parse("""{oops} then {"level":"SCAM"}""")?.level)
    }

    @Test
    fun `unusable output gives null`() {
        listOf(
            "",
            "I think this is a scam.",
            "{",
            "}{",
            """{"reason":"no level"}""",
            """{"level":"MAYBE"}""",
            """{"level":3}""",
            """["SCAM"]""",
            "\u0000\u0001 random bytes",
        ).forEach { assertNull(it, VerdictParser.parse(it)) }
    }
}
