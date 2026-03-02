package com.example.crowdmeasure.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.screens.settings.BackgroundWorkUiState
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing

@Composable
fun BackgroundWorkStatusCard(
    state: BackgroundWorkUiState,
    onRunNow: () -> Unit,
    onReschedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    SettingsSectionCard(
        title = "Background Collection Status",
        modifier = modifier
    ) {
        // Status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WorkManager State",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatusPill(label = state.workManagerStateLabel)
        }

        HorizontalDivider()

        // Metrics
        KeyValueRow("Interval", state.intervalMinutesLabel)
        KeyValueRow("Last Start", state.lastStartLabel)
        KeyValueRow("Last End", state.lastEndLabel)
        KeyValueRow("Result", state.lastResultLabel)
        KeyValueRow("Uploaded", state.lastUploadedLabel)

        HorizontalDivider()

        // Error (full width)
        Column {
            Text(
                text = "Last Error",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.lastErrorLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.lastErrorLabel != "None") {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
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
                Text("Re-schedule")
            }
        }

        AssistiveHint(
            text = "Periodic work is inexact and may run later due to battery optimizations or Doze mode."
        )
    }
}

@Preview
@Composable
private fun BackgroundWorkStatusCardPreview() {
    BackgroundWorkStatusCard(
        state = BackgroundWorkUiState.loading(),
        onRunNow = {},
        onReschedule = {},
    )
}