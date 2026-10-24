package com.ajayinnovations.argus.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.ajayinnovations.argus.core.Maker
import com.ajayinnovations.argus.core.ParentTitle
import com.ajayinnovations.argus.core.SetupStep
import com.ajayinnovations.argus.core.Support
import com.ajayinnovations.argus.device.DeviceCheckText
import com.ajayinnovations.argus.ui.components.BigButton
import com.ajayinnovations.argus.ui.components.ImageSlot
import com.ajayinnovations.argus.ui.components.ImageSlots
import com.ajayinnovations.argus.ui.components.SecondaryButton
import com.ajayinnovations.argus.ui.components.SegmentedProgress
import com.ajayinnovations.argus.ui.components.SideBar
import com.ajayinnovations.argus.ui.theme.Alarm
import com.ajayinnovations.argus.ui.theme.Ink
import com.ajayinnovations.argus.ui.theme.Line
import com.ajayinnovations.argus.ui.theme.Mist
import com.ajayinnovations.argus.ui.theme.Pine
import com.ajayinnovations.argus.ui.theme.Slate

/** What the wizard asks Android for; MainActivity does it, then the wizard re-checks on resume. */
class SetupActions(
    val requestSms: (includeSend: Boolean) -> Unit,
    val requestNotifications: () -> Unit,
    val openNotificationAccess: () -> Unit,
    val openAppInfo: () -> Unit,
    val openFullScreenSettings: () -> Unit,
    val openOverlaySettings: () -> Unit,
    val requestBatteryExemption: () -> Unit,
)

@Composable
fun SetupRoute(viewModel: SetupViewModel, actions: SetupActions) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SetupScreen(state, viewModel, actions)
}

@Composable
private fun SetupScreen(state: SetupUiState, vm: SetupViewModel, actions: SetupActions) {
    val step = state.step ?: return
    when (step) {
        SetupStep.PHONE_CHECK -> PhoneCheck(state, vm)
        SetupStep.GETTING_READY -> GettingReady(state, vm)
        SetupStep.WHO -> Who(state, vm)
        SetupStep.SMS -> Simple(
            state, R.string.setup_sms_title,
            listOfNotNull(
                stringResource(R.string.setup_sms_why),
                if (state.facts?.familyAlerts == true) stringResource(R.string.setup_sms_why_send) else null,
            ),
            R.string.setup_sms_button,
        ) { actions.requestSms(state.facts?.familyAlerts == true) }
        SetupStep.NOTIFICATIONS -> Simple(
            state, R.string.setup_notifications_title, listOf(stringResource(R.string.setup_notifications_why)),
            R.string.setup_notifications_button, actions.requestNotifications,
        )
        SetupStep.NOTIFICATION_ACCESS -> Simple(
            state, R.string.setup_access_title, listOf(stringResource(R.string.setup_access_why)), R.string.setup_open_settings,
        ) {
            vm.onNotificationAccessOpened()
            actions.openNotificationAccess()
        }
        SetupStep.RESTRICTED_SETTINGS -> Restricted(state, actions)
        SetupStep.FULL_SCREEN -> Simple(
            state, R.string.setup_full_screen_title, listOf(stringResource(R.string.setup_full_screen_why)),
            R.string.setup_open_settings, actions.openFullScreenSettings,
        )
        SetupStep.OVERLAY -> Overlay(state, vm, actions)
        SetupStep.BATTERY -> Simple(
            state, R.string.setup_battery_title, listOf(stringResource(R.string.setup_battery_why)),
            R.string.setup_battery_button, actions.requestBatteryExemption,
        )
        SetupStep.MAKER_EXTRA -> MakerExtra(state, vm, actions)
        SetupStep.TEST -> TestIt(state, vm)
    }
}

/**
 * Every step: "Step X of N" and the segmented bar, a title, the content, and the step's
 * buttons pinned at the bottom in thumb reach. A done step shows "Done" in Pine instead.
 */
@Composable
private fun StepLayout(
    state: SetupUiState,
    title: Int,
    buttons: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Mist).safeDrawingPadding()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.total > 0) {
                Text(stringResource(R.string.setup_step, state.number, state.total), style = MaterialTheme.typography.bodyMedium, color = Slate)
                SegmentedProgress(state.number - if (state.stepDone) 0 else 1, state.total)
            }
            Text(stringResource(title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            content()
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp, top = 8.dp),
            content = buttons,
        )
    }
}

