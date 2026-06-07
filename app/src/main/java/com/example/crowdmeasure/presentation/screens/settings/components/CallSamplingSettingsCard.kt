package com.example.crowdmeasure.presentation.screens.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.ui.components.cards.SettingsSectionCard

private const val WHATSAPP_CALL_SAMPLING_AVAILABLE = false

@Composable
fun CallSamplingSettingsCard(
    enabled: Boolean,
    whatsappEnabled: Boolean,
    phoneGranted: Boolean,
    fineGranted: Boolean,
    backgroundGranted: Boolean,
    notificationsGranted: Boolean,
    batteryIgnored: Boolean,
    locationServicesOn: Boolean,
    whatsappNotificationAccess: Boolean,
    lastMissedLabel: String,
    onEnableChanged: (Boolean) -> Unit,
    onWhatsappEnableChanged: (Boolean) -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val state = remember(
        enabled,
        whatsappEnabled,
        phoneGranted,
        fineGranted,
        backgroundGranted,
        notificationsGranted,
        batteryIgnored,
        locationServicesOn,
        whatsappNotificationAccess,
        lastMissedLabel
    ) {
        CallSamplingUiState(
            cellularEnabled = enabled,
            whatsappEnabled = whatsappEnabled && WHATSAPP_CALL_SAMPLING_AVAILABLE,
            phoneGranted = phoneGranted,
            fineGranted = fineGranted,
            backgroundGranted = backgroundGranted,
            notificationsGranted = notificationsGranted,
            batteryIgnored = batteryIgnored,
            locationServicesOn = locationServicesOn,
            whatsappNotificationAccess = whatsappNotificationAccess,
            lastMissedLabel = lastMissedLabel
        )
    }

    SettingsSectionCard(
        title = "Call Sampling",
        description = "Local-only cell stats during active calls",
        icon = Icons.Outlined.PhoneAndroid
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CallSamplingStatusHeader(
                cellularEnabled = state.cellularEnabled,
                whatsappEnabled = state.whatsappEnabled,
                cellularReady = state.cellularReady,
                whatsappReady = state.whatsappReady,
                whatsappAvailable = WHATSAPP_CALL_SAMPLING_AVAILABLE
            )

            SectionDivider()

            SamplingRequirementsList(
                requirements = state.requirements
            )

            SamplingFixActions(
                actions = state.fixActions,
                onOpenLocationSettings = onOpenLocationSettings,
                onOpenBatterySettings = onOpenBatterySettings,
                onOpenNotificationAccessSettings = onOpenNotificationAccessSettings
            )

            SectionDivider()

            SamplingToggleSection(
                cellularReady = state.cellularReady,
                whatsappReady = state.whatsappReady,
                cellularEnabled = state.cellularEnabled,
                whatsappEnabled = state.whatsappEnabled,
                onEnableChanged = onEnableChanged,
                onWhatsappEnableChanged = onWhatsappEnableChanged,
                whatsappAvailable = WHATSAPP_CALL_SAMPLING_AVAILABLE
            )

            LastMissedStartRow(
                lastMissedLabel = state.lastMissedLabel,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
private fun CallSamplingStatusHeader(
    cellularEnabled: Boolean,
    whatsappEnabled: Boolean,
    cellularReady: Boolean,
    whatsappReady: Boolean,
    whatsappAvailable: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SamplingStatusCard(
            title = "Cellular",
            status = samplingStatus(
                enabled = cellularEnabled,
                ready = cellularReady
            ),
            modifier = if (whatsappAvailable) Modifier.weight(1f) else Modifier.fillMaxWidth()
        )

        if (whatsappAvailable) {
            SamplingStatusCard(
                title = "WhatsApp",
                status = samplingStatus(
                    enabled = whatsappEnabled,
                    ready = whatsappReady
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun samplingStatus(
    enabled: Boolean,
    ready: Boolean
): SamplingStatus {
    return when {
        enabled -> SamplingStatus.Enabled
        ready -> SamplingStatus.Ready
        else -> SamplingStatus.NeedsSetup
    }
}

@Composable
private fun SamplingStatusCard(
    title: String,
    status: SamplingStatus,
    modifier: Modifier = Modifier
) {
    val colors = samplingStatusColors(status)

    Surface(
        modifier = modifier.defaultMinSize(minHeight = 72.dp),
        shape = MaterialTheme.shapes.large,
        color = colors.container,
        contentColor = colors.content,
        border = BorderStroke(
            width = 1.dp,
            color = colors.border
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = colors.content.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = status.label,
                style = MaterialTheme.typography.titleSmall,
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SamplingRequirementsList(
    requirements: List<SamplingRequirement>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        requirements.forEach { requirement ->
            RequirementStatusRow(
                label = requirement.label,
                granted = requirement.granted
            )
        }
    }
}

@Composable
private fun RequirementStatusRow(
    label: String,
    granted: Boolean
) {
    val icon = if (granted) {
        Icons.Outlined.CheckCircle
    } else {
        Icons.Outlined.ErrorOutline
    }

    val color = if (granted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 42.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        RequirementStatusChip(
            granted = granted
        )
    }
}

@Composable
private fun RequirementStatusChip(
    granted: Boolean
) {
    val label = if (granted) "Ready" else "Required"

    val containerColor = if (granted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = if (granted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = containerColor,
            disabledLabelColor = contentColor
        ),
        border = null
    )
}

@Composable
private fun SamplingFixActions(
    actions: Set<SamplingFixAction>,
    onOpenLocationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit
) {
    if (actions.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (SamplingFixAction.EnableLocationServices in actions) {
            FixActionButton(
                text = "Turn on location services",
                icon = Icons.Outlined.LocationOn,
                onClick = onOpenLocationSettings
            )
        }

        if (SamplingFixAction.AllowBatteryExemption in actions) {
            FixActionButton(
                text = "Allow battery exemption",
                icon = Icons.Outlined.BatterySaver,
                onClick = onOpenBatterySettings
            )
        }

        if (SamplingFixAction.AllowWhatsappNotificationAccess in actions) {
            FixActionButton(
                text = "Allow WhatsApp notification access",
                icon = Icons.Outlined.Notifications,
                onClick = onOpenNotificationAccessSettings
            )
        }
    }
}

@Composable
private fun FixActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SamplingToggleSection(
    cellularReady: Boolean,
    whatsappReady: Boolean,
    cellularEnabled: Boolean,
    whatsappEnabled: Boolean,
    onEnableChanged: (Boolean) -> Unit,
    onWhatsappEnableChanged: (Boolean) -> Unit,
    whatsappAvailable: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SamplingToggleRow(
            title = "Cellular calls",
            subtitle = if (cellularReady) {
                "Ready to sample during active calls"
            } else {
                "Complete required access first"
            },
            checked = cellularEnabled,
            enabled = cellularReady || cellularEnabled,
            onCheckedChange = onEnableChanged
        )

        if (whatsappAvailable) {
            SamplingToggleRow(
                title = "WhatsApp calls",
                subtitle = if (whatsappReady) {
                    "Ready when WhatsApp call notifications are detected"
                } else {
                    "Requires cellular readiness and notification access"
                },
                checked = whatsappEnabled,
                enabled = whatsappReady || whatsappEnabled,
                onCheckedChange = onWhatsappEnableChanged
            )
        }
    }
}

@Composable
private fun SamplingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "$title. $subtitle. ${if (checked) "Enabled" else "Disabled"}."
                role = Role.Switch
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        onClick = {
            if (enabled) {
                onCheckedChange(!checked)
            }
        },
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
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

            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun LastMissedStartRow(
    lastMissedLabel: String,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Last missed start",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = lastMissedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
//                    maxLines = 2,
//                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(
                onClick = onRefresh
            ) {
                Text("Refresh")
            }
        }
    }
}

@Composable
@NonRestartableComposable
private fun SectionDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Immutable
private data class CallSamplingUiState(
    val cellularEnabled: Boolean,
    val whatsappEnabled: Boolean,
    val phoneGranted: Boolean,
    val fineGranted: Boolean,
    val backgroundGranted: Boolean,
    val notificationsGranted: Boolean,
    val batteryIgnored: Boolean,
    val locationServicesOn: Boolean,
    val whatsappNotificationAccess: Boolean,
    val lastMissedLabel: String
) {
    val cellularReady: Boolean
        get() = phoneGranted &&
                fineGranted &&
                backgroundGranted &&
                notificationsGranted &&
                locationServicesOn

    val whatsappReady: Boolean
        get() = WHATSAPP_CALL_SAMPLING_AVAILABLE && cellularReady && whatsappNotificationAccess

    val requirements: List<SamplingRequirement>
        get() = buildList {
            add(SamplingRequirement("Phone", phoneGranted))
            add(SamplingRequirement("Fine location", fineGranted))
            add(SamplingRequirement("Background location", backgroundGranted))
            add(SamplingRequirement("Notifications", notificationsGranted))
            add(SamplingRequirement("Battery exemption", batteryIgnored))
            add(SamplingRequirement("Location services", locationServicesOn))
            if (WHATSAPP_CALL_SAMPLING_AVAILABLE) {
                add(SamplingRequirement("WhatsApp notification access", whatsappNotificationAccess))
            }
        }

    val fixActions: Set<SamplingFixAction>
        get() = buildSet {
            if (!locationServicesOn) {
                add(SamplingFixAction.EnableLocationServices)
            }

            if (!batteryIgnored) {
                add(SamplingFixAction.AllowBatteryExemption)
            }

            if (WHATSAPP_CALL_SAMPLING_AVAILABLE && !whatsappNotificationAccess) {
                add(SamplingFixAction.AllowWhatsappNotificationAccess)
            }
        }
}

@Immutable
private data class SamplingRequirement(
    val label: String,
    val granted: Boolean
)

@Immutable
private enum class SamplingStatus(
    val label: String
) {
    Enabled("Enabled"),
    Ready("Ready"),
    NeedsSetup("Needs setup")
}

@Immutable
private enum class SamplingFixAction {
    EnableLocationServices,
    AllowBatteryExemption,
    AllowWhatsappNotificationAccess
}

@Stable
private data class SamplingStatusColors(
    val container: Color,
    val content: Color,
    val border: Color
)

@Composable
private fun samplingStatusColors(
    status: SamplingStatus
): SamplingStatusColors {
    return when (status) {
        SamplingStatus.Enabled -> SamplingStatusColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            border = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        )

        SamplingStatus.Ready -> SamplingStatusColors(
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            border = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        )

        SamplingStatus.NeedsSetup -> SamplingStatusColors(
            container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
            content = MaterialTheme.colorScheme.onErrorContainer,
            border = MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CallSamplingSettingsCardPreview() {
    MaterialTheme {
        CallSamplingSettingsCard(
            enabled = true,
            whatsappEnabled = false,
            phoneGranted = true,
            fineGranted = true,
            backgroundGranted = true,
            notificationsGranted = true,
            batteryIgnored = false,
            locationServicesOn = true,
            whatsappNotificationAccess = false,
            lastMissedLabel = "5 minutes ago",
            onEnableChanged = {},
            onWhatsappEnableChanged = {},
            onOpenLocationSettings = {},
            onOpenBatterySettings = {},
            onOpenNotificationAccessSettings = {},
            onRefresh = {}
        )
    }
}
