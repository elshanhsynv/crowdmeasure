package com.example.crowdmeasure.domain.repo

interface CallUploadRepository {
    suspend fun uploadPending(limit: Int = 10): Result<Int>
}
