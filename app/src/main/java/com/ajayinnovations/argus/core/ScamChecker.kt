package com.ajayinnovations.argus.core

import kotlinx.coroutines.CancellationException

/** Which step decided. Shown on the debug screen; never includes the message. */
enum class DecidedBy { SKIP_FILTER, OTP_RULE, HARD_RULE, MODEL, SAFETY_FLOOR, MODEL_FAILED }

/** The ways to ask the model. Letters match docs/PLAN.md. */
enum class CheckMode(val label: String) {
    /** B: the model picks the level and writes the reason. */
    CLASSIFY("B classify"),
    /** D: 7 yes/no answers in one reply, decided by QuestionRules. */
    QUESTIONS("D 7 questions in one reply"),
    /** E: one yes/no question per turn in one conversation (the message is read once). */
    ASK_EACH("E one question per turn"),
    /** E without reuse: every question is a separate call that re-reads the message. */
    ASK_EACH_SEPARATE("E one question per call"),
    /** F: B, but SCAM only with a concrete sign; fixed reasons. */
    CLASSIFY_GUARDED("F classify + guard"),
}

/**
 * [answers]: compact answers in question modes. [replies]: the model's raw replies, for the dev
 * debug screen only; held in memory, never logged or stored. [findings]: what the rules found
 * in the message, most important first, for the fixed reason sentence.
 */
data class CheckResult(
    val verdict: Verdict,
    val decidedBy: DecidedBy,
    val redFlagCount: Int,
    val answers: String? = null,
    val replies: List<String> = emptyList(),
    val findings: List<Finding> = emptyList(),
)

/** Skip filter + OTP rule + hard rules + model + rules + safety floor. Pure Kotlin, no logging. */
class ScamChecker(private val model: TextGenerator) {

    suspend fun check(
        text: String,
        source: String,
        language: Language,
        mode: CheckMode = DEFAULT_MODE,
    ): CheckResult {
        checkWithoutModel(text)?.let { return it }
        val flags = RedFlags.find(text)
        val findings = Findings.find(text)
        val result = when (mode) {
            CheckMode.CLASSIFY -> classify(text, source, language, flags, guarded = false)
            CheckMode.CLASSIFY_GUARDED -> classify(text, source, language, flags, guarded = true)
            CheckMode.QUESTIONS -> questions(text, source, flags)
            CheckMode.ASK_EACH -> askEach(text, flags, reuse = true)
            CheckMode.ASK_EACH_SEPARATE -> askEach(text, flags, reuse = false)
        }
        return raiseOnStrongSigns(text, result).copy(findings = findings)
    }

    /**
     * A promised investment return or "call this number" about account trouble is never silent:
     * NONE (from the model, or a failed model) becomes CAREFUL, like the two-red-flag floor.
     */
    private fun raiseOnStrongSigns(text: String, result: CheckResult): CheckResult {
        // NONE, or the generic two-red-flag floor: the strong sign gives the more specific reason
        val generic = result.decidedBy == DecidedBy.SAFETY_FLOOR && result.verdict.reasonCode == ReasonCode.WARNING_SIGNS
        if (result.verdict.level != Level.NONE && !generic) return result
        val reason = ConcreteSigns.floorReason(ConcreteSigns.find(text)) ?: return result
        return result.copy(verdict = Verdict(Level.CAREFUL, reasonCode = reason), decidedBy = DecidedBy.SAFETY_FLOOR)
    }

    /**
     * The steps that need no model, in milliseconds: skip filter, OTP rule and hard rules.
     * Null when only the model can decide. CheckQueue runs this as soon as a message arrives,
     * so a hard-rule scam never waits behind model checks in a burst.
     */
    fun checkWithoutModel(text: String): CheckResult? {
        if (SkipFilter.isObviouslyHarmless(text)) return CheckResult(Verdict(Level.NONE), DecidedBy.SKIP_FILTER, 0)
        if (OtpCheck.isRealOtp(text)) return CheckResult(Verdict(Level.OTP), DecidedBy.OTP_RULE, 0)
        // SCAM whatever the model would say: alert at once, without waiting for it
        val rule = HardRules.match(text) ?: return null
        val reason = when (rule) {
            HardRules.Rule.CODE_REQUEST -> ReasonCode.ASKS_FOR_CODE
            HardRules.Rule.LINK_ACCOUNT -> ReasonCode.ACCOUNT_PROBLEM
            HardRules.Rule.MONEY_NEW_NUMBER -> ReasonCode.MONEY_UNDER_PRESSURE
            HardRules.Rule.PRIZE_FEE -> ReasonCode.TOO_GOOD_OFFER
            HardRules.Rule.INVESTMENT_MONEY -> ReasonCode.TOO_GOOD_OFFER
            HardRules.Rule.CALL_OFFICIAL -> ReasonCode.FAKE_OFFICIAL
        }
        return CheckResult(Verdict(Level.SCAM, reasonCode = reason), DecidedBy.HARD_RULE, RedFlags.find(text).size, findings = Findings.find(text))
    }

    /**
     * When the model can't answer in time: the rules alone, with the two-red-flag safety floor,
     * exactly as if the model had failed.
     */
    fun checkRulesOnly(text: String): CheckResult =
        checkWithoutModel(text) ?: raiseOnStrongSigns(text, floorOrNone(RedFlags.find(text))).copy(findings = Findings.find(text))

