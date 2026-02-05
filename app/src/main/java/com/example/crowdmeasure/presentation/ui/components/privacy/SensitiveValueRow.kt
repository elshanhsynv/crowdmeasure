package com.example.crowdmeasure.presentation.ui.components.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing


/**
 * Row for displaying sensitive data with reveal/hide toggle.
 *
 * Privacy Design:
 * - Data masked by default (shows "••••••••")
 * - User must explicitly tap to reveal
 * - Eye icon indicates state (open = revealed, closed = hidden)
 * - Clear visual feedback
 *
 * Usage:
 * ```
 * SensitiveValueRow(
 *     label = "Endpoint",
 *     value = "https://api.example.com",
 *     revealed = state.revealed.contains(RevealKey.Endpoint),
 *     onToggleReveal = { viewModel.toggleReveal(RevealKey.Endpoint) }
 * )
 * ```
 */
@Composable
fun SensitiveValueRow(
    label: String,
    value: String?,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )

        Spacer(Modifier.width(spacing.sm))

        // Value (masked or revealed)
        Row(
            modifier = Modifier.weight(0.6f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (value == null) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = if (revealed) value else "••••••••",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (revealed) FontFamily.Monospace else FontFamily.Default,
                    color = if (revealed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(Modifier.width(spacing.xs))

                IconButton(
                    onClick = onToggleReveal,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (revealed) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (revealed) "Hide $label" else "Reveal $label",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}