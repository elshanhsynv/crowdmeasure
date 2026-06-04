package com.example.crowdmeasure.data.measurement

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.crowdmeasure.BuildConfig
import com.example.crowdmeasure.data.measurement.collectors.DeviceCollector
import com.example.crowdmeasure.data.measurement.collectors.EnvironmentCollector
import com.example.crowdmeasure.data.measurement.collectors.PerformanceTester
import com.example.crowdmeasure.data.measurement.net.OkHttpClientProvider
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.example.crowdmeasure.domain.model.Measurement
import com.example.crowdmeasure.domain.model.Meta
import com.example.crowdmeasure.domain.model.TransportType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class MeasurementRunner(
    private val context: Context,
    private val prefs: AppPreferences,
    private val okHttpClientProvider: OkHttpClientProvider,
    private val io: CoroutineDispatcher
) {

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun runOnce(): Result<Measurement> = withContext(io) {
        runCatching {
            prefs.ensureInstallId()
            val settings = prefs.settings.first()
            val http = okHttpClientProvider.create()

            val device = DeviceCollector.collect(versionName = BuildConfig.VERSION_NAME)
            val env = EnvironmentCollector.collect(context, http)

            // "Collect only on Wi-Fi"
            if (settings.collectOnlyWifi && env.network.transport != TransportType.WIFI) {
                throw IllegalStateException("Collect only on Wi-Fi is enabled.")
            }

            // Performance test (light probe)
            val endpointUrl = settings.endpointUrl

            val perf = PerformanceTester.run(
                okHttp = http,
                endpointUrl = endpointUrl,
                endpointId = endpointUrl,
            )

//            val throughput = ThroughputTester.run(
//                okHttp = http,
//                config = settings.throughputTestConfig,
//            )
//
//            val performance = perf.copy(
//                downMbps = throughput.downMbps,
//                upMbps = throughput.upMbps,
//                downP95Mbps = throughput.downP95Mbps,
//                downStdDevMbps = throughput.downStdDevMbps,
//                upP95Mbps = throughput.upP95Mbps,
//                upStdDevMbps = throughput.upStdDevMbps,
//                testPayloadBytes = throughput.testPayloadBytes,
//            )

            val measurementId = UUID.randomUUID().toString()

            val meta = Meta(
                measurementId = measurementId,
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
                userIdHash = null
            )

            Measurement(
                meta = meta,
                environment = env,
                performance = perf
            )
        }
    }
}