    private suspend fun classify(
        text: String,
        source: String,
        language: Language,
        flags: List<String>,
        guarded: Boolean,
    ): CheckResult {
        // F never shows the model's words, so it always asks in English: same question in every language
        val promptLanguage = if (guarded) Language.ENGLISH else language
        val raw = ask(PromptBuilder.system(promptLanguage), PromptBuilder.user(text, source))
        val replies = listOfNotNull(raw)
        val verdict = raw?.let(VerdictParser::parse) ?: return floorOrNone(flags, replies = replies)
        if (verdict.level == Level.NONE) return floorOrNone(flags, Verdict(Level.NONE), replies)
        if (!guarded) return CheckResult(verdict, DecidedBy.MODEL, flags.size, replies = replies)

        // F: the model's words are never shown; its SCAM and CAREFUL both need a concrete sign
        val signs = ConcreteSigns.find(text)
        val guardedVerdict = if (verdict.level == Level.SCAM) {
            ConcreteSigns.guardScam(signs).let { Verdict(it.level, reasonCode = it.reason) }
        } else {
            ConcreteSigns.guardCareful(signs).let { Verdict(it.level, reasonCode = it.reason) }
        }
        if (guardedVerdict.level == Level.NONE) return floorOrNone(flags, guardedVerdict, replies)
        return CheckResult(guardedVerdict, DecidedBy.MODEL, flags.size, answers = signs.joinToString(","), replies = replies)
    }

    private suspend fun questions(text: String, source: String, flags: List<String>): CheckResult {
        val prompt = PromptBuilder.user(text, source)
        val replies = mutableListOf<String>()
        suspend fun attempt(p: String): Answers? =
            ask(PromptBuilder.questionsSystem, p, PromptBuilder.QUESTIONS_MAX_OUTPUT_TOKENS)?.also { replies += it }?.let(AnswerParser::parse)
        // One retry with a stricter hint; the sampler is near-deterministic, so the prompt must change
        val answers = attempt(prompt) ?: attempt(prompt + PromptBuilder.QUESTIONS_RETRY_HINT)
            ?: return floorOrNone(flags, replies = replies)
        val decision = QuestionRules.decide(answers, flags)
        return decided(decision, flags, answers.compact(), replies)
    }

    private suspend fun askEach(text: String, flags: List<String>, reuse: Boolean): CheckResult {
        val replies = mutableListOf<String>()
        val outcome = try {
            if (reuse) {
                model.chat(AskEachPrompts.SYSTEM, AskEachPrompts.MAX_OUTPUT_TOKENS) { send ->
                    var first = true
                    AskEachRules.run(flags) { q ->
                        val prompt = if (first) AskEachPrompts.withMessage(text, q) else AskEachPrompts.followUp(q)
                        first = false
                        send(prompt).also { replies += "${q.ordinal + 1}: $it" }.let(YesNoParser::parse)
                    }
                }
            } else {
                AskEachRules.run(flags) { q ->
                    model.generate(AskEachPrompts.SYSTEM, AskEachPrompts.withMessage(text, q), AskEachPrompts.MAX_OUTPUT_TOKENS)
                        .also { replies += "${q.ordinal + 1}: $it" }.let(YesNoParser::parse)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return floorOrNone(flags, replies = replies)
        }
        // Every answer unreadable: the model gave us nothing
        if (outcome.answers.values.all { it == YesNo.UNKNOWN }) return floorOrNone(flags, replies = replies)
        return decided(outcome.decision, flags, AskEachRules.compact(outcome.answers), replies)
    }

    /** Applies the safety floor to a rule decision. */
    private fun decided(decision: QuestionRules.Decision, flags: List<String>, answers: String, replies: List<String>): CheckResult =
        if (decision.level == Level.NONE && flags.size >= FLOOR_FLAGS) {
            CheckResult(FLOOR_VERDICT, DecidedBy.SAFETY_FLOOR, flags.size, answers, replies)
        } else {
            CheckResult(Verdict(decision.level, reasonCode = decision.reason), DecidedBy.MODEL, flags.size, answers, replies)
        }

    /**
     * Model said NONE (pass [none]) or gave nothing usable (null): two or more red flags still
     * raise it to CAREFUL, so no model mistake or "ignore your rules" trick makes it silent.
     */
    private fun floorOrNone(flags: List<String>, none: Verdict? = null, replies: List<String> = emptyList()): CheckResult =
        when {
            flags.size >= FLOOR_FLAGS -> CheckResult(FLOOR_VERDICT, DecidedBy.SAFETY_FLOOR, flags.size, replies = replies)
            none != null -> CheckResult(none, DecidedBy.MODEL, flags.size, replies = replies)
            else -> CheckResult(Verdict(Level.NONE), DecidedBy.MODEL_FAILED, flags.size, replies = replies)
        }

    /** Null when the model is unavailable or fails. */
    private suspend fun ask(
        system: String,
        prompt: String,
        maxOutputTokens: Int = TextGenerator.DEFAULT_MAX_OUTPUT_TOKENS,
    ): String? = try {
        model.generate(system, prompt, maxOutputTokens)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    companion object {
        const val FLOOR_FLAGS = 2
        /** Set after the Oct 4 comparison on the Pixel 8; see docs/PLAN.md. */
        val DEFAULT_MODE = CheckMode.CLASSIFY_GUARDED
        private val FLOOR_VERDICT = Verdict(Level.CAREFUL, reasonCode = ReasonCode.WARNING_SIGNS)
    }
}
