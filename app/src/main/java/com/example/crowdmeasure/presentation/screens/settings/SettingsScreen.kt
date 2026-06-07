package com.example.crowdmeasure.presentation.screens.settings

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices.PIXEL_9_PRO
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.presentation.ui.theme.CrowdMeasureTheme
import com.example.crowdmeasure.presentation.util.UiState

@Composable
fun SettingsRoute(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backgroundWorkState by viewModel.backgroundWorkState.collectAsStateWithLifecycle()
    val callSamplingStatus by viewModel.callSamplingStatus.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val callExportState by viewModel.callExportState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()

    val ensureMaintenanceScheduled by rememberUpdatedState(
        newValue = viewModel::ensureMaintenanceScheduled
    )

    LaunchedEffect(Unit) {
        ensureMaintenanceScheduled()
    }

    SettingsScreen(
        contentPadding = contentPadding,
        state = SettingsScreenState(
            settings = settings,
            exportState = exportState,
            callExportState = callExportState,
            deleteState = deleteState,
            backgroundWorkState = backgroundWorkState,
            callSamplingStatus = callSamplingStatus
        ),
        actions = SettingsScreenActions(
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
    )
}

@Composable
private fun SettingsScreen(
    contentPadding: PaddingValues,
    state: SettingsScreenState,
    actions: SettingsScreenActions
) {
    var selectedTab by rememberSaveable(
        stateSaver = SettingsTabSaver
    ) {
        mutableStateOf(SettingsTab.Privacy)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        SettingsSegmentedTabs(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        AnimatedContent(
            targetState = selectedTab,
            label = "SettingsTabContentAnimation",
//            transitionSpec = {
//                fadeIn(
//                    animationSpec = tween(
//                        durationMillis = 140,
//                        delayMillis = 40,
//                        easing = LinearOutSlowInEasing
//                    )
//                ) togetherWith fadeOut(
//                    animationSpec = tween(
//                        durationMillis = 90,
//                        easing = FastOutSlowInEasing
//                    )
//                ) using SizeTransform(clip = false)
//            },
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                SettingsTab.Privacy -> {
                    PrivacySettingsTab()
                }

                SettingsTab.Collection -> {
                    CollectionSettingsTab(
                        settings = state.settings,
                        backgroundWorkState = state.backgroundWorkState,
                        onRunNow = actions.onRunNow,
                        onReschedule = actions.onReschedule,
                        callSamplingStatus = state.callSamplingStatus,
                        onSetCallSamplingEnabled = actions.onSetCallSamplingEnabled,
                        onSetWhatsappCallSamplingEnabled = actions.onSetWhatsappCallSamplingEnabled
                    )
                }

                SettingsTab.Data -> {
                    DataSettingsTab(
                        exportState = state.exportState,
                        callExportState = state.callExportState,
                        deleteState = state.deleteState,
                        onExport = actions.onExport,
                        onClearExportState = actions.onClearExportState,
                        onExportCalls = actions.onExportCalls,
                        onClearCallExportState = actions.onClearCallExportState,
                        onDelete = actions.onDelete,
                        onClearDeleteState = actions.onClearDeleteState
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSegmentedTabs(
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsTab.entries.forEach { tab ->
                SettingsTabItem(
                    tab = tab,
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SettingsTabItem(
    tab: SettingsTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .clearAndSetSemantics {
                contentDescription = tab.title
                role = Role.Tab
                this.selected = selected
            },
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = contentColor
            )

            if (selected) {
                Text(
                    text = tab.title,
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Immutable
private data class SettingsScreenState(
    val settings: AppSettings?,
    val exportState: UiState<Unit>,
    val callExportState: UiState<Unit>,
    val deleteState: UiState<Unit>,
    val backgroundWorkState: BackgroundWorkUiState,
    val callSamplingStatus: CallSamplingStatusUiState
)

@Immutable
private data class SettingsScreenActions(
    val onRunNow: () -> Unit,
    val onReschedule: () -> Unit,
    val onSetCallSamplingEnabled: (Boolean) -> Unit,
    val onSetWhatsappCallSamplingEnabled: (Boolean) -> Unit,
    val onExport: (Context, Int) -> Unit,
    val onClearExportState: () -> Unit,
    val onExportCalls: (Context, Int) -> Unit,
    val onClearCallExportState: () -> Unit,
    val onDelete: () -> Unit,
    val onClearDeleteState: () -> Unit
)

@Immutable
private enum class SettingsTab(
    val title: String,
    val icon: ImageVector
) {
    Privacy(
        title = "Privacy",
        icon = Icons.Outlined.Security
    ),
    Collection(
        title = "Collection",
        icon = Icons.Outlined.Settings
    ),
    Data(
        title = "Data",
        icon = Icons.Outlined.Code
    )
}

private val SettingsTabSaver = Saver<SettingsTab, String>(
    save = { it.name },
    restore = { saved ->
        SettingsTab.entries.firstOrNull { it.name == saved } ?: SettingsTab.Privacy
    }
)

@Preview(showBackground = true, device = PIXEL_9_PRO)
@Composable
private fun SettingsScreenPreview() {
    CrowdMeasureTheme {
        SettingsScreen(
            contentPadding = PaddingValues(),
            state = SettingsScreenState(
                settings = null,
                exportState = UiState.Idle,
                callExportState = UiState.Idle,
                deleteState = UiState.Idle,
                backgroundWorkState = BackgroundWorkUiState.loading(),
                callSamplingStatus = CallSamplingStatusUiState.empty()
            ),
            actions = SettingsScreenActions(
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
        )
    }
}