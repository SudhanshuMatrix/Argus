package com.ajayinnovations.argus.debug

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajayinnovations.argus.BuildConfig
import com.ajayinnovations.argus.ai.EngineState
import com.ajayinnovations.argus.ai.LlmEngine
import com.ajayinnovations.argus.ai.ModelCache
import com.ajayinnovations.argus.ai.ModelSource
import com.ajayinnovations.argus.ai.ModelStatus
import com.ajayinnovations.argus.ai.ModelUnavailable
import com.ajayinnovations.argus.alert.AlertNotifier
import com.ajayinnovations.argus.core.Alert
import com.ajayinnovations.argus.core.CheckResult
import com.ajayinnovations.argus.core.DecidedBy
import com.ajayinnovations.argus.core.Findings
import com.ajayinnovations.argus.core.IncomingMessage
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.core.SourceType
import com.ajayinnovations.argus.core.ReasonCode
import com.ajayinnovations.argus.core.Verdict
import com.ajayinnovations.argus.core.CheckMode
import com.ajayinnovations.argus.core.CheckQueue
import com.ajayinnovations.argus.core.IngestStats
import com.ajayinnovations.argus.ingest.IngestHealth
import com.ajayinnovations.argus.ingest.ListenerState
import com.ajayinnovations.argus.core.DeviceCheck
import com.ajayinnovations.argus.core.ScamChecker
import com.ajayinnovations.argus.core.DeviceCheckResult
import com.ajayinnovations.argus.core.DeviceReport
import com.ajayinnovations.argus.core.DeviceSpecs
import com.ajayinnovations.argus.device.DeviceInfo
import com.ajayinnovations.argus.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DebugUiState(
    val specs: DeviceSpecs,
    val check: DeviceCheckResult,
    val modelStatus: ModelStatus,
    val engine: EngineState = EngineState(),
    val useGpu: Boolean = false,
    val gpuDisabled: Boolean = false,
    val input: String = "",
    val busy: Boolean = false,
    val reply: String? = null,
    val replySeconds: Double? = null,
    val error: String? = null,
    val samplesTotal: Int = 0,
    val samplesRunning: Boolean = false,
    val sampleMode: CheckMode = ScamChecker.DEFAULT_MODE,
    val sampleResults: List<SampleResult> = emptyList(),
    val sampleSet: SampleSet = SampleSet.MAIN,
    val holdoutCount: Int = 0,
    val holdout2Count: Int = 0,
    val setupComplete: Boolean = false,
    val smsPermission: Boolean = false,
    val notificationAccess: Boolean = false,
    val listenerConnected: Boolean = false,
    val ingest: IngestStats = IngestStats(),
    // Alerts
    val notificationsAllowed: Boolean = false,
    val fullScreenAllowed: Boolean = false,
    val sendSmsAllowed: Boolean = false,
) {
    val sampleSummary: SampleSummary get() = SampleSummary.of(sampleResults)
}

