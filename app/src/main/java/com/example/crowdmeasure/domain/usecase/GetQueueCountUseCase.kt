package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.MeasurementRepository
import javax.inject.Inject

class GetQueueCountUseCase @Inject constructor(
    private val repo: MeasurementRepository
) {
    operator fun invoke() = repo.observeQueueCount()
}