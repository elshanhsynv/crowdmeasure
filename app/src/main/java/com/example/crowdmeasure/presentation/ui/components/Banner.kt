package com.example.crowdmeasure.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        BannerTone.Info -> MaterialTheme.colorScheme.surfaceContainerHigh to
                MaterialTheme.colorScheme.onSurfaceVariant

        BannerTone.Warning -> MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer

        BannerTone.Critical -> MaterialTheme.colorScheme.errorContainer to
                MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = spacing.none
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (tone == BannerTone.Info) Icons.Filled.Info else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = contentColor
                    )
                }
            }

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

@Preview
@Composable
private fun InfoBannerPreview() {
    InfoBanner(
        title = "Warning",
        body = "This is a warning message.",
        tone = BannerTone.Warning
    )
}
