package com.ajayinnovations.argus.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajayinnovations.argus.ai.LlmEngine
import com.ajayinnovations.argus.ai.ModelCache
import com.ajayinnovations.argus.ai.ModelSource
import com.ajayinnovations.argus.ai.ModelUnavailable
import com.ajayinnovations.argus.alert.AlertNotifier
import com.ajayinnovations.argus.core.Alert
import com.ajayinnovations.argus.core.DeviceCheck
import com.ajayinnovations.argus.core.DeviceCheckResult
import com.ajayinnovations.argus.core.DeviceSpecs
import com.ajayinnovations.argus.core.IncomingMessage
import com.ajayinnovations.argus.core.Language
import com.ajayinnovations.argus.core.ParentTitle
import com.ajayinnovations.argus.core.ProfileForm
import com.ajayinnovations.argus.core.ScamChecker
import com.ajayinnovations.argus.core.SetupFacts
import com.ajayinnovations.argus.core.SetupFlow
import com.ajayinnovations.argus.core.SetupMark
import com.ajayinnovations.argus.core.SetupStep
import com.ajayinnovations.argus.core.SourceType
import com.ajayinnovations.argus.device.DeviceInfo
import com.ajayinnovations.argus.settings.AppLanguage
import com.ajayinnovations.argus.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReadyPhase { WAITING, COPYING, CHECKING }

sealed interface ReadyError {
    data class NoSpace(val freeUpGb: Double) : ReadyError
    /** This APK has no model (or, in dev, it wasn't pushed). */
    data object Missing : ReadyError
    data object Failed : ReadyError
}

data class SetupUiState(
    /** The screen on show; null until the first check. */
    val step: SetupStep? = null,
    val number: Int = 0,
    val total: Int = 0,
    /** The step on show is done: green, then the wizard moves on by itself. */
    val stepDone: Boolean = false,
    val finished: Boolean = false,
    val facts: SetupFacts? = null,
    val deviceCheck: DeviceCheckResult? = null,
    val specs: DeviceSpecs? = null,
    val readyPhase: ReadyPhase = ReadyPhase.WAITING,
    val readyProgress: Float = 0f,
    val readyError: ReadyError? = null,
    val form: ProfileForm = ProfileForm(),
    val showFormProblems: Boolean = false,
    val language: Language = Language.ENGLISH,
    val testSending: Boolean = false,
    val testFailed: Boolean = false,
)

