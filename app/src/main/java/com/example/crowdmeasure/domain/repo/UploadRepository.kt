package com.example.crowdmeasure.domain.repo

interface UploadRepository {
    suspend fun markReadyToUpload(measurementId: String, ready: Boolean)
}