package com.example.crowdmeasure.presentation.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.crowdmeasure.presentation.ui.components.SectionCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MeasurementDetailScreen(
    id: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    vm: MeasurementDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(id) { vm.load(id) }
    val item = vm.item.collectAsState().value

    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top header (more compact than a full-width button)
        HeaderBar(
            title = "Measurement details",
            subtitle = id,
            onBack = onBack
        )

        if (item == null) {
            SectionCard(
                title = "Not found",
                description = "This measurement may have been deleted."
            ) {
                Text(
                    "Try going back and selecting another item.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        // Header
        SectionCard(
            title = "Header",
            description = fmt.format(Date(item.header.timestampUtcMs))
        ) {
            KeyValue("Measurement ID", item.header.measurementId)
            KeyValue("App version", item.header.appVersion ?: "-")
            KeyValue("Android", item.header.androidVersion ?: "-")
            KeyValue("Device", item.header.deviceModel ?: "-")
            KeyValue("Consent version", item.header.userConsentVersion.toString() ?: "-")
        }

        // Context
        SectionCard(title = "Context") {
            // transport might be enum/sealed: always stringify safely
            KeyValue("Transport", item.context.transport?.toString() ?: "-")
            KeyValue("Validated", item.context.validatedInternet?.toString() ?: "-")
            KeyValue("Captive portal", item.context.captivePortal?.toString() ?: "-")
            KeyValue("Metered", item.context.metered?.toString() ?: "-")
            KeyValue("VPN", item.context.vpnPresent?.toString() ?: "-")

            Spacer(Modifier.height(6.dp))

            KeyValue("Battery saver", item.context.batterySaver.toString())
            KeyValue("Charging", item.context.charging.toString())
            KeyValue("Screen on", item.context.screenOn.toString())

            val loc = item.context.coarseLocation
            KeyValue(
                "Coarse location",
                loc?.let { "${it.lat}, ${it.lon} (~${it.accuracyMeters}m)" } ?: "-"
            )
        }

        // Wi-Fi (optional)
        item.wifi?.let { w ->
            SectionCard(title = "Wi-Fi") {
                KeyValue("RSSI", w.rssi?.let { "$it dBm" } ?: "-")
                KeyValue("Link speed", w.linkSpeedMbps?.let { "$it Mbps" } ?: "-")
                KeyValue("Frequency", w.frequencyMhz?.let { "$it MHz" } ?: "-")
                KeyValue("Channel width", w.channelWidthMhz?.let { "$it MHz" } ?: "-")
            }
        }

        // Cell (optional)
        item.cell?.let { c ->
            SectionCard(title = "Cell") {
                KeyValue("Carrier", c.carrierName ?: "-")
                KeyValue("MCC / MNC", "${c.mcc ?: "-"} / ${c.mnc ?: "-"}")
                KeyValue("Data type", c.dataNetworkType ?: "-")
                KeyValue("Voice type", c.voiceNetworkType ?: "-")
                KeyValue("Roaming", c.roaming?.toString() ?: "-")
                KeyValue("Registered RAT", c.registeredRat ?: "-")

                Spacer(Modifier.height(6.dp))

                val sc = c.servingCell
                KeyValue("CI", sc?.ci?.toString() ?: "-")
                KeyValue("NCI", sc?.nci?.toString() ?: "-")
                KeyValue("TAC", sc?.tac?.toString() ?: "-")
                KeyValue("PCI", sc?.pci?.toString() ?: "-")
                KeyValue("EARFCN", sc?.earfcn?.toString() ?: "-")
                KeyValue("NRARFCN", sc?.nrarfcn?.toString() ?: "-")
                KeyValue("Band", sc?.band?.toString() ?: "-")

                Spacer(Modifier.height(6.dp))

                val sig = c.signal
                KeyValue("RSRP", sig?.rsrp?.toString() ?: "-")
                KeyValue("RSRQ", sig?.rsrq?.toString() ?: "-")
                KeyValue("SINR", sig?.sinr?.toString() ?: "-")
                KeyValue("RSSI", sig?.rssi?.toString() ?: "-")

                Spacer(Modifier.height(6.dp))

                KeyValue(
                    "Availability",
                    "cellInfo=${c.availability.cellInfoAccessible}, ids=${c.availability.idsAccessible}, signal=${c.availability.signalAccessible}"
                )
            }
        }

        // Performance
        SectionCard(title = "Performance") {
            val p = item.performance
            KeyValue("Endpoint", p.endpointId ?: "-")
            KeyValue("Protocol", p.protocol?.toString() ?: "-")

            Spacer(Modifier.height(6.dp))

            GroupRow(
                leftLabel = "DNS",
                leftValue = p.dnsMs?.let { "$it ms" } ?: "-",
                rightLabel = "TCP",
                rightValue = p.tcpMs?.let { "$it ms" } ?: "-"
            )
            GroupRow(
                leftLabel = "TLS",
                leftValue = p.tlsMs?.let { "$it ms" } ?: "-",
                rightLabel = "TTFB",
                rightValue = p.ttfbMs?.let { "$it ms" } ?: "-"
            )

            Spacer(Modifier.height(6.dp))

            GroupRow(
                leftLabel = "RTT avg",
                leftValue = p.rttAvgMs?.let { "$it ms" } ?: "-",
                rightLabel = "RTT p95",
                rightValue = p.rttP95Ms?.let { "$it ms" } ?: "-"
            )

            GroupRow(
                leftLabel = "Jitter",
                leftValue = p.jitterMs?.let { "$it ms" } ?: "-",
                rightLabel = "Loss",
                rightValue = p.packetLossPct?.let { "$it %" } ?: "-"
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeaderBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KeyValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GroupRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        GroupCell(label = leftLabel, value = leftValue, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        GroupCell(label = rightLabel, value = rightValue, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GroupCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}