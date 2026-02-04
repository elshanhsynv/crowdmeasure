package com.example.crowdmeasure.domain.repo

interface UploadRepository {
    suspend fun markReadyToUpload(measurementId: String, ready: Boolean)
    suspend fun uploadPending(limit: Int = 50): Result<Int>
}