package com.example.crowdmeasure.presentation.screens.callsampling

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.domain.model.CallCellSample
import com.example.crowdmeasure.domain.model.CallSession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun CallSessionsScreen(
    contentPadding: PaddingValues,
    viewModel: CallSessionsViewModel = hiltViewModel()
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
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear Call Data")
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No call sessions",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            item {
                SectionTitle("Sessions")
            }
            items(sessions, key = { it.sessionId }) { session ->
                SessionRow(
                    session = session,
                    selected = session.sessionId == selectedSessionId,
                    onClick = {
                        selectedSessionId = session.sessionId
                        viewModel.selectSession(session.sessionId)
                    }
                )
            }

            item {
                SectionTitle("Samples")
            }
            items(samples, key = { it.id }) { sample ->
                SampleRow(sample)
            }
        }

        item { Spacer(Modifier.safeContentPadding()) }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Call Data?") },
            text = { Text("This deletes local call sampling sessions and samples.") },
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
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
            icon = { androidx.compose.material3.Icon(Icons.Outlined.Delete, null) }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SessionRow(
    session: CallSession,
    selected: Boolean,
    onClick: () -> Unit
) {
    val latest = session.latestSample
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(session.startedAtUtcMs), fontWeight = FontWeight.SemiBold)
                Text(
                    if (session.endedAtUtcMs == null) "Active" else durationLabel(session),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Samples ${session.sampleCount} • ${latest?.rat ?: "-"} • ${latest?.nrState ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "dbm ${latest?.dbm?.toString() ?: "-"} • PCI ${latest?.pci?.toString() ?: "-"} • TAC ${latest?.tac?.toString() ?: "-"} • Band ${latest?.band?.toString() ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SampleRow(sample: CallCellSample) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(sample.sampledAtUtcMs), fontWeight = FontWeight.SemiBold)
                Text("${sample.elapsedMs / 1_000}s")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            MetricLine("RAT", sample.rat, "NR", sample.nrState)
            MetricLine("dBm", sample.dbm, "RSRP", sample.rsrpDbm)
            MetricLine("RSRQ", sample.rsrqDb, "SINR", sample.sinrDb)
            MetricLine("PCI", sample.pci, "TAC", sample.tac)
            MetricLine("Band", sample.band, "Neighbors", sample.cell.neighbors.size)
        }
    }
}

@Composable
private fun MetricLine(labelA: String, valueA: Any?, labelB: String, valueB: Any?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$labelA ${valueA ?: "-"}", style = MaterialTheme.typography.bodySmall)
        Text("$labelB ${valueB ?: "-"}", style = MaterialTheme.typography.bodySmall)
    }
}

private val formatter = DateTimeFormatter
    .ofPattern("MMM dd HH:mm:ss")
    .withZone(ZoneId.systemDefault())

private fun formatTime(timestampUtcMs: Long): String =
    formatter.format(Instant.ofEpochMilli(timestampUtcMs))

private fun durationLabel(session: CallSession): String {
    val ended = session.endedAtUtcMs ?: System.currentTimeMillis()
    val seconds = max(0L, (ended - session.startedAtUtcMs) / 1_000L)
    return "${seconds}s"
}
