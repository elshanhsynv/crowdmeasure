package com.example.crowdmeasure.presentation.ui.components.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing


/**
 * Centered empty state component.
 *
 * Use for list empty states, no data states, etc.
 *
 * Usage:
 * ```
 * if (items.isEmpty()) {
 *     EmptyState(
 *         message = "No measurements yet",
 *         subtitle = "Tap 'Start measurement' to begin"
 *     )
 * }
 * ```
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val spacing = LocalSpacing.current

    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}