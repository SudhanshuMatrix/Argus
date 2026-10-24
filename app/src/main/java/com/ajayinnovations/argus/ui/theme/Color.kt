package com.ajayinnovations.argus.ui.theme

import androidx.compose.ui.graphics.Color

// Argus palette (docs/PLAN.md, design pass). Every text pair is 7:1 or better (WCAG AAA).
/** Page background: a cool off-white, not cream. */
val Mist = Color(0xFFF3F6F9)
/** Chat bubble, fields, secondary buttons. */
val Paper = Color(0xFFFFFFFF)
/** All text and the main button. 15.8:1 on Mist. */
val Ink = Color(0xFF0E1B33)
/** Secondary text. 8.3:1 on Mist. */
val Slate = Color(0xFF3E4A5E)
/** Field borders: 4.1:1 on Mist (3:1 needed for controls). */
val Edge = Color(0xFF6B788C)
/** Dividers only, never text. */
val Line = Color(0xFFD3DAE3)
/** "Protected". 7.6:1 on Mist; white on it 8.3:1. */
val Pine = Color(0xFF07594C)
val PineTint = Color(0xFFE2F1EC)
/** SCAM, and anything turned off. White on it 8.3:1. */
val Alarm = Color(0xFF991B12)
val AlarmTint = Color(0xFFFDECEA)
/** CAREFUL, always with Ink text (8.1:1). */
val Amber = Color(0xFFF0A202)
/** OTP. White on it 8.9:1. */
val Signal = Color(0xFF1A4790)
/** Behind the dangerous words in a message; Ink on it 13.5:1. */
val Marker = Color(0xFFFFE28A)
