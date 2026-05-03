package com.example.crowdmeasure.data.measurement.collectors

import android.os.Build
import kotlinx.serialization.Serializable

@Serializable
data class DeviceSnapshot(
    val appVersion: String,
    /** Human-readable Android release string, e.g. "15". */
    val androidRelease: String,
    /** Numeric API level, e.g. 35. */
    val androidSdk: Int,
    /** "<MANUFACTURER> <MODEL>" trimmed, e.g. "Google Pixel 9". */
    val deviceModel: String,
)

object DeviceCollector {

    fun collect(versionName: String): DeviceSnapshot = DeviceSnapshot(
        appVersion = versionName.ifBlank { "unknown" },
        androidRelease = Build.VERSION.RELEASE,
        androidSdk = Build.VERSION.SDK_INT,
        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
    )
}