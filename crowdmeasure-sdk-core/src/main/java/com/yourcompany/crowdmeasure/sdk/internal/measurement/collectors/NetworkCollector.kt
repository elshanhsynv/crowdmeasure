package com.crowdmeasure.sdk.internal.measurement.collectors

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.crowdmeasure.sdk.model.NetworkEnvironment
import com.crowdmeasure.sdk.model.TransportType
import com.crowdmeasure.sdk.CrowdMeasureConfig
import com.crowdmeasure.sdk.PublicIpPolicy
import com.crowdmeasure.sdk.model.IpInfo
import okhttp3.OkHttpClient

object NetworkCollector {

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun collect(context: Context, okHttp: OkHttpClient, config: CrowdMeasureConfig, ipHashSalt: String): NetworkEnvironment {
        val cm = context.getSystemService<ConnectivityManager>()
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val transport = caps.toTransportType()
        val ip = if (config.collectors.publicIpEnabled && config.publicIpPolicy == PublicIpPolicy.HASHED) {
            IpCollector.collect(okHttp, ipHashSalt)
        } else IpInfo()

        val wifi = if (config.collectors.wifiEnabled && transport == TransportType.WIFI) {
            WifiCollector.collect(context)
        } else null
        val cell = if (config.collectors.cellularEnabled && transport == TransportType.CELL) {
            TelephonyCollector.collect(context)
        } else null

        val dataUsage = DataUsageCollector.collect(context)

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
            cell = cell,
            dataUsage = dataUsage,
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
