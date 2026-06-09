package com.yourcompany.crowdmeasure.sdk

import com.yourcompany.crowdmeasure.sdk.model.Measurement
import kotlinx.coroutines.flow.Flow

interface MeasurementStore {
    suspend fun save(measurement: Measurement)
    fun observeLatest(): Flow<Measurement?>
    fun observeHistory(limit: Int): Flow<List<Measurement>>
    suspend fun getById(id: String): Measurement?
    suspend fun getLastN(limit: Int): List<Measurement>
    suspend fun deleteAll()
    suspend fun deleteOlderThan(cutoffUtcMs: Long): Int
    fun observePendingCount(): Flow<Int>
    fun observeFailedCount(): Flow<Int>
    suspend fun getUploadCandidates(limit: Int): List<Measurement>
    suspend fun markUploaded(ids: List<String>)
    suspend fun markFailed(ids: List<String>)
}

interface CrowdMeasureSettingsStore {
    val settings: Flow<CrowdMeasureSettings>
    suspend fun setEndpointUrl(url: String)
    suspend fun setRetentionDays(days: Int)

    /**
     * Compatibility hook for hosts migrating an existing Wi-Fi-only collection setting.
     * It is intentionally not exposed through [SettingsClient] in SDK v1.
     */
    suspend fun collectOnlyOnWifi(): Boolean = false
}
