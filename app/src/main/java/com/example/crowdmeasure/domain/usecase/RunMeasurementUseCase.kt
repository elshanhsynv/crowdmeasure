package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.MeasurementRepository
import javax.inject.Inject

class RunMeasurementUseCase @Inject constructor(
    private val repo: MeasurementRepository
) {
    suspend operator fun invoke() = repo.runSingleMeasurement()
}