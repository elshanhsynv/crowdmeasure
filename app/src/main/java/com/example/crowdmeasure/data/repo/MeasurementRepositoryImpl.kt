package com.example.crowdmeasure.data.repo

import com.example.crowdmeasure.data.db.Converters
import com.example.crowdmeasure.data.db.MeasurementDao
import com.example.crowdmeasure.data.db.MeasurementEntity
import com.crowdmeasure.sdk.model.Measurement
import com.crowdmeasure.sdk.model.RecordState
import com.crowdmeasure.sdk.CrowdMeasureResult
import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MeasurementRepositoryImpl(
    private val dao: MeasurementDao,
    private val sdk: CrowdMeasureSdk,
    private val io: CoroutineDispatcher
) : MeasurementRepository {

    override suspend fun runSingleMeasurement(): Result<Measurement> = withContext(io) {
        when (val result = sdk.measurements.runAndSave()) {
            is CrowdMeasureResult.Success -> Result.success(result.value)
            is CrowdMeasureResult.Failure -> Result.failure(
                IllegalStateException(result.error.toString())
            )
        }
    }

    override suspend fun insert(measurement: Measurement) = withContext(io) {
        // SDK runAndSave() already persisted this record through AppMeasurementStore.
        // Keep this method idempotent for existing app workers and ViewModels.
        if (dao.getById(measurement.meta.measurementId) == null) {
            val entity = MeasurementEntity(
                measurementId = measurement.meta.measurementId,
                timestampUtcMs = measurement.meta.timestampUtcMs,
                transport = measurement.environment.network.transport.name,
                json = Converters.measurementToJson(measurement),
                recordState = RecordState.PENDING.name
            )
            dao.upsert(entity)
        }
    }

    override fun observeLastMeasurement(): Flow<Measurement?> =
        sdk.measurements.observeLatest()

    override fun observeQueueCount(): Flow<Int> = dao.observeQueueCount()

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override fun observeFailedCount(): Flow<Int> = dao.observeFailedCount()

    override fun observeHistory(limit: Int, feedbackTag: String?): Flow<List<Measurement>> {
        return sdk.measurements.observeHistory(limit)
    }

    override suspend fun getMeasurementById(id: String): Measurement? = withContext(io) {
        sdk.measurements.getById(id)
    }

    override suspend fun deleteAll(): Result<Unit> = withContext(io) {
        runCatching { dao.deleteAll() }
    }

    override suspend fun deleteOlderThan(cutoffUtcMs: Long): Int = withContext(io) {
        dao.deleteOlderThan(cutoffUtcMs)
    }

    override suspend fun getLastN(limit: Int): List<Measurement> = withContext(io) {
        dao.getLastN(limit).mapNotNull { e -> runCatching { Converters.jsonToMeasurement(e.json) }.getOrNull() }
    }

    override suspend fun getPendingCount(): Int = withContext(io) {
        dao.getPendingCount()
    }

    override suspend fun getFailedCount(): Int = withContext(io) {
        dao.getFailedCount()
    }
}
