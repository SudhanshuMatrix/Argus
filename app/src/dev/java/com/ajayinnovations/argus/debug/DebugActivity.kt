package com.ajayinnovations.argus.debug

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ajayinnovations.argus.settings.AppLanguage
import com.ajayinnovations.argus.ui.theme.ArgusTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

/** dev flavor only: its own launcher entry, "Argus Dev". */
class DebugActivity : AppCompatActivity() {

    private val appLanguage: AppLanguage by inject()

    override fun onResume() {
        super.onResume()
        // Sample runs use Settings.language; keep it equal to Android's per-app language
        appLanguage.syncFromSystem()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ArgusTheme { DebugRoute(koinViewModel()) } }
    }
}
