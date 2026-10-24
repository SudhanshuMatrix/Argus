package com.ajayinnovations.argus.ui.check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajayinnovations.argus.ai.ModelSource
import com.ajayinnovations.argus.core.DecidedBy
import com.ajayinnovations.argus.core.Finding
import com.ajayinnovations.argus.core.Highlights
import com.ajayinnovations.argus.core.Mark
import com.ajayinnovations.argus.core.MessagePreview
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.core.ParentTitle
import com.ajayinnovations.argus.core.ReasonCode
import com.ajayinnovations.argus.core.ScamChecker
import com.ajayinnovations.argus.core.SkipFilter
import com.ajayinnovations.argus.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CheckOutcome {
    data class Result(
        val level: Level,
        val findings: List<Finding>,
        /** The start of the checked text and its dangerous parts, for the chat bubble. */
        val preview: String,
        val marks: List<Mark>,
        val reasonCode: ReasonCode?,
        val modelReason: String,
        val modelAdvice: String,
    ) : CheckOutcome

    /** The model file isn't ready (setup hasn't finished, or no adb-pushed file in dev). */
    data object NotReady : CheckOutcome

    /** The model failed and no rule decided: never pretend that means "no warning signs". */
    data object Failed : CheckOutcome
}

data class CheckUiState(
    val input: String = "",
    val checking: Boolean = false,
    val outcome: CheckOutcome? = null,
    val parentTitle: ParentTitle = ParentTitle.NEUTRAL,
    val parentName: String = "",
    val familyName: String = "",
)

/** Remembers only a hash of the last clipboard text checked, for the whole process. */
class LastClip {
    @Volatile
    var hash: Int? = null
}

/** Share to check, clipboard check and the paste box. The text lives only in memory. */
class CheckViewModel(
    private val checker: ScamChecker,
    private val models: ModelSource,
    private val settings: Settings,
    private val lastClip: LastClip,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckUiState())
    val state: StateFlow<CheckUiState> = _state.asStateFlow()

    fun onResume() = _state.update { it.copy(parentTitle = settings.parentTitle, parentName = settings.parentName, familyName = settings.familyName) }

    fun onInput(text: String) = _state.update { it.copy(input = text) }

    fun onCheck() = check(_state.value.input, SOURCE_PASTED)

    fun onShared(text: String) {
        _state.update { it.copy(input = text) }
        check(text, SOURCE_SHARED)
    }

    /**
     * Only when the clipboard changed since the last check, and not for "ok" or emojis.
     * Returns true when a check started, so the app can show the check screen.
     */
    fun onClipboard(text: String): Boolean {
        val hash = text.trim().hashCode()
        if (hash == lastClip.hash || _state.value.checking) return false
        lastClip.hash = hash
        if (SkipFilter.isObviouslyHarmless(text)) return false
        _state.update { it.copy(input = text) }
        check(text, SOURCE_COPIED)
        return true
    }

    private fun check(text: String, source: String) {
        if (text.isBlank() || _state.value.checking) return
        if (models.readyFile() == null) {
            _state.update { it.copy(outcome = CheckOutcome.NotReady) }
            return
        }
        _state.update { it.copy(checking = true, outcome = null) }
        viewModelScope.launch {
            val outcome = try {
                val result = checker.check(text, source, settings.language)
                if (result.decidedBy == DecidedBy.MODEL_FAILED) {
                    CheckOutcome.Failed
                } else {
                    val v = result.verdict
                    val preview = MessagePreview.keepingMarks(text, result.findings)
                    CheckOutcome.Result(v.level, result.findings, preview, Highlights.find(preview, result.findings), v.reasonCode, v.reason, v.advice)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                CheckOutcome.Failed
            }
            _state.update { it.copy(checking = false, outcome = outcome) }
        }
    }

    private companion object {
        // Only for the prompt ("Message received via …"); never logged
        const val SOURCE_SHARED = "a message the user shared"
        const val SOURCE_COPIED = "a message the user copied"
        const val SOURCE_PASTED = "a message the user pasted"
    }
}
