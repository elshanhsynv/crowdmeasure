package com.example.crowdmeasure.presentation.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.presentation.util.UiState

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backgroundWorkState by viewModel.backgroundWorkState.collectAsStateWithLifecycle()
    val callSamplingStatus by viewModel.callSamplingStatus.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val callExportState by viewModel.callExportState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.ensureMaintenanceScheduled()
    }

    SettingsScreenContent(
        contentPadding = contentPadding,
        settings = settings,
        exportState = exportState,
        callExportState = callExportState,
        deleteState = deleteState,
        backgroundWorkState = backgroundWorkState,
        callSamplingStatus = callSamplingStatus,
        onRunNow = viewModel::runAutoRunNow,
        onReschedule = viewModel::rescheduleBackgroundWork,
        onSetCallSamplingEnabled = viewModel::setCallSamplingEnabled,
        onSetWhatsappCallSamplingEnabled = viewModel::setWhatsappCallSamplingEnabled,
        onExport = viewModel::exportData,
        onClearExportState = viewModel::clearExportState,
        onExportCalls = viewModel::exportCallData,
        onClearCallExportState = viewModel::clearCallExportState,
        onDelete = viewModel::deleteAllData,
        onClearDeleteState = viewModel::clearDeleteState
    )
}

@Composable
private fun SettingsScreenContent(
    contentPadding: PaddingValues,
    settings: AppSettings?,
    exportState: UiState<Unit>,
    callExportState: UiState<Unit>,
    deleteState: UiState<Unit>,
    backgroundWorkState: BackgroundWorkUiState,
    callSamplingStatus: CallSamplingStatusUiState,
    onRunNow: () -> Unit,
    onReschedule: () -> Unit,
    onSetCallSamplingEnabled: (Boolean) -> Unit,
    onSetWhatsappCallSamplingEnabled: (Boolean) -> Unit,
    onExport: (Context, Int) -> Unit,
    onClearExportState: () -> Unit,
    onExportCalls: (Context, Int) -> Unit,
    onClearCallExportState: () -> Unit,
    onDelete: () -> Unit,
    onClearDeleteState: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        SettingsTabRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        when (selectedTab) {
            SettingsTab.Privacy.ordinal -> PrivacySettingsTab()
            SettingsTab.Collection.ordinal -> CollectionSettingsTab(
                settings = settings,
                backgroundWorkState = backgroundWorkState,
                onRunNow = onRunNow,
                onReschedule = onReschedule,
                callSamplingStatus = callSamplingStatus,
                onSetCallSamplingEnabled = onSetCallSamplingEnabled,
                onSetWhatsappCallSamplingEnabled = onSetWhatsappCallSamplingEnabled
            )

            SettingsTab.Data.ordinal -> DataSettingsTab(
                exportState = exportState,
                callExportState = callExportState,
                deleteState = deleteState,
                onExport = onExport,
                onClearExportState = onClearExportState,
                onExportCalls = onExportCalls,
                onClearCallExportState = onClearCallExportState,
                onDelete = onDelete,
                onClearDeleteState = onClearDeleteState
            )
        }
    }
}

private enum class SettingsTab(
    val title: String,
    val icon: ImageVector
) {
    Privacy("Privacy", Icons.Outlined.Security),
    Collection("Collection", Icons.Outlined.Settings),
    Data("Data", Icons.Outlined.Code)
}

@Composable
private fun SettingsTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
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
            SettingsTab.entries.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                Surface(
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
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
                            tint = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SettingsScreenContent(
        contentPadding = PaddingValues(),
        settings = null,
        exportState = UiState.Idle,
        callExportState = UiState.Idle,
        deleteState = UiState.Idle,
        backgroundWorkState = BackgroundWorkUiState.loading(),
        callSamplingStatus = CallSamplingStatusUiState.empty(),
        onRunNow = {},
        onReschedule = {},
        onSetCallSamplingEnabled = {},
        onSetWhatsappCallSamplingEnabled = {},
        onExport = { _, _ -> },
        onClearExportState = {},
        onExportCalls = { _, _ -> },
        onClearCallExportState = {},
        onDelete = {},
        onClearDeleteState = {}
    )
}