@Composable
private fun Body(text: String) = Text(text, style = MaterialTheme.typography.bodyLarge, color = Ink)

/** This step is done: Pine words beside a Pine bar. The wizard moves on by itself a moment later. */
@Composable
private fun DoneBanner(text: String = stringResource(R.string.setup_done)) {
    SideBar(Pine, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(text, style = MaterialTheme.typography.headlineMedium, color = Pine)
    }
}

/** Title, a few lines, and one big button; "Done" when done. */
@Composable
private fun Simple(state: SetupUiState, title: Int, lines: List<String>, button: Int, onClick: () -> Unit) {
    StepLayout(state, title, buttons = { if (state.stepDone) DoneBanner() else BigButton(stringResource(button), onClick, minHeight = 64.dp) }) {
        lines.forEach { Body(it) }
    }
}

/** Recommended but skippable: one plain line, "Open settings", and "Not now". */
@Composable
private fun Overlay(state: SetupUiState, vm: SetupViewModel, actions: SetupActions) {
    StepLayout(
        state, R.string.setup_overlay_title,
        buttons = {
            if (state.stepDone) {
                DoneBanner()
            } else {
                BigButton(stringResource(R.string.setup_open_settings), actions.openOverlaySettings, minHeight = 64.dp)
                SecondaryButton(stringResource(R.string.setup_overlay_skip), vm::onOverlaySkipped)
            }
        },
    ) {
        Body(stringResource(R.string.setup_overlay_why))
    }
}

@Composable
private fun PhoneCheck(state: SetupUiState, vm: SetupViewModel) {
    val context = LocalContext.current
    val check = state.deviceCheck ?: return
    val specs = state.specs ?: return
    val blocked = check.support == Support.NOT_SUPPORTED
    StepLayout(
        state, R.string.setup_phone_title,
        buttons = {
            when {
                blocked -> Unit
                state.stepDone -> DoneBanner()
                else -> BigButton(stringResource(R.string.setup_next), vm::onPhoneCheckContinue, minHeight = 64.dp)
            }
        },
    ) {
        SideBar(if (blocked) Alarm else Pine) {
            Text(stringResource(DeviceCheckText.headline(check.support)), style = MaterialTheme.typography.titleLarge, color = Ink)
            check.issues.forEach { Body(DeviceCheckText.reason(context, it, specs)) }
        }
        if (blocked) Body(stringResource(R.string.setup_phone_not_supported_body))
    }
}