/** The setup wizard. One step per screen; Android permissions are re-read on every resume. */
class SetupViewModel(
    private val settings: Settings,
    private val appLanguage: AppLanguage,
    private val checks: SetupChecks,
    private val deviceInfo: DeviceInfo,
    private val models: ModelSource,
    private val engine: LlmEngine,
    private val checker: ScamChecker,
    private val alerts: AlertNotifier,
    private val cache: ModelCache,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SetupUiState(finished = settings.setupComplete, form = savedForm(), language = appLanguage.current()),
    )
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    private var readyJob: Job? = null
    private var advanceJob: Job? = null

    fun onResume() {
        _state.update { it.copy(language = appLanguage.syncFromSystem()) }
        refresh()
    }

    /** Re-reads everything and moves to the right step. */
    fun refresh() {
        if (_state.value.finished) return
        var facts = checks.facts()
        // Keep steps that were needed once in the count, so "Step X of N" doesn't jump
        if (facts.sdkInt >= SetupFlow.SDK_UPSIDE_DOWN_CAKE && !facts.fullScreenAllowed) mark(SetupMark.FULL_SCREEN_NEEDED)
        facts = facts.copy(marks = settings.setupMarks)
        val current = SetupFlow.current(facts)
        if (current == SetupStep.RESTRICTED_SETTINGS) mark(SetupMark.RESTRICTED_SHOWN)
        facts = facts.copy(marks = settings.setupMarks)

        val shown = _state.value.step
        val specs = deviceInfo.specs()
        _state.update { it.copy(facts = facts, specs = specs, deviceCheck = DeviceCheck.evaluate(specs)) }

        when {
            // The last screen stays: "Argus is protecting Mummy" and Finish
            current == null -> show(SetupStep.TEST, facts, done = true)
            shown == null || shown == current -> show(current, facts, done = false)
            SetupFlow.isDone(shown, facts) && advanceJob?.isActive != true -> {
                // Green for a moment, so she sees it worked, then on to the next step
                show(shown, facts, done = true)
                advanceJob = viewModelScope.launch {
                    delay(DONE_PAUSE_MILLIS)
                    refreshNow()
                }
            }
            advanceJob?.isActive == true -> Unit
            else -> show(current, facts, done = false)
        }
        if (_state.value.step == SetupStep.GETTING_READY && !_state.value.stepDone) startGettingReady()
    }

    private fun refreshNow() {
        val facts = checks.facts()
        show(SetupFlow.current(facts) ?: SetupStep.TEST, facts, done = SetupFlow.current(facts) == null)
        if (_state.value.step == SetupStep.GETTING_READY) startGettingReady()
    }

    private fun show(step: SetupStep, facts: SetupFacts, done: Boolean) {
        val steps = SetupFlow.steps(facts)
        _state.update {
            it.copy(step = step, number = steps.indexOf(step) + 1, total = steps.size, stepDone = done, facts = facts)
        }
    }

    // Step 1
    fun onPhoneCheckContinue() {
        mark(SetupMark.PHONE_CHECKED)
        refresh()
    }

    // Step 2
    fun onRetryGettingReady() {
        readyJob?.cancel()
        _state.update { it.copy(readyError = null) }
        startGettingReady()
    }

    private fun startGettingReady() {
        if (readyJob?.isActive == true || _state.value.readyError != null) return
        readyJob = viewModelScope.launch {
            _state.update { it.copy(readyPhase = ReadyPhase.COPYING, readyProgress = 0f, readyError = null) }
            val error = try {
                val specs = deviceInfo.specs()
                if (specs.freeStorageBytes < specs.storageNeededBytes) {
                    ReadyError.NoSpace(DeviceCheck.shortfallGb(specs))
                } else {
                    models.prepare { p -> _state.update { it.copy(readyProgress = p) } }
                    _state.update { it.copy(readyPhase = ReadyPhase.CHECKING) }
                    // Loads the model once (writing its cache) and asks one tiny question
                    engine.generate(CHECK_SYSTEM, CHECK_PROMPT, CHECK_MAX_TOKENS)
                    mark(SetupMark.MODEL_WORKS)
                    null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ModelUnavailable.NotEnoughSpace) {
                ReadyError.NoSpace(DeviceCheck.freeUpGb(e.neededBytes - e.freeBytes))
            } catch (e: ModelUnavailable.Missing) {
                ReadyError.Missing
            } catch (t: Throwable) {
                ReadyError.Failed
            }
            _state.update { it.copy(readyPhase = ReadyPhase.WAITING, readyError = error) }
            if (error == null) refresh()
        }
    }

    // Step 3
    fun onForm(form: ProfileForm) = _state.update { it.copy(form = form) }

    fun onLanguage(language: Language) {
        _state.update { it.copy(language = language) }
        appLanguage.set(language)   // recreates the activity in the new language; this ViewModel survives
    }

    fun onWhoNext() {
        val form = _state.value.form
        if (form.problems().isNotEmpty()) {
            _state.update { it.copy(showFormProblems = true) }
            return
        }
        saveForm(settings, form)
        mark(SetupMark.WHO_DONE)
        _state.update { it.copy(showFormProblems = false) }
        refresh()
    }

    // Step 6
    fun onNotificationAccessOpened() = mark(SetupMark.ACCESS_ATTEMPTED)

    // Step 8b: recommended, never required
    fun onOverlaySkipped() {
        mark(SetupMark.OVERLAY_SKIPPED)
        refresh()
    }

    // Step 9b
    fun onMakerDone() {
        mark(SetupMark.MAKER_DONE)
        refresh()
    }

    // Step 10: the real checker and the real alert, on a made-up message
    fun onSendTest(text: String, sender: String) {
        if (_state.value.testSending) return
        _state.update { it.copy(testSending = true, testFailed = false) }
        viewModelScope.launch {
            val ok = try {
                val message = IncomingMessage(SourceType.SMS, text, sender)
                val result = checker.check(text, message.sourceLabel, settings.language)
                alerts.show(Alert.from(message, result)) != null
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                false
            }
            if (ok) mark(SetupMark.TEST_DONE)
            _state.update { it.copy(testSending = false, testFailed = !ok) }
            refresh()
        }
    }

    /**
     * Home's "Fix it" for the safety checker: back to "Getting ready", which copies the model
     * again (when the copy wouldn't load) and checks it works. Every other step is still done,
     * so the wizard goes straight on to Finish. setupComplete stays true meanwhile, so the
     * rules keep checking messages in the background.
     */
    fun repairModel() {
        viewModelScope.launch {
            val broken = engine.state.value.loadFailed
            try {
                engine.unload()
                if (broken) {
                    models.discardCopy()
                    cache.deleteAll()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                // Getting ready reports anything still wrong
            }
            settings.removeSetupMark(SetupMark.MODEL_WORKS)
            readyJob?.cancel()
            _state.update { it.copy(finished = false, step = null, stepDone = false, readyError = null) }
            refresh()
        }
    }

    fun onFinish() {
        settings.setupComplete = true
        _state.update { it.copy(finished = true) }
    }

    private fun mark(mark: SetupMark) = settings.addSetupMark(mark)

    private fun savedForm(): ProfileForm = loadForm(settings, requireTitle = SetupMark.WHO_DONE in settings.setupMarks)

    companion object {
        const val DONE_PAUSE_MILLIS = 1_200L
        private const val CHECK_SYSTEM = "Reply with one word."
        private const val CHECK_PROMPT = "Say OK."
        private const val CHECK_MAX_TOKENS = 4

        /** The form as saved; before the wizard's step 3, no title is picked yet. */
        fun loadForm(settings: Settings, requireTitle: Boolean): ProfileForm {
            val custom = settings.parentTitle == ParentTitle.NEUTRAL && settings.parentName.isNotBlank()
            return ProfileForm(
                title = if (requireTitle && !custom) settings.parentTitle else null,
                customName = settings.parentName,
                usesCustomName = custom,
                familyName = settings.familyName,
                familyNumber = settings.familyNumber.orEmpty(),
                familyAlerts = settings.familyAlerts,
            )
        }

        fun saveForm(settings: Settings, form: ProfileForm) {
            settings.parentTitle = form.savedTitle
            settings.parentName = form.savedCustomName
            settings.familyName = form.familyName
            settings.familyNumber = form.familyNumber
            settings.familyAlerts = form.familyAlerts
        }
    }
}
