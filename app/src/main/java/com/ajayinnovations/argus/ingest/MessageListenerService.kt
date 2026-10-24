package com.ajayinnovations.argus.ingest

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ajayinnovations.argus.BuildConfig
import com.ajayinnovations.argus.core.ChatCursor
import com.ajayinnovations.argus.core.CheckQueue
import com.ajayinnovations.argus.core.IncomingMessage
import com.ajayinnovations.argus.core.SourceType
import com.ajayinnovations.argus.core.Warmup
import org.koin.android.ext.android.inject

/**
 * Reads WhatsApp and WhatsApp Business notifications. SMS apps are ignored: SmsReceiver gets
 * every text directly. Android keeps this bound once notification access is granted.
 */
class MessageListenerService : NotificationListenerService() {

    private val queue: CheckQueue by inject()
    private val warmup: Warmup by inject()
    private val cursor: ChatCursor by inject()

    override fun onListenerConnected() {
        ListenerState.set(true)
        // After boot or a restart: load the model in ~2 minutes, once the phone has settled
        warmup.schedule()
        Log.i(TAG, "Listener connected; warm-up scheduled")
    }

    override fun onListenerDisconnected() {
        ListenerState.set(false)
        warmup.cancel()
        Log.i(TAG, "Listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Never let one odd notification take the service down
        try {
            if (sbn.packageName !in WATCHED) return
            val messages = NotificationText.extractAll(sbn.notification)
            // Every message new since the last update of this chat: in a burst Android drops updates
            cursor.newMessages(sbn.key, messages.map { it.timestamp }, sbn.postTime).forEach { i ->
                queue.submit(IncomingMessage(SourceType.WHATSAPP, messages[i].text, messages[i].sender, chatKey = sbn.key))
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not read a notification (${t.javaClass.simpleName})")
        }
    }

    private companion object {
        const val TAG = "MessageListener"
        /** dev builds also watch the adb shell, so `cmd notification post` can test this path. */
        val WATCHED = setOf("com.whatsapp", "com.whatsapp.w4b") + BuildConfig.EXTRA_WATCHED_PACKAGES.split(',').filter { it.isNotBlank() }
    }
}
