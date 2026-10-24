package com.ajayinnovations.argus.alert

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.core.FamilySmsTracker
import com.ajayinnovations.argus.core.Language
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.settings.Settings
import com.ajayinnovations.argus.settings.localized

/** SENT means handed to the radio; the send report then says whether the network took it. */
enum class FamilySmsResult { SENT, NOT_SCAM, TURNED_OFF, NO_NUMBER, NO_PERMISSION, FAILED }

/**
 * Texts the family member when a SCAM is flagged, if turned on. The text is generic: no reason,
 * no sender, no message. Always English: it goes to the family member, not the parent.
 * Uses the parent's normal SMS rate. Each part asks for a send report (FamilySmsSentReceiver);
 * a refusal shows on the home screen. Never retries, never throws.
 */
class FamilyNotifier(
    private val context: Context,
    private val settings: Settings,
    private val tracker: FamilySmsTracker,
) {

    fun notify(level: Level): FamilySmsResult {
        if (level != Level.SCAM) return FamilySmsResult.NOT_SCAM
        if (!settings.familyAlerts) return FamilySmsResult.TURNED_OFF
        val number = settings.familyNumber ?: return FamilySmsResult.NO_NUMBER
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Family SMS skipped: SEND_SMS not granted")
            return FamilySmsResult.NO_PERMISSION
        }
        return try {
            val sms = smsManager() ?: return FamilySmsResult.FAILED
            val parts = sms.divideMessage(text())
            val batch = tracker.start(parts.size)
            val reports = ArrayList(parts.indices.map { sentReport(batch, it) })
            sms.sendMultipartTextMessage(number, null, parts, reports, null)
            Log.i(TAG, "Family SMS handed to the radio (${parts.size} part(s))")
            FamilySmsResult.SENT
        } catch (e: SecurityException) {
            Log.w(TAG, "Family SMS not allowed")
            settings.familySmsFailed = true
            FamilySmsResult.NO_PERMISSION
        } catch (t: Throwable) {
            // No SIM, airplane mode, no default SIM on a dual-SIM phone…
            Log.w(TAG, "Family SMS failed (${t.javaClass.simpleName})")
            settings.familySmsFailed = true
            FamilySmsResult.FAILED
        }
    }

    private fun text(): String {
        val english = context.localized(Language.ENGLISH)
        val title = AlertText.titleName(english, settings.parentTitle, settings.parentName)
        return if (title != null) english.getString(R.string.family_sms, title) else english.getString(R.string.family_sms_neutral)
    }

    /** Explicit, immutable, one per part: the system sets the result code. No text inside. */
    private fun sentReport(batch: Int, part: Int): PendingIntent {
        val intent = Intent(context, FamilySmsSentReceiver::class.java)
            .putExtra(FamilySmsSentReceiver.EXTRA_BATCH, batch)
            .putExtra(FamilySmsSentReceiver.EXTRA_PART, part)
        return PendingIntent.getBroadcast(
            context, batch * MAX_PARTS + part, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun smsManager(): SmsManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

    private companion object {
        const val TAG = "FamilyNotifier"
        /** Request codes stay unique per part; the generic text is one or two parts. */
        const val MAX_PARTS = 16
    }
}
