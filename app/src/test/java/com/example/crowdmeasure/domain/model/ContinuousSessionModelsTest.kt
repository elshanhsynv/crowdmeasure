package com.example.crowdmeasure.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuousSessionModelsTest {

    @Test
    fun armedSessionWaitsForADifferentLatestMeasurement() {
        assertFalse(shouldStartArmedSession(afterMeasurementId = "a", latestMeasurementId = "a"))
        assertTrue(shouldStartArmedSession(afterMeasurementId = "a", latestMeasurementId = "b"))
    }

    @Test
    fun armedSessionWithNoPriorMeasurementStartsOnTheFirstOne() {
        assertFalse(shouldStartArmedSession(afterMeasurementId = null, latestMeasurementId = null))
        assertTrue(shouldStartArmedSession(afterMeasurementId = null, latestMeasurementId = "first"))
    }

    @Test
    fun durationLabelUsesMinutesUnderAnHour() {
        assertEquals("15 min", durationLabel(15))
        assertEquals("30 min", durationLabel(30))
    }

    @Test
    fun gapLabelShowsNoneForZeroAndUnitsOtherwise() {
        assertEquals("None", gapLabel(0))
        assertEquals("30 s", gapLabel(30))
        assertEquals("15 min", gapLabel(900))
    }

    @Test
    fun durationLabelUsesHoursFromAnHourUp() {
        assertEquals("1 h", durationLabel(60))
        assertEquals("8 h", durationLabel(480))
    }

    @Test
    fun durationLabelKeepsRemainingMinutes() {
        assertEquals("1 h 30 min", durationLabel(90))
    }

    @Test
    fun gapLabelKeepsRemainingSeconds() {
        assertEquals("1 min 30 s", gapLabel(90))
    }

    @Test
    fun customDurationMustBeBetweenOneMinuteAndOneDay() {
        assertFalse(isValidCustomDuration(null))
        assertFalse(isValidCustomDuration(0))
        assertTrue(isValidCustomDuration(1))
        assertTrue(isValidCustomDuration(MAX_CUSTOM_SESSION_MINUTES))
        assertFalse(isValidCustomDuration(MAX_CUSTOM_SESSION_MINUTES + 1))
    }

    @Test
    fun customGapMustBeBetweenZeroAndOneHour() {
        assertFalse(isValidCustomGap(null))
        assertFalse(isValidCustomGap(-1))
        assertTrue(isValidCustomGap(0))
        assertTrue(isValidCustomGap(MAX_CUSTOM_GAP_SECONDS))
        assertFalse(isValidCustomGap(MAX_CUSTOM_GAP_SECONDS + 1))
    }
}
