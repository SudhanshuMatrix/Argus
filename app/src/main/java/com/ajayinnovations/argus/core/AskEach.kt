package com.ajayinnovations.argus.core

/** Mode E: one short yes/no question per model call. */
enum class Question(val text: String) {
    CODE("Does this message ask the reader to share a code, OTP, PIN, BVN, NIN, password or card details?"),
    MONEY("Does this message ask the reader to send money or pay a fee?"),
    OFFICIAL("Does this message claim to be from a bank, government, company or customer care?"),
    ACCOUNT_OR_OFFER("Does this message say an account is blocked, restricted or needs updating, or promise a prize, job, loan or profit?"),
    PRESSURE("Does this message pressure the reader to act fast, or say the sender has a new number?"),
}

enum class YesNo { YES, NO, UNKNOWN }

object YesNoParser {
    private val leading = Regex("""^[\s"'`*_>.:\-]*(yes|no)\b""", RegexOption.IGNORE_CASE)

    /** Only whether the reply starts with yes or no counts; anything else is UNKNOWN. */
    fun parse(raw: String): YesNo = when (leading.find(raw)?.groupValues?.get(1)?.lowercase()) {
        "yes" -> YesNo.YES
        "no" -> YesNo.NO
        else -> YesNo.UNKNOWN
    }
}

object AskEachPrompts {
    const val SYSTEM = "You answer questions about one text message. Answer only yes or no."
    const val MAX_OUTPUT_TOKENS = 4
    private const val SUFFIX = " Answer only yes or no."

    /** First turn (or every call when nothing is reused): the message plus the question. */
    fun withMessage(message: String, question: Question): String =
        "Message:\n<<<\n${message.take(PromptBuilder.MAX_MESSAGE_CHARS)}\n>>>\n${question.text}$SUFFIX"

    /** Later turns in the same conversation: the question only. */
    fun followUp(question: Question): String = "${question.text}$SUFFIX"
}

/**
 * Mode E rules: Gemma answers, code decides. UNKNOWN counts as "not yes". Questions are asked
 * in this order and skipped once they can no longer change the result:
 *
 *   SCAM    CODE                                         ASKS_FOR_CODE
 *   SCAM    ACCOUNT_OR_OFFER and (MONEY or link or phone) ACCOUNT_OR_OFFER
 *   SCAM    OFFICIAL and (MONEY or link)                 FAKE_OFFICIAL
 *   SCAM    MONEY and PRESSURE                           MONEY_UNDER_PRESSURE
 *   CAREFUL MONEY or link                                ASKS_FOR_MONEY_OR_LINK
 *   CAREFUL PRESSURE and phone                           PRESSURE
 *   NONE    otherwise
 *
 * link and phone come from RedFlags (regex is more reliable for them than the model).
 */
object AskEachRules {

    data class Outcome(val decision: QuestionRules.Decision, val answers: Map<Question, YesNo>)

    suspend fun run(redFlags: List<String>, ask: suspend (Question) -> YesNo): Outcome {
        val link = "link" in redFlags
        val phone = "phone number" in redFlags
        val answers = linkedMapOf<Question, YesNo>()
        suspend fun yes(q: Question): Boolean = (answers[q] ?: ask(q).also { answers[q] = it }) == YesNo.YES
        fun done(level: Level, reason: ReasonCode?) = Outcome(QuestionRules.Decision(level, reason), answers)

        if (yes(Question.CODE)) return done(Level.SCAM, ReasonCode.ASKS_FOR_CODE)
        val money = yes(Question.MONEY)
        if ((money || link || phone) && yes(Question.ACCOUNT_OR_OFFER)) return done(Level.SCAM, ReasonCode.ACCOUNT_OR_OFFER)
        if ((money || link) && yes(Question.OFFICIAL)) return done(Level.SCAM, ReasonCode.FAKE_OFFICIAL)
        val pressure = (money || phone) && yes(Question.PRESSURE)
        return when {
            money && pressure -> done(Level.SCAM, ReasonCode.MONEY_UNDER_PRESSURE)
            money || link -> done(Level.CAREFUL, ReasonCode.ASKS_FOR_MONEY_OR_LINK)
            pressure && phone -> done(Level.CAREFUL, ReasonCode.PRESSURE)
            else -> done(Level.NONE, null)
        }
    }

    /** "a:Y b:N" style summary for the debug screen. */
    fun compact(answers: Map<Question, YesNo>): String =
        answers.entries.joinToString(" ") { (q, a) -> "${q.ordinal + 1}${a.name.first()}" }
}
