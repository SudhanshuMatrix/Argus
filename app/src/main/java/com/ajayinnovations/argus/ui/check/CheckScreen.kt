package com.ajayinnovations.argus.ui.check

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.alert.AlertText
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.ui.components.BigButton
import com.ajayinnovations.argus.ui.components.ChatBubble
import com.ajayinnovations.argus.ui.components.SideBar
import com.ajayinnovations.argus.ui.theme.Alarm
import com.ajayinnovations.argus.ui.theme.Amber
import com.ajayinnovations.argus.ui.theme.Edge
import com.ajayinnovations.argus.ui.theme.Ink
import com.ajayinnovations.argus.ui.theme.Mist
import com.ajayinnovations.argus.ui.theme.Paper
import com.ajayinnovations.argus.ui.theme.Pine
import com.ajayinnovations.argus.ui.theme.Signal
import com.ajayinnovations.argus.ui.theme.Slate

@Composable
fun CheckRoute(viewModel: CheckViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CheckScreen(state, onInput = viewModel::onInput, onCheck = viewModel::onCheck)
}

/** Paste box, one Check button, and the result: side bar in the level colour, bubble, reason. */
@Composable
fun CheckScreen(state: CheckUiState, onInput: (String) -> Unit, onCheck: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Mist)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.check_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 16.dp).semantics { heading() })
        when {
            state.checking -> SideBar(Edge) {
                Text(stringResource(R.string.check_checking), style = MaterialTheme.typography.titleLarge, color = Ink)
            }
            state.outcome != null -> Outcome(state.outcome, state)
            else -> Text(stringResource(R.string.check_intro), style = MaterialTheme.typography.bodyLarge, color = Slate)
        }
        OutlinedTextField(
            value = state.input,
            onValueChange = onInput,
            label = { Text(stringResource(R.string.check_hint), style = MaterialTheme.typography.bodyMedium) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Paper, unfocusedContainerColor = Paper,
                focusedBorderColor = Ink, unfocusedBorderColor = Edge,
            ),
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
        )
        BigButton(
            text = stringResource(if (state.checking) R.string.check_checking else R.string.check_button),
            onClick = onCheck,
            enabled = !state.checking && state.input.isNotBlank(),
            minHeight = 64.dp,
        )
    }
}

private fun barColor(level: Level): Color = when (level) {
    Level.SCAM -> Alarm
    Level.CAREFUL -> Amber
    Level.OTP -> Signal
    Level.NONE -> Pine
}

@Composable
private fun Outcome(outcome: CheckOutcome, state: CheckUiState) {
    val context = LocalContext.current
    val live = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    when (outcome) {
        CheckOutcome.NotReady -> SideBar(Edge, live) {
            Text(stringResource(R.string.check_not_ready), style = MaterialTheme.typography.titleLarge, color = Ink)
        }
        CheckOutcome.Failed -> SideBar(Edge, live) {
            Text(
                stringResource(R.string.check_failed, AlertText.familyName(context, state.familyName)),
                style = MaterialTheme.typography.titleLarge, color = Ink,
            )
        }
        is CheckOutcome.Result -> SideBar(barColor(outcome.level), live) {
            Text(
                AlertText.headline(context, outcome.level, state.parentTitle, state.parentName),
                style = MaterialTheme.typography.headlineMedium,
                color = if (outcome.level == Level.NONE) Pine else Ink,
            )
            if (outcome.level == Level.NONE) {
                // Never the model's words here: they might say "safe"
                Text(AlertText.body(context, outcome.level, state.familyName), style = MaterialTheme.typography.bodyLarge)
            } else {
                ChatBubble(outcome.preview, if (outcome.level == Level.OTP) emptyList() else outcome.marks, from = null)
                AlertText.reason(context, outcome.findings, outcome.reasonCode, outcome.modelReason)?.let {
                    Text(it, style = MaterialTheme.typography.titleLarge, color = Ink)
                }
                Text(
                    AlertText.advice(context, outcome.reasonCode, outcome.modelAdvice) ?: AlertText.body(context, outcome.level, state.familyName),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
