package com.example.crowdmeasure.presentation.ui.components.privacy

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val MaskedValue = "••••••••"

@Stable
private fun sensitiveValueText(
    value: String?,
    revealed: Boolean
): String {
    return when {
        value == null -> "—"
        revealed -> value
        else -> MaskedValue
    }
}

/**
 * Displays a sensitive value with an explicit reveal/hide control.
 *
 * Supports both short values and longer multi-line values.
 * The parent owns [revealed] state.
 */
@Composable
fun SensitiveValueRow(
    label: String,
    value: String?,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    modifier: Modifier = Modifier,
    maxValueLines: Int = Int.MAX_VALUE
) {
    val hasValue = value != null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 420f
                )
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                AnimatedContent(
                    targetState = revealed,
                    label = "SensitiveValueRevealAnimation",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(140)) togetherWith
                                fadeOut(animationSpec = tween(90)) using
                                SizeTransform(clip = false)
                    },
                    modifier = Modifier.padding(top = 6.dp)
                ) { isRevealed ->
                    Text(
                        text = sensitiveValueText(
                            value = value,
                            revealed = isRevealed
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            value == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            isRevealed -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontFamily = if (isRevealed && hasValue) {
                            FontFamily.Monospace
                        } else {
                            FontFamily.Default
                        },
                        maxLines = maxValueLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (hasValue) {
                FilledTonalIconButton(
                    onClick = onToggleReveal,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (revealed) {
                                "Hide $label"
                            } else {
                                "Reveal $label"
                            }
                        }
                ) {
                    Icon(
                        imageVector = if (revealed) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SensitiveValueRowShortHiddenPreview() {
    MaterialTheme {
        SensitiveValueRow(
            label = "API Key",
            value = "sk-xxxxxxxxxxxxxxxxxxxxxxxx",
            revealed = false,
            onToggleReveal = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SensitiveValueRowShortRevealedPreview() {
    MaterialTheme {
        SensitiveValueRow(
            label = "API Key",
            value = "sk-xxxxxxxxxxxxxxxxxxxxxxxx",
            revealed = true,
            onToggleReveal = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SensitiveValueRowLongRevealedPreview() {
    MaterialTheme {
        SensitiveValueRow(
            label = "Cell Identifiers",
            value = "Serving Cell 123456789 • CID 42 • NCI 9834759834 • Band n78 • ARFCN 635334 • NRARFCN 632000 • TAC 12091 • PCI 301 • RSRP -91 dBm • RSRQ -12 dB • SINR 19 dB • CQI 12 • RSSI -70 dBm • Bandwidth 100 MHz • Mimo Layers 4",
            revealed = true,
            onToggleReveal = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SensitiveValueRowEmptyPreview() {
    MaterialTheme {
        SensitiveValueRow(
            label = "Cell Identifiers",
            value = null,
            revealed = false,
            onToggleReveal = {}
        )
    }
}