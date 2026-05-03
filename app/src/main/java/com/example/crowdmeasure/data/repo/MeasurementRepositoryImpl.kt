package com.example.crowdmeasure.data.repo

import com.example.crowdmeasure.data.db.Converters
import com.example.crowdmeasure.data.db.MeasurementDao
import com.example.crowdmeasure.data.db.MeasurementEntity
import com.example.crowdmeasure.data.measurement.MeasurementRunner
import com.example.crowdmeasure.domain.model.Measurement
import com.example.crowdmeasure.domain.model.RecordState
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MeasurementRepositoryImpl(
    private val dao: MeasurementDao,
    private val runner: MeasurementRunner,
    private val io: CoroutineDispatcher
) : MeasurementRepository {

    override suspend fun runSingleMeasurement(): Result<Measurement> = withContext(io) {
        runner.runOnce()
    }

    override suspend fun insert(measurement: Measurement) = withContext(io) {
        val entity = MeasurementEntity(
            measurementId = measurement.header.measurementId,
            timestampUtcMs = measurement.header.timestampUtcMs,
            transport = measurement.context.transport.name,
            json = Converters.measurementToJson(measurement),
            recordState = RecordState.PENDING.name
        )
        dao.upsert(entity)
    }

    override fun observeLastMeasurement(): Flow<Measurement?> =
        dao.observeLast().map { e ->
            e?.let {
                runCatching { Converters.jsonToMeasurement(it.json) }.getOrNull()
            }
        }

    override fun observeQueueCount(): Flow<Int> = dao.observeQueueCount()

    override fun observeHistory(limit: Int, feedbackTag: String?): Flow<List<Measurement>> {
        val src = dao.observeHistory(limit)


        return src.map { list ->
            list.mapNotNull { e -> runCatching { Converters.jsonToMeasurement(e.json) }.getOrNull() }
        }
    }

    override suspend fun getMeasurementById(id: String): Measurement? = withContext(io) {
        dao.getById(id)?.let { runCatching { Converters.jsonToMeasurement(it.json) }.getOrNull() }
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
}
