package com.ajayinnovations.argus.ui.home

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajayinnovations.argus.ai.LlmEngine
import com.ajayinnovations.argus.ai.ModelSource
import com.ajayinnovations.argus.ai.ModelStatus
import com.ajayinnovations.argus.core.Language
import com.ajayinnovations.argus.core.ParentTitle
import com.ajayinnovations.argus.ingest.IngestHealth
import com.ajayinnovations.argus.ingest.ListenerState
import com.ajayinnovations.argus.settings.AppLanguage
import com.ajayinnovations.argus.settings.Settings
import com.ajayinnovations.argus.ui.setup.SetupChecks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One thing Argus needs; each can be turned off later in Android's settings. */
enum class StatusKind { MODEL, SMS, NOTIFICATIONS, WHATSAPP, FULL_SCREEN, OVERLAY, BATTERY }

/** [optional]: recommended only; when off it never turns the home screen red. */
data class StatusRow(val kind: StatusKind, val ok: Boolean, val optional: Boolean = false)

data class HomeUiState(
    val language: Language,
    val parentTitle: ParentTitle = ParentTitle.NEUTRAL,
    val parentName: String = "",
    val rows: List<StatusRow> = emptyList(),
    /** The last family alert SMS was refused (usually no airtime). */
    val familySmsFailed: Boolean = false,
) {
    val allOk: Boolean get() = rows.all { it.ok || it.optional }
}

/** The home screen after setup: what is protected, and what needs fixing. */
class HomeViewModel(
    private val appLanguage: AppLanguage,
    private val health: IngestHealth,
    private val settings: Settings,
    private val checks: SetupChecks,
    private val models: ModelSource,
    private val engine: LlmEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState(language = appLanguage.current()))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // The listener can connect or drop at any time
        viewModelScope.launch { ListenerState.connected.collect { refresh() } }
        // A background check may find the model broken while the screen is open
        viewModelScope.launch { engine.state.map { it.loadFailed }.distinctUntilChanged().collect { refresh() } }
    }

    /** On resume: a permission or the language may have changed in the phone's Settings. */
    fun onResume() {
        appLanguage.syncFromSystem()
        health.requestRebind()
        refresh()
    }

    /** After a fix attempt the result arrives on the next resume or listener change. */
    fun onFixAttempted() = health.requestRebind()

    fun refresh() {
        val sdk = Build.VERSION.SDK_INT
        val rows = buildList {
            // Without the model the hard rules still run, but most scams need Gemma
            val modelOk = runCatching { models.status() == ModelStatus.READY }.getOrDefault(false) && !engine.state.value.loadFailed
            add(StatusRow(StatusKind.MODEL, modelOk))
            add(StatusRow(StatusKind.SMS, checks.granted(Manifest.permission.RECEIVE_SMS) &&
                (!settings.familyAlerts || checks.granted(Manifest.permission.SEND_SMS))))
            add(StatusRow(StatusKind.NOTIFICATIONS, checks.notificationsAllowed()))
            add(StatusRow(StatusKind.WHATSAPP, health.notificationAccess() && ListenerState.connected.value))
            if (sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) add(StatusRow(StatusKind.FULL_SCREEN, checks.fullScreenAllowed()))
            add(StatusRow(StatusKind.OVERLAY, checks.overlayAllowed(), optional = true))
            add(StatusRow(StatusKind.BATTERY, checks.batteryIgnored()))
        }
        _state.update {
            it.copy(
                language = appLanguage.current(),
                parentTitle = settings.parentTitle,
                parentName = settings.parentName,
                rows = rows,
                familySmsFailed = settings.familySmsFailed,
            )
        }
    }
}
