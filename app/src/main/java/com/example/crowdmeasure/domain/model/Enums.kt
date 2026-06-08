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
    @SerialName("none")
    NONE,

    @SerialName("nsa")
    NSA,

    @SerialName("sa")
    SA,
}

/**
 * Typed representation of [android.os.PowerManager] thermal status constants.
 *
 * Maps 1-to-1 to [android.os.PowerManager.THERMAL_STATUS_*] (API 29+).
 * [UNKNOWN] is used when the API is unavailable (< API 29) or returns an
 * unrecognized value.
 */
@Serializable
enum class ThermalStatus {
    @SerialName("none")
    NONE,        // THERMAL_STATUS_NONE     = 0

    @SerialName("light")
    LIGHT,       // THERMAL_STATUS_LIGHT    = 1

    @SerialName("moderate")
    MODERATE,    // THERMAL_STATUS_MODERATE = 2

    @SerialName("severe")
    SEVERE,      // THERMAL_STATUS_SEVERE   = 3

    @SerialName("critical")
    CRITICAL,    // THERMAL_STATUS_CRITICAL = 4

    @SerialName("emergency")
    EMERGENCY,  // THERMAL_STATUS_EMERGENCY= 5

    @SerialName("shutdown")
    SHUTDOWN,    // THERMAL_STATUS_SHUTDOWN = 6

    @SerialName("unknown")
    UNKNOWN,     // < API 29 or unrecognized value
}

enum class CallType {
    INCOMING,
    OUTGOING,
    UNKNOWN
}

enum class CallSource {
    CELLULAR,
    WHATSAPP_VOICE,
    WHATSAPP_VIDEO,
    WHATSAPP_UNKNOWN,
    VOIP_GENERIC,
    UNKNOWN
}
