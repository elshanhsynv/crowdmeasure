package com.example.crowdmeasure.data.measurement

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.crowdmeasure.data.measurement.collectors.ContextCollector
import com.example.crowdmeasure.data.measurement.collectors.DeviceCollector
import com.example.crowdmeasure.data.measurement.collectors.DiagnosticsCollector
import com.example.crowdmeasure.data.measurement.collectors.LocationCollector
import com.example.crowdmeasure.data.measurement.collectors.PerformanceTester
import com.example.crowdmeasure.data.measurement.collectors.TelephonyCollector
import com.example.crowdmeasure.data.measurement.collectors.WifiCollector
import com.example.crowdmeasure.data.measurement.net.OkHttpClientProvider
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.example.crowdmeasure.domain.model.Measurement
import com.example.crowdmeasure.domain.model.ProtocolType
import com.example.crowdmeasure.domain.model.SnapshotHeader
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

            // HARD opt-in gate: no consent, no collection.
//            check(settings.consentAccepted && settings.collectionEnabled) {
//                "Consent not accepted or collection disabled."
//            }

            val device = DeviceCollector.collect()

            // Collect base context first (no location inside yet)
            val ctxBase = ContextCollector.collect(context)

            // Respect "Collect only on Wi-Fi"
            if (settings.collectOnlyWifi && ctxBase.transport != TransportType.WIFI) {
                throw IllegalStateException("Collect only on Wi-Fi is enabled.")
            }

            // Location one-shot (optional). Only do it once.
            val coarseLocation = LocationCollector.tryGetCoarseOneShot(context)
            val ctx = ctxBase.copy(coarseLocation = coarseLocation)

            // Transport-specific collectors
            val wifi = if (ctx.transport == TransportType.WIFI) WifiCollector.collect(context) else null
            val cell = if (ctx.transport == TransportType.CELL) TelephonyCollector.collect(context) else null

            // Performance test (light probe)
            val endpointUrl = settings.endpointUrl
            val http = okHttpClientProvider.create()
            val perf = PerformanceTester.run(
                okHttp = http,
                endpointUrl = endpointUrl,
                endpointId = endpointUrl,
                protocolHint = ProtocolType.UNKNOWN
            )

            val diagnostics = DiagnosticsCollector.collect(context)

            val measurementId = UUID.randomUUID().toString()
            val header = SnapshotHeader(
                timestampUtcMs = System.currentTimeMillis(),
                measurementId = measurementId,
                appVersion = device.appVersion,
                androidVersion = device.androidVersion,
                deviceModel = device.deviceModel,
                userConsentVersion = settings.consentVersion
            )

            Measurement(
                header = header,
                context = ctx,
                cell = cell,
                wifi = wifi,
                performance = perf,
                diagnostics = diagnostics,
                feedbackTag = null
            )
        }
    }
}
