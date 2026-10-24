package com.ajayinnovations.argus.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/** Real time with short delays (no coroutines-test dependency). */
class WarmupTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val model = FakeModel.answers("OK")

    @After
    fun tearDown() = scope.cancel()

    private fun warmup(ready: Boolean = true, loaded: Boolean = false, model: TextGenerator = this.model) =
        Warmup(scope, { ready }, { loaded }, model, delayMillis = 300)

    @Test
    fun `runs one tiny check after the delay, not before`() {
        warmup().schedule()
        Thread.sleep(150)
        assertEquals(0, model.calls)
        Thread.sleep(500)
        assertEquals(1, model.calls)
    }

    @Test
    fun `a reconnect restarts the countdown and warms once`() {
        val w = warmup()
        w.schedule()
        Thread.sleep(200)
        w.schedule()
        Thread.sleep(200)
        assertEquals(0, model.calls)
        Thread.sleep(500)
        assertEquals(1, model.calls)
    }

    @Test
    fun `skips before setup or when already loaded`() {
        warmup(ready = false).schedule()
        warmup(loaded = true).schedule()
        Thread.sleep(700)
        assertEquals(0, model.calls)
    }

    @Test
    fun `cancel stops it`() {
        val w = warmup()
        w.schedule()
        w.cancel()
        Thread.sleep(600)
        assertEquals(0, model.calls)
    }

    @Test
    fun `a failing model is harmless`() {
        warmup(model = FakeModel.failing()).schedule()
        Thread.sleep(600)
    }
}
