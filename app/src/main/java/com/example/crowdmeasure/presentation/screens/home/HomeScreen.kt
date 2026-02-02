package com.example.crowdmeasure.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
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

    Column(
        modifier = Modifier.padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (settings?.consentAccepted != true || settings.collectionEnabled != true) {
            SectionCard {
                Text("Collection is disabled", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("Enable consent + collection in Onboarding (reopen app) or in Settings.")
            }
        }

        SectionCard {
            Text("Actions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    text = "Start measurement",
                    onClick = vm::startMeasurement,
                    enabled = runState !is UiState.Loading
                )
                OutlinedButton(onClick = vm::stopMeasurement) { Text("Stop") }
            }
            Spacer(Modifier.height(8.dp))
            when (runState) {
                UiState.Idle -> Text("Idle")
                UiState.Loading -> Text("Running…")
                is UiState.Success -> Text("Saved locally.")
                is UiState.Error -> Text("Error: ${(runState as UiState.Error).message}")
            }
        }

        SectionCard {
            Text("Queue", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Queued records (not uploaded): $queue")
        }

        SectionCard {
            Text("Last measurement", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            if (last == null) {
                Text("No measurements yet.")
            } else {
                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                Text("Time: ${fmt.format(Date(last.header.timestampUtcMs))}")
                Text("Transport: ${last.context.transport}")
                Text("Endpoint: ${last.performance.endpointId}")
                Text("RTT avg: ${last.performance.rttAvgMs ?: "-"} ms")
                Text("TTFB: ${last.performance.ttfbMs ?: "-"} ms")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onOpenDetail(last.header.measurementId) }) {
                    Text("Open details")
                }
            }
        }
    }
}