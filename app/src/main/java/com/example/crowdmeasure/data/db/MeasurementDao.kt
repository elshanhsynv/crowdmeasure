package com.example.crowdmeasure.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MeasurementEntity)

    @Query("SELECT * FROM measurements ORDER BY timestampUtcMs DESC LIMIT 1")
    fun observeLast(): Flow<MeasurementEntity?>

    @Query("SELECT COUNT(*) FROM measurements WHERE recordState IN ('PENDING','FAILED')")
    fun observeQueueCount(): Flow<Int>

    @Query("SELECT * FROM measurements ORDER BY timestampUtcMs DESC LIMIT :limit")
    fun observeHistory(limit: Int): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE measurementId = :id LIMIT 1")
    suspend fun getById(id: String): MeasurementEntity?

    @Query("DELETE FROM measurements")
    suspend fun deleteAll()

    @Query("DELETE FROM measurements WHERE timestampUtcMs < :cutoffUtcMs")
    suspend fun deleteOlderThan(cutoffUtcMs: Long): Int

    @Query("SELECT * FROM measurements ORDER BY timestampUtcMs DESC LIMIT :limit")
    suspend fun getLastN(limit: Int): List<MeasurementEntity>

    @Query("UPDATE measurements SET recordState = :state WHERE measurementId = :id")
    suspend fun updateRecordState(id: String, state: String)

    @Query("SELECT * FROM measurements WHERE recordState IN ('PENDING','FAILED') ORDER BY timestampUtcMs ASC LIMIT :limit")
    suspend fun getUploadCandidates(limit: Int): List<MeasurementEntity>

    @Query("UPDATE measurements SET recordState = :newState WHERE measurementId IN (:ids)")
    suspend fun updateState(ids: List<String>, newState: String)
}
