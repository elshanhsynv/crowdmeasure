package com.example.crowdmeasure.presentation.screens.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.presentation.screens.settings.BackgroundWorkUiState
import com.example.crowdmeasure.presentation.ui.components.cards.AssistiveHint
import com.example.crowdmeasure.presentation.ui.components.cards.SettingsSectionCard

@Composable
fun BackgroundWorkStatusCard(
    state: BackgroundWorkUiState,
    onRunNow: () -> Unit,
    onReschedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState = remember(state) {
        BackgroundWorkStatusUiModel.from(state)
    }

    SettingsSectionCard(
        title = "Background Collection",
        description = "WorkManager schedule and sync diagnostics",
        icon = Icons.Outlined.Schedule,
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusSummaryRow(
                label = "Next work",
                value = uiState.nextWorkLabel,
                tone = uiState.nextWorkTone
            )

            DiagnosticsSection(
                title = "Auto-run",
                items = uiState.autoRunMetrics
            )

            DiagnosticsSection(
                title = "Upload",
                items = uiState.uploadMetrics
            )

            if (uiState.errorMessage != null) {
                ErrorMessageBox(
                    title = "Last error",
                    message = uiState.errorMessage
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
}

@Composable
private fun StatusSummaryRow(
    label: String,
    value: String,
    tone: StatusTone,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            StatusChip(
                text = value,
                tone = tone
            )
        }
    }
}

@Composable
private fun DiagnosticsSection(
    title: String,
    items: List<StatusMetric>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(title)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items.forEachIndexed { index, item ->
                    CompactKeyValueRow(
                        label = item.label,
                        value = item.value
                    )

                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun CompactKeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 34.dp)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .widthIn(min = 108.dp, max = 164.dp)
                .weight(0.44f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.56f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusChip(
    text: String,
    tone: StatusTone
) {
    val colors = statusToneColors(tone)

    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = colors.container,
            disabledLabelColor = colors.content
        ),
        border = BorderStroke(
            width = 1.dp,
            color = colors.border
        )
    )
}

@Composable
private fun ErrorMessageBox(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.76f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall
                )
            }
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

            Spacer(Modifier.width(8.dp))

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

            Spacer(Modifier.width(8.dp))

            Text("Reschedule")
        }
    }
}

@Immutable
private data class BackgroundWorkStatusUiModel(
    val nextWorkLabel: String,
    val nextWorkTone: StatusTone,
    val autoRunMetrics: List<StatusMetric>,
    val uploadMetrics: List<StatusMetric>,
    val errorMessage: String?
) {
    companion object {
        fun from(state: BackgroundWorkUiState): BackgroundWorkStatusUiModel {
            return BackgroundWorkStatusUiModel(
                nextWorkLabel = state.nextScheduledWorkStateLabel,
                nextWorkTone = state.nextScheduledWorkStateLabel.toStatusTone(),
                autoRunMetrics = listOf(
                    StatusMetric("Interval", state.intervalMinutesLabel),
                    StatusMetric("Last start", state.lastStartLabel),
                    StatusMetric("Last end", state.lastEndLabel),
                    StatusMetric("Result", state.lastResultLabel),
                    StatusMetric("Code", state.autoRunLastCodeLabel),
                    StatusMetric("Last successful collection", state.autoRunLastSuccessfulCollectionLabel),
                    StatusMetric("Last measurement", state.autoRunLastMeasurementLabel)
                ),
                uploadMetrics = listOf(
                    StatusMetric("Last start", state.uploadLastStartLabel),
                    StatusMetric("Last end", state.uploadLastEndLabel),
                    StatusMetric("Result", state.uploadLastResultLabel),
                    StatusMetric("Code", state.uploadLastCodeLabel),
                    StatusMetric("Last successful upload", state.uploadLastSuccessfulUploadLabel),
                    StatusMetric("Uploaded", state.lastUploadedLabel),
                    StatusMetric("Pending records", state.pendingRecordsLabel),
                    StatusMetric("Failed records", state.failedRecordsLabel)
                ),
                errorMessage = state.lastErrorLabel
                    .takeIf { it.isNotBlank() && !it.equals("None", ignoreCase = true) }
            )
        }
    }
}

@Immutable
private data class StatusMetric(
    val label: String,
    val value: String
)

@Immutable
private enum class StatusTone {
    Neutral,
    Success,
    Warning,
    Error
}

private fun String.toStatusTone(): StatusTone {
    val value = lowercase()

    return when {
        value.contains("succeeded") ||
                value.contains("success") ||
                value.contains("enqueued") ||
                value.contains("running") -> StatusTone.Success

        value.contains("blocked") ||
                value.contains("cancelled") ||
                value.contains("failed") -> StatusTone.Error

        value.contains("none") ||
                value.contains("unknown") ||
                value.contains("not scheduled") -> StatusTone.Warning

        else -> StatusTone.Neutral
    }
}

@Stable
private data class StatusToneColors(
    val container: Color,
    val content: Color,
    val border: Color
)

@Composable
private fun statusToneColors(
    tone: StatusTone
): StatusToneColors {
    return when (tone) {
        StatusTone.Neutral -> StatusToneColors(
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            border = Color.Transparent
        )

        StatusTone.Success -> StatusToneColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            border = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        )

        StatusTone.Warning -> StatusToneColors(
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            border = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
        )

        StatusTone.Error -> StatusToneColors(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
            border = MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        )
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