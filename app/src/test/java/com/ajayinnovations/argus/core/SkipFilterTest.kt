package com.ajayinnovations.argus.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipFilterTest {

    @Test
    fun `very short messages are skipped`() {
        listOf("ok", "Amen", "Thanks", " Yes ma ").forEach { assertTrue(it, SkipFilter.isObviouslyHarmless(it)) }
    }

    @Test
    fun `emoji and symbol only messages are skipped`() {
        listOf("🙏🙏🙏🙏🙏", "😂😂😂😂😂😂😂😂", "!!!???....", "❤️ 👍 🎉 🙌").forEach {
            assertTrue(it, SkipFilter.isObviouslyHarmless(it))
        }
    }

    @Test
    fun `short chat with no digits or flags is skipped`() {
        assertTrue(SkipFilter.isObviouslyHarmless("Good night, sleep well"))
    }

    @Test
    fun `short message with a digit goes to the model`() {
        assertFalse(SkipFilter.isObviouslyHarmless("Send 5000 now please"))
    }

    @Test
    fun `short message with a red flag goes to the model`() {
        assertFalse(SkipFilter.isObviouslyHarmless("Click here to verify"))
    }

    @Test
    fun `long messages go to the model`() {
        assertFalse(SkipFilter.isObviouslyHarmless("Good morning ma, I hope you slept well and the children are fine too"))
    }
}
