package com.example.crowdmeasure.presentation.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.presentation.ui.components.DetailSectionCard
import com.example.crowdmeasure.presentation.ui.components.MetricGridRow
import com.example.crowdmeasure.presentation.ui.components.MetricRow
import com.example.crowdmeasure.presentation.ui.components.SectionDivider
import com.example.crowdmeasure.presentation.ui.components.privacy.SensitiveValueRow
import com.example.crowdmeasure.presentation.ui.theme.LocalSpacing
import com.example.crowdmeasure.presentation.util.UiState

@Composable
fun MeasurementDetailScreen(
    id: String,
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    viewModel: MeasurementDetailViewModel = hiltViewModel<MeasurementDetailViewModel>()
) {
    LaunchedEffect(id) {
        viewModel.load(id)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MeasurementDetailContent(
        contentPadding = contentPadding,
        state = uiState,
        onToggleReveal = viewModel::toggleReveal,
        onRetry = { viewModel.load(id) }
    )
}

@Composable
private fun MeasurementDetailContent(
    contentPadding: PaddingValues,
    state: MeasurementDetailUiState,
    onToggleReveal: (RevealKey) -> Unit,
    onRetry: () -> Unit
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(
            horizontal = spacing.screenPadding,
            vertical = spacing.sm
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
    ) {
        when (val loadState = state.loadState) {
            UiState.Loading -> {
                item(key = "loading") {
                    LoadingState()
                }
            }

            is UiState.Error -> {
                item(key = "error") {
                    ErrorState(
                        message = loadState.message,
                        onRetry = onRetry
                    )
                }
            }

            is UiState.Success -> {
                val measurement = loadState.data

                item(key = "summary") {
                    SummarySection(measurement = measurement)
                }

                // Device section
                item(key = "device") {
                    DeviceSection(pairs = measurement.header)
                }

                // Context section (includes location)
                item(key = "context") {
                    ContextSection(
                        pairs = measurement.context,
                        locationText = measurement.coarseLocationText,
                        locationRevealed = state.revealed.contains(RevealKey.Location),
                        onToggleLocation = { onToggleReveal(RevealKey.Location) }
                    )
                }

                measurement.diagnostics?.let { diagPairs ->
                    item(key = "diagnostics") {
                        DiagnosticsSection(pairs = diagPairs)
                    }
                }


                // Wi-Fi section (if present)
                measurement.wifi?.let { wifiPairs ->
                    item(key = "wifi") {
                        WifiSection(pairs = wifiPairs)
                    }
                }

                // Cellular section (if present)
                measurement.cell?.let { cellPairs ->
                    item(key = "cell") {
                        CellularSection(
                            pairs = cellPairs,
                            cellIdsText = measurement.cellIdsText,
                            cellIdsRevealed = state.revealed.contains(RevealKey.CellIds),
                            onToggleCellIds = { onToggleReveal(RevealKey.CellIds) }
                        )
                    }
                }

                // Performance section
                item(key = "performance") {
                    PerformanceSection(
                        performance = measurement.performance,
                        endpointId = measurement.endpointId,
                        endpointRevealed = state.revealed.contains(RevealKey.Endpoint),
                        onToggleEndpoint = { onToggleReveal(RevealKey.Endpoint) }
                    )
                }
            }

            UiState.Idle -> { /* Should not happen */ }
        }

        // Bottom spacing
        item(key = "bottom_spacer") {
            Spacer(Modifier.height(spacing.xl))
        }
    }
}

@Composable
private fun SummarySection(measurement: MeasurementDetailUi) {
    val spacing = LocalSpacing.current

    DetailSectionCard(
        title = "Measurement Summary",
        description = measurement.timeText
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            // ID (de-emphasized, monospace)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = measurement.id.take(12) + "...",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Feedback tag (if present)
            measurement.feedbackTag?.let { tag ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tag",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceSection(pairs: List<Pair<String, String>>) {
    DetailSectionCard(
        title = "Device & App",
        description = "Environment information"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pairs.forEach { (label, value) ->
                MetricRow(label, value)
            }
        }
    }
}

@Composable
private fun ContextSection(
    pairs: List<Pair<String, String>>,
    locationText: String?,
    locationRevealed: Boolean,
    onToggleLocation: () -> Unit
) {
    val spacing = LocalSpacing.current

    DetailSectionCard(
        title = "Network Context",
        description = "Connection and device state"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            pairs.forEach { (label, value) ->
                MetricRow(label, value)
            }

            // Sensitive: Location
//            if (locationText != null) {
                SectionDivider()
                SensitiveValueRow(
                    label = "Coarse Location",
                    value = locationText,
                    revealed = locationRevealed,
                    onToggleReveal = onToggleLocation
                )
//            }
        }
    }
}

@Composable
private fun DiagnosticsSection(pairs: List<Pair<String, String>>) {
    val spacing = LocalSpacing.current

    DetailSectionCard(
        title = "Diagnostics",
        description = "Device and system constraints"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            pairs.forEach { (label, value) -> MetricRow(label, value) }
        }
    }
}


