package com.ajayinnovations.argus.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// One fixed light scheme. No dynamic colour, no dark mode: phone themes can't recolour the
// app, and screens match the setup pictures on every phone.
private val ArgusColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = PineTint,
    onPrimaryContainer = Pine,
    secondary = Pine,
    onSecondary = Paper,
    error = Alarm,
    onError = Paper,
    errorContainer = AlarmTint,
    onErrorContainer = Alarm,
    background = Mist,
    onBackground = Ink,
    surface = Mist,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = Slate,
    surfaceContainerHighest = Paper,
    outline = Edge,
    outlineVariant = Line,
)

/** Squared-off corners, not pills: 12dp for buttons and blocks, 20dp for the chat bubble. */
private val ArgusShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
)

@Composable
fun ArgusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ArgusColors, typography = Typography, shapes = ArgusShapes, content = content)
}
