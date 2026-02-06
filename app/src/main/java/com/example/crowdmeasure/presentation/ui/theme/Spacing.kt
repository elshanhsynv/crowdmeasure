package com.example.crowdmeasure.presentation.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    // ═══════════════════════════════════════════════════════════
    // Base Scale
    // ═══════════════════════════════════════════════════════════
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,      // Tight spacing (icon + text)
    val xs: Dp = 4.dp,       // Minimal gap
    val sm: Dp = 8.dp,       // Small gap
    val md: Dp = 12.dp,      // Default gap
    val lg: Dp = 16.dp,      // Large gap
    val xl: Dp = 24.dp,      // Section spacing
    val xxl: Dp = 32.dp,     // Screen sections
    val xxxl: Dp = 48.dp,    // Major sections

    // ═══════════════════════════════════════════════════════════
    // Semantic Spacing (named by purpose)
    // ═══════════════════════════════════════════════════════════

    // Screen edges
    val screenPadding: Dp = 16.dp,
    val screenPaddingHorizontal: Dp = 16.dp,
    val screenPaddingVertical: Dp = 16.dp,

    // Cards & containers
    val cardPadding: Dp = 16.dp,
    val cardContentSpacing: Dp = 12.dp,
    val cardSpacing: Dp = 12.dp,  // Between cards in a list

    // Lists
    val listItemPadding: Dp = 16.dp,
    val listItemSpacing: Dp = 8.dp,

    // Buttons
    val buttonPadding: Dp = 16.dp,
    val buttonSpacing: Dp = 12.dp,

    // Icons
    val iconTextGap: Dp = 8.dp,
    val iconSize: Dp = 24.dp,
    val iconSizeSmall: Dp = 20.dp,
    val iconSizeLarge: Dp = 32.dp,

    // Dialogs
    val dialogPadding: Dp = 24.dp,
    val dialogContentSpacing: Dp = 16.dp,

    // Sections
    val sectionSpacing: Dp = 24.dp,
    val sectionHeaderSpacing: Dp = 12.dp,

    // Dividers
    val dividerSpacing: Dp = 16.dp,

    // Bottom sheet
    val bottomSheetPadding: Dp = 16.dp,
    val bottomSheetHandleSpacing: Dp = 12.dp,
)

/**
 * CompositionLocal for accessing spacing throughout the app.
 * Provided at the theme level.
 */
val LocalSpacing = staticCompositionLocalOf { Spacing() }