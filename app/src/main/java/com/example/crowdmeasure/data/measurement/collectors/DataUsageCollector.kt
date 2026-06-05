package com.example.crowdmeasure.data.measurement.collectors

import android.content.Context
import android.net.TrafficStats
import androidx.datastore.preferences.core.edit
import com.example.crowdmeasure.data.prefs.DataStoreKeys
import com.example.crowdmeasure.data.prefs.dataStore
import com.example.crowdmeasure.domain.model.DataUsageInfo
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

object DataUsageCollector {

    suspend fun collect(context: Context): DataUsageInfo? {
        val currentTime = System.currentTimeMillis()
        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()

        if ((rxBytes == TrafficStats.UNSUPPORTED.toLong()) || (txBytes == TrafficStats.UNSUPPORTED.toLong())) {
            Timber.tag("DataUsageCollector").w("TrafficStats unsupported on this device")
            return null
        }

        val prefs = context.dataStore.data.firstOrNull()
        val lastRxBytes = prefs?.get(DataStoreKeys.DATA_USAGE_LAST_RX_BYTES) ?: 0L
        val lastTxBytes = prefs?.get(DataStoreKeys.DATA_USAGE_LAST_TX_BYTES) ?: 0L
        val lastTime = prefs?.get(DataStoreKeys.DATA_USAGE_LAST_TIME_MS) ?: 0L

        var dlKbps = 0.0
        var ulKbps = 0.0

        if (lastTime != 0L && rxBytes >= lastRxBytes && txBytes >= lastTxBytes) {
            val timeDiffSec = (currentTime - lastTime) / 1000.0
            if (timeDiffSec > 0) {
                // (Bytes difference * 8 bits / 1024 to get Kb) / seconds = Kbps
                dlKbps = ((rxBytes - lastRxBytes) * 8.0 / 1024.0) / timeDiffSec
                ulKbps = ((txBytes - lastTxBytes) * 8.0 / 1024.0) / timeDiffSec
            }
        }

        // Persist current values for next run
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.DATA_USAGE_LAST_RX_BYTES] = rxBytes
            settings[DataStoreKeys.DATA_USAGE_LAST_TX_BYTES] = txBytes
            settings[DataStoreKeys.DATA_USAGE_LAST_TIME_MS] = currentTime
        }

        Timber.tag("DataUsageCollector").d("Collected data usage: dl=%.2f Kbps, ul=%.2f Kbps", dlKbps, ulKbps)

        return DataUsageInfo(
            dlKbps = dlKbps,
            ulKbps = ulKbps,
        )
    }
}
