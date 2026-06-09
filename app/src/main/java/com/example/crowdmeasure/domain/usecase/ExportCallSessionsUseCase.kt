package com.example.crowdmeasure.domain.usecase

import android.net.Uri
import com.yourcompany.crowdmeasure.sdk.calls.CallSamplingClient
import com.yourcompany.crowdmeasure.sdk.calls.CallSamplingResult
import javax.inject.Inject

class ExportCallSessionsUseCase @Inject constructor(
    private val calls: CallSamplingClient,
) {
    suspend operator fun invoke(lastN: Int): Result<Uri> {
        return when (val result = calls.exportSessions(lastN)) {
            is CallSamplingResult.Success -> Result.success(result.value)
            is CallSamplingResult.Failure -> Result.failure(IllegalStateException(result.error.toString()))
        }
    }
}
