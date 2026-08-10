package com.crowdmeasure.sdk.internal.measurement

import android.content.Context
import com.crowdmeasure.sdk.CrowdMeasureSettingsStore
import com.crowdmeasure.sdk.CrowdMeasureConfig
import com.crowdmeasure.sdk.CrowdMeasureLogger
import com.crowdmeasure.sdk.internal.measurement.collectors.DeviceInfoCollector
import com.crowdmeasure.sdk.internal.measurement.collectors.EnvironmentCollector
import com.crowdmeasure.sdk.internal.measurement.collectors.PerformanceCollector
import com.crowdmeasure.sdk.internal.measurement.net.OkHttpClientProvider
import com.crowdmeasure.sdk.model.Measurement
import com.crowdmeasure.sdk.model.Meta
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

internal class MeasurementRunner(
    private val context: Context,
    private val settingsStore: CrowdMeasureSettingsStore,
    private val config: CrowdMeasureConfig,
    private val io: CoroutineDispatcher,
    private val logger: CrowdMeasureLogger,
) {
    suspend fun runOnce(): Result<Measurement> = withContext(io) {
        runCatching {
            logger.debug("Measurement phase started: settings.")
            val settings = settingsStore.settings.first()
            logger.debug("Measurement settings loaded: endpoint=${settings.endpointUrl}.")

            logger.debug("Measurement phase started: HTTP client.")
            val http = OkHttpClientProvider().create()

            logger.debug("Measurement phase started: device.")
            val device = DeviceInfoCollector.collect(versionName = hostVersionName(), context)
            logger.debug(
                "Device snapshot collected: sdk=${device.androidSdk}, " +
                        "manufacturer=${device.deviceManufacturer}, model=${device.deviceModel}.",
            )

            logger.debug("Measurement phase started: environment.")
            val environment = EnvironmentCollector.collect(context, http, config)
            logger.info(
                "Environment snapshot collected: transport=${environment.network.transport}, " +
                        "wifi=${environment.network.wifi != null}, " +
                        "cell=${environment.network.cell != null}, " +
                        "location=${environment.location != null}.",
            )

            val performance = if (config.collectors.performanceEnabled) {
                logger.debug("Measurement phase started: performance probe.")
                PerformanceCollector.run(
                    http,
                    settings.endpointUrl,
                    settings.endpointUrl,
                    config.performanceProbe.attempts,
                ).also {
                    logger.info(
                        "Performance probe finished: endpoint=${it.endpointId}, " +
                                "httpLatencyAvgMs=${it.httpLatencyAvgMs}, " +
                                "jitterMs=${it.jitterMs}, " +
                                "probes=${it.probesSucceeded}/${it.probesAttempted}.",
                    )
                }
            } else {
                logger.info("Performance probe skipped: collector disabled.")
                com.crowdmeasure.sdk.model.PerformanceInfo(endpointId = settings.endpointUrl)
            }

            logger.debug("Measurement phase started: model assembly.")
            Measurement(
                meta = Meta(
                    measurementId = UUID.randomUUID().toString(),
                    timestampUtcMs = System.currentTimeMillis(),
                    appName = device.appName,
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
            ).also {
                logger.info("Measurement assembled: id=${it.meta.measurementId}.")
            }
        }
    }

    private fun hostVersionName(): String =
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
}

private fun CrowdMeasureLogger.debug(message: String) =
    log(CrowdMeasureLogger.Level.DEBUG, message, null)

private fun CrowdMeasureLogger.info(message: String) =
    log(CrowdMeasureLogger.Level.INFO, message, null)
