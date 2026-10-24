package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatCursorTest {

    private val now = 1_000_000_000L

    @Test
    fun `first sight of a chat takes the recent messages, not old ones`() {
        val cursor = ChatCursor()
        val times = listOf(now - 3_600_000, now - 2_000, now - 1_000, now)
        assertEquals(listOf(1, 2, 3), cursor.newMessages("chat", times, postedAt = now))
    }

    @Test
    fun `first sight with only old messages still takes the newest`() {
        val cursor = ChatCursor()
        assertEquals(listOf(1), cursor.newMessages("chat", listOf(now - 7_200_000, now - 3_600_000), postedAt = now))
    }

    @Test
    fun `later updates take every message newer than the last one seen`() {
        val cursor = ChatCursor()
        cursor.newMessages("chat", listOf(now), postedAt = now)
        // Android dropped the updates for +1s and +2s: all three are new
        val times = listOf(now, now + 1_000, now + 2_000, now + 3_000)
        assertEquals(listOf(1, 2, 3), cursor.newMessages("chat", times, postedAt = now + 3_000))
    }

    @Test
    fun `a re-post with nothing new takes nothing`() {
        val cursor = ChatCursor()
        val times = listOf(now - 1_000, now)
        cursor.newMessages("chat", times, postedAt = now)
        assertEquals(emptyList<Int>(), cursor.newMessages("chat", times, postedAt = now + 500))
    }

    @Test
    fun `chats are tracked separately`() {
        val cursor = ChatCursor()
        cursor.newMessages("a", listOf(now), postedAt = now)
        assertEquals(listOf(0), cursor.newMessages("b", listOf(now), postedAt = now))
    }

    @Test
    fun `messages without a time take only the newest`() {
        assertEquals(listOf(2), ChatCursor().newMessages("chat", listOf(0, 0, 0), postedAt = now))
        assertEquals(emptyList<Int>(), ChatCursor().newMessages("chat", emptyList(), postedAt = now))
    }

    @Test
    fun `a burst of 30 group messages reaches the queue once each`() {
        val cursor = ChatCursor()
        val all = (0 until 30).map { now + it * 100L }
        val picked = mutableListOf<Long>()
        // Android delivered only every fifth update; each holds the last 25 messages (MessagingStyle's limit)
        for (shown in 4 until 30 step 5) {
            val held = all.subList(maxOf(0, shown - 24), shown + 1)
            cursor.newMessages("group", held, postedAt = all[shown]).forEach { picked += held[it] }
        }
        assertEquals(all, picked)
    }

    @Test
    fun `old chats are forgotten after the capacity`() {
        val cursor = ChatCursor(capacity = 2)
        cursor.newMessages("a", listOf(now), postedAt = now)
        cursor.newMessages("b", listOf(now), postedAt = now)
        cursor.newMessages("c", listOf(now), postedAt = now)
        // "a" is new again: treated as first sight, so the recent message is taken
        assertEquals(listOf(0), cursor.newMessages("a", listOf(now), postedAt = now))
    }
}
