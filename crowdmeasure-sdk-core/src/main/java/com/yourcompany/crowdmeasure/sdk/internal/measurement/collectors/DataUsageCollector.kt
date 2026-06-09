package com.yourcompany.crowdmeasure.sdk.internal.measurement.collectors

import android.content.Context
import android.net.TrafficStats
import com.yourcompany.crowdmeasure.sdk.model.DataUsageInfo

internal object DataUsageCollector {
    private var lastRxBytes: Long? = null
    private var lastTxBytes: Long? = null
    private var lastTimeMs: Long? = null

    suspend fun collect(context: Context): DataUsageInfo? {
        val now = System.currentTimeMillis()
        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()
        if (rxBytes == TrafficStats.UNSUPPORTED.toLong() ||
            txBytes == TrafficStats.UNSUPPORTED.toLong()
        ) return null

        val previousRx = lastRxBytes
        val previousTx = lastTxBytes
        val previousTime = lastTimeMs
        lastRxBytes = rxBytes
        lastTxBytes = txBytes
        lastTimeMs = now
        if (previousRx == null || previousTx == null || previousTime == null) {
            return DataUsageInfo(0.0, 0.0)
        }
        val seconds = (now - previousTime) / 1_000.0
        if (seconds <= 0 || rxBytes < previousRx || txBytes < previousTx) {
            return DataUsageInfo(0.0, 0.0)
        }
        return DataUsageInfo(
            dlKbps = ((rxBytes - previousRx) * 8.0 / 1024.0) / seconds,
            ulKbps = ((txBytes - previousTx) * 8.0 / 1024.0) / seconds,
        )
    }
}
