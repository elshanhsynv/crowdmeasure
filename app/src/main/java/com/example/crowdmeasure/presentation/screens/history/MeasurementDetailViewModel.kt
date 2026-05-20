package com.example.crowdmeasure.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.model.Measurement
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for Measurement Detail screen.
 *
 * Features:
 * - Load single measurement by ID
 * - Privacy-conscious reveal system for sensitive data
 * - Organized sections (header, context, wifi, cell, performance)
 * - Formatted display values
 *
 * Privacy Design:
 * - Sensitive fields (location, cell IDs, endpoint) masked by default
 * - User must explicitly reveal (tap to show)
 * - Reveal state preserved during navigation (within session)
 * - Separate reveal keys for different data types
 *
 * Performance:
 * - StateFlow with update() for efficient state changes
 * - Date formatter created once
 * - UI model conversion in background thread
 */
@HiltViewModel
class MeasurementDetailViewModel @Inject constructor(
    private val measurementRepository: MeasurementRepository
) : ViewModel() {
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    private val _uiState = MutableStateFlow(MeasurementDetailUiState())
    val uiState: StateFlow<MeasurementDetailUiState> = _uiState

    /**
     * Load measurement by ID.
     *
     * @param id Measurement ID to load
     */
    fun load(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loadState = UiState.Loading) }

            val result = runCatching {
                measurementRepository.getMeasurementById(id)
            }

            result.fold(onSuccess = { measurement ->
                if (measurement == null) {
                    _uiState.update {
                        it.copy(
                            loadState = UiState.Error("Measurement not found. It may have been deleted.")
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadState = UiState.Success(measurement.toDetailUi(dateFormatter))
                        )
                    }
                }
            }, onFailure = { error ->
                _uiState.update {
                    it.copy(
                        loadState = UiState.Error(
                            message = "Couldn't load measurement. Check connection and retry.",
                            throwable = error
                        )
                    )
                }
            })
        }
    }

    /**
     * Toggle visibility of sensitive data.
     *
     * @param key The reveal key (Endpoint, Location, CellIds)
     */
    fun toggleReveal(key: RevealKey) {
        _uiState.update { state ->
            val newRevealed = if (state.revealed.contains(key)) {
                state.revealed - key
            } else {
                state.revealed + key
            }
            state.copy(revealed = newRevealed)
        }
    }

    /**
     * Hide all sensitive data.
     */
    fun hideAllSensitive() {
        _uiState.update { it.copy(revealed = emptySet()) }
    }
}

/**
 * Map domain Measurement to detailed UI model.
 * Formats all values for display and organizes into sections.
 */

