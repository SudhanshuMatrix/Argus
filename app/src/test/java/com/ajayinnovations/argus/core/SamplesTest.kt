package com.ajayinnovations.argus.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The 20 synthetic samples through the rules, with the model stubbed out. */
class SamplesTest {

    @Test
    fun `there are 10 scams and 10 normal messages`() {
        assertEquals(10, Samples.scams.size)
        assertEquals(10, Samples.normals.size)
    }

    @Test
    fun `no scam is ever skipped or treated as a real OTP`() {
        Samples.scams.forEach {
            assertFalse("${it.name} skipped", SkipFilter.isObviouslyHarmless(it.text))
            assertFalse("${it.name} taken as real OTP", OtpCheck.isRealOtp(it.text))
        }
    }

    @Test
    fun `no normal message becomes SCAM or CAREFUL through rules alone`() = runBlocking {
        // The model says NONE / answers all N; only the rules (skip, OTP, RedFlags, floor) could raise it
        val modes = mapOf(
            CheckMode.CLASSIFY to FakeModel.answering("NONE"),
            CheckMode.CLASSIFY_GUARDED to FakeModel.answering("NONE"),
            CheckMode.QUESTIONS to FakeModel.answers("1N 2N 3N 4N 5N 6N 7N"),
            CheckMode.ASK_EACH to FakeModel.answers("No"),
            CheckMode.ASK_EACH_SEPARATE to FakeModel.answers("No"),
        )
        modes.forEach { (mode, model) ->
            val checker = ScamChecker(model)
            Samples.normals.forEach {
                val level = checker.check(it.text, "SMS", Language.ENGLISH, mode).verdict.level
                assertNotEquals("$mode: ${it.name} became SCAM", Level.SCAM, level)
                assertNotEquals("$mode: ${it.name} became CAREFUL", Level.CAREFUL, level)
            }
        }
    }

    @Test
    fun `no normal message becomes SCAM even when the model fails`() = runBlocking {
        val checker = ScamChecker(FakeModel.failing())
        CheckMode.entries.forEach { mode ->
            Samples.normals.forEach {
                assertNotEquals("$mode: ${it.name}", Level.SCAM, checker.check(it.text, "SMS", Language.ENGLISH, mode).verdict.level)
            }
        }
    }

    @Test
    fun `the real OTP sample gives OTP`() {
        val otp = Samples.normals.single { it.name == "04_real_otp.txt" }
        assertTrue(OtpCheck.isRealOtp(otp.text))
    }

    @Test
    fun `most scams are still flagged if the model fails`() = runBlocking {
        val checker = ScamChecker(FakeModel.failing())
        val flagged = Samples.scams.filter {
            checker.check(it.text, "SMS", Language.ENGLISH).verdict.level != Level.NONE
        }
        // Hard rules make some SCAM, the safety floor makes the rest CAREFUL. Only 07 (relative,
        // under two red flags and no hard rule) needs the model.
        assertTrue("only ${flagged.size}/10 flagged", flagged.size >= 9)
    }

    @Test
    fun `mode F guard keeps every scam sample as SCAM when the model says SCAM`() = runBlocking {
        val checker = ScamChecker(FakeModel.answering("SCAM"))
        Samples.scams.forEach {
            assertEquals(it.name, Level.SCAM, checker.check(it.text, "SMS", Language.ENGLISH, CheckMode.CLASSIFY_GUARDED).verdict.level)
        }
    }

    @Test
    fun `mode F guard turns no normal sample into a loud SCAM`() = runBlocking {
        val checker = ScamChecker(FakeModel.answering("SCAM"))
        Samples.normals.forEach {
            assertNotEquals(it.name, Level.SCAM, checker.check(it.text, "SMS", Language.ENGLISH, CheckMode.CLASSIFY_GUARDED).verdict.level)
        }
    }
}
