package com.example.crowdmeasure.data.measurement.collectors

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.ContextInfo
import com.example.crowdmeasure.domain.model.TransportType
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object ContextCollector {

    fun collect(context: Context): ContextInfo {
        val cm = context.getSystemService<ConnectivityManager>()
        val active = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(active)

        val transport = when {
            caps == null -> TransportType.NONE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TransportType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TransportType.CELL
            else -> TransportType.OTHER
        }

        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val captive = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
        val notMetered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val metered = notMetered?.let { !it }
        val vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

        val pm = context.getSystemService<PowerManager>()
        val batterySaver = pm?.isPowerSaveMode ?: false

        val bm = context.getSystemService<BatteryManager>()
        val charging = bm?.isCharging ?: false

        val screenOn = pm?.isInteractive ?: true

        // We only run from foreground UI or an explicit WorkManager job (auto-run enabled).
        // In both cases, set foreground=false for worker; but we don't know here, so assume true.
        // The worker will override if needed by passing foreground=false in a future extension.
        return ContextInfo(
            coarseLocation = null,
            transport = transport,
            validatedInternet = validated,
            captivePortal = captive,
            metered = metered,
            vpnPresent = vpn,
            batterySaver = batterySaver,
            charging = charging,
            screenOn = screenOn,
            foreground = true
        )
    }

    private val BatteryManager.isCharging: Boolean
        get() {
            val status = getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            // BATTERY_PROPERTY_STATUS returns 0 on many devices; fallback using isCharging not available.
            // Safer: check plugged state:
            val plugged = getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) // not plugged; ignore
            return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
}