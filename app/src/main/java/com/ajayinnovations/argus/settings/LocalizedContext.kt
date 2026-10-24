package com.ajayinnovations.argus.settings

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.ajayinnovations.argus.core.Language
import java.util.Locale

/**
 * A context whose string resources are in [language]. Background code (notifications, family
 * SMS) has no activity, so AppCompat's per-app locale doesn't reach it on Android 12 and below.
 */
fun Context.localized(language: Language): Context {
    val config = Configuration(resources.configuration)
    config.setLocales(LocaleList(Locale.forLanguageTag(language.tag)))
    return createConfigurationContext(config)
}
