package com.ajayinnovations.argus.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ajayinnovations.argus.core.Language

/**
 * Switches the in-app language and remembers it for background code. Android's per-app
 * language is the source of truth: on Android 13+ it can also be changed in the phone's
 * Settings, so [syncFromSystem] copies it into [Settings] at app start and on every resume.
 */
class AppLanguage(private val context: Context, private val settings: Settings) {

    fun current(): Language = settings.language

    fun set(language: Language) {
        settings.language = language
        // Recreates open activities in the new language
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    /** Copies Android's per-app language into Settings when it is set. Returns the current language. */
    fun syncFromSystem(): Language {
        Language.fromLocaleTags(systemTags())?.let { if (it != settings.language) settings.language = it }
        return settings.language
    }

    private fun systemTags(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Works without an activity, so also when a background check starts the process
            context.getSystemService(LocaleManager::class.java)?.applicationLocales?.toLanguageTags().orEmpty()
        } else {
            // Below 13 only our own button changes it; AppCompat knows it once an activity exists
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }
}
