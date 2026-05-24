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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Battery6Bar
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SettingsCell
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdmeasure.presentation.ui.components.DetailSectionCard
import com.example.crowdmeasure.presentation.ui.components.MetricChip
import com.example.crowdmeasure.presentation.ui.components.MetricRow
import com.example.crowdmeasure.presentation.ui.components.SectionDivider
import com.example.crowdmeasure.presentation.ui.components.privacy.SensitiveValueRow
import com.example.crowdmeasure.presentation.util.UiState

private fun iconForMetric(label: String): ImageVector = when (label) {
    // Device & OS
    "Device Model" -> Icons.Outlined.PhoneAndroid
    "OS Version" -> Icons.Outlined.Android
    "Android SDK" -> Icons.Outlined.Code
    "App Version" -> Icons.Outlined.Apps
    "Memory Usage" -> Icons.Outlined.Memory
    "Thermal State" -> Icons.Outlined.Tune

    // Battery & Power
    "Battery" -> Icons.Outlined.Battery6Bar
    "Charging" -> Icons.Outlined.Bolt
    "Battery Saver" -> Icons.Outlined.BatteryAlert
    "Screen On" -> Icons.Outlined.LightMode
    "Doze Mode" -> Icons.Outlined.Nightlight

    // Network General
    "Transport" -> Icons.AutoMirrored.Outlined.CompareArrows
    "Internet" -> Icons.Outlined.Language
    "Captive Portal" -> Icons.Outlined.OpenInBrowser
    "VPN" -> Icons.Outlined.VpnKey
    "Metered", "Data Saver" -> Icons.Outlined.DataUsage
    "Public IP" -> Icons.Outlined.Public
    "ISP" -> Icons.Outlined.Business
    "ASN" -> Icons.Outlined.Numbers
    "DNS", "Protocol" -> Icons.Outlined.Hub
    "TCP" -> Icons.AutoMirrored.Outlined.CompareArrows
    "TLS" -> Icons.Outlined.Lock

    // Wi-Fi
    "Signal Strength (RSSI)", "Wi-Fi Standard" -> Icons.Outlined.Wifi
    "Frequency" -> Icons.Outlined.Tune

    // Cellular / Mobile Network
    "Carrier" -> Icons.Outlined.SimCard
    "MCC", "MNC" -> Icons.Outlined.Numbers
    "ISO Country Code" -> Icons.Outlined.Flag
    "RAT", "Data Network Type", "Voice Network Type" -> Icons.Outlined.SettingsCell
    "Roaming" -> Icons.Outlined.Flight
    "Registered" -> Icons.AutoMirrored.Outlined.FactCheck
    "Cell ID", "LAC", "TAC", "PCI" -> Icons.Outlined.CellTower
    "Band", "ARFCN" -> Icons.Outlined.Sensors
    "RSRP", "RSRQ", "RSSNR" -> Icons.Outlined.SignalCellularAlt

    // Speed & Performance
    "Link Speed (legacy)", "Link Speed" -> Icons.Outlined.Speed
    "TX Link Speed", "Up", "Up P95", "Up StdDev" -> Icons.Outlined.Upload
    "RX Link Speed", "Down", "Down P95", "Down StdDev" -> Icons.Outlined.Download
    "TTFB" -> Icons.Outlined.Schedule
    "HTTP Status" -> Icons.Outlined.BarChart
    "Server Region" -> Icons.Outlined.Language
    "RTT Average" -> Icons.Outlined.Timeline
    "RTT P95" -> Icons.Outlined.BarChart
    "Jitter" -> Icons.AutoMirrored.Outlined.ShowChart
    "Packet Loss" -> Icons.Outlined.WifiOff
    "Stalls" -> Icons.Outlined.PauseCircle
    "Max Stall" -> Icons.Outlined.HourglassEmpty

    else -> Icons.Outlined.Info
}

// ─── Entry Point ─────────────────────────────────────────────────────────────

@Composable
fun MeasurementDetailScreen(
    id: String,
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    viewModel: MeasurementDetailViewModel = hiltViewModel()
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

// ─── Content ─────────────────────────────────────────────────────────────────

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
            UiState.Loading -> item(key = "loading") { LoadingState() }

            is UiState.Error -> item(key = "error") {
                ErrorState(message = loadState.message, onRetry = onRetry)
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

            UiState.Idle -> Unit
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(24.dp)) }
    }
}

// ─── Hero Summary Card ────────────────────────────────────────────────────────

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
        modifier = Modifier.fillMaxWidth().heightIn(max=150.dp),
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

// ─── Sections ─────────────────────────────────────────────────────────────────

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
                    "TCP" to performance.tcp,
                    "TLS" to performance.tls,
                    "TTFB" to performance.ttfb
                )
            )

            PerformanceGroup(
                title = "Latency",
                chips = listOf(
                    "RTT Average" to performance.rttAvg,
                    "RTT P95" to performance.rttP95,
                    "Jitter" to performance.jitter,
                    "Packet Loss" to performance.loss
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

// ─── States ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
            Text(
                text = "Loading measurement…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    DetailSectionCard(title = "Something went wrong") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

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
                icon = iconForMetric(label),
                modifier = if (columns > 1) Modifier.weight(1f) else Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
fun MeasurementDetailScreenPreview() {
    MeasurementDetailContent(
        contentPadding = PaddingValues(0.dp),
        state = MeasurementDetailUiState(loadState = UiState.Loading),
        onToggleReveal = {},
        onRetry = {}
    )
}