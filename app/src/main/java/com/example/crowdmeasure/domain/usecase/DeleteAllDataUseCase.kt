package com.example.crowdmeasure.domain.usecase

import com.crowdmeasure.sdk.CrowdMeasureResult
import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.crowdmeasure.sdk.calls.CallSamplingClient
import com.crowdmeasure.sdk.calls.CallSamplingResult
import javax.inject.Inject

/**
 * Deletes every measurement and call session stored on this device, plus their exported files.
 * Both deletions always run, so one failing does not leave the other data behind.
 * Data that was already uploaded is not affected.
 */
class DeleteAllDataUseCase @Inject constructor(
    private val sdk: CrowdMeasureSdk,
    private val calls: CallSamplingClient,
) {
    suspend operator fun invoke(): Result<Unit> {
        val errors = buildList {
            val measurements = sdk.data.deleteAllMeasurements()
            if (measurements is CrowdMeasureResult.Failure) add("measurements: ${measurements.error}")
            val callSessions = calls.deleteAll()
            if (callSessions is CallSamplingResult.Failure) add("call sessions: ${callSessions.error}")
        }
        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Delete failed for ${errors.joinToString("; ")}"))
        }
    }
}
