package com.example.crowdmeasure.domain.usecase

import com.crowdmeasure.sdk.CrowdMeasureSdk
import javax.inject.Inject

class SetEndpointUrlUseCase @Inject constructor(
    private val sdk: CrowdMeasureSdk
) {
    suspend operator fun invoke(url: String) = sdk.settings.setEndpointUrl(url)
}
