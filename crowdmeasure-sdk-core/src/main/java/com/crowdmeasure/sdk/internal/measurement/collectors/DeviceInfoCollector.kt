package com.crowdmeasure.sdk.internal.measurement.collectors

import android.content.Context
import android.os.Build
import android.util.Log
import com.crowdmeasure.sdk.model.DeviceInfo

object DeviceInfoCollector {

    fun collect(versionName: String, context: Context): DeviceInfo {
        val chipset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL
        } else {
            Build.BOARD
        }

        val chipsetManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER
        } else {
            "Unknown"
        }

        fun getAppName(context: Context): String {
            val stringId = context.applicationInfo.labelRes
            return if (stringId == 0) {
                context.applicationInfo.nonLocalizedLabel.toString()
            } else {
                context.getString(stringId)
            }
        }

        val appName = getAppName(context = context)

        Log.d(
            "DeviceInfoCollector",
            "Collecting device info for app: $appName, version: $versionName"
        )

        val deviceInfo = DeviceInfo(
            appName = appName.ifBlank { "unknown" },
            appVersion = versionName.ifBlank { "unknown" },
            androidRelease = Build.VERSION.RELEASE,
            androidSdk = Build.VERSION.SDK_INT,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            brand = Build.BRAND,
            deviceManufacturer = Build.MANUFACTURER,
            deviceOS = "Android", // Hardcoded as it's an Android platform collector
            buildID = Build.ID,
            hardware = Build.HARDWARE,
            chipset = chipset,
            chipsetManufacturer = chipsetManufacturer
        )
        return deviceInfo
    }
}
