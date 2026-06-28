package com.crowdmeasure.sdk.internal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.crowdmeasure.sdk.CrowdMeasureSettings
import com.crowdmeasure.sdk.CrowdMeasureSettingsStore
import com.crowdmeasure.sdk.MeasurementStore
import com.crowdmeasure.sdk.model.Measurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal class DefaultCrowdMeasureSettingsStore(
    context: Context,
    preferencesName: String,
    private val defaultEndpointUrl: String,
    private val defaultRetentionDays: Int,
) : CrowdMeasureSettingsStore {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(preferencesName)
    }

    override val settings: Flow<CrowdMeasureSettings> = dataStore.data.map { preferences ->
        CrowdMeasureSettings(
            endpointUrl = preferences[ENDPOINT_URL] ?: defaultEndpointUrl,
            retentionDays = preferences[RETENTION_DAYS] ?: defaultRetentionDays,
        )
    }

    override suspend fun setEndpointUrl(url: String) {
        dataStore.edit { it[ENDPOINT_URL] = url }
    }

    override suspend fun setRetentionDays(days: Int) {
        dataStore.edit { it[RETENTION_DAYS] = days }
    }

    private companion object {
        val ENDPOINT_URL = stringPreferencesKey("endpoint_url")
        val RETENTION_DAYS = intPreferencesKey("retention_days")
    }
}

internal class DefaultMeasurementStore private constructor(
    private val dao: SdkMeasurementDao,
) : MeasurementStore {
    override suspend fun save(measurement: Measurement) {
        dao.upsert(measurement.toEntity())
    }

    override fun observeLatest(): Flow<Measurement?> =
        dao.observeLatest().map { it?.toMeasurementOrNull() }

    override fun observeHistory(limit: Int): Flow<List<Measurement>> =
        dao.observeHistory(limit).map { items ->
            items.mapNotNull(SdkMeasurementEntity::toMeasurementOrNull)
        }

    override suspend fun getById(id: String): Measurement? =
        dao.getById(id)?.toMeasurementOrNull()

    override suspend fun getLastN(limit: Int): List<Measurement> =
        dao.getLastN(limit).mapNotNull(SdkMeasurementEntity::toMeasurementOrNull)

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun deleteOlderThan(cutoffUtcMs: Long): Int = dao.deleteOlderThan(cutoffUtcMs)

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()
    override fun observeFailedCount(): Flow<Int> = dao.observeFailedCount()
    override suspend fun getUploadCandidates(limit: Int): List<Measurement> =
        dao.getUploadCandidates(limit).mapNotNull { entity ->
            entity.toMeasurementOrNull() ?: run {
                dao.updateState(listOf(entity.measurementId), "FAILED")
                null
            }
        }
    override suspend fun markUploaded(ids: List<String>) = dao.updateState(ids, "UPLOADED")
    override suspend fun markFailed(ids: List<String>) = dao.updateState(ids, "FAILED")

    companion object {
        fun create(context: Context, databaseName: String): DefaultMeasurementStore {
            val database = Room.databaseBuilder(context, SdkDatabase::class.java, databaseName).build()
            return DefaultMeasurementStore(database.measurements())
        }
    }
}

@Entity(tableName = "measurements")
internal data class SdkMeasurementEntity(
    @PrimaryKey val measurementId: String,
    val timestampUtcMs: Long,
    val transport: String,
    val json: String,
    val recordState: String,
)

@Dao
internal interface SdkMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SdkMeasurementEntity)

    @Query("SELECT * FROM measurements ORDER BY timestampUtcMs DESC LIMIT 1")
    fun observeLatest(): Flow<SdkMeasurementEntity?>

    @Query("SELECT * FROM measurements ORDER BY timestampUtcMs DESC LIMIT :limit")
    fun observeHistory(limit: Int): Flow<List<SdkMeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE measurementId = :id LIMIT 1")
    suspend fun getById(id: String): SdkMeasurementEntity?

    @Query("SELECT * FROM measurements ORDER BY timestampUtcMs DESC LIMIT :limit")
    suspend fun getLastN(limit: Int): List<SdkMeasurementEntity>

    @Query("DELETE FROM measurements")
    suspend fun deleteAll()

    @Query("DELETE FROM measurements WHERE timestampUtcMs < :cutoffUtcMs")
    suspend fun deleteOlderThan(cutoffUtcMs: Long): Int

    @Query("SELECT COUNT(*) FROM measurements WHERE recordState = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM measurements WHERE recordState = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    @Query("SELECT * FROM measurements WHERE recordState IN ('PENDING','FAILED') ORDER BY timestampUtcMs ASC LIMIT :limit")
    suspend fun getUploadCandidates(limit: Int): List<SdkMeasurementEntity>

    @Query("UPDATE measurements SET recordState = :state WHERE measurementId IN (:ids)")
    suspend fun updateState(ids: List<String>, state: String)
}

@Database(entities = [SdkMeasurementEntity::class], version = 1, exportSchema = true)
internal abstract class SdkDatabase : RoomDatabase() {
    abstract fun measurements(): SdkMeasurementDao
}

private val measurementJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private fun Measurement.toEntity(): SdkMeasurementEntity = SdkMeasurementEntity(
    measurementId = meta.measurementId,
    timestampUtcMs = meta.timestampUtcMs,
    transport = environment.network.transport.name,
    json = measurementJson.encodeToString(Measurement.serializer(), this),
    recordState = "PENDING",
)

private fun SdkMeasurementEntity.toMeasurementOrNull(): Measurement? =
    runCatching { measurementJson.decodeFromString(Measurement.serializer(), json) }.getOrNull()
