package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.MeasurementRepository
import javax.inject.Inject

class DeleteAllDataUseCase @Inject constructor(
    private val repo: MeasurementRepository
) {
    suspend operator fun invoke(): Result<Unit> = repo.deleteAll()
}
