package com.ajayinnovations.argus.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CheckQueueTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val results = mutableListOf<Pair<SourceType, Level>>()

    @After
    fun tearDown() = scope.cancel()

    private fun queue(
        model: TextGenerator = FakeModel.answering("SCAM"),
        ready: () -> Boolean = { true },
        language: () -> Language = { Language.ENGLISH },
        capacity: Int = CheckQueue.DEFAULT_CAPACITY,
        timeoutMillis: Long = CheckQueue.CHECK_TIMEOUT_MILLIS,
    ) = CheckQueue(
        checker = ScamChecker(model),
        deduper = Deduper(),
        isReady = ready,
        language = language,
        onChecked = { message, result, _ -> synchronized(results) { results += message.type to result.verdict.level } },
        clock = System::currentTimeMillis,
        scope = scope,
        capacity = capacity,
        checkTimeoutMillis = timeoutMillis,
        onError = { synchronized(errors) { errors += it } },
    )
    private val errors = mutableListOf<String>()

    // No hard rule, so the (fake) model is asked
    private val scam = "Congratulations, you are a winner in our promo. Click bit.ly/xyz123 to claim today please"
    private fun msg(text: String = scam, type: SourceType = SourceType.SMS) = IncomingMessage(type, text, "0801")

    private fun CheckQueue.submitAndWait(vararg messages: IncomingMessage) {
        val latch = CountDownLatch(messages.size)
        messages.forEach { submit(it) { latch.countDown() } }
        assertTrue("timed out", latch.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun `checks a message and reports type and level`() {
        val q = queue()
        q.submitAndWait(msg(type = SourceType.WHATSAPP))
        assertEquals(listOf(SourceType.WHATSAPP to Level.SCAM), results)
        assertEquals(1, q.stats.value.checked)
    }

    @Test
    fun `duplicates are skipped and counted`() {
        val q = queue()
        q.submitAndWait(msg())
        assertFalse(q.submit(msg()))
        assertEquals(1, q.stats.value.duplicates)
        assertEquals(1, results.size)
    }

    @Test
    fun `a burst of 30 different group messages is checked in full, none dropped`() {
        val q = queue(model = FakeModel.answering("NONE"))
        q.submitAndWait(*Array(30) { i -> msg("Group message number $i about the wedding plans on Saturday", SourceType.WHATSAPP) })
        assertEquals(30, q.stats.value.checked)
        assertEquals(0, q.stats.value.dropped)
        assertEquals(30, results.size)
    }

    @Test
    fun `a hard-rule scam in a burst does not wait behind model checks`() {
        val release = CompletableDeferred<Unit>()
        val slow = object : TextGenerator {
            override suspend fun generate(system: String, prompt: String, maxOutputTokens: Int): String {
                release.await()
                return """{"level":"NONE"}"""
            }
        }
        val q = queue(model = slow)
        val latch = CountDownLatch(1)
        repeat(5) { i -> q.submit(msg("Group message number $i about the wedding plans on Saturday", SourceType.WHATSAPP)) }
        q.submit(msg("Dear customer, your BVN is blocked. Click bit.ly/abc12 to update now", SourceType.WHATSAPP)) { latch.countDown() }
        // The model is still stuck on the first message, yet the scam is already reported
        assertTrue("scam waited for the model", latch.await(2, TimeUnit.SECONDS))
        assertEquals(listOf(SourceType.WHATSAPP to Level.SCAM), synchronized(results) { results.toList() })
        release.complete(Unit)
    }

    @Test
    fun `a stray cancellation inside a check never stops the queue`() {
        val model = FakeModel { call -> if (call == 1) throw kotlinx.coroutines.CancellationException("stray") else """{"level":"SCAM"}""" }
        val q = queue(model = model)
        q.submitAndWait(msg("Group message number one about the wedding plans on Saturday"))
        q.submitAndWait(msg())
        assertEquals(1, q.stats.value.failed)
        assertEquals(listOf(SourceType.SMS to Level.SCAM), results)
        assertEquals(listOf("CancellationException"), errors)
    }

    @Test
    fun `a stuck model times out and the rules with the safety floor still decide`() {
        val stuck = object : TextGenerator {
            override suspend fun generate(system: String, prompt: String, maxOutputTokens: Int): String {
                kotlinx.coroutines.awaitCancellation()
            }
        }
        val q = queue(model = stuck, timeoutMillis = 200)
        q.submitAndWait(msg())   // two red flags, no hard rule
        q.submitAndWait(msg("Group message number one about the wedding plans on Saturday"))
        assertEquals(listOf(SourceType.SMS to Level.CAREFUL, SourceType.SMS to Level.NONE), results)
        assertEquals(listOf("CheckTimeout", "CheckTimeout"), errors)
    }

    @Test
    fun `the same message from SMS and WhatsApp is checked once`() {
        val q = queue()
        q.submitAndWait(msg(type = SourceType.SMS))
        assertFalse(q.submit(msg(type = SourceType.WHATSAPP)))
        assertEquals(1, results.size)
    }

    @Test
    fun `nothing is checked until setup has finished`() {
        val model = FakeModel.answering("SCAM")
        val q = queue(model = model, ready = { false })
        q.submitAndWait(msg())
        assertEquals(0, model.calls)
        assertEquals(1, q.stats.value.notReady)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `obviously harmless messages count as skipped`() {
        val q = queue()
        q.submitAndWait(msg("Amen 🙏"))
        assertEquals(1, q.stats.value.skippedByFilter)
        assertEquals(1, q.stats.value.skipped)
    }

    @Test
    fun `a failure is counted and the queue keeps going`() {
        val calls = AtomicInteger()
        val q = queue(language = { if (calls.incrementAndGet() == 1) error("boom") else Language.ENGLISH })
        q.submitAndWait(msg(), msg("$scam again"))
        assertEquals(1, q.stats.value.failed)
        assertEquals(1, q.stats.value.checked)
    }

    @Test
    fun `a failing model never crashes the queue`() {
        val q = queue(model = FakeModel.failing())
        q.submitAndWait(msg())
        assertEquals(1, q.stats.value.checked)   // the safety floor decided
        assertEquals(Level.CAREFUL, results.single().second)
    }

    @Test
    fun `when full the oldest waiting message is dropped`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val slow = object : TextGenerator {
            override suspend fun generate(system: String, prompt: String, maxOutputTokens: Int): String {
                started.complete(Unit)
                gate.await()
                return """{"level":"NONE"}"""
            }
        }
        val q = queue(model = slow, capacity = 2)
        val done = AtomicInteger()
        q.submit(msg("$scam 0")) { done.incrementAndGet() }
        withTimeout(5_000) { started.await() }     // the consumer is busy with message 0
        (1..4).forEach { q.submit(msg("$scam $it")) { done.incrementAndGet() } }  // 1 and 2 get pushed out
        assertEquals(2, q.stats.value.dropped)
        gate.complete(Unit)
        withTimeout(5_000) { while (done.get() < 5) kotlinx.coroutines.delay(10) }
        assertEquals(3, q.stats.value.checked)
    }

    @Test
    fun `a throwing callback never stops the queue`() {
        val q = queue()
        val latch = CountDownLatch(1)
        q.submit(msg()) { error("bad callback") }
        q.submit(msg("$scam two")) { latch.countDown() }
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals(2, q.stats.value.checked)
    }

    @Test
    fun `counts levels and remembers the last check for the debug screen`() {
        var now = 0L
        val q = CheckQueue(
            checker = ScamChecker(FakeModel.answering("SCAM")),
            deduper = Deduper(),
            isReady = { true },
            language = { Language.ENGLISH },
            onChecked = { _, _, _ -> },
            clock = { now.also { now += 1_500 } },
            scope = scope,
        )
        // One after the other: rule-decided messages skip the queue, so finishing order isn't submission order
        q.submitAndWait(msg())
        q.submitAndWait(msg("Amen 🙏", SourceType.WHATSAPP))
        val stats = q.stats.value
        assertEquals(mapOf(Level.SCAM to 1, Level.NONE to 1), stats.levels)
        assertEquals(LastCheck(SourceType.WHATSAPP, Level.NONE, DecidedBy.SKIP_FILTER, 1_500), stats.last)
    }

    @Test
    fun `the prompt names the source but logs never get the sender`() {
        assertEquals("WhatsApp from Mama Chidi", IncomingMessage(SourceType.WHATSAPP, "hi", "Mama Chidi").sourceLabel)
    }
}