private fun Measurement.toDetailUi(formatter: SimpleDateFormat): MeasurementDetailUi {
    val timeText = formatter.format(Date(meta.timestampUtcMs))

    val metaPairs = listOf(
        "Device Model" to meta.deviceModel,
        "OS Version" to meta.osVersion,
        "Android SDK" to meta.sdkInt.toString(),
        "App Version" to meta.appVersion,
        "Session ID" to meta.sessionId,
        "User ID" to meta.userIdHash
    )
    val envPairs = listOf(
        "Transport" to environment.network.transport.toString(),
        "Internet" to environment.network.validatedInternet.toString(),
        "Captive Portal" to environment.network.captivePortal.toString(),
        "VPN" to environment.network.vpn.toString(),
        "Metered" to environment.network.metered.toString(),
        "Battery" to "${environment.device.batteryPct}%",
        "Charging" to environment.device.charging.toString(),
        "Battery Saver" to environment.device.batterySaver.toString(),
        "Screen On" to environment.device.screenOn.toString(),
        "Doze Mode" to environment.device.dozeMode.toString(),
        "Data Saver" to environment.device.dataSaver.toString(),
        "Thermal State" to environment.device.thermalState.toString(),
        "Memory Usage" to "%.2f".format(environment.device.memoryUsagePct) + "%",
    )

    val locationText = environment.location?.let { loc ->
        "${loc.lat}, ${loc.lon} (±${loc.accuracyMeters}m)"
    }

    // Wi-Fi section
    val wifiPairs = environment.network.wifi?.let { w ->
        buildList {
            w.rssiDbm?.let { add("Signal Strength (RSSI)" to "$it dBm") }
            w.linkSpeedMbps?.let { add("Link Speed (legacy)" to "$it Mbps") }
            w.txLinkSpeedMbps?.let { add("TX Link Speed" to "$it Mbps") }
            w.rxLinkSpeedMbps?.let { add("RX Link Speed" to "$it Mbps") }
            w.frequencyMhz?.let { add("Frequency" to "$it MHz") }
            w.channelWidthMhz?.let { add("Channel Width" to "$it MHz") }
            w.standard?.let { add("Wi-Fi Standard" to it.name) }
            w.bssidHash?.let { add("BSSID Hash" to it) }
        }.takeIf { it.isNotEmpty() }
    }

    // Cell section
    val cellPairs = environment.network.cell?.let { c ->
        buildList {
            c.carrier.mcc?.let { add("MCC" to it) }
            c.carrier.mnc?.let { add("MNC" to it) }
            c.carrier.carrierName?.let { add("Carrier" to it) }
            c.rat?.let { add("RAT" to it) }
            c.dataNetworkType?.let { add("Data Network Type" to it) }
            c.voiceNetworkType?.let { add("Voice Network Type" to it) }
            c.roaming?.let { add("Roaming" to it.toString()) }
        }
    }

    // Sensitive: Cell IDs
    val cellIdsText = environment.network.cell?.serving?.let { sc ->
        buildList {
            add("Serving Cell" to sc.cellId.toString())
            sc.cid?.let { add("CID" to "$it") }
            sc.nci?.let { add("NCI" to "$it") }
            sc.band?.let { add("Band" to "$it") }
            sc.arfcn?.let { add("ARFCN" to "$it") }
            sc.nrarfcn?.let { add("NRARFCN" to "$it") }
            sc.tac?.let { add("TAC" to "$it") }
            sc.pci?.let { add("PCI" to "$it") }
            sc.rsrpDbm?.let { add("RSRP" to "$it dBm") }
            sc.rsrqDb?.let { add("RSRQ" to "$it dB") }
            sc.sinrDb?.let { add("SINR" to "$it dB") }
            sc.cqi?.let { add("CQI" to "$it") }
            sc.rssiDbm?.let { add("RSSI" to "$it dBm") }
            sc.bandwidthMhz?.let { add("Bandwidth" to "$it MHz") }
            sc.mimoLayers?.let { add("Mimo Layers") to "$it" }
        }.joinToString(" • ").takeIf {
            it.isNotEmpty() || it.isNotBlank()
        }
    }

    // IP info
    val ipUi = environment.network.ip.let { i ->
        buildList {
            add("Public IP" to i.publicIpHash.toString())
            add("ISP" to i.ispName.toString())
            add("ASN" to i.asn.toString())
        }.takeIf { it.isNotEmpty() }
    }

    val performanceUi = PerformanceUi(
        protocol = performance.protocol.toString(),
        dns = performance.dnsMs?.let { "$it ms" } ?: "—",
        tcp = performance.tcpMs?.let { "$it ms" } ?: "—",
        tls = performance.tlsMs?.let { "$it ms" } ?: "—",
        ttfb = performance.ttfbMs?.let { "$it ms" } ?: "—",
        rttAvg = performance.rttAvgMs?.let { "$it ms" } ?: "—",
        rttP95 = performance.rttP95Ms?.let { "$it ms" } ?: "—",
        jitter = performance.jitterMs?.let { "$it ms" } ?: "—",
        loss = performance.packetLossPct?.let { "${"%.1f".format(it)}%" } ?: "—",
        httpStatus = performance.httpStatus?.toString() ?: "—",
        serverRegion = performance.serverRegion ?: "—",
        stallsCount = performance.stallsCount?.toString() ?: "—",
        maxStall = performance.maxStallMs?.let { "$it ms" } ?: "—",
        down = performance.downMbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—",
        up = performance.upMbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—",
        downP95 = performance.downP95Mbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—",
        downStdDev = performance.downStdDevMbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—",
        upP95 = performance.upP95Mbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—",
        upStdDev = performance.upStdDevMbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—")

    return MeasurementDetailUi(
        id = meta.measurementId,
        timeText = timeText,
        meta = metaPairs,
        env = envPairs,
        wifi = wifiPairs,
        cell = cellPairs,
        ip = ipUi,
        performance = performanceUi,
        endpointId = performance.endpointId,
        locationText = locationText,
        cellIdsText = cellIdsText
    )
}
