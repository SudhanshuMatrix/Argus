package com.ajayinnovations.argus

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajayinnovations.argus.ingest.MessageListenerService
import com.ajayinnovations.argus.ui.about.AboutScreen
import com.ajayinnovations.argus.ui.components.BottomNav
import com.ajayinnovations.argus.ui.components.Tab
import com.ajayinnovations.argus.ui.theme.Mist
import com.ajayinnovations.argus.ui.check.CheckRoute
import com.ajayinnovations.argus.ui.check.CheckViewModel
import com.ajayinnovations.argus.ui.home.HomeRoute
import com.ajayinnovations.argus.ui.home.HomeViewModel
import com.ajayinnovations.argus.ui.home.StatusKind
import com.ajayinnovations.argus.ui.settings.SettingsRoute
import com.ajayinnovations.argus.ui.settings.SettingsViewModel
import com.ajayinnovations.argus.ui.setup.SetupActions
import com.ajayinnovations.argus.ui.setup.SetupRoute
import com.ajayinnovations.argus.ui.setup.SetupViewModel
import com.ajayinnovations.argus.ui.theme.ArgusTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * The setup wizard until it is finished, then the home screen (status, check, settings).
 * Every Android permission request and settings jump lives here; the screens re-check on resume.
 * AppCompatActivity, not ComponentActivity: AppCompatDelegate.setApplicationLocales needs it.
 */
@OptIn(ExperimentalLayoutApi::class)
class MainActivity : AppCompatActivity() {

    private val setup: SetupViewModel by viewModel()
    private val home: HomeViewModel by viewModel()
    private val check: CheckViewModel by viewModel()
    private val settingsVm: SettingsViewModel by viewModel()
    private val appSettings: com.ajayinnovations.argus.settings.Settings by inject()

    private val setupDone: Boolean get() = setup.state.value.finished

    /** Kept outside Compose so share and clipboard checks can open the Check tab. */
    private val screen = mutableStateOf(Tab.HOME)

