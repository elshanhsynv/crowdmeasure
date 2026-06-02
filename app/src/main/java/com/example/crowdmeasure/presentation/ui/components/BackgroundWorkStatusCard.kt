package com.example.crowdmeasure.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.screens.settings.BackgroundWorkUiState

@Composable
fun BackgroundWorkStatusCard(
    state: BackgroundWorkUiState,
    onRunNow: () -> Unit,
    onReschedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSectionCard(
        title = "Background Collection",
        description = "WorkManager scheduling status",
        icon = Icons.Outlined.Schedule,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Next Work State",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = state.nextScheduledWorkStateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Auto-run",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            KeyValueRow("Interval", state.intervalMinutesLabel)
            KeyValueRow("Last Start", state.lastStartLabel)
            KeyValueRow("Last End", state.lastEndLabel)
            KeyValueRow("Result", state.lastResultLabel)
            KeyValueRow("Code", state.autoRunLastCodeLabel)
            KeyValueRow("Last Successful Collection", state.autoRunLastSuccessfulCollectionLabel)
            KeyValueRow("Last Measurement", state.autoRunLastMeasurementLabel)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Upload",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            KeyValueRow("Last Start", state.uploadLastStartLabel)
            KeyValueRow("Last End", state.uploadLastEndLabel)
            KeyValueRow("Result", state.uploadLastResultLabel)
            KeyValueRow("Code", state.uploadLastCodeLabel)
            KeyValueRow("Last Successful Upload", state.uploadLastSuccessfulUploadLabel)
            KeyValueRow("Uploaded", state.lastUploadedLabel)
            KeyValueRow("Pending Records", state.pendingRecordsLabel)
            KeyValueRow("Failed Records", state.failedRecordsLabel)
        }

        if (state.lastErrorLabel != "None") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Last Error",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = state.lastErrorLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRunNow,
                enabled = state.canRunNow,
                modifier = Modifier.weight(1f)
            ) {
                Text("Run Now")
            }
            OutlinedButton(
                onClick = onReschedule,
                enabled = state.canReschedule,
                modifier = Modifier.weight(1f)
            ) {
                Text("Reschedule")
            }
        }

        AssistiveHint("Periodic work is inexact and may run later due to Doze mode or battery optimizations.")
    }
}

@Preview
@Composable
private fun BackgroundWorkStatusCardPreview() {
    BackgroundWorkStatusCard(
        state = BackgroundWorkUiState.loading(),
        onRunNow = {},
        onReschedule = {}
    )
}
