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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.presentation.screens.settings.components.BackgroundReliabilityCard
import com.example.crowdmeasure.presentation.screens.settings.components.BackgroundWorkStatusCard
import com.example.crowdmeasure.presentation.screens.settings.components.CallSamplingSettingsCard
import com.example.crowdmeasure.presentation.ui.components.cards.SettingsSectionCard
import com.example.crowdmeasure.presentation.ui.theme.CrowdMeasureTheme
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

@Preview(showBackground = true)
@Composable
private fun CollectionSettingsTabPreview() {
    CrowdMeasureTheme {
        CollectionSettingsTab(
            settings = null,
            backgroundWorkState = BackgroundWorkUiState(
                workManagerStateLabel = "ENQUEUED",
                nextScheduledWorkStateLabel = "In 12 minutes",
                intervalMinutesLabel = "Every 15 minutes",
                lastStartLabel = "Today, 12:00 PM",
                lastEndLabel = "Today, 12:01 PM",
                lastResultLabel = "SUCCESS",
                autoRunLastCodeLabel = "200 OK",
                autoRunLastSuccessfulCollectionLabel = "Today, 12:01 PM",
                autoRunLastMeasurementLabel = "42 metrics collected",
                uploadLastSuccessfulUploadLabel = "Today, 12:01 PM",
                uploadLastStartLabel = "Today, 12:01 PM",
                uploadLastEndLabel = "Today, 12:01 PM",
                uploadLastResultLabel = "SUCCESS",
                uploadLastCodeLabel = "201 Created",
                lastUploadedLabel = "14 records uploaded",
                pendingRecordsLabel = "0 pending",
                failedRecordsLabel = "0 failed",
                lastErrorLabel = "None",
                canRunNow = true,
                canReschedule = true
            ),
            onRunNow = {},
            onReschedule = {},
            callSamplingStatus = CallSamplingStatusUiState(
                lastMissedLabel = "2 missed calls since last run"
            ),
            onSetCallSamplingEnabled = {},
            onSetWhatsappCallSamplingEnabled = {}
        )
    }
}