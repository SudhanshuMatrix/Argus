package com.ajayinnovations.argus

import android.app.Application
import android.util.Log
import com.ajayinnovations.argus.ai.LlmEngine
import com.ajayinnovations.argus.core.TrimPolicy
import com.ajayinnovations.argus.di.appModule
import com.ajayinnovations.argus.di.flavorModule
import com.ajayinnovations.argus.settings.AppLanguage
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ArgusApp : Application() {

    private val engine: LlmEngine by inject()
    private val appLanguage: AppLanguage by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ArgusApp)
            modules(appModule, flavorModule)
        }
        // The user may have changed the app's language in the phone's Settings while we were closed
        appLanguage.syncFromSystem()
    }

    // The model stays loaded while the app runs; it is freed only under moderate memory
    // pressure or worse. Android 14+ sends only UI_HIDDEN and BACKGROUND, so there it stays
    // loaded until the process itself is killed.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (TrimPolicy.shouldUnload(level)) {
            Log.i(TAG, "Memory pressure (level $level): unloading the model")
            engine.unloadSoon()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        engine.unloadSoon()
    }

    private companion object {
        const val TAG = "ArgusApp"
    }
}
