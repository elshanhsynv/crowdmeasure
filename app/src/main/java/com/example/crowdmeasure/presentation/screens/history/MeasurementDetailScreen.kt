package com.example.crowdmeasure.presentation.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
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
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val scroll = rememberScrollState()

    Column(
        Modifier
            .padding(contentPadding)
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }

        if (item == null) {
            Text("Not found.")
            return
        }

        SectionCard {
            Text("Header", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Time: ${fmt.format(Date(item.header.timestampUtcMs))}")
            Text("ID: ${item.header.measurementId}")
            Text("App: ${item.header.appVersion}")
            Text("Android: ${item.header.androidVersion}")
            Text("Device: ${item.header.deviceModel}")
            Text("Consent version: ${item.header.userConsentVersion}")
        }

        SectionCard {
            Text("Context", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Transport: ${item.context.transport}")
            Text("Validated: ${item.context.validatedInternet ?: "-"}")
            Text("Captive portal: ${item.context.captivePortal ?: "-"}")
            Text("Metered: ${item.context.metered ?: "-"}")
            Text("VPN: ${item.context.vpnPresent ?: "-"}")
            Text("Battery saver: ${item.context.batterySaver}")
            Text("Charging: ${item.context.charging}")
            Text("Screen on: ${item.context.screenOn}")
            val loc = item.context.coarseLocation
            Text("Coarse location: ${loc?.let { "${it.lat}, ${it.lon} (~${it.accuracyMeters}m)" } ?: "-"}")
        }

        item.wifi?.let { w ->
            SectionCard {
                Text("Wi-Fi", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("RSSI: ${w.rssi ?: "-"} dBm")
                Text("Link speed: ${w.linkSpeedMbps ?: "-"} Mbps")
                Text("Frequency: ${w.frequencyMhz ?: "-"} MHz")
                Text("Channel width: ${w.channelWidthMhz ?: "-"} MHz")
            }
        }

        item.cell?.let { c ->
            SectionCard {
                Text("Cell", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("Carrier: ${c.carrierName ?: "-"}")
                Text("MCC/MNC: ${(c.mcc ?: "-")}/${(c.mnc ?: "-")}")
                Text("Data type: ${c.dataNetworkType ?: "-"}")
                Text("Voice type: ${c.voiceNetworkType ?: "-"}")
                Text("Roaming: ${c.roaming ?: "-"}")
                Text("Registered RAT: ${c.registeredRat ?: "-"}")
                Text(
                    "Serving cell: CI=${c.servingCell?.ci ?: "-"} " +
                            "NCI=${c.servingCell?.nci ?: "-"} " +
                            "TAC=${c.servingCell?.tac ?: "-"} " +
                            "PCI=${c.servingCell?.pci ?: "-"}"
                )
                Text(
                    "Signal: RSRP=${c.signal?.rsrp ?: "-"} " +
                            "RSRQ=${c.signal?.rsrq ?: "-"} " +
                            "SINR=${c.signal?.sinr ?: "-"} " +
                            "RSSI=${c.signal?.rssi ?: "-"}"
                )
                Text(
                    "Availability: cellInfo=${c.availability.cellInfoAccessible}, " +
                            "ids=${c.availability.idsAccessible}, " +
                            "signal=${c.availability.signalAccessible}"
                )
            }
        }

        SectionCard {
            Text("Performance", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            val p = item.performance
            Text("Endpoint: ${p.endpointId}")
            Text("DNS/TCP/TLS/TTFB: ${p.dnsMs ?: "-"} / ${p.tcpMs ?: "-"} / ${p.tlsMs ?: "-"} / ${p.ttfbMs ?: "-"} ms")
            Text("RTT avg/p95: ${p.rttAvgMs ?: "-"} / ${p.rttP95Ms ?: "-"} ms")
            Text("Jitter: ${p.jitterMs ?: "-"} ms")
            Text("Packet loss: ${p.packetLossPct ?: "-"} %")
            Text("Protocol: ${p.protocol}")
        }

        Spacer(Modifier.height(16.dp))
    }
}