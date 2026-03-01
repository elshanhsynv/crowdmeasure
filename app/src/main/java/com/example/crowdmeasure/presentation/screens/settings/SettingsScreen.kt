package com.example.crowdmeasure.presentation.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.presentation.ui.components.AssistiveHint
import com.example.crowdmeasure.presentation.ui.components.BackgroundReliabilityCard
import com.example.crowdmeasure.presentation.ui.components.BackgroundWorkStatusCard
import com.example.crowdmeasure.presentation.ui.components.BannerTone
import com.example.crowdmeasure.presentation.ui.components.InfoBanner
import com.example.crowdmeasure.presentation.ui.components.PermissionRow
import com.example.crowdmeasure.presentation.ui.components.SettingsSectionCard
import com.example.crowdmeasure.presentation.ui.components.SettingSwitchRow
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing
import com.example.crowdmeasure.presentation.util.AppPermissions
import com.example.crowdmeasure.presentation.util.AppPermissions.isLocationServicesEnabled
import com.example.crowdmeasure.presentation.util.SystemSettingsIntents
import com.example.crowdmeasure.presentation.util.UiState

/**
 * Settings screen with tabbed organization.
 *
 * Tabs:
 * 1. Privacy - Consent, permissions, uploads
 * 2. Collection - Endpoint, intervals, Wi-Fi only, auto-run
 * 3. Data - Export, delete
 */
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backgroundWorkState by viewModel.backgroundWorkState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.ensureMaintenanceScheduled()
    }

    SettingsScreenContent(
        contentPadding = contentPadding,
        settings = settings,
        exportState = exportState,
        deleteState = deleteState,
        backgroundWorkState = backgroundWorkState,
        onSaveEndpoint = viewModel::saveEndpoint,
        onCollectOnlyWifiChange = viewModel::setCollectOnlyWifi,
        onAutoRunChange = viewModel::setAutoRun,
        onRunNow = viewModel::runAutoRunNow,
        onReschedule = viewModel::rescheduleBackgroundWork,
        onExport = viewModel::exportData,
        onClearExportState = viewModel::clearExportState,
        onDelete = viewModel::deleteAllData,
        onClearDeleteState = viewModel::clearDeleteState,
        onSetConsent = viewModel::setConsent,
        onSetCollection = viewModel::setCollection,
        onSetFirestoreUploads = viewModel::setFirestoreUploads
    )

}

