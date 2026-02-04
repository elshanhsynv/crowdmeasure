package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UploadRepository
import javax.inject.Inject

class UploadNowUseCase @Inject constructor(
    private val repo: UploadRepository
) {
    suspend operator fun invoke(limit: Int = 50): Result<Int> = repo.uploadPending(limit)
}