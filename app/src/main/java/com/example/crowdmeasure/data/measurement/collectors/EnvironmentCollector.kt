package com.example.crowdmeasure.data.measurement.collectors

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import com.example.crowdmeasure.domain.model.EnvironmentInfo
import com.example.crowdmeasure.domain.model.NetworkCollector
import com.example.crowdmeasure.presentation.util.AppPermissions
import okhttp3.OkHttpClient

object EnvironmentCollector {

    @RequiresApi(Build.VERSION_CODES.Q)
    @WorkerThread
    suspend fun collect(context: Context, okHttp: OkHttpClient): EnvironmentInfo {
        val location = if (AppPermissions.hasCoarseLocation(context)) {
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
