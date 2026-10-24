package com.ajayinnovations.argus.debug

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.core.DeviceCheck
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.device.DeviceCheckText
import com.ajayinnovations.argus.ui.components.BigButton
import java.util.Locale

@Composable
fun DebugRoute(viewModel: DebugViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val copied = stringResource(R.string.debug_report_copied)
    val resultsCopied = stringResource(R.string.debug_results_copied)
    fun copy(label: String, text: String, toast: String) {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
    }
    val alerts = AlertActions(onTest = viewModel::onTestAlert)
    DebugScreen(
        state = state,
        alerts = alerts,
        onInput = viewModel::onInput,
        onAsk = viewModel::onAsk,
        onUnload = viewModel::onUnload,
        onRefresh = viewModel::onRefresh,
        onUseGpu = viewModel::onUseGpu,
        onCopyReport = { copy("Argus device report", viewModel.report(), copied) },
        onRunSamples = { viewModel.onRunSamples(SampleSet.MAIN) },
        onRunHoldout = { viewModel.onRunSamples(SampleSet.HOLDOUT) },
        onRunHoldout2 = { viewModel.onRunSamples(SampleSet.HOLDOUT2) },
        onNextSampleMode = viewModel::onNextSampleMode,
        onCopyResults = { copy("Argus sample results", viewModel.samplesReport(), resultsCopied) },
    )
}

