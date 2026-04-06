package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

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
    val ispName: String? = null,
)

@Serializable
enum class WifiStandard {
    UNKNOWN,
    WIFI_4,   // 802.11n
    WIFI_5,   // 802.11ac
    WIFI_6,   // 802.11ax
    WIFI_6E,  // 6 GHz Wi-Fi 6
    WIFI_7    // 802.11be
}
