package com.ajayinnovations.argus.ui.about

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ajayinnovations.argus.BuildConfig
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.ui.components.Divider
import com.ajayinnovations.argus.ui.components.SecondaryButton
import com.ajayinnovations.argus.ui.components.SideBar
import com.ajayinnovations.argus.ui.theme.Ink
import com.ajayinnovations.argus.ui.theme.Mist
import com.ajayinnovations.argus.ui.theme.Pine
import com.ajayinnovations.argus.ui.theme.Slate

/**
 * Static text only, so no ViewModel: how it works, the privacy promise, credits, the version
 * and the licence notices. The full Gemma Terms (res/raw/gemma_terms.txt, also in NOTICE) open
 * on request: the Terms require giving every user a copy.
 */
@Composable
fun AboutScreen() {
    var showTerms by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Mist)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.about_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 16.dp).semantics { heading() })
        SideBar(Pine) {
            Text(stringResource(R.string.about_privacy_title), style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.about_offline), style = MaterialTheme.typography.bodyLarge, color = Ink)
            Text(stringResource(R.string.about_privacy), style = MaterialTheme.typography.bodyLarge, color = Ink)
        }
        Divider()
        Text(stringResource(R.string.about_how_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        listOf(R.string.about_how_read, R.string.about_how_rules, R.string.about_how_model, R.string.about_how_alert).forEach {
            Text(stringResource(it), style = MaterialTheme.typography.bodyLarge)
        }
        Divider()
        Text(stringResource(R.string.about_model), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.about_voice), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyLarge)
        Divider()
        Text(stringResource(R.string.about_licences), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.about_gemma), style = MaterialTheme.typography.bodyMedium, color = Slate)
        SecondaryButton(
            stringResource(if (showTerms) R.string.about_terms_hide else R.string.about_terms_show),
            { showTerms = !showTerms },
        )
        if (showTerms) GemmaTerms()
        Text(stringResource(R.string.about_font), style = MaterialTheme.typography.bodyMedium, color = Slate)
    }
}

/** The bundled terms, in English only: they are a legal text and must not be paraphrased. */
@Composable
private fun GemmaTerms() {
    val context = LocalContext.current
    val terms = remember {
        runCatching { context.resources.openRawResource(R.raw.gemma_terms).bufferedReader().use { it.readText() } }
            .getOrDefault("")
    }
    Text(terms, style = MaterialTheme.typography.bodyMedium, color = Ink)
}