@Composable
fun DebugScreen(
    state: DebugUiState,
    alerts: AlertActions,
    onInput: (String) -> Unit,
    onAsk: () -> Unit,
    onUnload: () -> Unit,
    onRefresh: () -> Unit,
    onUseGpu: (Boolean) -> Unit,
    onCopyReport: () -> Unit,
    onRunSamples: () -> Unit,
    onRunHoldout: () -> Unit,
    onRunHoldout2: () -> Unit,
    onNextSampleMode: () -> Unit,
    onCopyResults: () -> Unit,
) {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.headlineMedium)

            Section(stringResource(R.string.debug_device))
            Text(stringResource(DeviceCheckText.headline(state.check.support)), style = MaterialTheme.typography.bodyLarge)
            state.check.issues.forEach {
                Text("• " + DeviceCheckText.reason(context, it, state.specs), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "RAM ${gb(state.specs.totalRamBytes)} · free ${gb(state.specs.freeStorageBytes)} · " +
                    "64-bit ${if (state.specs.supportsArm64) "yes" else "no"} · SDK ${state.specs.sdkInt}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Section(stringResource(R.string.debug_model))
            val engine = state.engine
            Text("File: ${state.modelStatus}", style = MaterialTheme.typography.bodyMedium)
            Text(
                when {
                    engine.loading -> "Engine: loading…"
                    engine.backend != null -> "Engine: loaded on ${engine.backend}"
                    else -> "Engine: not loaded"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Last load: ${engine.loadMillis?.let { seconds(it / 1000.0) } ?: "–"} · " +
                    "checks ${engine.checkCount}, average ${engine.averageCheckMillis?.let { seconds(it / 1000.0) } ?: "–"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.debug_use_gpu),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = state.useGpu, onCheckedChange = onUseGpu)
            }
            if (state.gpuDisabled) {
                Text(stringResource(R.string.debug_gpu_disabled), color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = state.input,
                onValueChange = onInput,
                label = { Text(stringResource(R.string.debug_input_label)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            BigButton(
                text = stringResource(if (state.busy) R.string.debug_asking else R.string.debug_ask),
                onClick = onAsk,
            )
            state.replySeconds?.let { Text("Took ${seconds(it)}", style = MaterialTheme.typography.titleLarge) }
            state.reply?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge) }

            IngestSection(state)
            AlertsSection(state, alerts)
            SamplesSection(state, onRunSamples, onRunHoldout, onRunHoldout2, onNextSampleMode, onCopyResults)

            HorizontalDivider()
            BigButton(text = stringResource(R.string.debug_unload), onClick = onUnload)
            BigButton(text = stringResource(R.string.debug_copy_report), onClick = onCopyReport)
            BigButton(text = stringResource(R.string.debug_refresh), onClick = onRefresh)
        }
    }
}

@Composable
private fun SamplesSection(
    state: DebugUiState,
    onRunSamples: () -> Unit,
    onRunHoldout: () -> Unit,
    onRunHoldout2: () -> Unit,
    onNextSampleMode: () -> Unit,
    onCopyResults: () -> Unit,
) {
    Section(stringResource(R.string.debug_samples))
    BigButton(text = stringResource(R.string.debug_sample_mode, state.sampleMode.label), onClick = onNextSampleMode)
    val done = state.sampleResults.size
    BigButton(
        text = if (state.samplesRunning) {
            stringResource(R.string.debug_samples_running, done + 1, state.samplesTotal)
        } else {
            stringResource(R.string.debug_run_samples)
        },
        onClick = onRunSamples,
    )
    if (state.holdoutCount > 0) {
        BigButton(text = stringResource(R.string.debug_run_holdout, state.holdoutCount), onClick = onRunHoldout)
    } else {
        Text(stringResource(R.string.debug_no_holdout), style = MaterialTheme.typography.bodyMedium)
    }
    if (state.holdout2Count > 0) {
        BigButton(text = stringResource(R.string.debug_run_holdout2, state.holdout2Count), onClick = onRunHoldout2)
    }
    if (done == 0) return
    if (state.sampleSet != SampleSet.MAIN) {
        Text(stringResource(R.string.debug_holdout_results), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
    }
    val summary = state.sampleSummary
    Text("Missed scams: ${summary.missedScams}", style = MaterialTheme.typography.titleLarge)
    Text("Loud false alarms: ${summary.loudFalseAlarms}", style = MaterialTheme.typography.titleLarge)
    Text("Quiet cautions: ${summary.quietCautions}", style = MaterialTheme.typography.titleLarge)
    Text(
        "Average ${SampleReport.seconds(summary.averageMillis)} · slowest " +
            (summary.slowest?.let { "${it.file} ${SampleReport.seconds(it.millis)}" } ?: "–"),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        "Skipped by the skip filter: " + summary.skipped.ifEmpty { listOf("none") }.joinToString(),
        style = MaterialTheme.typography.bodyMedium,
    )
    state.sampleResults.forEach { r ->
        HorizontalDivider()
        Text(
            "${r.outcome.label} · ${r.file}",
            style = MaterialTheme.typography.titleLarge,
            color = if (r.outcome == Outcome.OK) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
        Text(
            "Expected ${r.expected} · got ${r.level} by ${r.decidedBy}${r.answers?.let { " · $it" } ?: ""} · ${SampleReport.seconds(r.millis)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (r.reason.isNotBlank()) Text(r.reason, style = MaterialTheme.typography.bodyMedium)
        r.replies.forEach { reply ->
            Text("Raw: $reply", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
    if (!state.samplesRunning) BigButton(text = stringResource(R.string.debug_copy_results), onClick = onCopyResults)
}

@Composable
private fun IngestSection(state: DebugUiState) {
    Section(stringResource(R.string.debug_ingest))
    fun yesNo(b: Boolean) = if (b) "yes" else "NO"
    Text("Listener connected: ${yesNo(state.listenerConnected)}", style = MaterialTheme.typography.bodyLarge)
    Text("SMS permission: ${yesNo(state.smsPermission)}", style = MaterialTheme.typography.bodyLarge)
    Text("Notification access: ${yesNo(state.notificationAccess)}", style = MaterialTheme.typography.bodyLarge)
    val s = state.ingest
    Text("Since start: ${s.received} received · ${s.checked} checked · ${s.skipped} skipped", style = MaterialTheme.typography.bodyLarge)
    Text(
        "Skipped: ${s.duplicates} duplicate · ${s.notReady} not set up · ${s.skippedByFilter} harmless · ${s.dropped} dropped · failed ${s.failed}",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text("Results: " + Level.entries.joinToString(" · ") { "${s.levels[it] ?: 0} $it" }, style = MaterialTheme.typography.bodyMedium)
    Text(
        "Last check: " + (s.last?.let { "${it.type} ${it.level} by ${it.decidedBy} · ${seconds(it.millis / 1000.0)}" } ?: "–"),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text("Setup finished: ${yesNo(state.setupComplete)} (set by the wizard)", style = MaterialTheme.typography.bodyLarge)
}

class AlertActions(val onTest: (Level) -> Unit)

@Composable
private fun AlertsSection(state: DebugUiState, actions: AlertActions) {
    Section(stringResource(R.string.debug_alerts))
    fun yesNo(b: Boolean) = if (b) "yes" else "NO"
    Text("Notifications allowed: ${yesNo(state.notificationsAllowed)}", style = MaterialTheme.typography.bodyLarge)
    Text("Full-screen alerts allowed: ${yesNo(state.fullScreenAllowed)}", style = MaterialTheme.typography.bodyLarge)
    Text("Send SMS allowed: ${yesNo(state.sendSmsAllowed)}", style = MaterialTheme.typography.bodyLarge)
    BigButton(text = stringResource(R.string.debug_test_scam), onClick = { actions.onTest(Level.SCAM) })
    BigButton(text = stringResource(R.string.debug_test_careful), onClick = { actions.onTest(Level.CAREFUL) })
    BigButton(text = stringResource(R.string.debug_test_otp), onClick = { actions.onTest(Level.OTP) })

}

@Composable
private fun Section(title: String) {
    HorizontalDivider()
    Text(title, style = MaterialTheme.typography.titleLarge)
}

private fun gb(bytes: Long) = String.format(Locale.US, "%.1f GB", DeviceCheck.gib(bytes))
private fun seconds(value: Double) = String.format(Locale.US, "%.1f s", value)
