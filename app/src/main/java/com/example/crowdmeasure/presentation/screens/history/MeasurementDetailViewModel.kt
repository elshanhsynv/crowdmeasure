package com.example.crowdmeasure.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.crowdmeasure.sdk.model.CarrierInfo
import com.yourcompany.crowdmeasure.sdk.model.Measurement
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
        "OS Version" to meta.androidRelease,
        "Android SDK" to meta.androidSdk.toString(),
        "App Version" to meta.appVersion,

        "Brand" to meta.brand,
        "Device Manufacturer" to meta.deviceManufacturer,
        "Device OS" to meta.deviceOS,
        "Build ID" to meta.buildID,
        "Hardware" to meta.hardware,
        "Chipset" to meta.chipset,
        "Chipset Manufacturer" to meta.chipsetManufacturer,

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

    val usagePairs = environment.network.dataUsage?.let { usage ->
        listOf(
            "DL Rate" to "%.2f Kbps".format(usage.dlKbps),
            "UL Rate" to "%.2f Kbps".format(usage.ulKbps)
        )
    }.orEmpty()

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
            add("Wi-Fi Standard" to w.standard.name)
            w.bssidHash?.let { add("BSSID Hash" to it) }
        }.takeIf { it.isNotEmpty() }
    }

    // Cell section
    val cellPairs = environment.network.cell?.let { c ->
        buildList {
            c.rat?.let { add("RAT" to it) }
            c.dataNetworkType?.let { add("Data Network Type" to it) }
            c.voiceNetworkType?.let { add("Voice Network Type" to it) }
            c.roaming?.let { add("Roaming" to it.toString()) }
            c.neighbors.size.let { add("Neighbor Cells" to it.toString()) }
        }
    }

    val simUi = environment.network.cell?.let { c ->
        c.simCarriers.mapIndexed { index, sim ->
            sim.toSimCarrierUi(
                fallbackIndex = index,
                collectedSubscriptionId = c.collectedSubscriptionId,
                collectedSimSlotIndex = c.collectedSimSlotIndex
            )
        }
    }.orEmpty()

    val collectedSimText = environment.network.cell?.let { c ->
        val collected = simUi.firstOrNull { it.isCollected }
        collected?.title ?: c.collectedSimSlotIndex?.let { "SIM ${it + 1}" }
        ?: c.collectedSubscriptionId?.let { "Subscription $it" }
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
            sc.mimoLayers?.let { add("MIMO Layers" to "$it") }
        }.joinToString(" • ") { (name, value) -> "$name: $value" }.takeIf { it.isNotBlank() }
    }

    // IP info
    val ipUi = environment.network.ip.let { i ->
        buildList {
            add("Public IP" to i.publicIp.toString())
            add("ISP" to i.ispName.toString())
            add("ASN" to i.asn.toString())
        }.takeIf { it.isNotEmpty() }
    }

    val performanceUi = PerformanceUi(
        protocol = performance.protocol.toString(),
        dns = performance.dnsMs?.let { "$it ms" } ?: "—",
        connect = performance.connectMs?.let { "$it ms" } ?: "—",
        tls = performance.tlsMs?.let { "$it ms" } ?: "—",
        ttfbAvg = performance.ttfbAvgMs?.let { "$it ms" } ?: "—",
        httpLatencyAvg = performance.httpLatencyAvgMs?.let { "$it ms" } ?: "—",
        httpLatencyP95 = performance.httpLatencyP95Ms?.let { "$it ms" } ?: "—",
        jitter = performance.jitterMs?.let { "$it ms" } ?: "—",
        probeFailure = performance.probeFailurePct?.let { "${"%.1f".format(it)}%" } ?: "—",
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
        env = envPairs + usagePairs,
        wifi = wifiPairs,
        cell = cellPairs,
        sims = simUi,
        collectedSimText = collectedSimText,
        ip = ipUi,
        performance = performanceUi,
        endpointId = performance.endpointId,
        locationText = locationText,
        cellIdsText = cellIdsText
    )
}

private fun CarrierInfo.toSimCarrierUi(
    fallbackIndex: Int,
    collectedSubscriptionId: Int?,
    collectedSimSlotIndex: Int?,
): SimCarrierUi {
    val slotNumber = simSlotIndex?.plus(1)
    val title = slotNumber?.let { "SIM $it" } ?: "SIM ${fallbackIndex + 1}"
    val isCollected =
        (subscriptionId != null && subscriptionId == collectedSubscriptionId) || (simSlotIndex != null && simSlotIndex == collectedSimSlotIndex)

    val pairs = buildList {
        carrierName?.let { add("Carrier" to it) }
        displayName?.let { add("Display Name" to it) }
        simOperatorName?.let { add("SIM Operator" to it) }
        simOperatorId?.let { add("SIM Operator ID" to it) }
        mcc?.let { add("MCC" to it) }
        mnc?.let { add("MNC" to it) }
        countryIso?.let { add("Country" to it) }
        duplexMode?.takeIf { it.isNotBlank() }?.let { add("Duplex Mode" to it) }
        simSlotIndex?.let { add("Slot Index" to it.toString()) }
        subscriptionId?.let { add("Subscription ID" to it.toString()) }
        carrierId?.let { add("Carrier ID" to it.toString()) }
        portIndex?.let { add("Port Index" to it.toString()) }
        cardId?.let { add("Card ID" to it.toString()) }
        dataRoaming?.let { add("Data Roaming" to it.toString()) }
        isEmbedded?.let { add("eSIM" to it.toString()) }
        isOpportunistic?.let { add("Opportunistic" to it.toString()) }
        isActiveData?.takeIf { it }?.let { add("Active Data" to it.toString()) }
        isDefaultData?.takeIf { it }?.let { add("Default Data" to it.toString()) }
        isDefaultVoice?.takeIf { it }?.let { add("Default Voice" to it.toString()) }
        isDefaultSms?.takeIf { it }?.let { add("Default SMS" to it.toString()) }
        if (isCollected) add("Collected Here" to "true")
    }

    val subtitle = carrierName ?: simOperatorName ?: displayName
    return SimCarrierUi(
        title = title, subtitle = subtitle, isCollected = isCollected, pairs = pairs
    )
}
