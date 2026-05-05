package com.example.crowdmeasure.data.measurement.collectors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo as AndroidWifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.WifiInfo
import com.example.crowdmeasure.domain.model.WifiStandard
import timber.log.Timber
import java.security.MessageDigest

object WifiCollector {

    private val BSSID_PLACEHOLDER_VALUES = setOf(
        "00:00:00:00:00:00",
        "02:00:00:00:00:00",
    )

    @WorkerThread
    fun collect(context: Context): WifiInfo {
        val wifiInfo = resolveWifiInfo(context)

        val frequencyMhz = wifiInfo?.frequency?.takeIf { it > 0 }
        val channelWidthMhz = channelWidthMhz(wifiInfo)
        Timber.tag("WifiCollector").d(
            "Collected WifiInfo: channelWidth=%d MHz",
            channelWidthMhz
        )

        return WifiInfo(
            bssidHash = wifiInfo?.bssid
                ?.takeIf { it.isNotBlank() && it !in BSSID_PLACEHOLDER_VALUES }
                ?.let { hashBssid(it) },
            ssidHash = wifiInfo?.ssid,
            standard = deriveStandard(wifiInfo, frequencyMhz, channelWidthMhz),
            frequencyMhz = frequencyMhz,
            channelWidthMhz = channelWidthMhz,
            rssiDbm = wifiInfo?.rssi,
            linkSpeedMbps = wifiInfo?.linkSpeed,
            txLinkSpeedMbps = wifiInfo?.txLinkSpeedMbps(),
            rxLinkSpeedMbps = wifiInfo?.rxLinkSpeedMbps(),
        )
    }

    private fun resolveWifiInfo(context: Context): AndroidWifiInfo? {
        // API 31+: preferred path via NetworkCapabilities.transportInfo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cm = context.getSystemService<ConnectivityManager>()
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            val info = caps?.transportInfo as? AndroidWifiInfo
            if (info != null) return info
        }

        // API < 31 (and fallback): getConnectionInfo
        @Suppress("DEPRECATION")
        return context.applicationContext.getSystemService<WifiManager>()?.connectionInfo
    }

    private fun resolveScanResult(context: Context, bssid: String?): ScanResult? {
        if (bssid.isNullOrBlank() || bssid in BSSID_PLACEHOLDER_VALUES) return null

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val wifiManager = context.applicationContext.getSystemService<WifiManager>() ?: return null

        return try {
            @Suppress("MissingPermission")
            wifiManager.scanResults.firstOrNull { it.BSSID == bssid }
        } catch (e: Exception) {
            null
        }
    }

    private fun channelWidthMhz(info: AndroidWifiInfo?): Int? {
        Timber.tag("WifiCollector").d("Deriving channel width from WifiInfo: linkSpeed=%d Mbps", info?.linkSpeed)
        return when (info?.linkSpeed) {
            ScanResult.CHANNEL_WIDTH_20MHZ -> 20
            ScanResult.CHANNEL_WIDTH_40MHZ -> 40
            ScanResult.CHANNEL_WIDTH_80MHZ -> 80
            ScanResult.CHANNEL_WIDTH_160MHZ -> 160
            ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 80
            ScanResult.CHANNEL_WIDTH_320MHZ -> 320
            else -> null
        }
    }

    private fun AndroidWifiInfo.txLinkSpeedMbps(): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) txLinkSpeedMbps else null

    private fun AndroidWifiInfo.rxLinkSpeedMbps(): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) rxLinkSpeedMbps else null

    @SuppressLint("InlinedApi")
    private fun deriveStandard(
        info: AndroidWifiInfo?,
        freq: Int?,
        width: Int?,
    ): WifiStandard {
        // 1. Primary: OS-reported standard (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && info != null) {
            when (info.wifiStandard) {
                ScanResult.WIFI_STANDARD_11N -> return WifiStandard.WIFI_4
                ScanResult.WIFI_STANDARD_11AC -> return WifiStandard.WIFI_5
                ScanResult.WIFI_STANDARD_11AX -> {
                    return if (freq != null && freq in 5925..7125) {
                        WifiStandard.WIFI_6E
                    } else {
                        WifiStandard.WIFI_6
                    }
                }

                ScanResult.WIFI_STANDARD_11AD -> return WifiStandard.UNKNOWN // 60 GHz WiGig
                ScanResult.WIFI_STANDARD_11BE -> return WifiStandard.WIFI_7  // API 33+
                ScanResult.WIFI_STANDARD_UNKNOWN,
                ScanResult.WIFI_STANDARD_LEGACY -> { /* Fallthrough */
                }
            }
        }

        // 2. Fallback: Frequency + Width Heuristic
        if (freq == null || freq <= 0) return WifiStandard.UNKNOWN
        Timber.tag("WifiCollector").w(
            "OS did not report Wi-Fi standard; deriving from frequency=%d MHz, width=%d MHz",
            freq,
            width
        )
        return when {

            // 6 GHz Band
            freq >= 5925 -> if (width == 320) WifiStandard.WIFI_7 else WifiStandard.WIFI_6E

            // 5 GHz Band (approx 4900 - 5924 MHz): Default to Wi-Fi 5
            freq in 4900..5924 -> WifiStandard.WIFI_5

            // 2.4 GHz Band (approx 2400 - 2500 MHz): Default to Wi-Fi 4
            freq in 2400..2999 -> WifiStandard.WIFI_4

            else -> WifiStandard.UNKNOWN
        }
    }

    private fun hashBssid(bssid: String): String {
        val salt = "crowdmeasure_wifi_salt_v1"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest((bssid + salt).toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }
}
