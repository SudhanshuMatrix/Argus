package com.ajayinnovations.argus.debug

import com.ajayinnovations.argus.core.CheckMode
import com.ajayinnovations.argus.core.DecidedBy
import com.ajayinnovations.argus.core.Level
import org.junit.Assert.assertEquals
import org.junit.Test

class SampleReportTest {

    private fun r(file: String, scam: Boolean, level: Level, by: DecidedBy = DecidedBy.MODEL, ms: Long = 1000) =
        SampleResult(file, scam, level, by, "YNNNNNN", "why | here", ms)

    @Test
    fun `outcomes`() {
        assertEquals(Outcome.OK, r("a", true, Level.SCAM).outcome)
        assertEquals(Outcome.OK, r("a", true, Level.CAREFUL).outcome)
        assertEquals(Outcome.MISSED_SCAM, r("a", true, Level.NONE).outcome)
        assertEquals(Outcome.MISSED_SCAM, r("a", true, Level.OTP).outcome)
        assertEquals(Outcome.OK, r("a", false, Level.NONE).outcome)
        assertEquals(Outcome.OK, r("a", false, Level.OTP).outcome)
        assertEquals(Outcome.LOUD_FALSE_ALARM, r("a", false, Level.SCAM).outcome)
        assertEquals(Outcome.QUIET_CAUTION, r("a", false, Level.CAREFUL).outcome)
    }

    @Test
    fun `summary counts the three numbers, average, slowest and skipped`() {
        val results = listOf(
            r("scam/1.txt", true, Level.NONE, ms = 1000),
            r("normal/1.txt", false, Level.NONE, DecidedBy.SKIP_FILTER, ms = 3000),
            r("normal/2.txt", false, Level.CAREFUL, ms = 2000),
            r("normal/3.txt", false, Level.SCAM, ms = 2000),
        )
        val s = SampleSummary.of(results)
        assertEquals(1, s.missedScams)
        assertEquals(1, s.loudFalseAlarms)
        assertEquals(1, s.quietCautions)
        assertEquals(2000, s.averageMillis)
        assertEquals("normal/1.txt", s.slowest?.file)
        assertEquals(listOf("normal/1.txt"), s.skipped)
    }

    @Test
    fun `report is a markdown table with the three numbers`() {
        val lines = SampleReport.format(listOf(r("scam/1.txt", true, Level.SCAM)), "Pixel 8", CheckMode.QUESTIONS).lines()
        assertEquals("Pixel 8", lines[0])
        assertEquals("Mode: ${CheckMode.QUESTIONS.label}", lines[1])
        assertEquals("Missed scams: 0 · Loud false alarms: 0 · Quiet cautions: 0 (of 1)", lines[2])
        assertEquals("| scam/1.txt | scam | SCAM | ok | MODEL | YNNNNNN | 1.0 s | why / here |", lines.last())
    }
}
