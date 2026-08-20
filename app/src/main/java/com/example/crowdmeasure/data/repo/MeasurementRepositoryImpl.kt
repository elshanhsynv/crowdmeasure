package com.example.crowdmeasure.data.repo

import com.example.crowdmeasure.data.db.Converters
import com.example.crowdmeasure.data.db.MeasurementDao
import com.example.crowdmeasure.data.db.MeasurementEntity
import com.crowdmeasure.sdk.model.Measurement
import com.crowdmeasure.sdk.model.RecordState
import com.crowdmeasure.sdk.CrowdMeasureError
import com.crowdmeasure.sdk.CrowdMeasureResult
import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.crowdmeasure.sdk.DefaultDataMnoEligibilityState
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.presentation.util.AppLog
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
            is CrowdMeasureResult.Success -> {
                AppLog.i("MeasurementRepo", "Measurement succeeded: id=${result.value.meta.measurementId}")
                Result.success(result.value)
            }
            is CrowdMeasureResult.Failure -> {
                val exception = result.error.toException()
                AppLog.e("MeasurementRepo", exception.message.orEmpty(), exception.cause)
                Result.failure(exception)
            }
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

private fun CrowdMeasureError.toException(): IllegalStateException =
    when (this) {
        is CrowdMeasureError.MissingPermissions -> IllegalStateException(
            "Missing required permissions: ${permissions.joinToString()}",
        )
        CrowdMeasureError.LocationServicesDisabled -> IllegalStateException(
            "Location Services are disabled.",
        )
        CrowdMeasureError.UnsupportedAndroidVersion -> IllegalStateException(
            "Unsupported Android version. Android 10 / API 29 or newer is required.",
        )
        is CrowdMeasureError.CollectionFailed -> IllegalStateException(
            "Measurement collection failed: ${cause.message ?: cause::class.java.simpleName}",
            cause,
        )
        is CrowdMeasureError.PersistenceFailed -> IllegalStateException(
            "Measurement save failed: ${cause.message ?: cause::class.java.simpleName}",
            cause,
        )
        is CrowdMeasureError.ExportFailed -> IllegalStateException(
            "Measurement export failed: ${cause.message ?: cause::class.java.simpleName}",
            cause,
        )
        is CrowdMeasureError.InvalidConfiguration -> IllegalStateException(
            "Invalid SDK configuration: $message",
        )
        is CrowdMeasureError.DefaultDataMnoNotEligible -> IllegalStateException(
            when (eligibility.state) {
                DefaultDataMnoEligibilityState.MISMATCHED ->
                    "The default data MNO does not match the configured target."
                DefaultDataMnoEligibilityState.UNAVAILABLE ->
                    "The default data MNO could not be determined."
                else -> "The default data MNO is not eligible for collection."
            },
        )
    }
