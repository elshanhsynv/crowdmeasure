package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

/**
 * Wi-Fi link-layer snapshot collected from [android.net.wifi.WifiInfo].
 *
 * [bssidHash] — truncated SHA-256 of the BSSID; never the raw MAC address.
 *   Null when the BSSID is unavailable or a known Android placeholder value.
 */
@Serializable
data class WifiInfo(
    val rssi: Int? = null,
    val linkSpeedMbps: Int? = null,
    val txLinkSpeedMbps: Int? = null,
    val rxLinkSpeedMbps: Int? = null,
    val frequencyMhz: Int? = null,
    val channelWidthMhz: Int? = null,
    val standard: WifiStandard? = null,
    val bssidHash: String? = null,
)

