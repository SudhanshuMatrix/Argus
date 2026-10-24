package com.ajayinnovations.argus.ingest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import com.ajayinnovations.argus.core.CheckQueue
import com.ajayinnovations.argus.core.IncomingMessage
import com.ajayinnovations.argus.core.SmsParts
import com.ajayinnovations.argus.core.SourceType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Every incoming SMS, delivered by Android even when the app is closed. Long texts arrive in
 * parts; they are joined per sender before checking. goAsync keeps the process alive while
 * the check runs, up to a safety limit.
 */
class SmsReceiver : BroadcastReceiver(), KoinComponent {

    private val queue: CheckQueue by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        val finished = AtomicBoolean(false)
        val finish = { if (finished.compareAndSet(false, true)) pending.finish() }
        // Background broadcasts may run up to about a minute; never hold on longer than this
        Handler(Looper.getMainLooper()).postDelayed({ finish() }, MAX_HOLD_MILLIS)
        try {
            val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent).orEmpty().filterNotNull()
                .map { SmsParts.Part(it.originatingAddress, it.messageBody) }
            val messages = SmsParts.join(parts, IncomingMessage.UNKNOWN_SENDER)
            if (messages.isEmpty()) return finish()
            val remaining = AtomicInteger(messages.size)
            messages.forEach { (sender, text) ->
                queue.submit(IncomingMessage(SourceType.SMS, text, sender)) {
                    if (remaining.decrementAndGet() == 0) finish()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not read an SMS (${t.javaClass.simpleName})")
            finish()
        }
    }

    private companion object {
        const val TAG = "SmsReceiver"
        const val MAX_HOLD_MILLIS = 50_000L
    }
}
