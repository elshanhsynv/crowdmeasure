package com.example.crowdmeasure.presentation.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.ui.components.AssistiveHint
import com.example.crowdmeasure.presentation.ui.components.SettingsSectionCard
import com.example.crowdmeasure.presentation.util.UiState

@Composable
internal fun DataSettingsTab(
    exportState: UiState<Unit>,
    callExportState: UiState<Unit>,
    deleteState: UiState<Unit>,
    onExport: (Context, Int) -> Unit,
    onClearExportState: () -> Unit,
    onExportCalls: (Context, Int) -> Unit,
    onClearCallExportState: () -> Unit,
    onDelete: () -> Unit,
    onClearDeleteState: () -> Unit
) {
    val context = LocalContext.current
    var exportCount by remember { mutableStateOf("50") }
    var callExportCount by remember { mutableStateOf("100") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        ExportMeasurementsCard(
            exportCount = exportCount,
            onExportCountChange = { exportCount = it.filter(Char::isDigit).take(5) },
            exportState = exportState,
            onExport = {
                val count = exportCount.toIntOrNull()?.coerceIn(1, 10_000) ?: 50
                onExport(context, count)
            },
            onClearExportState = onClearExportState
        )

        ExportCallsCard(
            callExportCount = callExportCount,
            onCallExportCountChange = { callExportCount = it.filter(Char::isDigit).take(4) },
            callExportState = callExportState,
            onExportCalls = {
                val count = callExportCount.toIntOrNull()?.coerceIn(1, 1_000) ?: 100
                onExportCalls(context, count)
            },
            onClearCallExportState = onClearCallExportState
        )

        DeleteLocalDataCard(
            deleteState = deleteState,
            onDeleteClick = { showDeleteDialog = true },
            onClearDeleteState = onClearDeleteState
        )

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
private fun ExportMeasurementsCard(
    exportCount: String,
    onExportCountChange: (String) -> Unit,
    exportState: UiState<Unit>,
    onExport: () -> Unit,
    onClearExportState: () -> Unit
) {
    SettingsSectionCard(
        title = "Export Data",
        description = "Export measurements as JSON for analysis",
        icon = Icons.Outlined.Code
    ) {
        OutlinedTextField(
            value = exportCount,
            onValueChange = onExportCountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Number of records") },
            placeholder = { Text("50") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            supportingText = { Text("Export last N measurements (1-10 000)") }
        )
        FilledTonalButton(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export & Share JSON")
        }
        ExportStateRow(
            exportState = exportState,
            onClearExportState = onClearExportState
        )
    }
}

@Composable
private fun ExportCallsCard(
    callExportCount: String,
    onCallExportCountChange: (String) -> Unit,
    callExportState: UiState<Unit>,
    onExportCalls: () -> Unit,
    onClearCallExportState: () -> Unit
) {
    SettingsSectionCard(
        title = "Export Call Data",
        description = "Export cellular and WhatsApp call samples as JSON",
        icon = Icons.Outlined.PhoneAndroid
    ) {
        OutlinedTextField(
            value = callExportCount,
            onValueChange = onCallExportCountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Number of sessions") },
            placeholder = { Text("100") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            supportingText = { Text("Export last N call sessions (1-1 000)") }
        )
        FilledTonalButton(
            onClick = onExportCalls,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Call Sessions JSON")
        }
        ExportStateRow(
            exportState = callExportState,
            onClearExportState = onClearCallExportState
        )
    }
}

@Composable
private fun DeleteLocalDataCard(
    deleteState: UiState<Unit>,
    onDeleteClick: () -> Unit,
    onClearDeleteState: () -> Unit
) {
    SettingsSectionCard(
        title = "Delete Local Data",
        description = "Permanently delete all measurements from this device",
        icon = Icons.Outlined.Warning
    ) {
        AssistiveHint("This action cannot be undone. All local measurements will be permanently deleted.")
        Button(
            onClick = onDeleteClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text("Delete All Data")
        }
        DeleteStateRow(
            deleteState = deleteState,
            onClearDeleteState = onClearDeleteState
        )
    }
}

@Composable
private fun ExportStateRow(
    exportState: UiState<Unit>,
    onClearExportState: () -> Unit
) {
    when (exportState) {
        UiState.Idle -> Unit
        UiState.Loading -> AssistiveHint("Exporting...")
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
private fun DeleteStateRow(
    deleteState: UiState<Unit>,
    onClearDeleteState: () -> Unit
) {
    when (deleteState) {
        UiState.Idle -> Unit
        UiState.Loading -> AssistiveHint("Deleting...")
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
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
