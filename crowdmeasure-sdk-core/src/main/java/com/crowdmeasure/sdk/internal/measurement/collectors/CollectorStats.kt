package com.crowdmeasure.sdk.internal.measurement.collectors

import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToLong

internal fun elapsedMs(startNs: Long): Long? {
    if (startNs <= 0L) return null
    return TimeUnit.NANOSECONDS
        .toMillis(System.nanoTime() - startNs)
        .coerceAtLeast(0L)
}

internal fun jitter(samples: List<Long>): Long? {
    if (samples.size < 2) return null

    var sum = 0L

    for (i in 1 until samples.size) {
        sum += abs(samples[i] - samples[i - 1])
    }

    return (sum.toDouble() / (samples.size - 1))
        .roundToLong()
}
