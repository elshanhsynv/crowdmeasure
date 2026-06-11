package com.crowdmeasure.sdk.internal.measurement

import android.content.Context
import com.crowdmeasure.sdk.CrowdMeasureSettingsStore
import com.crowdmeasure.sdk.CrowdMeasureConfig
import com.crowdmeasure.sdk.IpHashSaltProvider
import com.crowdmeasure.sdk.internal.measurement.collectors.DeviceInfoCollector
import com.crowdmeasure.sdk.internal.measurement.collectors.EnvironmentCollector
import com.crowdmeasure.sdk.internal.measurement.collectors.PerformanceCollector
import com.crowdmeasure.sdk.internal.measurement.net.OkHttpClientProvider
import com.crowdmeasure.sdk.model.Measurement
import com.crowdmeasure.sdk.model.Meta
import com.crowdmeasure.sdk.model.TransportType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

internal class MeasurementRunner(
    private val context: Context,
    private val settingsStore: CrowdMeasureSettingsStore,
    private val config: CrowdMeasureConfig,
    private val ipHashSaltProvider: IpHashSaltProvider,
    private val io: CoroutineDispatcher,
) {
    suspend fun runOnce(): Result<Measurement> = withContext(io) {
        runCatching {
            val settings = settingsStore.settings.first()
            val http = OkHttpClientProvider().create()
            val device = DeviceInfoCollector.collect(versionName = hostVersionName())
            val environment = EnvironmentCollector.collect(context, http, config, ipHashSaltProvider.getSalt())

            if (settingsStore.collectOnlyOnWifi() &&
                environment.network.transport != TransportType.WIFI
            ) {
                error("Collect only on Wi-Fi is enabled.")
            }

            val performance = if (config.collectors.performanceEnabled) {
                PerformanceCollector.run(
                    http,
                    settings.endpointUrl,
                    settings.endpointUrl,
                    config.performanceProbe.attempts,
                )
            } else com.crowdmeasure.sdk.model.PerformanceInfo(endpointId = settings.endpointUrl)
            Measurement(
                meta = Meta(
                    measurementId = UUID.randomUUID().toString(),
                    timestampUtcMs = System.currentTimeMillis(),
                    appVersion = device.appVersion,
                    androidRelease = device.androidRelease,
                    androidSdk = device.androidSdk,
                    deviceModel = device.deviceModel,
                    brand = device.brand,
                    deviceManufacturer = device.deviceManufacturer,
                    deviceOS = device.deviceOS,
                    buildID = device.buildID,
                    hardware = device.hardware,
                    chipset = device.chipset,
                    chipsetManufacturer = device.chipsetManufacturer,
                    sessionId = null,
                    userIdHash = null,
                ),
                environment = environment,
                performance = performance,
            )
        }
    }

    private fun hostVersionName(): String =
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
}
