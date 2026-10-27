package com.example.crowdmeasure.domain.model

/** Session lengths the user can pick before starting. */
val ContinuousSessionDurationsMinutes: List<Int> = listOf(15, 30, 60, 120, 240, 480)

const val DEFAULT_CONTINUOUS_SESSION_MINUTES = 60

/** Pauses between measurements in seconds. 0 means the next measurement starts as soon as one finishes. */
val ContinuousSessionGapsSeconds: List<Int> = listOf(0, 30, 60, 300, 900)

const val DEFAULT_CONTINUOUS_SESSION_GAP_SECONDS = 0

data class ContinuousSessionStatus(
    val running: Boolean = false,
    val endsAtUtcMs: Long? = null,
    val collectedCount: Int = 0,
    /** The message from the last failed attempt in the current session; null after a success. */
    val lastError: String? = null,
)

/** A session waiting to start when the next measurement finishes. */
data class ArmedContinuousSession(
    val durationMinutes: Int,
    val gapSeconds: Int,
    /** Latest measurement id at the time of arming; null if there were no measurements yet. */
    val afterMeasurementId: String?,
)

/**
 * A chained session starts only when the latest measurement is a different one from the
 * measurement that was current when the session was armed.
 */
fun shouldStartArmedSession(afterMeasurementId: String?, latestMeasurementId: String?): Boolean =
    latestMeasurementId != null && latestMeasurementId != afterMeasurementId

const val MAX_CUSTOM_SESSION_MINUTES = 24 * 60
const val MAX_CUSTOM_GAP_SECONDS = 60 * 60

fun isValidCustomDuration(minutes: Int?): Boolean =
    minutes != null && minutes in 1..MAX_CUSTOM_SESSION_MINUTES

fun isValidCustomGap(seconds: Int?): Boolean =
    seconds != null && seconds in 0..MAX_CUSTOM_GAP_SECONDS

fun durationLabel(minutes: Int): String {
    if (minutes < 60) return "$minutes min"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) "$hours h" else "$hours h $rest min"
}

fun gapLabel(seconds: Int): String {
    if (seconds == 0) return "None"
    if (seconds < 60) return "$seconds s"
    val minutes = seconds / 60
    val rest = seconds % 60
    return if (rest == 0) "$minutes min" else "$minutes min $rest s"
}