/** Dev flavor only. Shows raw model output on screen; never logs it. */
class DebugViewModel(
    private val context: Context,
    private val alerts: AlertNotifier,
    private val engine: LlmEngine,
    private val models: ModelSource,
    private val deviceInfo: DeviceInfo,
    private val settings: Settings,
    private val cache: ModelCache,
    private val samples: SampleRunner,
    private val queue: CheckQueue,
    private val health: IngestHealth,
) : ViewModel() {

    private val local = MutableStateFlow(snapshot())

    val state: StateFlow<DebugUiState> =
        combine(local, engine.state, ListenerState.connected, queue.stats) { ui, engineState, connected, ingest ->
            ui.copy(engine = engineState, listenerConnected = connected, ingest = ingest)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    private fun snapshot(): DebugUiState {
        val specs = deviceInfo.specs()
        return DebugUiState(
            specs = specs,
            check = DeviceCheck.evaluate(specs),
            modelStatus = models.status(),
            useGpu = settings.useGpu,
            gpuDisabled = settings.gpuDisabled,
            holdoutCount = samples.count(SampleSet.HOLDOUT),
            holdout2Count = samples.count(SampleSet.HOLDOUT2),
            setupComplete = settings.setupComplete,
            smsPermission = health.smsPermission(),
            notificationAccess = health.notificationAccess(),
            notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || granted(Manifest.permission.POST_NOTIFICATIONS)),
            fullScreenAllowed = alerts.canUseFullScreen(),
            sendSmsAllowed = granted(Manifest.permission.SEND_SMS),
        )
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * The full alert path for a level, family SMS included when it's turned on. Made-up
     * sender and text, run through the real rules so the reason is what a real one would get.
     */
    fun onTestAlert(level: Level) {
        val (verdict, text) = when (level) {
            Level.SCAM -> Verdict(Level.SCAM, reasonCode = ReasonCode.ACCOUNT_PROBLEM) to
                "Dear customer, your BVN is blocked. Click bit.ly/x1abc to update it today or your account will be closed."
            Level.CAREFUL -> Verdict(Level.CAREFUL, reasonCode = ReasonCode.ASKS_FOR_MONEY_OR_LINK) to
                "Hello, please pay the 5,000 naira for the meeting into my new account today."
            else -> Verdict(level) to "Your OTP is 482913. It expires in 5 minutes. Do not share this code with anyone."
        }
        val message = IncomingMessage(SourceType.SMS, text, TEST_SENDER)
        alerts.show(Alert.from(message, CheckResult(verdict, DecidedBy.HARD_RULE, 0, findings = Findings.find(text))))
    }

    /** Re-reads the device and model status, e.g. after an adb push. */
    fun onRefresh() {
        val fresh = snapshot()
        local.update {
            it.copy(
                specs = fresh.specs, check = fresh.check, modelStatus = fresh.modelStatus,
                useGpu = fresh.useGpu, gpuDisabled = fresh.gpuDisabled,
                holdoutCount = fresh.holdoutCount, holdout2Count = fresh.holdout2Count, setupComplete = fresh.setupComplete,
                smsPermission = fresh.smsPermission, notificationAccess = fresh.notificationAccess,
                notificationsAllowed = fresh.notificationsAllowed, fullScreenAllowed = fresh.fullScreenAllowed,
                sendSmsAllowed = fresh.sendSmsAllowed,
            )
        }
    }

    fun onInput(text: String) = local.update { it.copy(input = text) }

    fun onAsk() {
        val prompt = local.value.input.trim()
        if (prompt.isEmpty() || local.value.busy) return
        local.update { it.copy(busy = true, error = null, reply = null, replySeconds = null) }
        viewModelScope.launch {
            val start = SystemClock.elapsedRealtime()
            try {
                val reply = engine.generate(SYSTEM, prompt)
                val seconds = (SystemClock.elapsedRealtime() - start) / 1000.0
                local.update { it.copy(busy = false, reply = reply, replySeconds = seconds) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ModelUnavailable) {
                local.update { it.copy(busy = false, error = "Model not ready: ${e.message}") }
            } catch (e: Throwable) {
                local.update { it.copy(busy = false, error = "Failed: ${e.javaClass.simpleName}") }
            }
            onRefresh()
        }
    }

    fun onRunSamples(set: SampleSet = SampleSet.MAIN) {
        if (local.value.samplesRunning || local.value.busy) return
        local.update { it.copy(samplesRunning = true, sampleResults = emptyList(), sampleSet = set, samplesTotal = samples.count(set), error = null) }
        viewModelScope.launch {
            try {
                samples.runAll(set, local.value.sampleMode) { result -> local.update { it.copy(sampleResults = it.sampleResults + result) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                local.update { it.copy(error = "Samples failed: ${e.javaClass.simpleName}") }
            } finally {
                local.update { it.copy(samplesRunning = false) }
            }
        }
    }

    fun onNextSampleMode() {
        if (local.value.samplesRunning) return
        val modes = CheckMode.entries
        local.update { it.copy(sampleMode = modes[(modes.indexOf(it.sampleMode) + 1) % modes.size], sampleResults = emptyList()) }
    }

    fun samplesReport(): String {
        val ui = state.value
        val header = "Argus ${if (ui.sampleSet != SampleSet.MAIN) "${ui.sampleSet} " else ""}samples · ${deviceInfo.manufacturer} ${deviceInfo.model} · " +
            "${BuildConfig.VERSION_NAME} · ${ui.engine.backend ?: "not loaded"} · ${settings.language}"
        return SampleReport.format(ui.sampleResults, header, ui.sampleMode)
    }

    fun onUnload() {
        viewModelScope.launch { engine.unload() }
    }

    fun onUseGpu(enabled: Boolean) {
        settings.useGpu = enabled
        local.update { it.copy(useGpu = enabled) }
        // The backend is picked at load time, so reload on the next ask
        viewModelScope.launch {
            engine.unload()
            // GPU off: its ~0.5 GB weight cache would only waste space
            if (!enabled) cache.deleteGpuCaches()
            onRefresh()
        }
    }

    fun report(): String {
        val ui = state.value
        return DeviceReport(
            appVersion = BuildConfig.VERSION_NAME,
            manufacturer = deviceInfo.manufacturer,
            model = deviceInfo.model,
            androidVersion = deviceInfo.androidVersion,
            sdkInt = deviceInfo.sdkInt,
            totalRamBytes = ui.specs.totalRamBytes,
            abis = deviceInfo.abis,
            support = ui.check.support,
            issues = ui.check.issues,
            backend = ui.engine.backend?.name,
            useGpu = ui.useGpu,
            gpuDisabled = ui.gpuDisabled,
            loadMillis = ui.engine.loadMillis,
            checkCount = ui.engine.checkCount,
            averageCheckMillis = ui.engine.averageCheckMillis,
        ).format()
    }

    private companion object {
        const val SYSTEM = "You are a helpful assistant. Answer in one or two short sentences."
        const val TEST_SENDER = "Argus test"
    }
}
