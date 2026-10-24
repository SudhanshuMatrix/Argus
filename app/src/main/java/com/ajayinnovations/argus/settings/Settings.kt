package com.ajayinnovations.argus.settings

import android.content.Context
import androidx.core.content.edit
import com.ajayinnovations.argus.core.Language
import com.ajayinnovations.argus.core.ParentTitle
import com.ajayinnovations.argus.core.SetupMark

/**
 * The few choices made in setup. Never holds message text.
 * Background code reads [language] from here because AppCompat's per-app locale
 * is not visible to services on Android 12 and below.
 */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var language: Language
        get() = Language.fromTag(prefs.getString(KEY_LANGUAGE, null))
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value.tag) }

    var parentTitle: ParentTitle
        get() = ParentTitle.fromKey(prefs.getString(KEY_PARENT_TITLE, null))
        set(value) = prefs.edit { putString(KEY_PARENT_TITLE, value.key) }

    /** "Their name" (e.g. "Mama Chidi"), used with the NEUTRAL title; empty otherwise. */
    var parentName: String
        get() = prefs.getString(KEY_PARENT_NAME, null).orEmpty()
        set(value) = prefs.edit { putString(KEY_PARENT_NAME, value.trim()) }

    /** Wizard progress that can't be read from Android (no message content). */
    var setupMarks: Set<SetupMark>
        get() = prefs.getStringSet(KEY_SETUP_MARKS, emptySet()).orEmpty()
            .mapNotNull { name -> SetupMark.entries.firstOrNull { it.name == name } }.toSet()
        set(value) = prefs.edit { putStringSet(KEY_SETUP_MARKS, value.map { it.name }.toSet()) }

    fun addSetupMark(mark: SetupMark) {
        if (mark !in setupMarks) setupMarks = setupMarks + mark
    }

    fun removeSetupMark(mark: SetupMark) {
        if (mark in setupMarks) setupMarks = setupMarks - mark
    }

    /** The family member the "Call" button and family SMS go to. */
    var familyName: String
        get() = prefs.getString(KEY_FAMILY_NAME, null).orEmpty()
        set(value) = prefs.edit { putString(KEY_FAMILY_NAME, value.trim()) }

    var familyNumber: String?
        get() = prefs.getString(KEY_FAMILY_NUMBER, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit { putString(KEY_FAMILY_NUMBER, value?.trim()) }

    var familyAlerts: Boolean
        get() = prefs.getBoolean(KEY_FAMILY_ALERTS, false)
        set(value) = prefs.edit { putBoolean(KEY_FAMILY_ALERTS, value) }

    /** The last family SMS was refused (no airtime, SMS barred). Cleared by the next one that goes out. */
    var familySmsFailed: Boolean
        get() = prefs.getBoolean(KEY_FAMILY_SMS_FAILED, false)
        set(value) = prefs.edit { putBoolean(KEY_FAMILY_SMS_FAILED, value) }

    /** Off by default: silent mode is respected, but the phone still vibrates and shows the red screen. */
    var speakOnSilent: Boolean
        get() = prefs.getBoolean(KEY_SPEAK_ON_SILENT, false)
        set(value) = prefs.edit { putBoolean(KEY_SPEAK_ON_SILENT, value) }

    /** Set when the wizard ends. Background checks skip until then, so they never trigger the model copy. */
    var setupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit { putBoolean(KEY_SETUP_COMPLETE, value) }

    /** Off by default: CPU everywhere. Only the dev debug screen can turn it on for now. */
    var useGpu: Boolean
        get() = prefs.getBoolean(KEY_USE_GPU, false)
        set(value) = prefs.edit { putBoolean(KEY_USE_GPU, value) }

    /** Set for good once the GPU threw an error or the app died twice in a row during a GPU call. */
    var gpuDisabled: Boolean
        get() = prefs.getBoolean(KEY_GPU_DISABLED, false)
        set(value) = prefs.edit(commit = true) { putBoolean(KEY_GPU_DISABLED, value) }

    /**
     * True only while a GPU native call runs. Written with commit(), not apply(), because a
     * native crash right after would lose an apply(). Still true at the next start means the
     * app died on the GPU.
     */
    var gpuCallInProgress: Boolean
        get() = prefs.getBoolean(KEY_GPU_CALL_IN_PROGRESS, false)
        set(value) = prefs.edit(commit = true) { putBoolean(KEY_GPU_CALL_IN_PROGRESS, value) }

    /** Times in a row the app died during a GPU call; reset by any GPU call that finishes. */
    var gpuDeathsInARow: Int
        get() = prefs.getInt(KEY_GPU_DEATHS_IN_A_ROW, 0)
        set(value) = prefs.edit(commit = true) { putInt(KEY_GPU_DEATHS_IN_A_ROW, value) }

    private companion object {
        const val FILE = "argus_settings"
        const val KEY_LANGUAGE = "language"
        const val KEY_PARENT_TITLE = "parent_title"
        const val KEY_FAMILY_NAME = "family_name"
        const val KEY_PARENT_NAME = "parent_name"
        const val KEY_SETUP_MARKS = "setup_marks"
        const val KEY_FAMILY_NUMBER = "family_number"
        const val KEY_FAMILY_ALERTS = "family_alerts"
        const val KEY_SPEAK_ON_SILENT = "speak_on_silent"
        const val KEY_FAMILY_SMS_FAILED = "family_sms_failed"
        const val KEY_SETUP_COMPLETE = "setup_complete"
        const val KEY_USE_GPU = "use_gpu"
        const val KEY_GPU_DISABLED = "gpu_disabled"
        const val KEY_GPU_CALL_IN_PROGRESS = "gpu_call_in_progress"
        const val KEY_GPU_DEATHS_IN_A_ROW = "gpu_deaths_in_a_row"
    }
}
