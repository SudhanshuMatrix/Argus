package com.ajayinnovations.argus.alert

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.ui.theme.ArgusTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * The full-screen warning, also over the lock screen. Receives only a verdict ID and its level;
 * the verdict itself comes from [VerdictStore]. AppCompatActivity so it follows the in-app language.
 */
class WarningActivity : AppCompatActivity() {

    private val vm: WarningViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        load(intent)
        setContent {
            ArgusTheme {
                WarningRoute(
                    viewModel = vm,
                    onCall = ::callFamily,
                    onDone = { if (!vm.onDone()) finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        load(intent)
    }

    private fun load(intent: Intent?) {
        val id = intent?.getIntExtra(EXTRA_ID, NO_ID)?.takeIf { it != NO_ID }
        val level = intent?.getStringExtra(EXTRA_LEVEL)
            ?.let { name -> Level.entries.firstOrNull { it.name == name } }
            ?.takeIf { it != Level.NONE }
            ?: Level.SCAM
        vm.show(id, level)
        vm.onShown()
        styleSystemBars(vm.state.value.level)
    }

    /** Clock icons readable on the band: light on red and blue, dark on amber. The bottom is Mist: dark icons. */
    private fun styleSystemBars(level: Level) {
        val status = if (level == Level.CAREFUL) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = status, navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        // Otherwise 3-button navigation gets a light scrim over the coloured background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    /** Opens the dialer with the family number filled in; she presses call herself. */
    private fun callFamily() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard?.isKeyguardLocked == true) {
            // The dialer can't show over the lock screen: unlock first, then dial
            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = dial()
            })
        } else {
            dial()
        }
    }

    private fun dial() {
        val number = vm.state.value.familyNumber
        val uri = if (number != null) Uri.fromParts("tel", number, null) else Uri.parse("tel:")
        try {
            startActivity(Intent(Intent.ACTION_DIAL, uri))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No dialer")
        } catch (e: SecurityException) {
            Log.w(TAG, "Dialer not allowed")
        }
    }

    companion object {
        private const val TAG = "WarningActivity"
        private const val EXTRA_ID = "verdict_id"
        private const val EXTRA_LEVEL = "level"
        private const val NO_ID = -1

        /** Only an ID and the level: never message text, reason or sender. */
        fun intent(context: Context, id: Int, level: Level): Intent =
            Intent(context, WarningActivity::class.java)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_LEVEL, level.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