    private val smsPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        // Denied twice, Android stops asking: send them to the app's settings page instead
        val blocked = result.filterValues { !it }.keys.any { !shouldShowRequestPermissionRationale(it) }
        if (blocked) openAppInfo()
        refresh()
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            openNotificationSettings()
        }
        refresh()
    }

    private val actions = SetupActions(
        requestSms = ::requestSms,
        requestNotifications = ::requestNotifications,
        openNotificationAccess = ::openListenerSettings,
        openAppInfo = ::openAppInfo,
        openFullScreenSettings = ::openFullScreenSettings,
        openOverlaySettings = ::openOverlaySettings,
        requestBatteryExemption = ::requestBatteryExemption,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString(KEY_SCREEN)?.let { name -> Tab.entries.firstOrNull { it.name == name } }?.let { screen.value = it }
        if (screen.value == Tab.SETTINGS) settingsVm.loadIfNeeded()
        // Not after a recreate (rotation, language change): that share was already checked
        if (savedInstanceState == null) handleShare(intent)
        val finishedAtStart = setup.state.value.finished
        setContent {
            ArgusTheme {
                val setupState by setup.state.collectAsStateWithLifecycle()
                val current by screen
                // The one motion moment: the status block settles in when setup has just finished
                val settleIn = !finishedAtStart && setupState.finished
                val onHome = setupState.finished && current == Tab.HOME
                LaunchedEffect(onHome) { styleSystemBars(lightIcons = onHome) }
                if (setupState.finished && current != Tab.HOME) BackHandler { show(Tab.HOME) }
                if (!setupState.finished) {
                    SetupRoute(setup, actions)
                } else {
                    Column(Modifier.fillMaxSize().background(Mist).imePadding()) {
                        Box(Modifier.weight(1f)) {
                            when (current) {
                                Tab.CHECK -> CheckRoute(check)
                                Tab.SETTINGS -> SettingsRoute(
                                    settingsVm,
                                    onSaved = { familyAlerts -> if (familyAlerts) requestSms(includeSend = true) },
                                )
                                Tab.ABOUT -> AboutScreen()
                                Tab.HOME -> HomeRoute(home, settleIn = settleIn, onFix = ::fix, onCheck = { show(Tab.CHECK) })
                            }
                        }
                        // Out of the way while typing in the check box or the settings fields
                        if (!WindowInsets.isImeVisible) BottomNav(current, onSelect = ::show)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SCREEN, screen.value.name)
    }

    private fun show(target: Tab) {
        if (target == Tab.SETTINGS && screen.value != Tab.SETTINGS) settingsVm.load()
        screen.value = target
        refresh()
    }

    /** Light clock icons over the home status block; dark icons on Mist everywhere else. */
    private fun styleSystemBars(lightIcons: Boolean) {
        val transparent = android.graphics.Color.TRANSPARENT
        val status = if (lightIcons) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        enableEdgeToEdge(statusBarStyle = status, navigationBarStyle = SystemBarStyle.light(transparent, transparent))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    override fun onResume() {
        super.onResume()
        if (setupDone) {
            home.onResume()   // also asks Android to rebind the listener
            check.onResume()
        } else {
            setup.onResume()
        }
    }

    private fun refresh() {
        if (setupDone) home.refresh() else setup.refresh()
    }

    /** Share to check: long-press any text, Share, Argus. */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = try {
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        } catch (t: Throwable) {
            null   // a malformed extra from another app
        }
        if (!text.isNullOrBlank()) {
            check.onShared(text)
            screen.value = Tab.CHECK
        }
    }

    // Android 10+ only lets the app read the clipboard while it has focus
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || !setupDone) return
        val clip = readClipboard() ?: return
        if (check.onClipboard(clip)) screen.value = Tab.CHECK
    }

    private fun readClipboard(): String? = try {
        val clip = getSystemService(ClipboardManager::class.java)?.primaryClip
        val sensitive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            clip?.description?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true
        // Passwords copied from a password manager are none of our business
        if (clip == null || clip.itemCount == 0 || sensitive) null
        else clip.getItemAt(0)?.coerceToText(this)?.toString()?.takeIf { it.isNotBlank() }
    } catch (t: Throwable) {
        null
    }

    /** A home status row's Fix button: the same action as its wizard step. */
    private fun fix(kind: StatusKind) {
        when (kind) {
            StatusKind.MODEL -> setup.repairModel()
            StatusKind.SMS -> requestSms(includeSend = settingsFamilyAlerts())
            StatusKind.NOTIFICATIONS -> requestNotifications()
            StatusKind.WHATSAPP -> {
                home.onFixAttempted()
                openListenerSettings()
            }
            StatusKind.FULL_SCREEN -> openFullScreenSettings()
            StatusKind.OVERLAY -> openOverlaySettings()
            StatusKind.BATTERY -> requestBatteryExemption()
        }
    }

    private fun settingsFamilyAlerts(): Boolean = appSettings.familyAlerts

    private fun requestSms(includeSend: Boolean) {
        val wanted = listOfNotNull(Manifest.permission.RECEIVE_SMS, if (includeSend) Manifest.permission.SEND_SMS else null)
        val missing = wanted.filter { ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) refresh() else smsPermissions.launch(missing.toTypedArray())
    }

    private fun requestNotifications() {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        // Permission given but notifications switched off for the app: only its settings page can fix that
        if (granted) openNotificationSettings() else notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openListenerSettings() {
        val component = ComponentName(this, MessageListenerService::class.java)
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        } else {
            null
        }
        open(detail, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun openAppInfo() =
        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)), null)

    private fun openNotificationSettings() =
        open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName), appInfoIntent())

    private fun openFullScreenSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.fromParts("package", packageName, null))
        } else {
            null
        }
        open(intent, appInfoIntent())
    }

    private fun openOverlaySettings() =
        open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", packageName, null)), appInfoIntent())

    @android.annotation.SuppressLint("BatteryLife") // a scam protector must run in the background; see docs/PLAN.md
    private fun requestBatteryExemption() =
        open(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.fromParts("package", packageName, null)),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        )

    private fun appInfoIntent() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))

    /** Some phones lack a settings screen: fall back, and never crash. */
    private fun open(first: Intent?, fallback: Intent?) {
        for (intent in listOfNotNull(first, fallback, Intent(Settings.ACTION_SETTINGS))) {
            try {
                startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "No screen for ${intent.action}")
            } catch (e: SecurityException) {
                Log.w(TAG, "Not allowed: ${intent.action}")
            }
        }
    }

    private companion object {
        const val TAG = "MainActivity"
        const val KEY_SCREEN = "screen"
    }
}
