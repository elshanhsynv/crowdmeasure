package com.example.crowdmeasure.presentation.screens.settings

import android.Manifest
import android.os.Build
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.LightbulbCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.presentation.ui.components.AssistiveHint
import com.example.crowdmeasure.presentation.ui.components.BackgroundReliabilityCard
import com.example.crowdmeasure.presentation.ui.components.BackgroundWorkStatusCard
import com.example.crowdmeasure.presentation.ui.components.PermissionRow
import com.example.crowdmeasure.presentation.ui.components.SettingsSectionCard
import com.example.crowdmeasure.presentation.util.AppPermissions
import com.example.crowdmeasure.presentation.util.AppPermissions.isLocationServicesEnabled
import com.example.crowdmeasure.presentation.util.SystemSettingsIntents
import com.example.crowdmeasure.presentation.util.UiState

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>(),
    onOpenCallSessions: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backgroundWorkState by viewModel.backgroundWorkState.collectAsStateWithLifecycle()
    val callSamplingStatus by viewModel.callSamplingStatus.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.ensureMaintenanceScheduled() }

    SettingsScreenContent(
        contentPadding = contentPadding,
        settings = settings,
        exportState = exportState,
        deleteState = deleteState,
        backgroundWorkState = backgroundWorkState,
        callSamplingStatus = callSamplingStatus,
        onRunNow = viewModel::runAutoRunNow,
        onReschedule = viewModel::rescheduleBackgroundWork,
        onSetCallSamplingEnabled = viewModel::setCallSamplingEnabled,
        onOpenCallSessions = onOpenCallSessions,
        onExport = viewModel::exportData,
        onClearExportState = viewModel::clearExportState,
        onDelete = viewModel::deleteAllData,
        onClearDeleteState = viewModel::clearDeleteState
    )
}

@Composable
private fun SettingsScreenContent(
    contentPadding: PaddingValues,
    settings: AppSettings?,
    exportState: UiState<Unit>,
    deleteState: UiState<Unit>,
    backgroundWorkState: BackgroundWorkUiState,
    callSamplingStatus: CallSamplingStatusUiState,
    onRunNow: () -> Unit,
    onReschedule: () -> Unit,
    onSetCallSamplingEnabled: (Boolean) -> Unit,
    onOpenCallSessions: () -> Unit,
    onExport: (Context, Int) -> Unit,
    onClearExportState: () -> Unit,
    onDelete: () -> Unit,
    onClearDeleteState: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        SettingsTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

        when (selectedTab) {
            0 -> PrivacyTab()
            1 -> CollectionTab(
                settings = settings,
                backgroundWorkState = backgroundWorkState,
                onRunNow = onRunNow,
                onReschedule = onReschedule,
                callSamplingStatus = callSamplingStatus,
                onSetCallSamplingEnabled = onSetCallSamplingEnabled,
                onOpenCallSessions = onOpenCallSessions
            )
            2 -> DataTab(
                exportState = exportState,
                deleteState = deleteState,
                onExport = onExport,
                onClearExportState = onClearExportState,
                onDelete = onDelete,
                onClearDeleteState = onClearDeleteState
            )
        }
    }
}

// ── Tab Row ──────────────────────────────────────────────────────────────────

private data class TabItem(val title: String, val icon: ImageVector)

