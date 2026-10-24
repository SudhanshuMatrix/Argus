package com.ajayinnovations.argus.ui.home

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.alert.AlertText
import com.ajayinnovations.argus.core.Language
import com.ajayinnovations.argus.core.ParentTitle
import com.ajayinnovations.argus.ui.components.BigButton
import com.ajayinnovations.argus.ui.components.Divider
import com.ajayinnovations.argus.ui.components.SecondaryButton
import com.ajayinnovations.argus.ui.components.SideBar
import com.ajayinnovations.argus.ui.theme.Alarm
import com.ajayinnovations.argus.ui.theme.Ink
import com.ajayinnovations.argus.ui.theme.Mist
import com.ajayinnovations.argus.ui.theme.Paper
import com.ajayinnovations.argus.ui.theme.Pine
import com.ajayinnovations.argus.ui.theme.ArgusTheme
import com.ajayinnovations.argus.ui.theme.Slate

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    settleIn: Boolean,
    onFix: (StatusKind) -> Unit,
    onCheck: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state, settleIn, onFix, onCheck)
}

/** The dashboard: status block, one divided row per need, and "Check a message". Tabs sit below. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    settleIn: Boolean,
    onFix: (StatusKind) -> Unit,
    onCheck: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Mist)) {
        StatusBlock(state, settleIn)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            state.rows.forEachIndexed { i, row ->
                if (i > 0) Divider()
                StatusRowView(row, onFix)
            }
            if (state.familySmsFailed) {
                Divider()
                SideBar(Alarm, Modifier.padding(vertical = 16.dp)) {
                    Text(stringResource(R.string.home_family_sms_failed), style = MaterialTheme.typography.bodyLarge, color = Alarm)
                }
            }
            Divider()
        }
        // The main action stays big and labelled, just above the tabs, in thumb reach
        BigButton(stringResource(R.string.home_check), onCheck, minHeight = 64.dp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
    }
}

/**
 * Full width, under the status bar: Pine "Mummy is protected", or Alarm "Argus is off"
 * with what to fix. The one motion moment: it settles in once after setup finishes, unless
 * the phone has animations turned off.
 */
@Composable
private fun StatusBlock(state: HomeUiState, settleIn: Boolean) {
    val context = LocalContext.current
    val motion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val animate = settleIn && motion
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) {
        if (animate) progress.animateTo(1f, tween(durationMillis = 400, easing = FastOutSlowInEasing))
    }
    val rise = with(LocalDensity.current) { 12.dp.toPx() }
    val off = state.rows.filterNot { it.ok || it.optional }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(if (off.isEmpty()) Pine else Alarm)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * rise
            },
    ) {
        if (off.isEmpty()) {
            val name = AlertText.titleName(context, state.parentTitle, state.parentName)
            Text(
                name?.let { stringResource(R.string.home_protected, it) } ?: stringResource(R.string.home_protected_plain),
                style = MaterialTheme.typography.displaySmall,
                color = Paper,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.home_protected_detail), style = MaterialTheme.typography.bodyLarge, color = Paper)
        } else {
            Text(
                stringResource(R.string.home_off_title),
                style = MaterialTheme.typography.displaySmall,
                color = Paper,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.home_off_fix, off.joinToString(", ") { context.getString(label(it.kind)) }),
                style = MaterialTheme.typography.bodyLarge,
                color = Paper,
            )
        }
    }
}

private fun label(kind: StatusKind): Int = when (kind) {
    StatusKind.MODEL -> R.string.status_label_model
    StatusKind.SMS -> R.string.status_label_sms
    StatusKind.NOTIFICATIONS -> R.string.status_label_notifications
    StatusKind.WHATSAPP -> R.string.status_label_whatsapp
    StatusKind.FULL_SCREEN -> R.string.status_label_full_screen
    StatusKind.OVERLAY -> R.string.status_label_overlay
    StatusKind.BATTERY -> R.string.status_label_battery
}

private fun offSentence(kind: StatusKind): Int = when (kind) {
    StatusKind.MODEL -> R.string.status_model_off
    StatusKind.SMS -> R.string.status_sms_off
    StatusKind.NOTIFICATIONS -> R.string.status_notifications_off
    StatusKind.WHATSAPP -> R.string.status_whatsapp_off
    StatusKind.FULL_SCREEN -> R.string.status_full_screen_off
    StatusKind.OVERLAY -> R.string.status_overlay_off
    StatusKind.BATTERY -> R.string.status_battery_off
}

/** "Text messages ... On". Off: Alarm side bar, the reason in words, and Fix it. */
@Composable
private fun StatusRowView(row: StatusRow, onFix: (StatusKind) -> Unit) {
    val name = stringResource(label(row.kind))
    if (row.ok) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.state_on), style = MaterialTheme.typography.labelLarge, color = Pine)
        }
    } else if (row.optional) {
        // Recommended only: calm Slate, "Turn on", never Alarm red
        SideBar(Slate, Modifier.padding(vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.state_off), style = MaterialTheme.typography.labelLarge, color = Slate)
            }
            Text(stringResource(offSentence(row.kind)), style = MaterialTheme.typography.bodyMedium, color = Slate)
            SecondaryButton(stringResource(R.string.home_turn_on), { onFix(row.kind) })
        }
    } else {
        SideBar(Alarm, Modifier.padding(vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.state_off), style = MaterialTheme.typography.labelLarge, color = Alarm)
            }
            Text(stringResource(offSentence(row.kind)), style = MaterialTheme.typography.bodyMedium, color = Slate)
            SecondaryButton(stringResource(R.string.home_fix), { onFix(row.kind) }, color = Alarm)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ArgusTheme {
        HomeScreen(
            HomeUiState(
                Language.ENGLISH, ParentTitle.MUMMY,
                rows = listOf(StatusRow(StatusKind.SMS, true), StatusRow(StatusKind.WHATSAPP, false)),
            ),
            settleIn = false, onFix = {}, onCheck = {},
        )
    }
}
