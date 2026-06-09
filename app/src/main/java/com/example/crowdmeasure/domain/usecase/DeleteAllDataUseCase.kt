package com.example.crowdmeasure.domain.usecase

import com.yourcompany.crowdmeasure.sdk.CrowdMeasureResult
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSdk
import com.yourcompany.crowdmeasure.sdk.calls.CallSamplingClient
import com.yourcompany.crowdmeasure.sdk.calls.CallSamplingResult
import javax.inject.Inject

class DeleteAllDataUseCase @Inject constructor(
    private val sdk: CrowdMeasureSdk,
    private val calls: CallSamplingClient,
) {
    suspend operator fun invoke(): Result<Unit> =
        when (val result = sdk.data.deleteAllMeasurements()) {
            is CrowdMeasureResult.Success -> Result.success(Unit)
            is CrowdMeasureResult.Failure -> Result.failure(
                IllegalStateException(result.error.toString())
            )
        }.mapCatching {
            when (val callsResult = calls.deleteAll()) {
                is CallSamplingResult.Success -> Unit
                is CallSamplingResult.Failure -> error(callsResult.error.toString())
            }
        }
}
