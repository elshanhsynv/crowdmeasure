package com.example.crowdmeasure.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TransportType { WIFI, CELL, OTHER, NONE }

@Serializable
enum class RecordState { PENDING, UPLOADED, FAILED }

@Serializable
enum class ProtocolType { HTTP1_1, HTTP2, UNKNOWN }

@Serializable
enum class WifiStandard {
    UNKNOWN,
    WIFI_4,   // 802.11n
    WIFI_5,   // 802.11ac
    WIFI_6,   // 802.11ax
    WIFI_6E,  // Wi-Fi 6 / 7 on 6 GHz (conservative lower-bound without getWifiStandard())
    WIFI_7,   // 802.11be (only set when getWifiStandard() confirms it, API 30+)
}

/**
 * 5G NR operating mode.
 *
 * [NONE] — no NR component detected.
 * [NSA] — Non-Standalone: NR secondary cell anchored to LTE.
 * [SA]  — Standalone: NR serving cell with no LTE anchor.
 */
@Serializable
enum class NrState {
    @SerialName("none") NONE,
    @SerialName("nsa")  NSA,
    @SerialName("sa")   SA,
}