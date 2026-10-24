package com.ajayinnovations.argus.debug

import com.ajayinnovations.argus.core.CheckMode
import com.ajayinnovations.argus.core.DecidedBy
import com.ajayinnovations.argus.core.Level
import java.util.Locale

enum class Outcome(val label: String) {
    OK("ok"),
    /** A scam that stayed silent. Target 0. */
    MISSED_SCAM("MISSED SCAM"),
    /** SCAM on a normal message. Target 0. */
    LOUD_FALSE_ALARM("LOUD FALSE ALARM"),
    /** CAREFUL on a normal message. A few are acceptable. */
    QUIET_CAUTION("quiet caution"),
}

/** One sample's outcome. File names and levels only go to logcat; reasons stay on screen. */
data class SampleResult(
    val file: String,
    val expectedScam: Boolean,
    val level: Level,
    val decidedBy: DecidedBy,
    val answers: String?,
    val reason: String,
    val millis: Long,
    /** The model's raw replies: dev screen and Copy results only, never logged. */
    val replies: List<String> = emptyList(),
) {
    val outcome: Outcome
        get() = when {
            expectedScam && (level == Level.SCAM || level == Level.CAREFUL) -> Outcome.OK
            expectedScam -> Outcome.MISSED_SCAM
            level == Level.SCAM -> Outcome.LOUD_FALSE_ALARM
            level == Level.CAREFUL -> Outcome.QUIET_CAUTION
            else -> Outcome.OK
        }

    val expected: String get() = if (expectedScam) "scam" else "normal"
}

data class SampleSummary(
    val missedScams: Int,
    val loudFalseAlarms: Int,
    val quietCautions: Int,
    val total: Int,
    val averageMillis: Long,
    val slowest: SampleResult?,
    val skipped: List<String>,
) {
    companion object {
        fun of(results: List<SampleResult>) = SampleSummary(
            missedScams = results.count { it.outcome == Outcome.MISSED_SCAM },
            loudFalseAlarms = results.count { it.outcome == Outcome.LOUD_FALSE_ALARM },
            quietCautions = results.count { it.outcome == Outcome.QUIET_CAUTION },
            total = results.size,
            averageMillis = if (results.isEmpty()) 0 else results.sumOf { it.millis } / results.size,
            slowest = results.maxByOrNull { it.millis },
            skipped = results.filter { it.decidedBy == DecidedBy.SKIP_FILTER }.map { it.file },
        )
    }
}

/** The "Copy results" text: a Markdown table to paste back into the chat or the plan. */
object SampleReport {

    fun format(results: List<SampleResult>, header: String, mode: CheckMode): String = buildString {
        val s = SampleSummary.of(results)
        appendLine(header)
        appendLine("Mode: ${mode.label}")
        appendLine("Missed scams: ${s.missedScams} · Loud false alarms: ${s.loudFalseAlarms} · Quiet cautions: ${s.quietCautions} (of ${s.total})")
        appendLine("Average: ${seconds(s.averageMillis)}, slowest: ${s.slowest?.let { "${it.file} ${seconds(it.millis)}" } ?: "–"}")
        appendLine("Skipped by the skip filter: ${s.skipped.ifEmpty { listOf("none") }.joinToString()}")
        appendLine()
        appendLine("| File | Expected | Level | Outcome | Decided by | Answers | Seconds | Reason |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- |")
        results.forEach {
            appendLine(
                "| ${it.file} | ${it.expected} | ${it.level} | ${it.outcome.label} | ${it.decidedBy} | ${it.answers ?: "–"} | " +
                    "${seconds(it.millis)} | ${oneLine(it.reason)} |",
            )
        }
        if (results.any { it.replies.isNotEmpty() }) {
            appendLine()
            appendLine("Raw model replies:")
            results.filter { it.replies.isNotEmpty() }.forEach { appendLine("- ${it.file}: ${it.replies.joinToString(" ⏐ ") { r -> oneLine(r) }}") }
        }
    }.trimEnd()

    private fun oneLine(s: String) = s.replace("|", "/").replace("\n", " ").trim()

    fun seconds(millis: Long): String = String.format(Locale.US, "%.1f s", millis / 1000.0)
}
