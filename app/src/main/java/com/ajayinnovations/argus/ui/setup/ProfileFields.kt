package com.ajayinnovations.argus.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ajayinnovations.argus.R
import com.ajayinnovations.argus.core.Language
import com.ajayinnovations.argus.core.ParentTitle
import com.ajayinnovations.argus.core.ProfileForm
import com.ajayinnovations.argus.ui.components.ChoiceRow
import com.ajayinnovations.argus.ui.components.Divider
import com.ajayinnovations.argus.ui.theme.Alarm
import com.ajayinnovations.argus.ui.theme.Edge
import com.ajayinnovations.argus.ui.theme.Ink
import com.ajayinnovations.argus.ui.theme.Paper
import com.ajayinnovations.argus.ui.theme.Pine
import com.ajayinnovations.argus.ui.theme.Slate

/** Who is this for: title, language, family name and number, family alerts. Wizard and settings. */
@Composable
fun ProfileFields(
    form: ProfileForm,
    language: Language,
    showProblems: Boolean,
    onForm: (ProfileForm) -> Unit,
    onLanguage: (Language) -> Unit,
) {
    val problems = if (showProblems) form.problems() else emptyList()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Section(R.string.setup_who_call_them)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.selectableGroup()) {
            listOf(
                ParentTitle.MUMMY to R.string.title_mummy,
                ParentTitle.DADDY to R.string.title_daddy,
                ParentTitle.GRANDMA to R.string.title_grandma,
            ).forEach { (title, label) ->
                ChoiceRow(stringResource(label), selected = !form.usesCustomName && form.title == title) {
                    onForm(form.copy(title = title, usesCustomName = false))
                }
            }
            ChoiceRow(stringResource(R.string.title_their_name), selected = form.usesCustomName) {
                onForm(form.copy(title = null, usesCustomName = true))
            }
        }
        if (form.usesCustomName) {
            Field(form.customName, R.string.setup_who_custom_name, KeyboardType.Text) { onForm(form.copy(customName = it)) }
        }
        Problem(problems, ProfileForm.Problem.NO_TITLE, R.string.problem_no_title)
        Problem(problems, ProfileForm.Problem.NO_CUSTOM_NAME, R.string.problem_no_custom_name)

        Divider()
        Section(R.string.setup_who_language)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.selectableGroup()) {
            ChoiceRow(stringResource(R.string.language_english), selected = language == Language.ENGLISH) { onLanguage(Language.ENGLISH) }
            ChoiceRow(stringResource(R.string.language_hindi), selected = language == Language.HINDI) { onLanguage(Language.HINDI) }
        }

        Divider()
        Field(form.familyName, R.string.setup_who_family_name, KeyboardType.Text) { onForm(form.copy(familyName = it)) }
        Problem(problems, ProfileForm.Problem.NO_FAMILY_NAME, R.string.problem_no_family_name)
        Field(form.familyNumber, R.string.setup_who_family_number, KeyboardType.Phone) { onForm(form.copy(familyNumber = it)) }
        Problem(problems, ProfileForm.Problem.BAD_NUMBER, R.string.problem_bad_number)

        SwitchRow(
            title = stringResource(R.string.setup_who_family_alerts),
            hint = stringResource(R.string.setup_who_family_alerts_hint),
            checked = form.familyAlerts,
            onChange = { onForm(form.copy(familyAlerts = it)) },
        )
    }
}

@Composable
private fun Section(text: Int) =
    Text(stringResource(text), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp).semantics { heading() })

/** A label, an optional hint and a large switch; Pine when on. */
@Composable
fun SwitchRow(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Ink)
            hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Slate) }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Pine, uncheckedBorderColor = Edge, uncheckedThumbColor = Edge),
        )
    }
}

@Composable
private fun Field(value: String, label: Int, type: KeyboardType, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label), style = MaterialTheme.typography.bodyMedium) },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Paper, unfocusedContainerColor = Paper,
            focusedBorderColor = Ink, unfocusedBorderColor = Edge,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = type, capitalization = KeyboardCapitalization.Words),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Problem(problems: List<ProfileForm.Problem>, problem: ProfileForm.Problem, text: Int) {
    if (problem in problems) Text(stringResource(text), color = Alarm, style = MaterialTheme.typography.bodyLarge)
}
