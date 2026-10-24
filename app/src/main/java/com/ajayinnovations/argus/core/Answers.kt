package com.ajayinnovations.argus.core

/** The model's yes/no answers to the 7 questions in [PromptBuilder.questionsSystem]. */
data class Answers(
    val asksMoney: Boolean,      // 1
    val asksCode: Boolean,       // 2
    val asksClickOrCall: Boolean, // 3
    val claimsOfficial: Boolean, // 4
    val accountProblem: Boolean, // 5
    val promisesGain: Boolean,   // 6
    val pressure: Boolean,       // 7
) {
    /** "YNNYYNY": for the debug screen and logs (answers only, never message text). */
    fun compact(): String = listOf(asksMoney, asksCode, asksClickOrCall, claimsOfficial, accountProblem, promisesGain, pressure)
        .joinToString("") { if (it) "Y" else "N" }
}

object AnswerParser {

    /** "1Y", "1: yes", "1) N", "1.no", "1? Y" (template echoed); a digit 1–7 not inside a longer number. */
    private val answer = Regex("""(?<![0-9])([1-7])\s*[.:)=?-]?\s*(yes|no|y|n)(?![a-z])""", RegexOption.IGNORE_CASE)

    /** Null unless all 7 questions got an answer. The first answer to each question counts. */
    fun parse(raw: String): Answers? {
        val found = mutableMapOf<Int, Boolean>()
        answer.findAll(raw).forEach { m ->
            val question = m.groupValues[1].toInt()
            if (question !in found) found[question] = m.groupValues[2].startsWith("y", ignoreCase = true)
        }
        if (found.size != 7) return null
        return Answers(
            asksMoney = found.getValue(1),
            asksCode = found.getValue(2),
            asksClickOrCall = found.getValue(3),
            claimsOfficial = found.getValue(4),
            accountProblem = found.getValue(5),
            promisesGain = found.getValue(6),
            pressure = found.getValue(7),
        )
    }
}
