package com.example.crowdmeasure.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.crowdmeasure.presentation.ui.components.PrimaryButton
import com.example.crowdmeasure.presentation.ui.components.SectionCard
import com.example.crowdmeasure.presentation.util.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenDetail: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val last = vm.last.collectAsState().value
    val queue = vm.queueCount.collectAsState().value
    val runState = vm.runState.collectAsState().value
    val settings = vm.settings.collectAsState().value

    val consentAccepted = settings?.consentAccepted == true
    val collectionEnabled = settings?.collectionEnabled == true
    val canCollect = consentAccepted && collectionEnabled
    val isRunning = runState is UiState.Loading

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!canCollect) {
            StatusBanner(
                title = "Collection is off",
                body = "Enable consent and data collection in Settings to start measuring."
            )
        }

        SectionCard(
            title = "Actions",
            description = if (canCollect) "Run a measurement now." else "Enable collection to run measurements."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton(
                    text = if (isRunning) "Measuring…" else "Start measurement",
                    onClick = vm::startMeasurement,
                    enabled = canCollect && !isRunning
                )

                OutlinedButton(
                    onClick = vm::stopMeasurement,
                    enabled = isRunning
                ) { Text("Stop") }
            }

            Spacer(Modifier.height(8.dp))
            RunStateText(runState)
        }

        SectionCard(
            title = "Queue",
            description = "Measurements waiting to upload"
        ) {
            MetricRow(label = "Queued records", value = queue.toString())
        }

        SectionCard(
            title = "Last measurement",
            description = "Most recent saved record"
        ) {
            if (last == null) {
                Text(
                    text = "No measurements yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val fmt = rememberDateFormat()

                MetricRow("Time", fmt.format(Date(last.header.timestampUtcMs)))
                MetricRow("Transport", last.context.transport.name)
                MetricRow("Endpoint", last.performance.endpointId)
                MetricRow("RTT avg", last.performance.rttAvgMs?.let { "$it ms" } ?: "-")
                MetricRow("TTFB", last.performance.ttfbMs?.let { "$it ms" } ?: "-")

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onOpenDetail(last.header.measurementId) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open details") }
            }
        }
    }
}

@Composable
private fun StatusBanner(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun RunStateText(state: UiState<Unit>) {
    val text = when (state) {
        UiState.Idle -> "Idle"
        UiState.Loading -> "Running…"
        is UiState.Success -> "Saved locally."
        is UiState.Error -> "Error: ${state.message}"
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun rememberDateFormat(): SimpleDateFormat {
    // Recreated per composition is fine, but this avoids repeated allocations.
    return androidx.compose.runtime.remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }
}