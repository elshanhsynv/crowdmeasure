package com.example.crowdmeasure.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing

/**
 * Standard card component for grouping related content.
 *
 * Features:
 * - Title + optional description
 * - Optional trailing action in header
 * - Consistent spacing and elevation
 * - Content slot for custom layout
 *
 * Usage:
 * ```
 * AppCard(
 *     title = "Measurement Queue",
 *     description = "Pending uploads"
 * ) {
 *     MetricRow("Count", "42")
 *     Button(...) { Text("Upload") }
 * }
 * ```
 */
@Composable
fun AppCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    headerTrailing: (@Composable () -> Unit)? = null,
    elevation: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = LocalSpacing.current

    val newTonalElevation = CardDefaults.cardElevation(
        elevation
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = newTonalElevation,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.cardContentSpacing)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (headerTrailing != null) {
                    Spacer(Modifier.width(spacing.sm))
                    headerTrailing()
                }
            }

            // Content
            content()
        }
    }
}