@Composable
private fun SettingsTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        TabItem("Privacy", Icons.Outlined.Security),
        TabItem("Collection", Icons.Outlined.Settings),
        TabItem("Data", Icons.Outlined.Code)
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                Surface(
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selected) {
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Privacy Tab ───────────────────────────────────────────────────────────────

@Composable
private fun PrivacyTab() {
    val context = LocalContext.current

    var coarseGranted by remember { mutableStateOf(AppPermissions.hasCoarseLocation(context)) }
    var fineGranted by remember { mutableStateOf(hasFineLocation(context)) }
    var phoneGranted by remember { mutableStateOf(AppPermissions.hasPhoneState(context)) }
    var locationServicesOn by remember { mutableStateOf(isLocationServicesEnabled(context)) }

    fun refresh() {
        coarseGranted = AppPermissions.hasCoarseLocation(context)
        fineGranted = hasFineLocation(context)
        phoneGranted = AppPermissions.hasPhoneState(context)
        locationServicesOn = isLocationServicesEnabled(context)
    }

    val requestCoarse = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    val requestFine = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    val requestPhone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // Permissions card
        SettingsSectionCard(
            title = "Permissions",
            description = "Improve measurement quality",
            icon = Icons.Outlined.Lock
        ) {
            if (!locationServicesOn) {
                LocationServicesWarningBanner(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                )
            }

            PermissionRow(
                title = "Coarse Location",
                subtitle = "Adds approximate coordinates to measurements",
                granted = coarseGranted,
                enabled = true,
                onRequest = { requestCoarse.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                icon = Icons.Outlined.MyLocation
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            PermissionRow(
                title = "Fine Location",
                subtitle = "Required for detailed cell signal on some devices (e.g., Samsung)",
                granted = fineGranted,
                enabled = true,
                onRequest = { requestFine.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                icon = Icons.Outlined.PinDrop
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            PermissionRow(
                title = "Phone State",
                subtitle = "Enables additional cell metrics on some devices",
                granted = phoneGranted,
                enabled = true,
                onRequest = { requestPhone.launch(Manifest.permission.READ_PHONE_STATE) },
                icon = Icons.Outlined.PhoneAndroid
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { refresh() }) {
                    Text("Refresh Status")
                }
            }
        }

        // Usage section
        PermissionUsageSection()

        // Tips section
        TipsSection()

        Spacer(Modifier.safeContentPadding())
    }
}

@Composable
private fun LocationServicesWarningBanner(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Location services are OFF — cell metrics may be empty",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = "Open location settings",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PermissionUsageSection() {
    SettingsSectionCard(
        title = "What the permissions are used for",
        description = "We use these permissions to improve measurement accuracy",
        icon = Icons.Outlined.Shield
    ) {
        val items = listOf(
            Icons.Outlined.CellTower to "Cell signal\naccuracy",
            Icons.Outlined.Map to "Location\ncontext",
            Icons.Outlined.BarChart to "Network\nanalytics",
            Icons.Outlined.Security to "Data\nquality"
        )
        // 2×2 grid via two Rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.take(2).forEach { (icon, label) ->
                UsageFeatureChip(icon = icon, label = label, modifier = Modifier.weight(1f))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.drop(2).forEach { (icon, label) ->
                UsageFeatureChip(icon = icon, label = label, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun UsageFeatureChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TipsSection() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Outlined.LightbulbCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "Tips",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "To get the best results, grant all permissions and keep Location Services ON.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Outlined.PinDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

// ── Collection Tab ───────────────────────────────────────────────────────────

@Composable
private fun CollectionTab(
    settings: AppSettings?,
    backgroundWorkState: BackgroundWorkUiState,
    onRunNow: () -> Unit,
    onReschedule: () -> Unit,
    callSamplingStatus: CallSamplingStatusUiState,
    onSetCallSamplingEnabled: (Boolean) -> Unit,
    onOpenCallSessions: () -> Unit
) {
    val context = LocalContext.current
    var phoneGranted by remember { mutableStateOf(AppPermissions.hasPhoneState(context)) }
    var fineGranted by remember { mutableStateOf(AppPermissions.hasFineLocation(context)) }
    var backgroundGranted by remember { mutableStateOf(AppPermissions.hasBackgroundLocation(context)) }
    var notificationsGranted by remember { mutableStateOf(AppPermissions.hasPostNotifications(context)) }
    var batteryIgnored by remember { mutableStateOf(AppPermissions.ignoresBatteryOptimizations(context)) }
    var locationServicesOn by remember { mutableStateOf(isLocationServicesEnabled(context)) }

    fun refreshCallSamplingPrerequisites() {
        phoneGranted = AppPermissions.hasPhoneState(context)
        fineGranted = AppPermissions.hasFineLocation(context)
        backgroundGranted = AppPermissions.hasBackgroundLocation(context)
        notificationsGranted = AppPermissions.hasPostNotifications(context)
        batteryIgnored = AppPermissions.ignoresBatteryOptimizations(context)
        locationServicesOn = isLocationServicesEnabled(context)
    }

    val requestPhone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshCallSamplingPrerequisites()
    }
    val requestFine = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshCallSamplingPrerequisites()
    }
    val requestBackground = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshCallSamplingPrerequisites()
    }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshCallSamplingPrerequisites()
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
            phoneGranted = phoneGranted,
            fineGranted = fineGranted,
            backgroundGranted = backgroundGranted,
            notificationsGranted = notificationsGranted,
            batteryIgnored = batteryIgnored,
            locationServicesOn = locationServicesOn,
            lastMissedLabel = callSamplingStatus.lastMissedLabel,
            onEnableChanged = onSetCallSamplingEnabled,
            onRequestPhone = { requestPhone.launch(Manifest.permission.READ_PHONE_STATE) },
            onRequestFine = { requestFine.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            onRequestBackground = { requestBackground.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onOpenLocationSettings = {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            },
            onOpenBatterySettings = {
                SystemSettingsIntents.openBatteryOptimizationSettings(context)
            },
            onRefresh = ::refreshCallSamplingPrerequisites,
            onOpenSessions = onOpenCallSessions
        )
        Spacer(Modifier.safeContentPadding())
    }
}

@Composable
private fun CallSamplingSettingsCard(
    enabled: Boolean,
    phoneGranted: Boolean,
    fineGranted: Boolean,
    backgroundGranted: Boolean,
    notificationsGranted: Boolean,
    batteryIgnored: Boolean,
    locationServicesOn: Boolean,
    lastMissedLabel: String,
    onEnableChanged: (Boolean) -> Unit,
    onRequestPhone: () -> Unit,
    onRequestFine: () -> Unit,
    onRequestBackground: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSessions: () -> Unit
) {
    val ready = phoneGranted &&
        fineGranted &&
        backgroundGranted &&
        notificationsGranted &&
        batteryIgnored &&
        locationServicesOn

    SettingsSectionCard(
        title = "Call Cell Sampling",
        description = "Local-only cell stats during active calls",
        icon = Icons.Outlined.PhoneAndroid
    ) {
        KeyValueLine("Status", if (enabled) "Enabled" else "Disabled")
        KeyValueLine("Ready", if (ready) "Yes" else "No")
        KeyValueLine("Last Missed Start", lastMissedLabel)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        PermissionRow(
            title = "Phone State",
            subtitle = "Required to detect active calls",
            granted = phoneGranted,
            enabled = true,
            onRequest = onRequestPhone,
            icon = Icons.Outlined.PhoneAndroid
        )
        PermissionRow(
            title = "Fine Location",
            subtitle = "Required for detailed cell info",
            granted = fineGranted,
            enabled = true,
            onRequest = onRequestFine,
            icon = Icons.Outlined.PinDrop
        )
        PermissionRow(
            title = "Background Location",
            subtitle = "Required for background call sampling",
            granted = backgroundGranted,
            enabled = true,
            onRequest = onRequestBackground,
            icon = Icons.Outlined.MyLocation
        )
        PermissionRow(
            title = "Notifications",
            subtitle = "Required for the foreground service notification",
            granted = notificationsGranted,
            enabled = true,
            onRequest = onRequestNotifications,
            icon = Icons.Outlined.Warning
        )

        if (!locationServicesOn) {
            Button(onClick = onOpenLocationSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Turn On Location Services")
            }
        }
        if (!batteryIgnored) {
            Button(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
                Text("Allow Battery Exemption")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onEnableChanged(!enabled) },
                enabled = ready || enabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (enabled) "Disable" else "Enable")
            }
            FilledTonalButton(
                onClick = onOpenSessions,
                modifier = Modifier.weight(1f)
            ) {
                Text("View Sessions")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onRefresh) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun KeyValueLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Data Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun DataTab(
    exportState: UiState<Unit>,
    deleteState: UiState<Unit>,
    onExport: (Context, Int) -> Unit,
    onClearExportState: () -> Unit,
    onDelete: () -> Unit,
    onClearDeleteState: () -> Unit
) {
    val context = LocalContext.current
    var exportCount by remember { mutableStateOf("50") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        SettingsSectionCard(
            title = "Export Data",
            description = "Export measurements as JSON for analysis",
            icon = Icons.Outlined.Code
        ) {
            OutlinedTextField(
                value = exportCount,
                onValueChange = { exportCount = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Number of records") },
                placeholder = { Text("50") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = { Text("Export last N measurements (1–10 000)") }
            )
            FilledTonalButton(
                onClick = {
                    val count = exportCount.toIntOrNull()?.coerceIn(1, 10_000) ?: 50
                    onExport(context, count)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export & Share JSON")
            }
            ExportStateRow(exportState = exportState, onClearExportState = onClearExportState)
        }

        SettingsSectionCard(
            title = "Delete Local Data",
            description = "Permanently delete all measurements from this device",
            icon = Icons.Outlined.Warning
        ) {
            AssistiveHint("This action cannot be undone. All local measurements will be permanently deleted.")
            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete All Data")
            }
            DeleteStateRow(deleteState = deleteState, onClearDeleteState = onClearDeleteState)
        }

        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = {
                    onDelete()
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        Spacer(Modifier.safeContentPadding())
    }
}

@Composable
private fun ExportStateRow(exportState: UiState<Unit>, onClearExportState: () -> Unit) {
    when (exportState) {
        UiState.Idle -> Unit
        UiState.Loading -> AssistiveHint("Exporting…")
        is UiState.Success -> {
            Text(
                text = "Exported successfully",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3_000)
                onClearExportState()
            }
        }
        is UiState.Error -> {
            Text(
                text = exportState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = onClearExportState) { Text("Dismiss") }
        }
    }
}

@Composable
private fun DeleteStateRow(deleteState: UiState<Unit>, onClearDeleteState: () -> Unit) {
    when (deleteState) {
        UiState.Idle -> Unit
        UiState.Loading -> AssistiveHint("Deleting…")
        is UiState.Success -> {
            Text(
                text = "All data deleted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3_000)
                onClearDeleteState()
            }
        }
        is UiState.Error -> {
            Text(
                text = deleteState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = onClearDeleteState) { Text("Dismiss") }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete All Data?") },
        text = { Text("This will permanently delete all local measurements. This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun hasFineLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SettingsScreenContent(
        contentPadding = PaddingValues(),
        settings = null,
        exportState = UiState.Idle,
        deleteState = UiState.Idle,
        backgroundWorkState = BackgroundWorkUiState.loading(),
        callSamplingStatus = CallSamplingStatusUiState.empty(),
        onRunNow = {},
        onReschedule = {},
        onSetCallSamplingEnabled = {},
        onOpenCallSessions = {},
        onExport = { _, _ -> },
        onClearExportState = {},
        onDelete = {},
        onClearDeleteState = {}
    )
}
