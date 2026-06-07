@file:OptIn(ExperimentalLayoutApi::class)

package com.example.crowdmeasure.presentation.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.presentation.ui.components.cards.DetailSectionCard
import com.example.crowdmeasure.presentation.ui.components.cards.SectionDivider
import com.example.crowdmeasure.presentation.ui.components.metrics.MetricChip
import com.example.crowdmeasure.presentation.ui.components.metrics.MetricRow
import com.example.crowdmeasure.presentation.ui.components.privacy.SensitiveValueRow
import com.example.crowdmeasure.presentation.ui.components.states.AppEmptyState
import com.example.crowdmeasure.presentation.ui.components.states.AppErrorState
import com.example.crowdmeasure.presentation.ui.components.states.AppLoadingState
import com.example.crowdmeasure.presentation.ui.theme.CrowdMeasureTheme
import com.example.crowdmeasure.presentation.util.UiState


@Composable
fun MeasurementDetailScreen(
    id: String,
    contentPadding: PaddingValues,
    viewModel: MeasurementDetailViewModel = hiltViewModel<MeasurementDetailViewModel>()
) {
    LaunchedEffect(id) { viewModel.load(id) }
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (val loadState = state.loadState) {
            UiState.Loading -> item(key = "loading") {
                AppLoadingState(message = "Loading measurement...")
            }

            is UiState.Error -> item(key = "error") {
                AppErrorState(message = loadState.message, onRetry = onRetry)
            }

            is UiState.Success -> {
                val m = loadState.data

                item(key = "summary") { SummaryHeroCard(m) }

                item(key = "device") {
                    DeviceSection(pairs = m.meta)
                }

                item(key = "environment") {
                    EnvironmentSection(
                        pairs = m.env,
                        locationText = m.locationText,
                        locationRevealed = state.revealed.contains(RevealKey.Location),
                        onToggleLocation = { onToggleReveal(RevealKey.Location) }
                    )
                }

                m.wifi?.let { wifiPairs ->
                    item(key = "wifi") { WifiSection(pairs = wifiPairs) }
                }

                m.cell?.let { cellPairs ->
                    item(key = "cell") {
                        CellularSection(
                            pairs = cellPairs,
                            sims = m.sims,
                            collectedSimText = m.collectedSimText,
                            cellIdsText = m.cellIdsText,
                            cellIdsRevealed = state.revealed.contains(RevealKey.CellIds),
                            onToggleCellIds = { onToggleReveal(RevealKey.CellIds) }
                        )
                    }
                }

                m.ip?.let { ipPairs ->
                    item(key = "ip") { IpSection(pairs = ipPairs) }
                }

                item(key = "performance") {
                    PerformanceSection(
                        performance = m.performance,
                        endpointId = m.endpointId,
                        endpointRevealed = state.revealed.contains(RevealKey.Endpoint),
                        onToggleEndpoint = { onToggleReveal(RevealKey.Endpoint) }
                    )
                }
            }

            UiState.Idle -> item(key = "empty") {
                AppEmptyState(
                    title = "Measurement unavailable",
                    description = "This measurement has not been loaded yet."
                )
            }
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SummaryHeroCard(measurement: MeasurementDetailUi) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val summaryIcon = if (measurement.wifi != null) {
        Icons.Outlined.Wifi
    } else {
        Icons.Outlined.CellTower
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 100.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val cx = size.width - 64.dp.toPx()
                val cy = size.height / 2f
                listOf(40.dp, 68.dp, 98.dp).forEach { r ->
                    drawCircle(
                        color = onContainerColor.copy(alpha = 0.07f),
                        radius = r.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, bottom = 16.dp, end = 96.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Measurement",
                    style = MaterialTheme.typography.titleSmall,
                    color = onContainerColor.copy(alpha = 0.65f)
                )
                Text(
                    text = "Summary",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = onContainerColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = measurement.timeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainerColor.copy(alpha = 0.75f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = onContainerColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "ID  ${measurement.id.take(8)}…",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = onContainerColor.copy(alpha = 0.75f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = onContainerColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 36.dp)
                    .size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = summaryIcon,
                        contentDescription = null,
                        tint = onContainerColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceSection(pairs: List<Pair<String, String?>>) {
    val chips = pairs.mapNotNull { (k, v) -> v?.let { k to it } }
    if (chips.isEmpty()) return

    DetailSectionCard(
        title = "Device & App",
        description = "Environment information",
        icon = Icons.Outlined.PhoneAndroid
    ) {
        MetricChipGrid(pairs = chips)
    }
}

@Composable
private fun EnvironmentSection(
    pairs: List<Pair<String, String>>,
    locationText: String?,
    locationRevealed: Boolean,
    onToggleLocation: () -> Unit
) {
    DetailSectionCard(
        title = "Network Context",
        description = "Connection and device state",
        icon = Icons.Outlined.Language
    ) {
        MetricChipGrid(pairs = pairs, columns = 3)
        SectionDivider(modifier = Modifier.padding(vertical = 4.dp))
        SensitiveValueRow(
            label = "Location",
            value = locationText,
            revealed = locationRevealed,
            onToggleReveal = onToggleLocation
        )
    }
}

@Composable
private fun WifiSection(pairs: List<Pair<String, String>>) {
    DetailSectionCard(
        title = "Wi-Fi",
        description = "Wireless network details",
        icon = Icons.Outlined.Wifi
    ) {
        MetricChipGrid(pairs = pairs)
    }
}

@Composable
private fun IpSection(pairs: List<Pair<String, String>>) {
    DetailSectionCard(
        title = "IP Information",
        description = "Network identity and provider",
        icon = Icons.Outlined.Security
    ) {
        MetricChipGrid(pairs = pairs, columns = 3)
    }
}

@Composable
private fun CellularSection(
    pairs: List<Pair<String, String>>,
    sims: List<SimCarrierUi>,
    collectedSimText: String?,
    cellIdsText: String?,
    cellIdsRevealed: Boolean,
    onToggleCellIds: () -> Unit
) {
    DetailSectionCard(
        title = "Cellular Network",
        description = "Mobile carrier and signal",
        icon = Icons.Outlined.CellTower
    ) {
        MetricChipGrid(pairs = pairs)
        if (collectedSimText != null || sims.isNotEmpty()) {
            SectionDivider(modifier = Modifier.padding(vertical = 4.dp))
            collectedSimText?.let {
                MetricChipGrid(
                    pairs = listOf("Collected From" to it),
                    columns = 1
                )
            }
            if (sims.isNotEmpty()) {
                SimCarrierGroups(sims = sims)
            }
        }
        SectionDivider(modifier = Modifier.padding(vertical = 4.dp))
        SensitiveValueRow(
            label = "Cell Identifiers",
            value = cellIdsText,
            revealed = cellIdsRevealed,
            onToggleReveal = onToggleCellIds
        )
    }
}

@Composable
private fun SimCarrierGroups(sims: List<SimCarrierUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sims.forEachIndexed { index, sim ->
            if (index > 0 && sims.size > 1) {
                SectionDivider()
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (sim.isCollected) {
                            Icons.AutoMirrored.Outlined.FactCheck
                        } else {
                            Icons.Outlined.SimCard
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = sim.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        sim.subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                MetricChipGrid(pairs = sim.pairs)
            }
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
    DetailSectionCard(
        title = "Performance",
        description = "Latency and throughput metrics",
        icon = Icons.Outlined.Speed
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

            MetricChipGrid(
                pairs = listOf(
                    "HTTP Status" to performance.httpStatus,
                    "Server Region" to performance.serverRegion
                )
            )

            PerformanceGroup(
                title = "Connection",
                chips = listOf(
                    "DNS" to performance.dns,
                    "Connection" to performance.connect,
                    "TLS" to performance.tls,
                    "TTFB Average" to performance.ttfbAvg
                )
            )

            PerformanceGroup(
                title = "Latency",
                chips = listOf(
                    "HTTP Latency Average" to performance.httpLatencyAvg,
                    "HTTP Latency P95" to performance.httpLatencyP95,
                    "Jitter" to performance.jitter,
                    "Probe Failure %" to performance.probeFailure
                )
            )

            PerformanceGroup(
                title = "Stability",
                chips = listOf(
                    "Stalls" to performance.stallsCount,
                    "Max Stall" to performance.maxStall
                )
            )

            PerformanceGroup(
                title = "Throughput",
                chips = listOf(
                    "Down" to performance.down,
                    "Up" to performance.up,
                    "Down P95" to performance.downP95,
                    "Up P95" to performance.upP95,
                    "Down StdDev" to performance.downStdDev,
                    "Up StdDev" to performance.upStdDev
                )
            )
        }
    }
}

@Composable
private fun PerformanceGroup(
    title: String,
    chips: List<Pair<String, String>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        MetricChipGrid(pairs = chips)
    }
}

@Composable
private fun MetricChipGrid(
    modifier: Modifier = Modifier,
    pairs: List<Pair<String, String>>,
    columns: Int = 2
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = columns
    ) {
        pairs.forEach { (label, value) ->
            MetricChip(
                label = label,
                value = value.ifBlank { "—" },
                icon = MetricIconMapper.iconFor(label),
                modifier = if (columns > 1) Modifier.weight(1f) else Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MeasurementDetailScreenPreview() {
    CrowdMeasureTheme {
        MeasurementDetailContent(
            contentPadding = PaddingValues(0.dp),
            state = MeasurementDetailUiState(loadState = UiState.Loading),
            onToggleReveal = {},
            onRetry = {}
        )
    }
}
