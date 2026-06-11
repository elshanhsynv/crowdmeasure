package com.example.crowdmeasure.domain.usecase

import android.net.Uri
import com.crowdmeasure.sdk.CrowdMeasureResult
import com.crowdmeasure.sdk.CrowdMeasureSdk
import javax.inject.Inject

class ExportMeasurementsUseCase @Inject constructor(
    private val sdk: CrowdMeasureSdk,
) {
    suspend operator fun invoke(lastN: Int): Result<Uri> =
        when (val result = sdk.data.exportMeasurements(lastN)) {
            is CrowdMeasureResult.Success -> Result.success(result.value)
            is CrowdMeasureResult.Failure -> Result.failure(
                IllegalStateException(result.error.toString())
            )
        }
}
