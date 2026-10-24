package com.ajayinnovations.argus.alert

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ajayinnovations.argus.core.FamilySmsOutcome
import com.ajayinnovations.argus.core.FamilySmsTracker
import com.ajayinnovations.argus.settings.Settings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The radio's send report for each part of a family SMS. Records the outcome for the home
 * screen ("Family alert couldn't be sent"). Never retries. Logs result codes only.
 */
class FamilySmsSentReceiver : BroadcastReceiver(), KoinComponent {

    private val tracker: FamilySmsTracker by inject()
    private val settings: Settings by inject()

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val batch = intent.getIntExtra(EXTRA_BATCH, -1)
            val ok = resultCode == Activity.RESULT_OK
            if (!ok) Log.w(TAG, "Family SMS part failed (result $resultCode)")
            when (tracker.onPartResult(batch, ok)) {
                FamilySmsOutcome.SENT -> {
                    settings.familySmsFailed = false
                    Log.i(TAG, "Family SMS sent")
                }
                FamilySmsOutcome.FAILED -> settings.familySmsFailed = true
                null -> Unit
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Send report failed (${t.javaClass.simpleName})")
        }
    }

    companion object {
        private const val TAG = "FamilyNotifier"
        const val EXTRA_BATCH = "batch"
        const val EXTRA_PART = "part"
    }
}
