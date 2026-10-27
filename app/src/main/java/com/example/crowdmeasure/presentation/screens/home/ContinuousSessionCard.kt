package com.example.crowdmeasure.presentation.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.crowdmeasure.domain.model.ContinuousSessionDurationsMinutes
import com.example.crowdmeasure.domain.model.ContinuousSessionGapsSeconds
import com.example.crowdmeasure.domain.model.MAX_CUSTOM_GAP_SECONDS
import com.example.crowdmeasure.domain.model.MAX_CUSTOM_SESSION_MINUTES
import com.example.crowdmeasure.domain.model.durationLabel
import com.example.crowdmeasure.domain.model.gapLabel
import com.example.crowdmeasure.domain.model.isValidCustomDuration
import com.example.crowdmeasure.domain.model.isValidCustomGap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ContinuousSessionCard(
    state: HomeUiState,
    onSelectDuration: (Int) -> Unit,
    onSelectGap: (Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onArm: () -> Unit,
    onCancelArmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val running = state.continuous.running
    val armed = state.armedSession

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Continuous",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )

            when {
                running -> RunningContent(state = state, onStop = onStop)
                armed != null -> ArmedContent(
                    durationMinutes = armed.durationMinutes,
                    gapSeconds = armed.gapSeconds,
                    onCancel = onCancelArmed
                )
                else -> IdleContent(
                    state = state,
                    onSelectDuration = onSelectDuration,
                    onSelectGap = onSelectGap,
                    onStart = onStart,
                    onArm = onArm
                )
            }

            state.continuous.lastError?.let { error ->
                Text(
                    text = "Last attempt failed: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun RunningContent(state: HomeUiState, onStop: () -> Unit) {
    val endsAt = state.continuous.endsAtUtcMs
    val endsText = endsAt?.let { "Ends ${formatClock(it)}" } ?: "Running"

    Text(
        text = "$endsText · ${state.continuous.collectedCount} collected",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    FilledTonalButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
        Text("Stop")
    }
}

@Composable
private fun ArmedContent(durationMinutes: Int, gapSeconds: Int, onCancel: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Starts after next measurement · ${durationLabel(durationMinutes)} · gap ${gapLabel(gapSeconds)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
private fun IdleContent(
    state: HomeUiState,
    onSelectDuration: (Int) -> Unit,
    onSelectGap: (Int) -> Unit,
    onStart: () -> Unit,
    onArm: () -> Unit,
) {
    val canStart = state.canCollect && !state.isMeasuring
    var editingTimer by remember { mutableStateOf(false) }
    var editingGap by remember { mutableStateOf(false) }

    ChoiceRow(
        label = "Timer",
        options = ContinuousSessionDurationsMinutes,
        selected = state.selectedSessionMinutes,
        labelOf = ::durationLabel,
        onSelect = onSelectDuration,
        onCustom = { editingTimer = true },
    )
    ChoiceRow(
        label = "Gap",
        options = ContinuousSessionGapsSeconds,
        selected = state.selectedSessionGapSeconds,
        labelOf = ::gapLabel,
        onSelect = onSelectGap,
        onCustom = { editingGap = true },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onStart,
            enabled = canStart,
            modifier = Modifier.weight(1f)
        ) {
            Text("Start")
        }
        OutlinedButton(
            onClick = onArm,
            enabled = canStart,
            modifier = Modifier.weight(1f)
        ) {
            Text("After next")
        }
    }

    if (editingTimer) {
        CustomValueDialog(
            title = "Custom timer",
            unit = "Minutes",
            supporting = "1 to $MAX_CUSTOM_SESSION_MINUTES",
            initial = state.selectedSessionMinutes,
            isValid = ::isValidCustomDuration,
            onConfirm = {
                onSelectDuration(it)
                editingTimer = false
            },
            onDismiss = { editingTimer = false },
        )
    }
    if (editingGap) {
        CustomValueDialog(
            title = "Custom gap",
            unit = "Seconds",
            supporting = "0 to $MAX_CUSTOM_GAP_SECONDS",
            initial = state.selectedSessionGapSeconds,
            isValid = ::isValidCustomGap,
            onConfirm = {
                onSelectGap(it)
                editingGap = false
            },
            onDismiss = { editingGap = false },
        )
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    options: List<Int>,
    selected: Int,
    labelOf: (Int) -> String,
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    val isCustom = selected !in options

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(labelOf(option), style = MaterialTheme.typography.labelMedium) }
                )
            }
            FilterChip(
                selected = isCustom,
                onClick = onCustom,
                label = {
                    Text(
                        text = if (isCustom) labelOf(selected) else "Custom",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

@Composable
private fun CustomValueDialog(
    title: String,
    unit: String,
    supporting: String,
    initial: Int,
    isValid: (Int?) -> Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial.toString()) }
    val value = text.toIntOrNull()
    val valid = isValid(value)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(5) },
                label = { Text(unit) },
                supportingText = { Text(supporting) },
                isError = !valid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { value?.let(onConfirm) }
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun formatClock(utcMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(utcMs))
