package com.example.crowdmeasure.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════════════
// Primary -  (Blue)
// ══════════════════════════════════════════════════════════════════════
private val Blue50 = Color(0xFFE8F1FF)
private val Blue100 = Color(0xFFD0E3FF)
private val Blue200 = Color(0xFFA8CBFF)
private val Blue300 = Color(0xFF7AAFFF)
private val Blue400 = Color(0xFF4D8FFF)
private val Blue500 = Color(0xFF1F6FEB)  // Primary light
private val Blue600 = Color(0xFF1558D6)
private val Blue700 = Color(0xFF0E4199)  // Primary dark base
private val Blue800 = Color(0xFF0A2F6B)
private val Blue900 = Color(0xFF061D3D)

// ══════════════════════════════════════════════════════════════════════
// Secondary - (Teal)
// ══════════════════════════════════════════════════════════════════════
private val Teal50 = Color(0xFFE6FFFA)
private val Teal100 = Color(0xFFB3F5E8)
private val Teal200 = Color(0xFF80EDD6)
private val Teal300 = Color(0xFF4DE4C4)
private val Teal400 = Color(0xFF26D9B2)
private val Teal500 = Color(0xFF0D9F7E)  // Secondary light
private val Teal600 = Color(0xFF0A8268)
private val Teal700 = Color(0xFF076652)  // Secondary dark
private val Teal800 = Color(0xFF04493C)
private val Teal900 = Color(0xFF022D26)

// ══════════════════════════════════════════════════════════════════════
// Neutral - UI Foundation
// ══════════════════════════════════════════════════════════════════════
private val Gray50 = Color(0xFFFAFAFC)
private val Gray100 = Color(0xFFF4F5F7)
private val Gray200 = Color(0xFFE9EAEF)
private val Gray300 = Color(0xFFD6D8E1)
private val Gray400 = Color(0xFFB0B4C3)
private val Gray500 = Color(0xFF8B90A5)
private val Gray600 = Color(0xFF6B7087)
private val Gray700 = Color(0xFF4A4E63)
private val Gray800 = Color(0xFF2E3140)
private val Gray900 = Color(0xFF1A1C28)

// ══════════════════════════════════════════════════════════════════════
// Semantic Colors
// ══════════════════════════════════════════════════════════════════════
private val Error50 = Color(0xFFFEF2F2)
private val Error500 = Color(0xFFDC2626)  // Light mode error
private val Error600 = Color(0xFFB91C1C)
private val Error700 = Color(0xFF991B1B)
private val ErrorDark = Color(0xFFFCA5A5)  // Dark mode error

private val Success50 = Color(0xFFF0FDF4)
private val Success500 = Color(0xFF22C55E)
private val Success700 = Color(0xFF15803D)

private val Warning50 = Color(0xFFFFFBEB)
private val Warning500 = Color(0xFFF59E0B)
private val Warning700 = Color(0xFFB45309)

// ══════════════════════════════════════════════════════════════════════
// Light Theme Color Scheme
// ══════════════════════════════════════════════════════════════════════
val LightColorScheme = androidx.compose.material3.lightColorScheme(
    // Primary
    primary = Blue500,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,

    // Secondary
    secondary = Teal500,
    onSecondary = Color.White,
    secondaryContainer = Teal100,
    onSecondaryContainer = Teal900,

    // Tertiary (for accents, less common actions)
    tertiary = Gray600,
    onTertiary = Color.White,
    tertiaryContainer = Gray100,
    onTertiaryContainer = Gray900,

    // Background & Surface
    background = Gray50,
    onBackground = Gray900,
    surface = Color.White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    surfaceTint = Blue500,

    // Surface containers (for cards, elevated surfaces)
    surfaceContainer = Gray100,
    surfaceContainerHigh = Gray200,
    surfaceContainerHighest = Gray300,
    surfaceContainerLow = Gray50,
    surfaceContainerLowest = Color.White,

    // Outlines
    outline = Gray300,
    outlineVariant = Gray200,

    // Inverse (for snackbars, tooltips)
    inverseSurface = Gray900,
    inverseOnSurface = Gray50,
    inversePrimary = Blue300,

    // Semantic
    error = Error500,
    onError = Color.White,
    errorContainer = Error50,
    onErrorContainer = Error700,

    // Scrim (for modals, dialogs)
    scrim = Color.Black.copy(alpha = 0.32f),
)

// ══════════════════════════════════════════════════════════════════════
// Dark Theme Color Scheme
// ══════════════════════════════════════════════════════════════════════
val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    // Primary
    primary = Blue300,
    onPrimary = Blue900,
    primaryContainer = Blue700,
    onPrimaryContainer = Blue100,

    // Secondary
    secondary = Teal300,
    onSecondary = Teal900,
    secondaryContainer = Teal700,
    onSecondaryContainer = Teal100,

    // Tertiary
    tertiary = Gray400,
    onTertiary = Gray900,
    tertiaryContainer = Gray700,
    onTertiaryContainer = Gray100,

    // Background & Surface
    background = Color(0xFF0F1117),  // Very dark, but not pure black
    onBackground = Gray100,
    surface = Color(0xFF16181F),
    onSurface = Gray100,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray400,
    surfaceTint = Blue300,

    // Surface containers
    surfaceContainer = Gray800,
    surfaceContainerHigh = Gray700,
    surfaceContainerHighest = Gray600,
    surfaceContainerLow = Gray900,
    surfaceContainerLowest = Color(0xFF0A0B0F),

    // Outlines
    outline = Gray700,
    outlineVariant = Gray800,

    // Inverse
    inverseSurface = Gray100,
    inverseOnSurface = Gray900,
    inversePrimary = Blue500,

    // Semantic
    error = ErrorDark,
    onError = Error700,
    errorContainer = Error700,
    onErrorContainer = Error50,

    // Scrim
    scrim = Color.Black.copy(alpha = 0.5f),
)

// ══════════════════════════════════════════════════════════════════════
// Extended Semantic Colors (for custom use)
// ══════════════════════════════════════════════════════════════════════
object ExtendedColors {
    val successLight = Success500
    val onSuccessLight = Color.White
    val successContainerLight = Success50
    val onSuccessContainerLight = Success700

    val successDark = Success500
    val onSuccessDark = Success50
    val successContainerDark = Success700
    val onSuccessContainerDark = Success50

    val warningLight = Warning500
    val onWarningLight = Color.White
    val warningContainerLight = Warning50
    val onWarningContainerLight = Warning700

    val warningDark = Warning500
    val onWarningDark = Warning50
    val warningContainerDark = Warning700
    val onWarningContainerDark = Warning50
}