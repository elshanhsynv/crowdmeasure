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

            result.fold(
                onSuccess = { measurement ->
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
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            loadState = UiState.Error(
                                message = "Couldn't load measurement. Check connection and retry.",
                                throwable = error
                            )
                        )
                    }
                }
            )
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
    val timeText = formatter.format(Date(header.timestampUtcMs))

    val headerPairs = listOf(
        "App Version" to (header.appVersion ?: "—"),
        "Android Version" to (header.androidVersion ?: "—"),
        "Device Model" to (header.deviceModel ?: "—"),
        "Consent Version" to header.userConsentVersion.toString()
    )

    val contextPairs = buildList {
        add("Transport" to (context.transport.toString()))
        add("Internet Validated" to (context.validatedInternet?.toString() ?: "—"))
        add("Captive Portal" to (context.captivePortal?.toString() ?: "—"))
        add("Metered Connection" to (context.metered?.toString() ?: "—"))
        add("VPN Active" to (context.vpnPresent?.toString() ?: "—"))
        add("Battery Saver" to context.batterySaver.toString())
        add("Charging" to context.charging.toString())
        add("Screen On" to context.screenOn.toString())
        add("Foreground" to context.foreground.toString())
    }

    val coarseLocationText = context.coarseLocation?.let { loc ->
        "${loc.lat}, ${loc.lon} (±${loc.accuracyMeters}m)"
    }

    // NEW: Diagnostics section
    val diagnosticsPairs = diagnostics?.let { d ->
        buildList {
            d.thermalStatus?.let { add("Thermal Status" to it.toString()) }
            d.dozeMode?.let { add("Doze Mode" to it.toString()) }
            d.dataSaverEnabled?.let { add("Data Saver" to it.toString()) }
            d.handoverCount?.let { add("Handover Count" to it.toString()) }
            d.handoverDuringTest?.let { add("Handover During Test" to it.toString()) }
            d.asn?.let { add("ASN" to it.toString()) }
            d.ispName?.let { add("ISP" to it) }
            d.publicIpHash?.let { add("Public IP Hash" to it) }
        }.takeIf { it.isNotEmpty() }
    }

    // Wi-Fi section
    val wifiPairs = wifi?.let { w ->
        buildList {
            w.rssi?.let { add("Signal Strength (RSSI)" to "$it dBm") }
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
    val cellPairs = cell?.let { c ->
        buildList {
            add("Carrier" to (c.carrierName ?: "—"))
            add("MCC / MNC" to "${c.mcc ?: "—"} / ${c.mnc ?: "—"}")
            add("Data Network" to (c.dataNetworkType ?: "—"))
            add("Voice Network" to (c.voiceNetworkType ?: "—"))
            add("Roaming" to (c.roaming?.toString() ?: "—"))
            add("Registered RAT" to (c.registeredRat ?: "—"))

            // NEW
            c.nrState?.let { add("NR State" to it.name) }
            c.radioMetrics?.mimoLayers?.let { add("MIMO Layers" to it.toString()) }

            // Serving signal extras (non-sensitive)
            c.signal?.cqi?.let { add("CQI" to it.toString()) }
            c.signal?.timingAdvance?.let { add("Timing Advance" to it.toString()) }

            // CA summary
            c.aggregation?.active?.let { add("Carrier Aggregation" to it.toString()) }
            c.aggregation?.secondaryCells?.size?.let { add("Secondary Cells" to it.toString()) }

            add("Signal Accessible" to c.availability.signalAccessible.toString())
            add("IDs Accessible" to c.availability.idsAccessible.toString())
        }.takeIf { it.isNotEmpty() }
    }

    // Sensitive: Cell IDs (keep here)
    val cellIdsText = cell?.servingCell?.let { sc ->
        buildList {
            sc.ci?.let { add("CI: $it") }
            sc.nci?.let { add("NCI: $it") }
            sc.tac?.let { add("TAC: $it") }
            sc.pci?.let { add("PCI: $it") }
            sc.earfcn?.let { add("EARFCN: $it") }
            sc.nrarfcn?.let { add("NRARFCN: $it") }
            sc.band?.let { add("Band: $it") }
            sc.bandwidthMhz?.let { add("BW: ${it}MHz") } // NEW
        }.joinToString(" • ")
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

        // NEW
        httpStatus = performance.httpStatus?.toString() ?: "—",
        serverRegion = performance.serverRegion ?: "—",
        stallsCount = performance.stallsCount?.toString() ?: "—",
        maxStall = performance.maxStallMs?.let { "$it ms" } ?: "—",
        down = performance.downMbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—",
        up = performance.upMbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—",
        downP95 = performance.downP95Mbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—",
        downStdDev = performance.downStdDevMbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—",
        upP95 = performance.upP95Mbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—",
        upStdDev = performance.upStdDevMbps?.let { "${"%.2f".format(it)} Mbps" } ?: "—"
    )

    return MeasurementDetailUi(
        id = header.measurementId,
        timeText = timeText,
        feedbackTag = feedbackTag,
        header = headerPairs,
        context = contextPairs,
        diagnostics = diagnosticsPairs, // NEW
        wifi = wifiPairs,
        cell = cellPairs,
        performance = performanceUi,
        endpointId = performance.endpointId,
        coarseLocationText = coarseLocationText,
        cellIdsText = cellIdsText
    )
}
