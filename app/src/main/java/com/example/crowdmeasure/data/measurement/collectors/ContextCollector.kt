package com.example.crowdmeasure.data.measurement.collectors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.ContextInfo
import com.example.crowdmeasure.domain.model.TransportType
import com.example.crowdmeasure.presentation.util.AppPermissions

object ContextCollector {

    /**
     * Collects "context" fields. Coarse location is optional and best-effort:
     * - only attempted if permission granted
     * - may still be null if location services off / no fix available
     */
    suspend fun collect(context: Context): ContextInfo {
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
        val screenOn = pm?.isInteractive ?: true

        val charging = isCharging(context)

        val coarseLoc = if (AppPermissions.hasCoarseLocation(context)) {
            LocationCollector.tryGetCoarseOneShot(context)
        } else null

        return ContextInfo(
            coarseLocation = coarseLoc,
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

    private fun isCharging(context: Context): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }
}