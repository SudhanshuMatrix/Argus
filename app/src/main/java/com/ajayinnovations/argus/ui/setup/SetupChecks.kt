package com.ajayinnovations.argus.ui.setup

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ajayinnovations.argus.ai.ModelStatus
import com.ajayinnovations.argus.ai.ModelSource
import com.ajayinnovations.argus.core.DeviceCheck
import com.ajayinnovations.argus.core.Maker
import com.ajayinnovations.argus.core.SetupFacts
import com.ajayinnovations.argus.device.DeviceInfo
import com.ajayinnovations.argus.ingest.IngestHealth
import com.ajayinnovations.argus.settings.Settings

/** Reads every permission and phone fact the wizard and the home status rows need. Never throws. */
class SetupChecks(
    private val context: Context,
    private val settings: Settings,
    private val models: ModelSource,
    private val deviceInfo: DeviceInfo,
    private val health: IngestHealth,
) {
    fun facts(): SetupFacts = SetupFacts(
        sdkInt = Build.VERSION.SDK_INT,
        support = DeviceCheck.evaluate(deviceInfo.specs()).support,
        modelReady = safe { models.status() == ModelStatus.READY },
        familyAlerts = settings.familyAlerts,
        receiveSmsGranted = granted(Manifest.permission.RECEIVE_SMS),
        sendSmsGranted = granted(Manifest.permission.SEND_SMS),
        notificationsGranted = notificationsAllowed(),
        accessGranted = safe { health.notificationAccess() },
        sideloaded = sideloaded(),
        fullScreenAllowed = fullScreenAllowed(),
        batteryIgnored = batteryIgnored(),
        overlayAllowed = overlayAllowed(),
        maker = Maker.from(Build.MANUFACTURER.orEmpty(), Build.BRAND.orEmpty()),
        marks = settings.setupMarks,
    )

    fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun notificationsAllowed(): Boolean = safe {
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || granted(Manifest.permission.POST_NOTIFICATIONS))
    }

    fun fullScreenAllowed(): Boolean = safe {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == true
    }

    /** "Display over other apps": the SCAM warning can open over the app in use. */
    fun overlayAllowed(): Boolean = safe { android.provider.Settings.canDrawOverlays(context) }

    fun batteryIgnored(): Boolean = safe {
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    /** Not from the Play Store (adb, a file, Xender…). */
    private fun sideloaded(): Boolean = safe(default = true) {
        val pm = context.packageManager
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }
        installer != PLAY_STORE
    }

    private inline fun safe(default: Boolean = false, block: () -> Boolean): Boolean =
        try {
            block()
        } catch (t: Throwable) {
            default
        }

    private companion object {
        const val PLAY_STORE = "com.android.vending"
    }
}
