package com.example.crowdmeasure.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing

/**
 * Banner tone/severity.
 */
enum class BannerTone {
    Info,
    Warning,
    Critical
}

/**
 * Informational banner for alerts, warnings, or critical messages.
 *
 * Features:
 * - Semantic color based on tone
 * - Title + body text
 * - Optional trailing action
 * - Accessible color combinations
 *
 * Usage:
 * ```
 * InfoBanner(
 *     title = "Collection Disabled",
 *     body = "Enable in Settings to run measurements.",
 *     tone = BannerTone.Warning
 * ) {
 *     TextButton(onClick = {...}) { Text("Settings") }
 * }
 * ```
 */
@Composable
fun InfoBanner(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Info,
    trailing: (@Composable () -> Unit)? = null
) {
    val spacing = LocalSpacing.current

    val (containerColor, contentColor) = when (tone) {
        BannerTone.Info -> MaterialTheme.colorScheme.surfaceVariant to
                MaterialTheme.colorScheme.onSurfaceVariant
        BannerTone.Warning -> MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer
        BannerTone.Critical -> MaterialTheme.colorScheme.errorContainer to
                MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = MaterialTheme.shapes.large,
        tonalElevation = spacing.none  // Flat for banners
    ) {
        Row(
            modifier = Modifier.padding(spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }

            trailing?.invoke()
        }
    }
}