@Composable
private fun WifiSection(pairs: List<Pair<String, String>>) {
    val spacing = LocalSpacing.current

    DetailSectionCard(
        title = "Wi-Fi",
        description = "Wireless network details"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            pairs.forEach { (label, value) ->
                MetricRow(label, value)
            }
        }
    }
}

@Composable
private fun CellularSection(
    pairs: List<Pair<String, String>>,
    cellIdsText: String?,
    cellIdsRevealed: Boolean,
    onToggleCellIds: () -> Unit
) {
    val spacing = LocalSpacing.current

    DetailSectionCard(
        title = "Cellular Network",
        description = "Mobile carrier and signal"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            // Regular cell metrics
            pairs.forEach { (label, value) ->
                MetricRow(label, value)
            }

            // Sensitive: Cell IDs
//            if (cellIdsText != null) {
                SectionDivider()
                SensitiveValueRow(
                    label = "Cell Identifiers",
                    value = cellIdsText,
                    revealed = cellIdsRevealed,
                    onToggleReveal = onToggleCellIds
                )
//            }
        }
    }
}


@Composable
private fun PerformanceSection(
    performance: PerformanceUi,
    endpointId: String?,
    endpointRevealed: Boolean,
    onToggleEndpoint: () -> Unit
) {
    val spacing = LocalSpacing.current

    DetailSectionCard(
        title = "Performance Metrics",
        description = "Latency breakdown and statistics"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {

            if (endpointId != null) {
                SensitiveValueRow(
                    label = "Endpoint",
                    value = endpointId,
                    revealed = endpointRevealed,
                    onToggleReveal = onToggleEndpoint
                )
                SectionDivider()
            }

            MetricRow("Protocol", performance.protocol)

            // NEW: server info (always shown; values may be "—")
            MetricGridRow("HTTP Status", performance.httpStatus, "Server Region", performance.serverRegion)

            SectionDivider()

            Text(
                text = "Connection Establishment",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MetricGridRow("DNS", performance.dns, "TCP", performance.tcp)
            MetricGridRow("TLS", performance.tls, "TTFB", performance.ttfb)

            SectionDivider()

            Text(
                text = "Latency Statistics",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MetricGridRow("RTT Average", performance.rttAvg, "RTT P95", performance.rttP95)
            MetricGridRow("Jitter", performance.jitter, "Packet Loss", performance.loss)

            // NEW: stalls
            SectionDivider()
            Text(
                text = "Stability",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MetricGridRow("Stalls", performance.stallsCount, "Max Stall", performance.maxStall)

            // NEW: throughput (may be "—" if you don’t run speed tests)
            SectionDivider()
            Text(
                text = "Throughput",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MetricGridRow("Down", performance.down, "Up", performance.up)
            MetricGridRow("Down P95", performance.downP95, "Down StdDev", performance.downStdDev)
            MetricGridRow("Up P95", performance.upP95, "Up StdDev", performance.upStdDev)
        }
    }
}

@Composable
private fun LoadingState() {
    DetailSectionCard(title = "Loading...") {
        Text(
            text = "Fetching measurement details...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    val spacing = LocalSpacing.current

    DetailSectionCard(
        title = "Error",
        description = "Couldn't load measurement"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            FilledTonalButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}