@Composable
private fun GettingReady(state: SetupUiState, vm: SetupViewModel) {
    val error = state.readyError
    StepLayout(
        state, R.string.setup_ready_title,
        buttons = {
            when {
                state.stepDone -> DoneBanner(stringResource(R.string.setup_ready_done))
                error != null -> BigButton(stringResource(R.string.setup_try_again), vm::onRetryGettingReady, minHeight = 64.dp)
            }
        },
    ) {
        when {
            state.stepDone -> Unit
            error != null -> SideBar(Alarm) {
                Body(
                    when (error) {
                        is ReadyError.NoSpace -> stringResource(R.string.setup_ready_no_space, DeviceCheckText.gb(error.freeUpGb))
                        ReadyError.Missing -> stringResource(R.string.setup_ready_missing)
                        ReadyError.Failed -> stringResource(R.string.setup_ready_failed)
                    },
                )
            }
            state.readyPhase == ReadyPhase.CHECKING -> {
                Text(stringResource(R.string.setup_ready_checking), style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(color = Pine, trackColor = Line, modifier = Modifier.fillMaxWidth().height(8.dp))
                Body(stringResource(R.string.setup_ready_keep_open))
            }
            else -> {
                val percent = (state.readyProgress * 100).toInt().coerceIn(0, 100)
                Text(stringResource(R.string.setup_ready_progress, percent), style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(
                    progress = { state.readyProgress }, color = Pine, trackColor = Line,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Body(stringResource(R.string.setup_ready_keep_open))
            }
        }
    }
}

@Composable
private fun Who(state: SetupUiState, vm: SetupViewModel) {
    StepLayout(
        state, R.string.setup_who_title,
        buttons = { if (state.stepDone) DoneBanner() else BigButton(stringResource(R.string.setup_next), vm::onWhoNext, minHeight = 64.dp) },
    ) {
        ProfileFields(state.form, state.language, state.showFormProblems, vm::onForm, vm::onLanguage)
    }
}

@Composable
private fun Restricted(state: SetupUiState, actions: SetupActions) {
    StepLayout(
        state, R.string.setup_restricted_title,
        buttons = {
            if (state.stepDone) {
                DoneBanner()
            } else {
                SecondaryButton(stringResource(R.string.setup_open_app_info), actions.openAppInfo)
                BigButton(stringResource(R.string.setup_restricted_access), actions.openNotificationAccess, minHeight = 64.dp)
            }
        },
    ) {
        Body(stringResource(R.string.setup_restricted_why))
        listOf(R.string.setup_restricted_step1, R.string.setup_restricted_step2, R.string.setup_restricted_step3, R.string.setup_restricted_step4)
            .forEach { Body(stringResource(it)) }
        ImageSlot(ImageSlots.RESTRICTED_SETTINGS, stringResource(R.string.setup_restricted_step3))
    }
}

@Composable
private fun MakerExtra(state: SetupUiState, vm: SetupViewModel, actions: SetupActions) {
    StepLayout(
        state, R.string.setup_maker_title,
        buttons = {
            if (state.stepDone) {
                DoneBanner()
            } else {
                SecondaryButton(stringResource(R.string.setup_open_app_info), actions.openAppInfo)
                BigButton(stringResource(R.string.setup_maker_done), vm::onMakerDone, minHeight = 64.dp)
            }
        },
    ) {
        if (state.facts?.maker == Maker.XIAOMI) {
            Body(stringResource(R.string.setup_maker_xiaomi))
            Body(stringResource(R.string.setup_maker_xiaomi_step1))
            ImageSlot(ImageSlots.XIAOMI_AUTOSTART, stringResource(R.string.setup_maker_xiaomi_step1))
            Body(stringResource(R.string.setup_maker_xiaomi_step2))
            Body(stringResource(R.string.setup_maker_xiaomi_step3))
            ImageSlot(ImageSlots.XIAOMI_OTHER_PERMISSIONS, stringResource(R.string.setup_maker_xiaomi_step2))
        } else {
            Body(stringResource(R.string.setup_maker_transsion))
            Body(stringResource(R.string.setup_maker_transsion_step1))
            Body(stringResource(R.string.setup_maker_transsion_step2))
            ImageSlot(ImageSlots.TRANSSION_AUTOSTART, stringResource(R.string.setup_maker_transsion_step2))
        }
        Body(stringResource(R.string.setup_maker_back))
    }
}

@Composable
private fun TestIt(state: SetupUiState, vm: SetupViewModel) {
    val context = LocalContext.current
    val message = stringResource(R.string.setup_test_message)
    val sender = stringResource(R.string.setup_test_sender)
    val send = { vm.onSendTest(message, sender) }
    StepLayout(
        state, R.string.setup_test_title,
        buttons = {
            if (state.stepDone) {
                BigButton(stringResource(R.string.setup_finish), vm::onFinish, minHeight = 64.dp)
                SecondaryButton(stringResource(if (state.testSending) R.string.setup_test_sending else R.string.setup_test_again), send)
            } else {
                BigButton(stringResource(if (state.testSending) R.string.setup_test_sending else R.string.setup_test_button), send, minHeight = 64.dp)
            }
        },
    ) {
        Body(stringResource(R.string.setup_test_why))
        if (state.facts?.familyAlerts == true) {
            Body(stringResource(R.string.setup_test_family_note, AlertText.familyName(context, state.form.familyName)))
        }
        if (state.testFailed) SideBar(Alarm) { Body(stringResource(R.string.setup_test_failed)) }
        if (state.stepDone) DoneBanner(protecting(context, state.form.savedTitle, state.form.savedCustomName))
    }
}

/** "Argus is protecting Mummy". */
fun protecting(context: android.content.Context, title: ParentTitle, customName: String): String =
    AlertText.titleName(context, title, customName)?.let { context.getString(R.string.protecting, it) }
        ?: context.getString(R.string.protecting_plain)
