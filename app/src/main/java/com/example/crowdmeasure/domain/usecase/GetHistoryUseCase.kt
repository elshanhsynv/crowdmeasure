package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.MeasurementRepository
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val repo: MeasurementRepository
) {
    operator fun invoke(limit: Int, feedbackTag: String?) = repo.observeHistory(limit, feedbackTag)
}