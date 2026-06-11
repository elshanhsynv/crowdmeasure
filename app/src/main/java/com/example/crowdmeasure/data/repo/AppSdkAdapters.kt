package com.example.crowdmeasure.data.repo

import com.example.crowdmeasure.data.db.Converters
import com.example.crowdmeasure.data.db.MeasurementDao
import com.example.crowdmeasure.data.db.MeasurementEntity
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.crowdmeasure.sdk.CrowdMeasureSettings
import com.crowdmeasure.sdk.CrowdMeasureSettingsStore
import com.crowdmeasure.sdk.MeasurementStore
import com.crowdmeasure.sdk.model.Measurement
import com.crowdmeasure.sdk.model.RecordState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AppMeasurementStore(
    private val dao: MeasurementDao,
) : MeasurementStore {
    override suspend fun save(measurement: Measurement) {
        dao.upsert(
            MeasurementEntity(
                measurementId = measurement.meta.measurementId,
                timestampUtcMs = measurement.meta.timestampUtcMs,
                transport = measurement.environment.network.transport.name,
                json = Converters.measurementToJson(measurement),
                recordState = RecordState.PENDING.name,
            )
        )
    }

    override fun observeLatest(): Flow<Measurement?> =
        dao.observeLast().map { it?.toMeasurementOrNull() }

    override fun observeHistory(limit: Int): Flow<List<Measurement>> =
        dao.observeHistory(limit).map { list -> list.mapNotNull { it.toMeasurementOrNull() } }

    override suspend fun getById(id: String): Measurement? = dao.getById(id)?.toMeasurementOrNull()

    override suspend fun getLastN(limit: Int): List<Measurement> =
        dao.getLastN(limit).mapNotNull { it.toMeasurementOrNull() }

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun deleteOlderThan(cutoffUtcMs: Long): Int = dao.deleteOlderThan(cutoffUtcMs)

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()
    override fun observeFailedCount(): Flow<Int> = dao.observeFailedCount()
    override suspend fun getUploadCandidates(limit: Int): List<Measurement> =
        dao.getUploadCandidates(limit).mapNotNull { it.toMeasurementOrNull() }
    override suspend fun markUploaded(ids: List<String>) = dao.updateState(ids, RecordState.UPLOADED.name)
    override suspend fun markFailed(ids: List<String>) = dao.updateState(ids, RecordState.FAILED.name)

    private fun MeasurementEntity.toMeasurementOrNull(): Measurement? =
        runCatching { Converters.jsonToMeasurement(json) }.getOrNull()
}

class AppSdkSettingsStore(
    private val preferences: AppPreferences,
) : CrowdMeasureSettingsStore {
    override val settings: Flow<CrowdMeasureSettings> = preferences.settings.map {
        CrowdMeasureSettings(endpointUrl = it.endpointUrl, retentionDays = it.retentionDays)
    }

    override suspend fun setEndpointUrl(url: String) = preferences.setEndpointUrl(url)

    override suspend fun setRetentionDays(days: Int) = preferences.setRetentionDays(days)

    override suspend fun collectOnlyOnWifi(): Boolean = preferences.settings.first().collectOnlyWifi
}
