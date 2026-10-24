package com.ajayinnovations.argus.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.ui.components.BigButton
import com.ajayinnovations.argus.ui.components.Divider
import com.ajayinnovations.argus.ui.components.SideBar
import com.ajayinnovations.argus.ui.setup.ProfileFields
import com.ajayinnovations.argus.ui.setup.SwitchRow
import com.ajayinnovations.argus.ui.theme.Mist
import com.ajayinnovations.argus.ui.theme.Pine

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, onSaved: (familyAlerts: Boolean) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Mist)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 16.dp).semantics { heading() })
        ProfileFields(state.form, state.language, state.showProblems, viewModel::onForm, viewModel::onLanguage)
        Divider()
        SwitchRow(stringResource(R.string.settings_speak_on_silent), null, state.speakOnSilent, viewModel::onSpeakOnSilent)
        if (state.saved) {
            SideBar(Pine, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Text(stringResource(R.string.settings_saved), style = MaterialTheme.typography.titleLarge, color = Pine)
            }
        }
        BigButton(stringResource(R.string.settings_save), { if (viewModel.onSave()) onSaved(state.form.familyAlerts) }, minHeight = 64.dp)
    }
}
