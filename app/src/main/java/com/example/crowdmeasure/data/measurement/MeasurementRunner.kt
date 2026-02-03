package com.example.crowdmeasure.data.measurement

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.crowdmeasure.data.measurement.collectors.ContextCollector
import com.example.crowdmeasure.data.measurement.collectors.DeviceCollector
import com.example.crowdmeasure.data.measurement.collectors.LocationCollector
import com.example.crowdmeasure.data.measurement.collectors.PerformanceTester
import com.example.crowdmeasure.data.measurement.collectors.TelephonyCollector
import com.example.crowdmeasure.data.measurement.collectors.WifiCollector
import com.example.crowdmeasure.data.measurement.net.OkHttpClientProvider
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.example.crowdmeasure.domain.model.Measurement
import com.example.crowdmeasure.domain.model.ProtocolType
import com.example.crowdmeasure.domain.model.SnapshotHeader
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
        try {
            prefs.ensureInstallId()
            val settings = prefs.settings.first()

            // HARD opt-in gate: no consent, no collection.
            if (!settings.consentAccepted || !settings.collectionEnabled) {
                return@withContext Result.failure(IllegalStateException("Consent not accepted or collection disabled."))
            }

            val device = DeviceCollector.collect()
            val ctx = ContextCollector.collect(context)

            // Respect "Collect only on Wi-Fi"
            if (settings.collectOnlyWifi && ctx.transport.name != "WIFI") {
                return@withContext Result.failure(IllegalStateException("Collect only on Wi-Fi is enabled."))
            }

            val coarseLocation = LocationCollector.tryGetCoarseOneShot(context)
            val ctxWithLoc = ctx.copy(coarseLocation = coarseLocation)

            val wifi = if (ctxWithLoc.transport.name == "WIFI") WifiCollector.collect(context) else null
            val cell = if (ctxWithLoc.transport.name == "CELL") TelephonyCollector.collect(context) else null

            val endpointUrl = settings.endpointUrl
            val http = okHttpClientProvider.create()
            val perf = PerformanceTester.run(
                okHttp = http,
                endpointUrl = endpointUrl,
                endpointId = endpointUrl,
                protocolHint = ProtocolType.UNKNOWN
            )

            val measurementId = UUID.randomUUID().toString()
            val header = SnapshotHeader(
                timestampUtcMs = System.currentTimeMillis(),
                measurementId = measurementId,
                appVersion = device.appVersion,
                androidVersion = device.androidVersion,
                deviceModel = device.deviceModel,
                userConsentVersion = settings.consentVersion
            )

            Result.success(
                Measurement(
                    header = header,
                    context = ctxWithLoc,
                    cell = cell,
                    wifi = wifi,
                    performance = perf,
                    feedbackTag = null
                )
            )
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}