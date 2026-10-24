package com.ajayinnovations.argus.ingest

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.content.ContextCompat

/** Checks permissions and the listener connection, and nudges Android to reconnect it. */
class IngestHealth(private val context: Context) {

    private val listener = ComponentName(context, MessageListenerService::class.java)

    fun smsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    /**
     * Not NotificationManagerCompat.getEnabledListenerPackages: it caches, and keeps the old
     * answer when the setting becomes empty (Argus was the only listener), so a revoked
     * access still looked granted.
     */
    fun notificationAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            context.getSystemService(NotificationManager::class.java)?.isNotificationListenerAccessGranted(listener) == true
        } else {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
                .split(':').mapNotNull(ComponentName::unflattenFromString).any { it == listener }
        }

    /** OEM battery savers often kill the listener without Android rebinding it. Safe to call any time. */
    fun requestRebind() {
        if (!notificationAccess()) return
        runCatching { NotificationListenerService.requestRebind(listener) }
            .onFailure { Log.w(TAG, "requestRebind failed (${it.javaClass.simpleName})") }
    }

    private companion object {
        const val TAG = "IngestHealth"
    }
}
