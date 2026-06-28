package com.crowdmeasure.sdk.internal.measurement.collectors

import android.net.TrafficStats
import com.crowdmeasure.sdk.model.DataUsageInfo

internal object DataUsageCollector {
    private data class State(
        val lastRxBytes: Long,
        val lastTxBytes: Long,
        val lastTimeMs: Long,
    )

    private val states = mutableMapOf<String, State>()

    @Synchronized
    fun collect(scope: String = "default"): DataUsageInfo? {
        val now = android.os.SystemClock.elapsedRealtime()
        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()

        if (
            rxBytes == TrafficStats.UNSUPPORTED.toLong() ||
            txBytes == TrafficStats.UNSUPPORTED.toLong()
        ) {
            return null
        }

        val previous = states.put(scope, State(rxBytes, txBytes, now))
        if (previous == null) {
            return DataUsageInfo(0.0, 0.0, 0.0, 0.0)
        }

        val seconds = (now - previous.lastTimeMs) / 1_000.0
        val rxDelta = rxBytes - previous.lastRxBytes
        val txDelta = txBytes - previous.lastTxBytes

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
