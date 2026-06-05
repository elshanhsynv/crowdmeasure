package com.example.crowdmeasure.data.measurement.collectors

import android.os.Build
import com.example.crowdmeasure.domain.model.DeviceInfo
import timber.log.Timber

object DeviceInfoCollector {

    fun collect(versionName: String): DeviceInfo {
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

        val deviceInfo = DeviceInfo(
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
        Timber.tag("DeviceCollector").d("Collected DeviceInfo: %s", deviceInfo)
        return deviceInfo
    }
}