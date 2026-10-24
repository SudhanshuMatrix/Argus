package com.ajayinnovations.argus.alert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.core.Alert
import com.ajayinnovations.argus.core.AlertChannel
import com.ajayinnovations.argus.core.AlertPlan
import com.ajayinnovations.argus.core.CarefulVoiceLimit
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.core.SoundPolicy
import com.ajayinnovations.argus.settings.Settings
import com.ajayinnovations.argus.settings.localized

/**
 * Turns a verdict into an alert, following [AlertPlan]: SCAM is loud with a full-screen red
 * warning, CAREFUL and OTP are quiet, NONE does nothing. The private notification shows the
 * fixed headline, the sender and the fixed reason; the lock-screen public version is generic.
 * The message preview appears only inside WarningActivity. Each step fails on its own, so a broken
 * notification still vibrates and speaks. Never throws.
 */
class AlertNotifier(
    private val context: Context,
    private val settings: Settings,
    private val store: VerdictStore,
    private val voice: VoicePlayer,
    private val family: FamilyNotifier,
    private val sound: PhoneSound,
    private val voiceLimit: CarefulVoiceLimit,
) {
    /** Returns the alert ID, or null for NONE. */
    fun show(alert: Alert): Int? {
        val verdict = alert.verdict
        val plan = AlertPlan.forLevel(verdict.level) ?: return null
        val id = store.put(alert)
        val posted = step("notification") { post(id, alert, plan, seen = false) } ?: false
        // "Display over other apps" allowed: the red warning also opens over the app in use.
        // Without it, Android shows the full screen only on the lock screen, as before.
        if (plan.fullScreen) step("over other apps") { if (canDrawOverlays()) context.startActivity(WarningActivity.intent(context, id, verdict.level)) }
        step("vibration") { if (SoundPolicy.vibrateDirectly(plan, sound.ringer(), posted)) sound.vibrateAlarm() }
        step("voice") {
            // A busy group gets one spoken "be careful" per 10 minutes; the notifications still post
            if (voiceLimit.allow(verdict.level, alert.chatId, android.os.SystemClock.elapsedRealtime())) voice.play(verdict.level)
        }
        if (plan.familySms) step("family SMS") { family.notify(verdict.level) }
        if (plan.fullScreen) main.postDelayed({ step("voice repeat") { repeatVoice(id) } }, VOICE_REPEAT_MILLIS)
        return id
    }

    /** Still not "I understand" after a while, and still the newest warning: say it once more. */
    private fun repeatVoice(id: Int) {
        if (store.pendingScams().lastOrNull() == id) voice.play(Level.SCAM)
    }

    /**
     * The red screen has shown this alert: its notification may now be swiped away. Re-posted
     * quietly without the full-screen part, and only if it is still there (tapping it removes it).
     */
    fun markSeen(id: Int) {
        step("seen") {
            val alert = store.get(id) ?: return@step
            val plan = AlertPlan.forLevel(alert.verdict.level)?.takeIf { it.fullScreen } ?: return@step
            val showing = context.getSystemService(NotificationManager::class.java)?.activeNotifications?.any { it.id == id } == true
            if (showing) post(id, alert, plan, seen = true)
        }
    }

    /** "I understand": no more repeats for it, and its notification goes. */
    fun acknowledge(id: Int) {
        store.acknowledge(id)
        cancel(id)
    }

    fun cancel(id: Int) {
        step("cancel") { NotificationManagerCompat.from(context).cancel(id) }
    }

    private fun canDrawOverlays(): Boolean = android.provider.Settings.canDrawOverlays(context)

    /** Android 14+ may refuse full-screen alerts; setup will send them to the settings page. */
    fun canUseFullScreen(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == true
        } else {
            true
        }

    private fun post(id: Int, alert: Alert, plan: AlertPlan, seen: Boolean): Boolean {
        val level = alert.verdict.level
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled() || !hasPostPermission()) {
            Log.w(TAG, "Notifications are off; level=$level not posted")
            return false
        }
        // Built in the parent's language: services don't see AppCompat's per-app locale on Android 12-
        val text = context.localized(settings.language)
        createChannels(text)

        val open = PendingIntent.getActivity(
            context, id,
            WarningActivity.intent(context, id, level),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val channel = channelId(plan.channel)
        val headline = AlertText.headline(text, level, settings.parentTitle, settings.parentName)
        val body = AlertText.body(text, level, settings.familyName)
        val from = AlertText.from(text, alert.source, alert.sender)
        val reason = AlertText.reason(text, alert.findings, alert.verdict.reasonCode, alert.verdict.reason)
        // Sender and reason; the fixed level advice when there is no reason (OTP)
        val details = listOfNotNull(from, reason ?: body).joinToString("\n")

        // Lock screen: no level details, just "unlock to see it"
        val public = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text.getString(R.string.alert_public_title))
            .setContentText(text.getString(R.string.alert_public_body))
            .build()

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(headline)
            .setContentText(details.substringBefore('\n').ifEmpty { body })
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)

        if (plan.fullScreen && seen) {
            // Seen on the red screen: same text, now swipeable, and no new sound or full screen
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOnlyAlertOnce(true)
                .setSilent(true)
        } else if (plan.fullScreen) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(PhoneSound.PATTERN)
                .setFullScreenIntent(open, true)   // the red warning, also over the lock screen
                .setOngoing(true)                  // can't be swiped away until she has seen it
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
        }

        return try {
            manager.notify(id, builder.build())
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission revoked")
            false
        }
    }

    /** Re-created every time so the names follow a language change; settings the user changed are kept. */
    private fun createChannels(text: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_SCAM, text.getString(R.string.channel_scam), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = PhoneSound.PATTERN
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_CAREFUL, text.getString(R.string.channel_careful), NotificationManager.IMPORTANCE_DEFAULT).apply {
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )
    }

    private fun hasPostPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun channelId(channel: AlertChannel) = when (channel) {
        AlertChannel.SCAM -> CH_SCAM
        AlertChannel.CAREFUL -> CH_CAREFUL
    }

    private inline fun <T> step(name: String, block: () -> T): T? = try {
        block()
    } catch (t: Throwable) {
        Log.w(TAG, "Alert $name failed (${t.javaClass.simpleName})")
        null
    }

    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    companion object {
        /** One repeat of the SCAM voice if "I understand" hasn't been tapped by then. */
        const val VOICE_REPEAT_MILLIS = 2 * 60 * 1000L
        private const val TAG = "AlertNotifier"
        const val CH_SCAM = "scam"
        const val CH_CAREFUL = "careful"
    }
}
