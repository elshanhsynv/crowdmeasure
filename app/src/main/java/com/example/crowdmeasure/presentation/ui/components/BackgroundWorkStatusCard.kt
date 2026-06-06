package com.example.crowdmeasure.presentation.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
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
    val hasError = state.lastErrorLabel.isNotBlank() && state.lastErrorLabel != "None"

    SettingsSectionCard(
        title = "Background Collection",
        description = "WorkManager schedule and sync diagnostics",
        icon = Icons.Outlined.Schedule,
        modifier = modifier
    ) {
        StatusSummaryRow(
            label = "Next work",
            value = state.nextScheduledWorkStateLabel
        )

        StatusMetricSection(
            title = "Auto-run",
            items = listOf(
                StatusMetric("Interval", state.intervalMinutesLabel),
                StatusMetric("Last start", state.lastStartLabel),
                StatusMetric("Last end", state.lastEndLabel),
                StatusMetric("Result", state.lastResultLabel),
                StatusMetric("Code", state.autoRunLastCodeLabel),
                StatusMetric("Last successful collection", state.autoRunLastSuccessfulCollectionLabel),
                StatusMetric("Last measurement", state.autoRunLastMeasurementLabel)
            )
        )

        StatusMetricSection(
            title = "Upload",
            items = listOf(
                StatusMetric("Last start", state.uploadLastStartLabel),
                StatusMetric("Last end", state.uploadLastEndLabel),
                StatusMetric("Result", state.uploadLastResultLabel),
                StatusMetric("Code", state.uploadLastCodeLabel),
                StatusMetric("Last successful upload", state.uploadLastSuccessfulUploadLabel),
                StatusMetric("Uploaded", state.lastUploadedLabel),
                StatusMetric("Pending records", state.pendingRecordsLabel),
                StatusMetric("Failed records", state.failedRecordsLabel)
            )
        )

        if (hasError) {
            ErrorMessageBox(
                title = "Last error",
                message = state.lastErrorLabel
            )
        }

        BackgroundWorkActions(
            onRunNow = onRunNow,
            onReschedule = onReschedule,
            canRunNow = state.canRunNow,
            canReschedule = state.canReschedule
        )

        AssistiveHint(
            text = "Periodic work is inexact and may run later because of Doze mode or battery optimizations."
        )
    }
}

@Composable
private fun StatusSummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        StatusPill(text = value)
    }
}

@Composable
private fun StatusPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1
        )
    }
}

@Immutable
private data class StatusMetric(
    val label: String,
    val value: String
)

@Composable
private fun StatusMetricSection(
    title: String,
    items: List<StatusMetric>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(title)

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                CompactKeyValueRow(
                    key = item.label,
                    value = item.value
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CompactKeyValueRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .widthIn(min = 112.dp, max = 168.dp)
                .weight(0.45f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun ErrorMessageBox(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun BackgroundWorkActions(
    canRunNow: Boolean,
    canReschedule: Boolean,
    onRunNow: () -> Unit,
    onReschedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onRunNow,
            enabled = canRunNow,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null
            )
            Text("Run now")
        }

        OutlinedButton(
            onClick = onReschedule,
            enabled = canReschedule,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null
            )
            Text("Reschedule")
        }
    }
}



@Preview(showBackground = true)
@Composable
private fun BackgroundWorkStatusCardPreview() {
    MaterialTheme {
        BackgroundWorkStatusCard(
            state = BackgroundWorkUiState.loading(),
            onRunNow = {},
            onReschedule = {}
        )
    }
}