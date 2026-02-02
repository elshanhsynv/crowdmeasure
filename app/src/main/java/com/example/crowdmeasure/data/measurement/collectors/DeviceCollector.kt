package com.example.crowdmeasure.data.measurement.collectors

import android.os.Build

data class DeviceSnapshot(
    val appVersion: String,
    val androidVersion: String,
    val deviceModel: String
)

object DeviceCollector {
    fun collect(): DeviceSnapshot {
        val androidVersion = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        val model = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        // appVersion will be filled by BuildConfig at callsite? We keep it simple and pass from BuildConfig in UI.
        // But we can read it safely via reflection fallback; here we just return placeholder and override in header via BuildConfig.
        return DeviceSnapshot(
            appVersion = safeBuildConfigVersionName(),
            androidVersion = androidVersion,
            deviceModel = model
        )
    }

    private fun safeBuildConfigVersionName(): String {
        return try {
            val clazz = Class.forName("com.example.crowdmeasure.BuildConfig")
            val field = clazz.getDeclaredField("VERSION_NAME")
            field.get(null) as? String ?: "unknown"
        } catch (_: Throwable) {
            "unknown"
        }
    }
}