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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
 * Tabs:
 * 1. Privacy - Permissions
 * 2. Collection - WorkManager status
 * 3. Data - Export, delete
 */
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues, viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>()
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
        onRunNow = viewModel::runAutoRunNow,
        onReschedule = viewModel::rescheduleBackgroundWork,
        onExport = viewModel::exportData,
        onClearExportState = viewModel::clearExportState,
        onDelete = viewModel::deleteAllData,
        onClearDeleteState = viewModel::clearDeleteState,
    )

}

@Composable
private fun SettingsScreenContent(
    contentPadding: PaddingValues,
    settings: AppSettings?,
    exportState: UiState<Unit>,
    deleteState: UiState<Unit>,
    backgroundWorkState: BackgroundWorkUiState,
    onRunNow: () -> Unit,
    onReschedule: () -> Unit,
    onExport: (Context, Int) -> Unit,
    onClearExportState: () -> Unit,
    onDelete: () -> Unit,
    onClearDeleteState: () -> Unit,
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
            selectedTab = selectedTab, onTabSelected = { selectedTab = it })

        // Tab content
        when (selectedTab) {
            0 -> PrivacyTab()

            1 -> CollectionTab(
                settings = settings,
                backgroundWorkState = backgroundWorkState,
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
    selectedTab: Int, onTabSelected: (Int) -> Unit
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
                        imageVector = tab.icon, contentDescription = null
                    )
                })
        }
    }
}

private data class SettingsTab(
    val title: String, val icon: ImageVector
)

// ══════════════════════════════════════════════════════════════════════
// Privacy Tab
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun PrivacyTab(
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
        SettingsSectionCard(
            title = "Permissions", description = "Improve measurement quality"
        ) {
            if (!locationServicesOn) {
                AssistChip(onClick = { refreshPermissions() }, label = {
                    Text("⚠️ Location services OFF — cell metrics may be empty")
                })
                Spacer(Modifier.height(spacing.sm))
            }

            PermissionRow(
                title = "Coarse Location",
                subtitle = "Adds approximate coordinates to measurements",
                granted = coarseGranted,
                enabled = true,
                onRequest = { requestCoarse.launch(Manifest.permission.ACCESS_COARSE_LOCATION) })

            HorizontalDivider()

            PermissionRow(
                title = "Fine Location",
                subtitle = "Required for detailed cell signal on some devices (e.g., Samsung)",
                granted = fineGranted,
                enabled = true,
                onRequest = { requestFine.launch(Manifest.permission.ACCESS_FINE_LOCATION) })

            HorizontalDivider()

            PermissionRow(
                title = "Phone State",
                subtitle = "Enables additional cell metrics on some devices",
                granted = phoneGranted,
                enabled = true,
                onRequest = { requestPhone.launch(Manifest.permission.READ_PHONE_STATE) })

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
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
    onRunNow: () -> Unit,
    onReschedule: () -> Unit
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenPadding)
            .padding(vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
    ) {
        BackgroundWorkStatusCard(
            state = backgroundWorkState, onRunNow = onRunNow, onReschedule = onReschedule
        )

        BackgroundReliabilityCard(
            onFixScheduling = onReschedule, onOpenBatterySettings = {
                SystemSettingsIntents.openBatteryOptimizationSettings(context)
            })

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
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenPadding)
            .padding(vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
    ) {
        SettingsSectionCard(
            title = "Export Data", description = "Export measurements as JSON for analysis"
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
                })

            FilledTonalButton(
                onClick = {
                    val count = exportCount.toIntOrNull()?.coerceIn(1, 10_000) ?: 50
                    onExport(context, count)
                }, modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export & Share JSON")
            }

            when (exportState) {
                UiState.Idle -> {}

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
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete All Data")
            }
            if (showDialog) {
                OpenDeleteDialog(onDeleted = onDelete, onDismiss = { showDialog = false })
            }

            when (deleteState) {
                UiState.Idle -> {}

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

@Composable
private fun OpenDeleteDialog(onDeleted: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        confirmButton = {
            TextButton(
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                onClick = {
                    onDeleted()
                    onDismiss()
                }) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        },
        title = { Text("Delete All Data") },
        text = { Text("Are you sure you want to delete all data?") })
}

private fun hasFineLocation(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
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
        onRunNow = {},
        onReschedule = {},
        onExport = { _, _ -> },
        onClearExportState = {},
        onDelete = {},
        onClearDeleteState = {},
    )
}

