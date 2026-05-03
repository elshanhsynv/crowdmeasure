package com.example.crowdmeasure.data.measurement.collectors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import androidx.annotation.WorkerThread
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.ContextInfo
import com.example.crowdmeasure.domain.model.TransportType
import com.example.crowdmeasure.presentation.util.AppPermissions

object ContextCollector {

    @WorkerThread
    suspend fun collect(context: Context): ContextInfo {
        val cm = context.getSystemService<ConnectivityManager>()
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val transport = caps.toTransportType()

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

        val pm = context.getSystemService<PowerManager>()
        val batterySaver = pm?.isPowerSaveMode ?: false
        val screenOn = pm?.isInteractive ?: false

        val (batteryPercentage, charging) = getBatteryData(context)

        val location = if (AppPermissions.hasCoarseLocation(context)) {
            LocationCollector.tryGetCoarseOneShot(context)
        } else null

        return ContextInfo(
            location = location,
            transport = transport,
            validatedInternet = validatedInternet,
            captivePortal = captivePortal,
            metered = metered,
            vpnPresent = vpnPresent,
            batterySaver = batterySaver,
            batteryPercentage = batteryPercentage,
            charging = charging,
            screenOn = screenOn,
        )
    }

    /**
     * Reads the sticky battery intent to extract both percentage and charging state.
     * Returns a Pair where first is the percentage (0-100) and second is the charging boolean.
     */
    private fun getBatteryData(context: Context): Pair<Int?, Boolean> {
        val intent: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )

        if (intent == null) return Pair(null, false)

        // Determine Charging State
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL

        // Determine Percentage
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        val percentage = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100).toInt()
        } else {
            null
        }

        return Pair(percentage, isCharging)
    }

    private fun NetworkCapabilities?.toTransportType(): TransportType = when {
        this == null -> TransportType.NONE
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TransportType.WIFI
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TransportType.CELL
        else -> TransportType.OTHER
    }
}