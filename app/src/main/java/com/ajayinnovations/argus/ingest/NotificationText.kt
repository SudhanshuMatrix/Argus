package com.ajayinnovations.argus.ingest

import android.app.Notification
import androidx.core.app.NotificationCompat
import com.ajayinnovations.argus.core.IncomingMessage

/** Pulls the message texts and their senders out of a chat notification. */
object NotificationText {

    /** [timestamp] is 0 when the notification doesn't say. */
    data class Extracted(val text: String, val sender: String, val timestamp: Long = 0)

    /**
     * MessagingStyle: every message it holds, oldest first (ChatCursor picks the new ones).
     * Otherwise big text, then text, as one message. Empty for summaries or empty ones.
     */
    fun extractAll(notification: Notification): List<Extracted> {
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return emptyList()
        val extras = notification.extras ?: return emptyList()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

        val style = runCatching { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification) }.getOrNull()
        val fromStyle = style?.messages.orEmpty().mapNotNull { message ->
            val text = message.text?.toString()
            if (text.isNullOrBlank()) return@mapNotNull null
            val sender = message.person?.name?.toString() ?: style?.conversationTitle?.toString() ?: title
            Extracted(text, sender ?: UNKNOWN_SENDER, message.timestamp)
        }
        if (fromStyle.isNotEmpty()) return fromStyle

        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (text.isNullOrBlank()) return emptyList()
        return listOf(Extracted(text, title ?: UNKNOWN_SENDER))
    }

    const val UNKNOWN_SENDER = IncomingMessage.UNKNOWN_SENDER
}
