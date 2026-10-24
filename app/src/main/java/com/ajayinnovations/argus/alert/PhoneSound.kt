package com.ajayinnovations.argus.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.ajayinnovations.argus.core.Ringer

/** Reads the ringer switch and vibrates the phone. Never throws. */
class PhoneSound(private val context: Context) {

    fun ringer(): Ringer = try {
        when (context.getSystemService(AudioManager::class.java)?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> Ringer.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> Ringer.VIBRATE
            else -> Ringer.NORMAL
        }
    } catch (t: Throwable) {
        Ringer.NORMAL
    }

    /** A strong alarm-style vibration; alarm usage still vibrates when the phone is on silent. */
    fun vibrateAlarm() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                context.getSystemService(Vibrator::class.java)
            }
            if (vibrator == null || !vibrator.hasVibrator()) return
            val effect = VibrationEffect.createWaveform(PATTERN, -1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Vibration failed (${t.javaClass.simpleName})")
        }
    }

    companion object {
        private const val TAG = "PhoneSound"
        /** Off, on, off, on…: three long buzzes. Also the scam channel's pattern. */
        val PATTERN = longArrayOf(0, 600, 300, 600, 300, 600)
    }
}
