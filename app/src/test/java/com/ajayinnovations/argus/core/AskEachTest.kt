package com.ajayinnovations.argus.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AskEachTest {

    // YesNoParser

    @Test
    fun `replies that start with yes or no`() {
        assertEquals(YesNo.YES, YesNoParser.parse("Yes"))
        assertEquals(YesNo.YES, YesNoParser.parse("yes, it asks for a PIN."))
        assertEquals(YesNo.NO, YesNoParser.parse("  NO."))
        assertEquals(YesNo.NO, YesNoParser.parse("**No**"))
        assertEquals(YesNo.YES, YesNoParser.parse("\"Yes\""))
    }

    @Test
    fun `anything else is unknown`() {
        listOf("", "Maybe", "The answer is yes", "Nope", "Yesterday", "Y", "N").forEach {
            assertEquals(it, YesNo.UNKNOWN, YesNoParser.parse(it))
        }
    }

    // AskEachRules: answers given per question; unasked questions must not be asked

    private fun run(answers: Map<Question, YesNo>, flags: List<String> = emptyList()): Pair<AskEachRules.Outcome, List<Question>> {
        val asked = mutableListOf<Question>()
        val outcome = runBlocking {
            AskEachRules.run(flags) { q -> asked += q; answers[q] ?: YesNo.NO }
        }
        return outcome to asked
    }

    @Test
    fun `code request is SCAM after one question`() {
        val (o, asked) = run(mapOf(Question.CODE to YesNo.YES))
        assertEquals(QuestionRules.Decision(Level.SCAM, ReasonCode.ASKS_FOR_CODE), o.decision)
        assertEquals(listOf(Question.CODE), asked)
    }

    @Test
    fun `a normal message needs only two questions`() {
        val (o, asked) = run(emptyMap())
        assertEquals(QuestionRules.Decision(Level.NONE, null), o.decision)
        assertEquals(listOf(Question.CODE, Question.MONEY), asked)
    }

    @Test
    fun `account or offer plus money, link or phone is SCAM`() {
        assertEquals(ReasonCode.ACCOUNT_OR_OFFER, run(mapOf(Question.MONEY to YesNo.YES, Question.ACCOUNT_OR_OFFER to YesNo.YES)).first.decision.reason)
        assertEquals(ReasonCode.ACCOUNT_OR_OFFER, run(mapOf(Question.ACCOUNT_OR_OFFER to YesNo.YES), listOf("link")).first.decision.reason)
        assertEquals(ReasonCode.ACCOUNT_OR_OFFER, run(mapOf(Question.ACCOUNT_OR_OFFER to YesNo.YES), listOf("phone number")).first.decision.reason)
    }

    @Test
    fun `account or offer is not asked when nothing is requested`() {
        val (o, asked) = run(mapOf(Question.ACCOUNT_OR_OFFER to YesNo.YES))
        assertEquals(Level.NONE, o.decision.level)
        assertEquals(listOf(Question.CODE, Question.MONEY), asked)
    }

    @Test
    fun `official plus money or link is SCAM`() {
        assertEquals(ReasonCode.FAKE_OFFICIAL, run(mapOf(Question.MONEY to YesNo.YES, Question.OFFICIAL to YesNo.YES)).first.decision.reason)
        assertEquals(ReasonCode.FAKE_OFFICIAL, run(mapOf(Question.OFFICIAL to YesNo.YES), listOf("link")).first.decision.reason)
    }

    @Test
    fun `money plus pressure or new number is SCAM`() {
        val (o, asked) = run(mapOf(Question.MONEY to YesNo.YES, Question.PRESSURE to YesNo.YES))
        assertEquals(QuestionRules.Decision(Level.SCAM, ReasonCode.MONEY_UNDER_PRESSURE), o.decision)
        // Asked in rule order: code, money, account/offer, official, pressure
        assertEquals(listOf(Question.CODE, Question.MONEY, Question.ACCOUNT_OR_OFFER, Question.OFFICIAL, Question.PRESSURE), asked)
    }

    @Test
    fun `money or link alone is CAREFUL`() {
        assertEquals(QuestionRules.Decision(Level.CAREFUL, ReasonCode.ASKS_FOR_MONEY_OR_LINK), run(mapOf(Question.MONEY to YesNo.YES)).first.decision)
        assertEquals(ReasonCode.ASKS_FOR_MONEY_OR_LINK, run(emptyMap(), listOf("link")).first.decision.reason)
    }

    @Test
    fun `pressure plus a phone number is CAREFUL`() {
        assertEquals(QuestionRules.Decision(Level.CAREFUL, ReasonCode.PRESSURE), run(mapOf(Question.PRESSURE to YesNo.YES), listOf("phone number")).first.decision)
    }

    @Test
    fun `unknown counts as not yes`() {
        assertEquals(Level.NONE, run(mapOf(Question.CODE to YesNo.UNKNOWN, Question.MONEY to YesNo.UNKNOWN)).first.decision.level)
    }

    @Test
    fun `compact answers in asking order`() {
        assertEquals("1N 2Y 4N 3N 5Y", AskEachRules.compact(run(mapOf(Question.MONEY to YesNo.YES, Question.PRESSURE to YesNo.YES)).first.answers))
    }
}
