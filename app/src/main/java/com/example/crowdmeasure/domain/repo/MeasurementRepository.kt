package com.example.crowdmeasure.domain.repo

import com.yourcompany.crowdmeasure.sdk.model.Measurement
import kotlinx.coroutines.flow.Flow

interface MeasurementRepository {
    suspend fun runSingleMeasurement(): Result<Measurement>
    suspend fun insert(measurement: Measurement)
    fun observeLastMeasurement(): Flow<Measurement?>
    fun observeQueueCount(): Flow<Int>
    fun observePendingCount(): Flow<Int>
    fun observeFailedCount(): Flow<Int>
    fun observeHistory(limit: Int, feedbackTag: String?): Flow<List<Measurement>>
    suspend fun getMeasurementById(id: String): Measurement?
    suspend fun deleteAll(): Result<Unit>
    suspend fun deleteOlderThan(cutoffUtcMs: Long): Int
    suspend fun getLastN(limit: Int): List<Measurement>
    suspend fun getPendingCount(): Int
    suspend fun getFailedCount(): Int
}
