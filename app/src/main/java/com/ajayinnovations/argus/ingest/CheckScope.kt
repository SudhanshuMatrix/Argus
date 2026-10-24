package com.ajayinnovations.argus.ingest

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** App-wide scope for the check queue; an uncaught error is logged (class only), never crashes the app. */
object CheckScope {
    private const val TAG = "CheckQueue"

    val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default +
            CoroutineExceptionHandler { _, t -> Log.w(TAG, "Queue error (${t.javaClass.simpleName})") },
    )
}
