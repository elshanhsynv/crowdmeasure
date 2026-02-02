package com.example.crowdmeasure.domain.repo

import com.example.crowdmeasure.domain.model.Measurement
import kotlinx.coroutines.flow.Flow

interface MeasurementRepository {
    suspend fun runSingleMeasurement(): Result<Measurement>
    suspend fun insert(measurement: Measurement)
    fun observeLastMeasurement(): Flow<Measurement?>
    fun observeQueueCount(): Flow<Int>
    fun observeHistory(limit: Int, feedbackTag: String?): Flow<List<Measurement>>
    suspend fun getMeasurementById(id: String): Measurement?
    suspend fun deleteAll()
    suspend fun deleteOlderThan(cutoffUtcMs: Long): Int
    suspend fun getLastN(limit: Int): List<Measurement>
}