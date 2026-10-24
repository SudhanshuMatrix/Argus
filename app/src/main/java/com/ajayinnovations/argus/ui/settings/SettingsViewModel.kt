package com.ajayinnovations.argus.ui.settings

import androidx.lifecycle.ViewModel
import com.ajayinnovations.argus.core.Language
import com.ajayinnovations.argus.core.ProfileForm
import com.ajayinnovations.argus.settings.AppLanguage
import com.ajayinnovations.argus.settings.Settings
import com.ajayinnovations.argus.ui.setup.SetupViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val form: ProfileForm = ProfileForm(),
    val language: Language = Language.ENGLISH,
    val speakOnSilent: Boolean = false,
    val showProblems: Boolean = false,
    val saved: Boolean = false,
)

/** Title, language, family name and number, family alerts, speak on silent. */
class SettingsViewModel(
    private val settings: Settings,
    private val appLanguage: AppLanguage,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private var loaded = false

    /** After Android restores the app straight onto this tab: load once, keep any edits after. */
    fun loadIfNeeded() {
        if (!loaded) load()
    }

    /** Fresh from Settings each time the tab opens. */
    fun load() = _state.update {
        loaded = true
        SettingsUiState(
            form = SetupViewModel.loadForm(settings, requireTitle = true),
            language = appLanguage.current(),
            speakOnSilent = settings.speakOnSilent,
        )
    }

    fun onForm(form: ProfileForm) = _state.update { it.copy(form = form, saved = false) }

    fun onLanguage(language: Language) {
        _state.update { it.copy(language = language) }
        appLanguage.set(language)   // recreates the activity; this ViewModel survives
    }

    fun onSpeakOnSilent(on: Boolean) = _state.update { it.copy(speakOnSilent = on, saved = false) }

    /** Returns true when saved; the caller then asks for SEND_SMS if family alerts are on. */
    fun onSave(): Boolean {
        val ui = _state.value
        if (ui.form.problems().isNotEmpty()) {
            _state.update { it.copy(showProblems = true) }
            return false
        }
        SetupViewModel.saveForm(settings, ui.form)
        settings.speakOnSilent = ui.speakOnSilent
        _state.update { it.copy(showProblems = false, saved = true) }
        return true
    }
}
