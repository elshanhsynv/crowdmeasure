package com.crowdmeasure.sdk.model

import kotlinx.serialization.Serializable

/**
 * Wi-Fi link-layer snapshot collected from [android.net.wifi.WifiInfo].
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