@Composable
private fun SettingsScreenContent(
    contentPadding: PaddingValues,
    settings: AppSettings?,
    exportState: UiState<Unit>,
    deleteState: UiState<Unit>,
    backgroundWorkState: BackgroundWorkUiState,
    onSaveEndpoint: (String) -> Unit,
    onCollectOnlyWifiChange: (Boolean) -> Unit,
    onAutoRunChange: (Boolean, Int) -> Unit,
    onRunNow: () -> Unit,
    onReschedule: () -> Unit,
    onExport: (Context, Int) -> Unit,
    onClearExportState: () -> Unit,
    onDelete: () -> Unit,
    onClearDeleteState: () -> Unit,
    onSetConsent: (Boolean) -> Unit,
    onSetCollection: (Boolean) -> Unit,
    onSetFirestoreUploads: (Boolean) -> Unit
) {

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Tab row
        SettingsTabRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // Tab content
        when (selectedTab) {
            0 -> PrivacyTab(
                settings = settings,
                onConsentChange = onSetConsent,
                onCollectionChange = onSetCollection,
                onFirestoreUploadsChange = onSetFirestoreUploads
            )

            1 -> CollectionTab(
                settings = settings,
                backgroundWorkState = backgroundWorkState,
                onSaveEndpoint = onSaveEndpoint,
                onCollectOnlyWifiChange = onCollectOnlyWifiChange,
                onAutoRunChange = onAutoRunChange,
                onRunNow = onRunNow,
                onReschedule = onReschedule
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

// ══════════════════════════════════════════════════════════════════════
// Tab Row
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        SettingsTab("Privacy", Icons.Filled.Security),
        SettingsTab("Collection", Icons.Filled.Settings),
        SettingsTab("Data", Icons.Filled.Code)
    )

    SecondaryTabRow(
        selectedTabIndex = selectedTab,
        divider = { HorizontalDivider() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = { Text(tab.title) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

private data class SettingsTab(
    val title: String,
    val icon: ImageVector
)

// ══════════════════════════════════════════════════════════════════════
// Privacy Tab
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun PrivacyTab(
    settings: AppSettings?,
    onConsentChange: (Boolean) -> Unit,
    onCollectionChange: (Boolean) -> Unit,
    onFirestoreUploadsChange: (Boolean) -> Unit
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    var coarseGranted by remember { mutableStateOf(AppPermissions.hasCoarseLocation(context)) }
    var fineGranted by remember { mutableStateOf(hasFineLocation(context)) }
    var phoneGranted by remember { mutableStateOf(AppPermissions.hasPhoneState(context)) }
    var locationServicesOn by remember { mutableStateOf(isLocationServicesEnabled(context)) }

    fun refreshPermissions() {
        coarseGranted = AppPermissions.hasCoarseLocation(context)
        fineGranted = hasFineLocation(context)
        phoneGranted = AppPermissions.hasPhoneState(context)
        locationServicesOn = isLocationServicesEnabled(context)
    }

    val requestCoarse = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val requestFine = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val requestPhone = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenPadding)
            .padding(vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
    ) {
        // Consent & Collection
        SettingsSectionCard(
            title = "Consent & Collection",
            description = "Control what data is collected and uploaded"
        ) {
            AssistiveHint(
                text = "CrowdMeasure collects network measurements only when you opt in. " +
                        "Optional permissions improve measurement quality."
            )

            HorizontalDivider()

            SettingSwitchRow(
                title = "I Understand and Agree",
                subtitle = "Required before enabling collection",
                checked = settings?.consentAccepted ?: false,
                onCheckedChange = onConsentChange
            )

            HorizontalDivider()

            SettingSwitchRow(
                title = "Enable Data Collection",
                subtitle = "Allows measurement collection in the background",
                checked = settings?.collectionEnabled ?: false,
                enabled = settings?.consentAccepted == true,
                onCheckedChange = onCollectionChange
            )

            HorizontalDivider()

            SettingSwitchRow(
                title = "Enable Firestore Uploads",
                subtitle = "Uploads queued measurements when you tap 'Upload now'",
                checked = settings?.firestoreUploadsEnabled ?: false,
                enabled = settings?.consentAccepted == true && settings?.collectionEnabled == true,
                onCheckedChange = onFirestoreUploadsChange
            )
        }

        SettingsSectionCard(
            title = "Optional Permissions",
            description = "Improve measurement quality"
        ) {
            if (!locationServicesOn) {
                AssistChip(
                    onClick = { refreshPermissions() },
                    label = {
                        Text("⚠️ Location services OFF — cell metrics may be empty")
                    }
                )
                Spacer(Modifier.height(spacing.sm))
            }

            PermissionRow(
                title = "Coarse Location",
                subtitle = "Adds approximate coordinates to measurements",
                granted = coarseGranted,
                enabled = settings?.consentAccepted == true,
                onRequest = { requestCoarse.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
            )

            HorizontalDivider()

            PermissionRow(
                title = "Fine Location",
                subtitle = "Required for detailed cell signal on some devices (e.g., Samsung)",
                granted = fineGranted,
                enabled = settings?.consentAccepted == true,
                onRequest = { requestFine.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
            )

            HorizontalDivider()

            PermissionRow(
                title = "Phone State",
                subtitle = "Enables additional cell metrics on some devices",
                granted = phoneGranted,
                enabled = settings?.consentAccepted == true,
                onRequest = { requestPhone.launch(Manifest.permission.READ_PHONE_STATE) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { refreshPermissions() }) {
                    Text("Refresh Status")
                }
            }
        }

        Spacer(Modifier.height(spacing.xl))
    }
}

// ══════════════════════════════════════════════════════════════════════
// Collection Tab
// ══════════════════════════════════════════════════════════════════════
@Composable
private fun CollectionTab(
    settings: AppSettings?,
    backgroundWorkState: BackgroundWorkUiState,
    onSaveEndpoint: (String) -> Unit,
    onCollectOnlyWifiChange: (Boolean) -> Unit,
    onAutoRunChange: (Boolean, Int) -> Unit,
    onRunNow: () -> Unit,
    onReschedule: () -> Unit
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    val endpointInitial = settings?.endpointUrl.orEmpty()
    var endpoint by rememberSaveable(endpointInitial) { mutableStateOf(endpointInitial) }

    val intervalInitial = (settings?.autoRunIntervalMinutes ?: 20).toString()
    var intervalText by rememberSaveable(intervalInitial) { mutableStateOf(intervalInitial) }

    val autoRunAllowed = settings?.consentAccepted == true && settings.collectionEnabled
    val currentInterval = settings?.autoRunIntervalMinutes ?: 20

    val parsedInterval by remember(intervalText) {
        derivedStateOf { intervalText.toIntOrNull() }
    }
    val intervalValid by remember(parsedInterval) {
        derivedStateOf { parsedInterval != null && parsedInterval in 15..10_080 }
    }
    val intervalChanged by remember(parsedInterval, currentInterval) {
        derivedStateOf { parsedInterval != null && parsedInterval != currentInterval }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenPadding)
            .padding(vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
    ) {
        SettingsSectionCard(
            title = "Test Endpoint",
            description = "HTTPS URL used for network performance checks"
        ) {
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("HTTPS URL") },
                placeholder = { Text("https://api.example.com") },
                singleLine = true,
                supportingText = { Text("Use a stable HTTPS endpoint (no personal tokens).") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onSaveEndpoint(endpoint) },
                    enabled = endpoint.startsWith("https://")
                ) { Text("Save") }
            }
        }

        SettingsSectionCard(
            title = "Collection Rules",
            description = "Control when background work is allowed to use network"
        ) {
            SettingSwitchRow(
                title = "Wi-Fi only",
                subtitle = "Avoid using mobile data",
                checked = settings?.collectOnlyWifi ?: false,
                onCheckedChange = onCollectOnlyWifiChange
            )
        }

        SettingsSectionCard(
            title = "Auto-Run",
            description = "Periodic background measurements"
        ) {
            if (!autoRunAllowed) {
                InfoBanner(
                    title = "To enable Auto-Run, accept consent and enable collection.",
                    body = "You can opt out at any time.",
                    tone = BannerTone.Warning
                )
                Spacer(Modifier.height(spacing.sm))
            }

            SettingSwitchRow(
                title = "Enable Auto-Run",
                subtitle = "Runs periodically in the background",
                checked = settings?.autoRunEnabled ?: true,
                enabled = autoRunAllowed,
                onCheckedChange = { enabled ->
                    val safeInterval = (parsedInterval ?: currentInterval).coerceIn(15, 10_080)
                    onAutoRunChange(enabled, safeInterval)
                }
            )

            Spacer(Modifier.height(spacing.sm))

            OutlinedTextField(
                value = intervalText,
                onValueChange = { text ->
                    intervalText = text.filter(Char::isDigit).take(5)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Interval (minutes)") },
                placeholder = { Text("20") },
                isError = intervalText.isNotEmpty() && !intervalValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = autoRunAllowed,
                supportingText = {
                    Text(
                        when {
                            intervalText.isEmpty() -> "15–10080 minutes"
                            !intervalValid -> "Enter a value between 15 and 10080"
                            else -> "Example: 20 (default), 60 (hourly), 360 (6 hours)"
                        }
                    )
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { intervalText = currentInterval.toString() },
                    enabled = autoRunAllowed && intervalText.isNotBlank() && intervalText != currentInterval.toString()
                ) { Text("Reset") }

                Spacer(Modifier.width(spacing.sm))

                Button(
                    onClick = {
                        val safeInterval = (parsedInterval ?: currentInterval).coerceIn(15, 10_080)
                        onAutoRunChange(settings?.autoRunEnabled ?: true, safeInterval)
                    },
                    enabled = autoRunAllowed && intervalValid && intervalChanged
                ) { Text("Apply") }
            }
        }

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

        Spacer(Modifier.height(spacing.xl))
    }
}

// ══════════════════════════════════════════════════════════════════════
// Data Tab
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun DataTab(
    exportState: UiState<Unit>,
    deleteState: UiState<Unit>,
    onExport: (Context, Int) -> Unit,
    onClearExportState: () -> Unit,
    onDelete: () -> Unit,
    onClearDeleteState: () -> Unit
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    var exportCount by remember { mutableStateOf("50") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenPadding)
            .padding(vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
    ) {
        SettingsSectionCard(
            title = "Export Data",
            description = "Export measurements as JSON for analysis"
        ) {
            OutlinedTextField(
                value = exportCount,
                onValueChange = { text ->
                    exportCount = text.filter(Char::isDigit).take(5)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Number of records") },
                placeholder = { Text("50") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                supportingText = {
                    Text("Export last N measurements (1–10000)")
                }
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

            // Export status
            when (exportState) {
                UiState.Idle -> { /* Nothing */
                }

                UiState.Loading -> {
                    Text(
                        text = "Exporting...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is UiState.Success -> {
                    Text(
                        text = "✓ Exported successfully",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(3000)
                        onClearExportState()
                    }
                }

                is UiState.Error -> {
                    Text(
                        text = "⚠️ ${exportState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onClearExportState) {
                        Text("Dismiss")
                    }
                }
            }
        }

        SettingsSectionCard(
            title = "Delete Local Data",
            description = "Permanently delete all measurements from this device"
        ) {
            AssistiveHint(
                text = "⚠️ This action cannot be undone. All local measurements will be deleted."
            )

            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete All Data")
            }

            // Delete status
            when (deleteState) {
                UiState.Idle -> { /* Nothing */
                }

                UiState.Loading -> {
                    Text(
                        text = "Deleting...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is UiState.Success -> {
                    Text(
                        text = "✓ All data deleted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(3000)
                        onClearDeleteState()
                    }
                }

                is UiState.Error -> {
                    Text(
                        text = "⚠️ ${deleteState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onClearDeleteState) {
                        Text("Dismiss")
                    }
                }
            }
        }

        Spacer(Modifier.height(spacing.xl))
    }
}

private fun hasFineLocation(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SettingsScreenContent(
        contentPadding = PaddingValues(),
        settings = null,
        exportState = UiState.Idle,
        deleteState = UiState.Idle,
        backgroundWorkState = BackgroundWorkUiState.loading(),
        onSaveEndpoint = {},
        onCollectOnlyWifiChange = {},
        onAutoRunChange = { _, _ -> },
        onRunNow = {},
        onReschedule = {},
        onExport = { _, _ -> },
        onClearExportState = {},
        onDelete = {},
        onClearDeleteState = {},
        onSetConsent = {},
        onSetCollection = {},
        onSetFirestoreUploads = {}
    )
}

