package com.crowdmeasure.sdk.internal.measurement.collectors

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.content.getSystemService
import com.crowdmeasure.sdk.model.DeviceEnvironment
import com.crowdmeasure.sdk.model.ThermalStatus

object DiagnosticsCollector {

    @WorkerThread
    fun collect(context: Context): DeviceEnvironment {
        val pm = context.getSystemService<PowerManager>()
        val cm = context.getSystemService<ConnectivityManager>()

        val (batteryPercentage, charging) = getBatteryData(context)

        val batterySaverStatus = pm?.isPowerSaveMode

        val screenOn = pm?.isInteractive ?: false
        val thermalStatus = pm?.thermalStatus()

        return DeviceEnvironment(
            batteryPct = batteryPercentage,
            charging = charging,
            batterySaver = batterySaverStatus,
            screenOn = screenOn,
            dozeMode = pm?.isDeviceIdleMode,
            dataSaver = cm?.dataSaverEnabled(),
            thermalState = thermalStatus?.name,
            cpuUsagePct = getCpuUsagePct(),
            memoryUsagePct = getMemoryUsagePct(context)
        )
    }

    /**
     * Calculates system-wide memory usage percentage.
     */
    private fun getMemoryUsagePct(context: Context): Double? {
        return try {
            val am = context.getSystemService<ActivityManager>() ?: return null
            val memoryInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memoryInfo)

            val total = memoryInfo.totalMem.toDouble()
            val avail = memoryInfo.availMem.toDouble()

            if (total > 0.0) {
                ((total - avail) / total) * 100.0
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns a coarse estimate of CPU usage.
     */
    private fun getCpuUsagePct(): Double? {
        // NOTE: Since Android 8.0 (API 26), Google heavily locked down system metrics.
        // Reading `/proc/stat` to get global system CPU usage is blocked for standard apps.
        // It will throw a Permission Denied exception.
        // Thus, for a modern Android crowdsourcing app, returning null is standard.
        return null
    }

    private fun PowerManager.thermalStatus(): ThermalStatus? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return currentThermalStatus.toThermalStatus()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun Int.toThermalStatus(): ThermalStatus = when (this) {
        PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
        PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.SHUTDOWN
        else -> ThermalStatus.UNKNOWN
    }

    /**
     * Returns whether Data Saver is globally active.
     */
    private fun ConnectivityManager.dataSaverEnabled(): Boolean? = when (restrictBackgroundStatus) {
        ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
        ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> true

        ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED -> false
        else -> null
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

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        val percentage = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100).toInt()
        } else {
            null
        }

        return Pair(percentage, isCharging)
    }
}