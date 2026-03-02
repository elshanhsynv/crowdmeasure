package com.example.crowdmeasure.data.measurement.collectors

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.DiagnosticsInfo

object DiagnosticsCollector {

    fun collect(context: Context): DiagnosticsInfo {
        val pm = context.getSystemService<PowerManager>()

        val thermalStatus: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 0..6 values from PowerManager.THERMAL_STATUS_*
            pm?.currentThermalStatus
        } else null

        val dozeMode: Boolean? = pm?.isDeviceIdleMode

        val cm = context.getSystemService<android.net.ConnectivityManager>()
        val dataSaverEnabled: Boolean? = when (cm?.restrictBackgroundStatus) {
            android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED -> true
            android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED -> false
            android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> false
            else -> null
        }
        return DiagnosticsInfo(
            thermalStatus = thermalStatus,
            dozeMode = dozeMode,
            dataSaverEnabled = dataSaverEnabled,
            handoverCount = null,
            handoverDuringTest = null,
            publicIpHash = null,
            asn = null,
            ispName = null
        )
    }
}
