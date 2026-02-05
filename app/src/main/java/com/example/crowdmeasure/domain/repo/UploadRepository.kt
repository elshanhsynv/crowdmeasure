package com.example.crowdmeasure.domain.repo

interface UploadRepository {
    suspend fun uploadPending(limit: Int = 50): Result<Int>
}