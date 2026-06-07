package com.example.crowdmeasure.presentation.screens.callsampling

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.domain.model.CallCellSample
import com.example.crowdmeasure.domain.model.CallSession
import com.example.crowdmeasure.presentation.ui.components.states.AppEmptyState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun CallSessionsScreen(
    contentPadding: PaddingValues,
    viewModel: CallSessionsViewModel = hiltViewModel<CallSessionsViewModel>()
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val samples by viewModel.samples.collectAsStateWithLifecycle()
    var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(sessions) {
        if (selectedSessionId == null && sessions.isNotEmpty()) {
            selectedSessionId = sessions.first().sessionId
            viewModel.selectSession(sessions.first().sessionId)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (sessions.isEmpty()) {
            item(key = "empty") {
                AppEmptyState(
                    modifier = Modifier.fillParentMaxHeight(),
                    title = "No call sessions",
                    description = "Sessions appear here once a call is sampled.",
                    icon = Icons.Outlined.PhoneAndroid
                )
            }
        } else {
            item(key = "sessions_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Clear") }
                }
            }

            items(sessions, key = { it.sessionId }) { session ->
                SessionCard(
                    session = session,
                    selected = session.sessionId == selectedSessionId,
                    onClick = {
                        selectedSessionId = session.sessionId
                        viewModel.selectSession(session.sessionId)
                    }
                )
            }

            if (samples.isNotEmpty()) {
                item(key = "samples_header") {
                    Text(
                        "Samples",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(samples, key = { it.id }) { sample ->
                    SampleCard(sample)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Clear Call Data?") },
            text = { Text("This permanently deletes all local call sessions and samples.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearData()
                        selectedSessionId = null
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SessionCard(
    session: CallSession,
    selected: Boolean,
    onClick: () -> Unit
) {
    val latest = session.latestSample
    val isActive = session.endedAtUtcMs == null

    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(session.startedAtUtcMs),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = if (isActive) "Active" else durationLabel(session),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${session.callSource.name.lowercase()} • ${session.callType.name.lowercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = buildString {
                    append("${session.sampleCount} sample${if (session.sampleCount != 1) "s" else ""}")
                    latest?.rat?.let { append(" · $it") }
                    latest?.nrState?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (latest != null) {
                Text(
                    text = buildString {
                        latest.dbm?.let { append("$it dBm") }
                        latest.pci?.let { if (isNotEmpty()) append(" · "); append("PCI $it") }
                        latest.tac?.let { if (isNotEmpty()) append(" · "); append("TAC $it") }
                        latest.band?.let { if (isNotEmpty()) append(" · "); append("Band $it") }
                    }.ifEmpty { "No signal data" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SampleCard(sample: CallCellSample) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = formatTime(sample.sampledAtUtcMs),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = sample.rat ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("${sample.elapsedMs / 1000}s") }
                )
            }

            HorizontalDivider()

            MetricSection(
                title = "Signal",
                metrics = listOf(
                    "RSSI" to sample.cell.serving?.rssiDbm,
                    "dBm" to sample.dbm,
                    "RSRP" to sample.rsrpDbm,
                    "RSRQ" to sample.rsrqDb,
                    "SINR" to sample.sinrDb
                )
            )

            MetricSection(
                title = "Cell",
                metrics = listOf(
                    "Cell ID" to sample.cell.serving?.cellId,
                    "PCI" to sample.pci,
                    "TAC" to sample.tac,
                    "Band" to sample.band,
                    "NR" to sample.nrState,
                    "Neighbors" to sample.cell.neighbors.size
                )
            )
        }
    }
}

@Composable
private fun MetricSection(title: String, metrics: List<Pair<String, Any?>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            metrics.forEach { (label, value) ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = value?.toString() ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private val timeFormatter = DateTimeFormatter
    .ofPattern("MMM dd yyyy HH:mm:ss")
    .withZone(ZoneId.systemDefault())

private fun formatTime(timestampUtcMs: Long): String =
    timeFormatter.format(Instant.ofEpochMilli(timestampUtcMs))

private fun durationLabel(session: CallSession): String {
    val ended = session.endedAtUtcMs ?: System.currentTimeMillis()
    val seconds = max(0L, (ended - session.startedAtUtcMs) / 1_000L)
    return "${seconds}s"
}
