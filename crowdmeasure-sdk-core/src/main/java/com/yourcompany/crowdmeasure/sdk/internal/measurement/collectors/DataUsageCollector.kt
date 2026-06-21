package com.crowdmeasure.sdk.internal.measurement.collectors

import android.net.TrafficStats
import com.crowdmeasure.sdk.model.DataUsageInfo

internal object DataUsageCollector {
    private var lastRxBytes: Long? = null
    private var lastTxBytes: Long? = null
    private var lastTimeMs: Long? = null

    @Synchronized
    fun collect(): DataUsageInfo? {
        val now = android.os.SystemClock.elapsedRealtime()
        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()

        if (
            rxBytes == TrafficStats.UNSUPPORTED.toLong() ||
            txBytes == TrafficStats.UNSUPPORTED.toLong()
        ) {
            return null
        }

        val previousRx = lastRxBytes
        val previousTx = lastTxBytes
        val previousTime = lastTimeMs

        lastRxBytes = rxBytes
        lastTxBytes = txBytes
        lastTimeMs = now

        if (previousRx == null || previousTx == null || previousTime == null) {
            return DataUsageInfo(0.0, 0.0, 0.0, 0.0)
        }

        val seconds = (now - previousTime) / 1_000.0
        val rxDelta = rxBytes - previousRx
        val txDelta = txBytes - previousTx

        if (seconds <= 0.0 || rxDelta < 0 || txDelta < 0) {
            return DataUsageInfo(0.0, 0.0, 0.0, 0.0)
        }

        return DataUsageInfo(
            dlMB = rxDelta / 1_000_000.0,
            ulMB = txDelta / 1_000_000.0,
            dlKbps = rxDelta * 8.0 / 1_000.0 / seconds,
            ulKbps = txDelta * 8.0 / 1_000.0 / seconds,
        )
    }
}