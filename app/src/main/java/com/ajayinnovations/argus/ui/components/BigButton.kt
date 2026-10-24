package com.ajayinnovations.argus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ajayinnovations.argus.ui.theme.Ink
import com.ajayinnovations.argus.ui.theme.Line
import com.ajayinnovations.argus.ui.theme.Paper
import com.ajayinnovations.argus.ui.theme.Slate

/** The one main action on a screen: filled Ink, full width, 60dp+, 12dp corners. Words, never icons. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 60.dp,
    enabled: Boolean = true,
    containerColor: Color = Ink,
    contentColor: Color = Paper,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Line,
            disabledContentColor = Slate,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Every other action: Paper with a 2dp edge in the text colour. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Ink,
    background: Color = Paper,
    minHeight: Dp = 60.dp,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(2.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = background, contentColor = color),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/**
 * Quiet navigation ("Settings", "About", "Back"): underlined words that line up with the text
 * around them (no button minimum width), still 56dp tall and read as a button by TalkBack.
 */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(
            text = text,
            color = Ink,
            style = MaterialTheme.typography.labelLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
        )
    }
}
