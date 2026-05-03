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

@Serializable
enum class WifiStandard {
    UNKNOWN,
    WIFI_4,   // 802.11n
    WIFI_5,   // 802.11ac
    WIFI_6,   // 802.11ax
    WIFI_6E,  // Wi-Fi 6 / 7 on 6 GHz (conservative lower-bound without getWifiStandard())
    WIFI_7,   // 802.11be (only set when getWifiStandard() confirms it, API 30+)
}