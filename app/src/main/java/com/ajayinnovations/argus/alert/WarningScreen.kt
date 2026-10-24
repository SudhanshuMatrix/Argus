package com.ajayinnovations.argus.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.core.AccountTrouble
import com.ajayinnovations.argus.core.Finding
import com.ajayinnovations.argus.core.Highlights
import com.ajayinnovations.argus.core.Level
import com.ajayinnovations.argus.core.ParentTitle
import com.ajayinnovations.argus.core.ReasonCode
import com.ajayinnovations.argus.core.SourceType
import com.ajayinnovations.argus.ui.components.BigButton
import com.ajayinnovations.argus.ui.components.ChatBubble
import com.ajayinnovations.argus.ui.components.SecondaryButton
import com.ajayinnovations.argus.ui.theme.Alarm
import com.ajayinnovations.argus.ui.theme.Amber
import com.ajayinnovations.argus.ui.theme.Ink
import com.ajayinnovations.argus.ui.theme.Mist
import com.ajayinnovations.argus.ui.theme.Paper
import com.ajayinnovations.argus.ui.theme.Pine
import com.ajayinnovations.argus.ui.theme.ArgusTheme
import com.ajayinnovations.argus.ui.theme.Signal

/** The band colour per level, and the text on it; every pair is 8:1 or better. */
data class LevelColors(val background: Color, val content: Color)

fun levelColors(level: Level): LevelColors = when (level) {
    Level.SCAM -> LevelColors(Alarm, Paper)
    Level.CAREFUL -> LevelColors(Amber, Ink)
    Level.OTP -> LevelColors(Signal, Paper)
    Level.NONE -> LevelColors(Pine, Paper)
}

@Composable
fun WarningRoute(viewModel: WarningViewModel, onCall: () -> Unit, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WarningScreen(state, onCall = onCall, onHearAgain = viewModel::onHearAgain, onDone = onDone)
}

/**
 * A coloured band with the headline, then calm Mist: who sent it, the message as a chat
 * bubble with the dangerous parts marked, why, and what to do. Buttons at the bottom.
 */
@Composable
fun WarningScreen(state: WarningUiState, onCall: () -> Unit, onHearAgain: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val band = levelColors(state.level)
    Column(Modifier.fillMaxSize().background(Mist)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(band.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Text(
                AlertText.headline(context, state.level, state.parentTitle, state.parentName),
                style = MaterialTheme.typography.displaySmall,
                color = band.content,
                modifier = Modifier.semantics { heading() },
            )
            // Several scams waiting: one screen, the newest first, never a stack of screens
            if (state.pendingCount > 1) {
                Text(stringResource(R.string.warning_count, state.pendingCount), style = MaterialTheme.typography.titleLarge, color = band.content)
            }
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // The whole message, inside this scrolling column; ChatBubble never makes links clickable
            val shown = state.message ?: state.preview
            val bubble: @Composable () -> Unit = {
                shown?.let { text ->
                    ChatBubble(
                        text = text,
                        marks = if (state.level == Level.OTP) emptyList() else state.marks,
                        from = AlertText.from(context, state.source, state.sender),
                    )
                }
            }
            val why: @Composable () -> Unit = {
                AlertText.reason(context, state.findings, state.reasonCode, state.modelReason)?.let {
                    Text(it, style = MaterialTheme.typography.titleLarge, color = Ink)
                }
                // The level's generic line only when there is no specific advice, so the buttons stay near
                val advice = AlertText.advice(context, state.reasonCode, state.modelAdvice)
                Text(advice ?: AlertText.body(context, state.level, state.familyName), style = MaterialTheme.typography.bodyLarge, color = Ink)
            }
            // With very large text or a long message, why and what to do come first, so they show without scrolling
            if (LocalDensity.current.fontScale > LARGE_TEXT || (shown?.length ?: 0) > LONG_MESSAGE) {
                why()
                bubble()
            } else {
                bubble()
                why()
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        ) {
            val callLabel = if (state.familyName.isBlank()) {
                stringResource(R.string.warning_call_family)
            } else {
                stringResource(R.string.warning_call, state.familyName)
            }
            BigButton(callLabel, onCall, minHeight = 64.dp)
            SecondaryButton(stringResource(R.string.warning_hear_again), onHearAgain)
            SecondaryButton(stringResource(R.string.warning_understand), onDone)
        }
    }
}

/** Above this font scale (Android's 130%+), the reason moves above the message. */
private const val LARGE_TEXT = 1.3f

/** Longer than the old preview: the reason goes first so it isn't pushed below the fold. */
private const val LONG_MESSAGE = 200

@Preview
@Composable
private fun WarningScreenPreview() {
    val preview = "Dear customer, your BVN is blocked. Click bit.ly/x1abc to update today"
    val findings = listOf(Finding.Link, Finding.AccountProblem(AccountTrouble.BLOCKED))
    ArgusTheme {
        WarningScreen(
            WarningUiState(
                Level.SCAM, generic = false, source = SourceType.SMS, sender = "GTBank",
                preview = preview, findings = findings, marks = Highlights.find(preview, findings),
                reasonCode = ReasonCode.ACCOUNT_PROBLEM, parentTitle = ParentTitle.MUMMY, familyName = "Abraham",
            ),
            onCall = {}, onHearAgain = {}, onDone = {},
        )
    }
}
