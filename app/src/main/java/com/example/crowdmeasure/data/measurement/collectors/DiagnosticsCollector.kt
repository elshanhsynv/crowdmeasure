package com.example.crowdmeasure.data.measurement.collectors

import android.net.ConnectivityManager
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.DiagnosticsInfo
import com.example.crowdmeasure.domain.model.ThermalStatus

object DiagnosticsCollector {

    @WorkerThread
    fun collect(context: android.content.Context): DiagnosticsInfo {
        val pm = context.getSystemService<PowerManager>()
        val cm = context.getSystemService<ConnectivityManager>()

        return DiagnosticsInfo(
            thermalStatus = pm?.thermalStatus(),
            dozeMode = pm?.isDeviceIdleMode,
            dataSaverEnabled = cm?.dataSaverEnabled(),
        )
    }

    private fun PowerManager.thermalStatus(): ThermalStatus? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return currentThermalStatus.toThermalStatus()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun Int.toThermalStatus(): ThermalStatus = when (this) {
        PowerManager.THERMAL_STATUS_NONE      -> ThermalStatus.NONE
        PowerManager.THERMAL_STATUS_LIGHT     -> ThermalStatus.LIGHT
        PowerManager.THERMAL_STATUS_MODERATE  -> ThermalStatus.MODERATE
        PowerManager.THERMAL_STATUS_SEVERE    -> ThermalStatus.SEVERE
        PowerManager.THERMAL_STATUS_CRITICAL  -> ThermalStatus.CRITICAL
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
        PowerManager.THERMAL_STATUS_SHUTDOWN  -> ThermalStatus.SHUTDOWN
        else                                  -> ThermalStatus.UNKNOWN
    }

    /**
     * Returns whether Data Saver is globally active, regardless of whether
     * this app is whitelisted.
     *
     * [ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED] means the
     * app has an exemption, but Data Saver is still ON system-wide — so this
     * correctly maps to `true`.
     *
     * [ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED] is the only
     * state where Data Saver is genuinely off → `false`.
     */
    private fun ConnectivityManager.dataSaverEnabled(): Boolean? =
        when (restrictBackgroundStatus) {
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> true
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED -> false
            else -> null
        }
}