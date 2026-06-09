package com.yourcompany.crowdmeasure.sdk.background.internal

import com.yourcompany.crowdmeasure.sdk.background.BackgroundWorkState
import com.yourcompany.crowdmeasure.sdk.background.CrowdMeasureBackground
import java.util.concurrent.TimeUnit

internal fun isValidBackgroundInterval(intervalMinutes: Long): Boolean =
    intervalMinutes in CrowdMeasureBackground.MIN_INTERVAL_MINUTES..
        CrowdMeasureBackground.MAX_INTERVAL_MINUTES

internal fun shouldSkipRecentRun(
    latestTimestampUtcMs: Long?,
    nowUtcMs: Long,
    intervalMinutes: Long,
): Boolean = latestTimestampUtcMs != null &&
    nowUtcMs - latestTimestampUtcMs < TimeUnit.MINUTES.toMillis(intervalMinutes)

internal fun mapWorkState(stateName: String?): BackgroundWorkState =
    runCatching { stateName?.let(BackgroundWorkState::valueOf) }
        .getOrNull()
        ?: BackgroundWorkState.UNKNOWN
