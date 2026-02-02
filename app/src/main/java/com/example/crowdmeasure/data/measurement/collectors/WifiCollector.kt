package com.example.crowdmeasure.data.measurement.collectors

import android.content.Context
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.WifiInfo

object WifiCollector {

    fun collect(context: Context): WifiInfo {
        val wm = context.applicationContext.getSystemService<WifiManager>()
        val info = wm?.connectionInfo

        val frequencyMhz: Int? = runCatching {
            // compiles even if frequency isn't in your stubs
            info?.javaClass?.getMethod("getFrequency")?.invoke(info) as? Int
        }.getOrNull()

        val channelWidthMhz: Int? = runCatching {
            // getChannelWidth() returns: 0=20, 1=40, 2=80, 3=160, 4=80+80
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

        return WifiInfo(
            rssi = info?.rssi,
            linkSpeedMbps = info?.linkSpeed,
            frequencyMhz = frequencyMhz,
            channelWidthMhz = channelWidthMhz
        )
    }
}