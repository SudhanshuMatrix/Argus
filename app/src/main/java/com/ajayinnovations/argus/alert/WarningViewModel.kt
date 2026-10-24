package com.ajayinnovations.argus.alert

import androidx.lifecycle.ViewModel
import com.ajayinnovations.argus.core.Finding
import com.ajayinnovations.argus.core.Highlights
import com.ajayinnovations.argus.core.Mark
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.core.ParentTitle
import com.ajayinnovations.argus.core.ReasonCode
import com.ajayinnovations.argus.core.SourceType
import com.ajayinnovations.argus.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [generic]: the alert is gone (the process died since it fired), so only the fixed words
 * for [level] are shown. [sender] and [preview] are for the phone's owner only: in memory, on screen.
 */
data class WarningUiState(
    val level: Level = Level.SCAM,
    val generic: Boolean = true,
    val source: SourceType? = null,
    val sender: String? = null,
    val preview: String? = null,
    /** The whole message, when known (scrollable on the screen); else [preview]. */
    val message: String? = null,
    /** SCAM warnings not yet acknowledged, this one included: "2 warnings" when more than one. */
    val pendingCount: Int = 0,
    val findings: List<Finding> = emptyList(),
    /** The dangerous parts of [preview], for the highlighted chat bubble. */
    val marks: List<Mark> = emptyList(),
    val reasonCode: ReasonCode? = null,
    val modelReason: String = "",
    val modelAdvice: String = "",
    val parentTitle: ParentTitle = ParentTitle.NEUTRAL,
    val parentName: String = "",
    val familyName: String = "",
    val familyNumber: String? = null,
)

class WarningViewModel(
    private val store: VerdictStore,
    private val settings: Settings,
    private val voice: VoicePlayer,
    private val alerts: AlertNotifier,
) : ViewModel() {

    private val _state = MutableStateFlow(WarningUiState())
    val state: StateFlow<WarningUiState> = _state.asStateFlow()
    private var id: Int? = null

    /** [level] comes with the ID so the right generic warning shows if the verdict is gone. */
    fun show(id: Int?, level: Level) {
        this.id = id
        val alert = id?.let(store::get)
        val verdict = alert?.verdict
        val text = alert?.message ?: alert?.preview
        _state.value = WarningUiState(
            level = verdict?.level ?: level,
            generic = alert == null,
            source = alert?.source,
            sender = alert?.sender,
            preview = alert?.preview,
            message = alert?.message,
            pendingCount = store.pendingScams().size,
            findings = alert?.findings.orEmpty(),
            marks = text?.let { Highlights.find(it, alert?.findings.orEmpty()) }.orEmpty(),
            reasonCode = verdict?.reasonCode,
            modelReason = verdict?.reason.orEmpty(),
            modelAdvice = verdict?.advice.orEmpty(),
            parentTitle = settings.parentTitle,
            parentName = settings.parentName,
            familyName = settings.familyName,
            familyNumber = settings.familyNumber,
        )
    }

    fun onHearAgain() = voice.play(_state.value.level, userAsked = true)

    /** The screen is showing it: its notification may now be swiped away. */
    fun onShown() {
        id?.let(alerts::markSeen)
    }

    /**
     * "I understand": this alert has done its job. Returns true when another unacknowledged
     * scam warning is waiting, which is then shown instead of closing the screen.
     */
    fun onDone(): Boolean {
        voice.stop()
        id?.let(alerts::acknowledge)
        val next = store.pendingScams().lastOrNull() ?: return false
        show(next, Level.SCAM)
        onShown()
        return true
    }
}
