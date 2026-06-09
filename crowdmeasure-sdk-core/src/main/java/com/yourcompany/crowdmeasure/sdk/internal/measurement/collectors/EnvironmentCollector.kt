package com.yourcompany.crowdmeasure.sdk.internal.measurement.collectors

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import com.yourcompany.crowdmeasure.sdk.model.EnvironmentInfo
import okhttp3.OkHttpClient

object EnvironmentCollector {

    @RequiresApi(Build.VERSION_CODES.Q)
    @WorkerThread
    suspend fun collect(context: Context, okHttp: OkHttpClient): EnvironmentInfo {
        val location = if (PlatformChecks.hasCoarseLocation(context)) {
            LocationCollector.tryGetCoarseOneShot(context)
        } else null

        val networkCollector = NetworkCollector.collect(context, okHttp)
        val diagnosticsCollector = DiagnosticsCollector.collect(context)


        val environmentInfo = EnvironmentInfo(
            location = location,
            network = networkCollector,
            device = diagnosticsCollector
        )

        return environmentInfo
    }

}
