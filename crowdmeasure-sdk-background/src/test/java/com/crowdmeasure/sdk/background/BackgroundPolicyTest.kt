package com.crowdmeasure.sdk.background

import com.crowdmeasure.sdk.background.internal.isValidBackgroundInterval
import com.crowdmeasure.sdk.background.internal.mapWorkState
import com.crowdmeasure.sdk.background.internal.shouldSkipRecentRun
import org.junit.Assert
import org.junit.Test

class BackgroundPolicyTest {
    @Test
    fun validatesSupportedIntervals() {
        Assert.assertFalse(isValidBackgroundInterval(19))
        Assert.assertTrue(isValidBackgroundInterval(20))
        Assert.assertTrue(isValidBackgroundInterval(10_080))
        Assert.assertFalse(isValidBackgroundInterval(10_081))
    }

    @Test
    fun preventsRunsInsideConfiguredInterval() {
        Assert.assertTrue(shouldSkipRecentRun(1_000, 1_000 + 59 * 60_000, 60))
        Assert.assertFalse(shouldSkipRecentRun(1_000, 1_000 + 60 * 60_000, 60))
        Assert.assertFalse(shouldSkipRecentRun(null, 1_000, 60))
    }

    @Test
    fun mapsWorkStatesAndUnknownValues() {
        Assert.assertEquals(BackgroundWorkState.RUNNING, mapWorkState("RUNNING"))
        Assert.assertEquals(BackgroundWorkState.UNKNOWN, mapWorkState(null))
        Assert.assertEquals(BackgroundWorkState.UNKNOWN, mapWorkState("NEW_STATE"))
    }
}