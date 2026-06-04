package com.example.crowdmeasure.presentation.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.presentation.ui.components.BackgroundReliabilityCard
import com.example.crowdmeasure.presentation.ui.components.BackgroundWorkStatusCard
import com.example.crowdmeasure.presentation.ui.components.SettingsSectionCard
import com.example.crowdmeasure.presentation.util.AppPermissions
import com.example.crowdmeasure.presentation.util.AppPermissions.isLocationServicesEnabled
import com.example.crowdmeasure.presentation.util.SystemSettingsIntents

@Composable
internal fun CollectionSettingsTab(
    settings: AppSettings?,
    backgroundWorkState: BackgroundWorkUiState,
    onRunNow: () -> Unit,
    onReschedule: () -> Unit,
    callSamplingStatus: CallSamplingStatusUiState,
    onSetCallSamplingEnabled: (Boolean) -> Unit,
    onSetWhatsappCallSamplingEnabled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var phoneGranted by remember { mutableStateOf(AppPermissions.hasPhoneState(context)) }
    var fineGranted by remember { mutableStateOf(AppPermissions.hasFineLocation(context)) }
    var backgroundGranted by remember { mutableStateOf(AppPermissions.hasBackgroundLocation(context)) }
    var notificationsGranted by remember {
        mutableStateOf(AppPermissions.hasPostNotifications(context))
    }
    var batteryIgnored by remember {
        mutableStateOf(AppPermissions.ignoresBatteryOptimizations(context))
    }
    var locationServicesOn by remember { mutableStateOf(isLocationServicesEnabled(context)) }
    var whatsappNotificationAccess by remember {
        mutableStateOf(AppPermissions.hasWhatsappNotificationAccess(context))
    }

    fun refreshCallSamplingPrerequisites() {
        phoneGranted = AppPermissions.hasPhoneState(context)
        fineGranted = AppPermissions.hasFineLocation(context)
        backgroundGranted = AppPermissions.hasBackgroundLocation(context)
        notificationsGranted = AppPermissions.hasPostNotifications(context)
        batteryIgnored = AppPermissions.ignoresBatteryOptimizations(context)
        locationServicesOn = isLocationServicesEnabled(context)
        whatsappNotificationAccess = AppPermissions.hasWhatsappNotificationAccess(context)
    }

    LaunchedEffect(Unit) { refreshCallSamplingPrerequisites() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        BackgroundWorkStatusCard(
            state = backgroundWorkState,
            onRunNow = onRunNow,
            onReschedule = onReschedule
        )

        BackgroundReliabilityCard(
            onFixScheduling = onReschedule,
            onOpenBatterySettings = {
                SystemSettingsIntents.openBatteryOptimizationSettings(context)
            }
        )

        CallSamplingSettingsCard(
            enabled = settings?.callSamplingEnabled == true,
            whatsappEnabled = settings?.whatsappCallSamplingEnabled == true,
            phoneGranted = phoneGranted,
            fineGranted = fineGranted,
            backgroundGranted = backgroundGranted,
            notificationsGranted = notificationsGranted,
            batteryIgnored = batteryIgnored,
            locationServicesOn = locationServicesOn,
            whatsappNotificationAccess = whatsappNotificationAccess,
            lastMissedLabel = callSamplingStatus.lastMissedLabel,
            onEnableChanged = onSetCallSamplingEnabled,
            onWhatsappEnableChanged = onSetWhatsappCallSamplingEnabled,
            onOpenLocationSettings = {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            },
            onOpenBatterySettings = {
                SystemSettingsIntents.openBatteryOptimizationSettings(context)
            },
            onOpenNotificationAccessSettings = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            onRefresh = ::refreshCallSamplingPrerequisites
        )

        Spacer(Modifier.safeContentPadding())
    }
}

@Composable
private fun CallSamplingSettingsCard(
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
    val ready = phoneGranted &&
            fineGranted &&
            backgroundGranted &&
            notificationsGranted
            // && batteryIgnored && locationServicesOn
    val whatsappReady = ready && whatsappNotificationAccess

    SettingsSectionCard(
        title = "Call Cell Sampling",
        description = "Local-only cell stats during active calls",
        icon = Icons.Outlined.PhoneAndroid
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SamplingStatusHeader(
                cellularEnabled = enabled,
                whatsappEnabled = whatsappEnabled,
                cellularReady = ready,
                whatsappReady = whatsappReady
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PermissionStatusRow(label = "Phone", granted = phoneGranted)
                PermissionStatusRow(label = "Fine location", granted = fineGranted)
                PermissionStatusRow(label = "Background location", granted = backgroundGranted)
                PermissionStatusRow(label = "Notifications", granted = notificationsGranted)
                PermissionStatusRow(label = "Battery exemption", granted = batteryIgnored)
                PermissionStatusRow(label = "Location services", granted = locationServicesOn)
                PermissionStatusRow(
                    label = "WhatsApp notification access",
                    granted = whatsappNotificationAccess
                )
            }

            CallSamplingFixActions(
                locationServicesOn = locationServicesOn,
                batteryIgnored = batteryIgnored,
                whatsappNotificationAccess = whatsappNotificationAccess,
                onOpenLocationSettings = onOpenLocationSettings,
                onOpenBatterySettings = onOpenBatterySettings,
                onOpenNotificationAccessSettings = onOpenNotificationAccessSettings
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SamplingToggleRow(
                    title = "Cellular calls",
                    subtitle = if (ready) {
                        "Ready to sample during active calls"
                    } else {
                        "Complete required access first"
                    },
                    checked = enabled,
                    enabled = ready || enabled,
                    onCheckedChange = onEnableChanged
                )

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

            LastMissedStartRow(
                lastMissedLabel = lastMissedLabel,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
private fun CallSamplingFixActions(
    locationServicesOn: Boolean,
    batteryIgnored: Boolean,
    whatsappNotificationAccess: Boolean,
    onOpenLocationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit
) {
    if (locationServicesOn && batteryIgnored && whatsappNotificationAccess) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!locationServicesOn) {
            OutlinedButton(
                onClick = onOpenLocationSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Outlined.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Turn on location services")
            }
        }

        if (!batteryIgnored) {
            OutlinedButton(
                onClick = onOpenBatterySettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Outlined.BatterySaver, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Allow battery exemption")
            }
        }

        if (!whatsappNotificationAccess) {
            OutlinedButton(
                onClick = onOpenNotificationAccessSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Allow WhatsApp notification access")
            }
        }
    }
}

@Composable
private fun SamplingStatusHeader(
    cellularEnabled: Boolean,
    whatsappEnabled: Boolean,
    cellularReady: Boolean,
    whatsappReady: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SamplingStatusPill(
            title = "Cellular",
            state = when {
                cellularEnabled -> "Enabled"
                cellularReady -> "Ready"
                else -> "Needs setup"
            },
            success = cellularEnabled || cellularReady,
            modifier = Modifier.weight(1f)
        )

        SamplingStatusPill(
            title = "WhatsApp",
            state = when {
                whatsappEnabled -> "Enabled"
                whatsappReady -> "Ready"
                else -> "Needs setup"
            },
            success = whatsappEnabled || whatsappReady,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SamplingStatusPill(
    title: String,
    state: String,
    success: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (success) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (success) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.8f)
            )
            Text(
                text = state,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor
            )
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = if (granted) "Granted" else "Required",
            style = MaterialTheme.typography.labelMedium,
            color = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
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
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
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
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Last missed start",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = lastMissedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            TextButton(onClick = onRefresh) {
                Text("Refresh")
            }
        }
    }
}
