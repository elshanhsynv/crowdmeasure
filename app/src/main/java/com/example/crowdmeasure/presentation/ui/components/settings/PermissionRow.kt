package com.example.crowdmeasure.presentation.ui.components.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Immutable
enum class PermissionStatus {
    Granted,
    NotGranted,
    Disabled
}

@Composable
fun PermissionRow(
    title: String,
    subtitle: String,
    status: PermissionStatus,
    onRequest: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    buttonText: String = "Grant"
) {
    val enabled = status != PermissionStatus.Disabled
    val granted = status == PermissionStatus.Granted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .padding(vertical = 8.dp)
            .clearAndSetSemantics {
                contentDescription = buildString {
                    append(title)
                    append(". ")
                    append(subtitle)
                    append(". Status: ")
                    append(
                        when (status) {
                            PermissionStatus.Granted -> "granted"
                            PermissionStatus.NotGranted -> "not granted"
                            PermissionStatus.Disabled -> "disabled"
                        }
                    )
                }

                if (!granted && enabled) {
                    role = Role.Button
                }
            },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PermissionIconBox(
            icon = icon,
            enabled = enabled,
            granted = granted
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        PermissionTrailingContent(
            status = status,
            buttonText = buttonText,
            onRequest = onRequest
        )
    }
}

@Composable
private fun PermissionTrailingContent(
    status: PermissionStatus,
    buttonText: String,
    onRequest: () -> Unit
) {
    when (status) {
        PermissionStatus.Granted -> {
            GrantedChip()
        }

        PermissionStatus.NotGranted -> {
            OutlinedButton(
                onClick = onRequest
            ) {
                Text(buttonText)
            }
        }

        PermissionStatus.Disabled -> {
            DisabledChip()
        }
    }
}

@Composable
@NonRestartableComposable
private fun PermissionIconBox(
    icon: ImageVector,
    enabled: Boolean,
    granted: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        granted -> MaterialTheme.colorScheme.primaryContainer
        enabled -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when {
        granted -> MaterialTheme.colorScheme.onPrimaryContainer
        enabled -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
    }

    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.size(42.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun GrantedChip() {
    StatusChip(
        text = "Granted",
        icon = Icons.Outlined.CheckCircle,
        colors = StatusChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            borderColor = Color.Transparent
        )
    )
}

@Composable
private fun DisabledChip() {
    StatusChip(
        text = "Disabled",
        icon = Icons.Outlined.RadioButtonUnchecked,
        colors = StatusChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    )
}

@Composable
private fun StatusChip(
    text: String,
    icon: ImageVector,
    colors: StatusChipColors
) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        border = BorderStroke(
            width = 1.dp,
            color = colors.borderColor
        ),
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = colors.containerColor,
            disabledLabelColor = colors.contentColor,
            disabledLeadingIconContentColor = colors.contentColor
        )
    )
}

@Stable
private data class StatusChipColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color
)

@Preview(showBackground = true)
@Composable
private fun PermissionRowGrantedPreview() {
    MaterialTheme {
        PermissionRow(
            title = "Coarse Location",
            subtitle = "Adds approximate coordinates to measurements",
            status = PermissionStatus.Granted,
            onRequest = {},
            icon = Icons.Outlined.MyLocation
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionRowNotGrantedPreview() {
    MaterialTheme {
        PermissionRow(
            title = "Fine Location",
            subtitle = "Required for detailed cell signal on some devices",
            status = PermissionStatus.NotGranted,
            onRequest = {},
            icon = Icons.Outlined.MyLocation
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionRowDisabledPreview() {
    MaterialTheme {
        PermissionRow(
            title = "Background Location",
            subtitle = "Available after foreground location is granted",
            status = PermissionStatus.Disabled,
            onRequest = {},
            icon = Icons.Outlined.MyLocation,
            buttonText = "Enable"
        )
    }
}