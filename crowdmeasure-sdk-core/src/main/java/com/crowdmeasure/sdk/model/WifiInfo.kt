package com.crowdmeasure.sdk.model

import kotlinx.serialization.Serializable

/**
 * Wi-Fi link-layer snapshot collected from [android.net.wifi.WifiInfo].
 *
 * [bssidHash] — truncated SHA-256 of the BSSID; never the raw MAC address.
 *   Null when the BSSID is unavailable or a known Android placeholder value.
 */
@Serializable
data class WifiInfo(
    val bssidHash: String?,
    val ssid: String?,

    val standard: WifiStandard,
    val frequencyMhz: Int?,
    val channelWidthMhz: Int?,

    val rssiDbm: Int?,
    val linkSpeedMbps: Int?,

    val txLinkSpeedMbps: Int?,
    val rxLinkSpeedMbps: Int?,
)