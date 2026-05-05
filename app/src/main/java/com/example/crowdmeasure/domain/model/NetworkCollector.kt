package com.example.crowdmeasure.domain.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.example.crowdmeasure.data.measurement.collectors.IpCollector
import com.example.crowdmeasure.data.measurement.collectors.TelephonyCollector
import com.example.crowdmeasure.data.measurement.collectors.WifiCollector
import okhttp3.OkHttpClient

object NetworkCollector {

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun collect(context: Context, okHttp: OkHttpClient): NetworkEnvironment {
        val cm = context.getSystemService<ConnectivityManager>()
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val transport = caps.toTransportType()
        val ip = IpCollector.collect(okHttp)
        val wifi = WifiCollector.collect(context)
        val cell = TelephonyCollector.collect(context)


        val validatedInternet: Boolean?
        val captivePortal: Boolean?
        val metered: Boolean?
        val vpnPresent: Boolean?

        if (caps != null) {
            validatedInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            captivePortal = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
            metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            vpnPresent = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } else {
            validatedInternet = null
            captivePortal = null
            metered = null
            vpnPresent = null
        }

        val networkInfo = NetworkEnvironment(
            transport = transport,
            ip = ip,
            validatedInternet = validatedInternet,
            captivePortal = captivePortal,
            vpn = vpnPresent,
            metered = metered,
            wifi = wifi,
            cell = cell
        )

        return networkInfo
    }
}


private fun NetworkCapabilities?.toTransportType(): TransportType = when {
    this == null -> TransportType.NONE
    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TransportType.WIFI
    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TransportType.CELL
    else -> TransportType.OTHER
}