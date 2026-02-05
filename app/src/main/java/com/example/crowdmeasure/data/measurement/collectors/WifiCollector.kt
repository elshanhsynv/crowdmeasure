package com.example.crowdmeasure.data.measurement.collectors

import android.content.Context
import android.net.wifi.WifiInfo as AndroidWifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.WifiInfo
import com.example.crowdmeasure.domain.model.WifiStandard
import java.security.MessageDigest

object WifiCollector {

    fun collect(context: Context): WifiInfo {
        val wm = context.applicationContext.getSystemService<WifiManager>()
        val info = wm?.connectionInfo

        val frequencyMhz = getFrequency(info)
        val channelWidthMhz = getChannelWidth(info)
        val txSpeed = getTxLinkSpeed(info)
        val rxSpeed = getRxLinkSpeed(info)

        return WifiInfo(
            rssi = info?.rssi,
            linkSpeedMbps = info?.linkSpeed, // legacy
            txLinkSpeedMbps = txSpeed,
            rxLinkSpeedMbps = rxSpeed,
            frequencyMhz = frequencyMhz,
            channelWidthMhz = channelWidthMhz,
            standard = deriveStandard(frequencyMhz, channelWidthMhz),
            bssidHash = info?.bssid?.let { hashBssid(it) }
        )
    }

    // ---------------------------------------------------------
    // Safe getters (work across API levels)
    // ---------------------------------------------------------

    private fun getFrequency(info: AndroidWifiInfo?): Int? =
        runCatching {
            info?.javaClass?.getMethod("getFrequency")?.invoke(info) as? Int
        }.getOrNull()

    private fun getChannelWidth(info: AndroidWifiInfo?): Int? =
        runCatching {
            // 0=20, 1=40, 2=80, 3=160, 4=80+80
            val cw = info?.javaClass?.getMethod("getChannelWidth")?.invoke(info) as? Int
            when (cw) {
                0 -> 20
                1 -> 40
                2 -> 80
                3 -> 160
                4 -> 80
                else -> null
            }
        }.getOrNull()

    private fun getTxLinkSpeed(info: AndroidWifiInfo?): Int? =
        if (Build.VERSION.SDK_INT >= 29) info?.txLinkSpeedMbps else null

    private fun getRxLinkSpeed(info: AndroidWifiInfo?): Int? =
        if (Build.VERSION.SDK_INT >= 29) info?.rxLinkSpeedMbps else null

    // ---------------------------------------------------------
    // Derived metrics
    // ---------------------------------------------------------

    private fun deriveStandard(freq: Int?, width: Int?): WifiStandard {
        if (freq == null) return WifiStandard.UNKNOWN

        return when {
            // 6 GHz = Wi-Fi 6E/7
            freq >= 5925 -> WifiStandard.WIFI_6E

            // 5 GHz
            freq in 4900..5924 && (width ?: 0) >= 160 -> WifiStandard.WIFI_7
            freq in 4900..5924 -> WifiStandard.WIFI_5

            // 2.4 GHz
            freq < 3000 -> WifiStandard.WIFI_4

            else -> WifiStandard.UNKNOWN
        }
    }

    /**
     * Privacy-safe stable AP identity.
     * Hash instead of storing raw BSSID.
     */
    private fun hashBssid(bssid: String): String {
        val salt = "crowdmeasure_wifi_salt_v1" // keep constant or rotate per app version
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((bssid + salt).toByteArray())
        return bytes.take(8).joinToString("") { "%02x".format(it) } // short hash
    }
}
