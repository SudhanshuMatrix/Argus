package com.ajayinnovations.argus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ajayinnovations.argus.R

/** Atkinson Hyperlegible Next (SIL OFL 1.1, Braille Institute): made for low vision. */
val Hyperlegible = FontFamily(
    Font(R.font.atkinson_hyperlegible_next_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_next_semibold, FontWeight.SemiBold),
    Font(R.font.atkinson_hyperlegible_next_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight) =
    TextStyle(fontFamily = Hyperlegible, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

// One family, sentence case. Body 19sp, nothing below 18sp.
val Typography = Typography(
    /** Status block and warning headline. */
    displaySmall = style(30, 38, FontWeight.Bold),
    headlineLarge = style(26, 34, FontWeight.Bold),
    headlineMedium = style(26, 34, FontWeight.Bold),
    titleLarge = style(21, 28, FontWeight.SemiBold),
    titleMedium = style(21, 28, FontWeight.SemiBold),
    bodyLarge = style(19, 28, FontWeight.Normal),
    bodyMedium = style(18, 26, FontWeight.Normal),
    bodySmall = style(18, 26, FontWeight.Normal),
    labelLarge = style(20, 24, FontWeight.SemiBold),
    labelMedium = style(18, 22, FontWeight.SemiBold),
)

/** The chat bubble on the warning and check screens. */
val BubbleText = style(20, 30, FontWeight.Normal)
