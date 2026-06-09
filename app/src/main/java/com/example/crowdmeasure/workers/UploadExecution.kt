package com.example.crowdmeasure.workers

data class UploadExecution(
    val outcome: Outcome,
    val code: String,
    val uploadedCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val cause: Throwable? = null,
) {
    enum class Outcome { SUCCESS, RETRY, FAILURE }
}
