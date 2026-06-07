package com.example.crowdmeasure.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CallSamplingDao {
    @Query("SELECT * FROM call_sessions WHERE endedAtUtcMs IS NULL ORDER BY startedAtUtcMs DESC LIMIT 1")
    suspend fun getActiveSession(): CallSessionEntity?

    @Query("SELECT * FROM call_sessions ORDER BY startedAtUtcMs DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<CallSessionEntity>>

    @Query("SELECT * FROM call_sessions ORDER BY startedAtUtcMs DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<CallSessionEntity>

    @Query(
        "SELECT * FROM call_sessions " +
            "WHERE endedAtUtcMs IS NOT NULL AND uploadState = :pendingState " +
            "ORDER BY startedAtUtcMs ASC LIMIT :limit"
    )
    suspend fun getUploadCandidates(
        limit: Int,
        pendingState: String = "PENDING"
    ): List<CallSessionEntity>

    @Query("SELECT * FROM call_cell_samples WHERE sessionId = :sessionId ORDER BY sampledAtUtcMs ASC")
    fun observeSamples(sessionId: String): Flow<List<CallCellSampleEntity>>

    @Query("SELECT * FROM call_cell_samples WHERE sessionId = :sessionId ORDER BY sampledAtUtcMs ASC")
    suspend fun getSamples(sessionId: String): List<CallCellSampleEntity>

    @Query(
        "SELECT * FROM call_cell_samples WHERE sessionId IN " +
            "(SELECT sessionId FROM call_sessions ORDER BY startedAtUtcMs DESC LIMIT :sessionLimit) " +
            "ORDER BY sampledAtUtcMs DESC"
    )
    fun observeRecentSamples(sessionLimit: Int): Flow<List<CallCellSampleEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(entity: CallSessionEntity)

    @Update
    suspend fun updateSession(entity: CallSessionEntity)

    @Insert
    suspend fun insertSample(entity: CallCellSampleEntity)

    @Query("UPDATE call_sessions SET sampleCount = sampleCount + 1 WHERE sessionId = :sessionId")
    suspend fun incrementSampleCount(sessionId: String)

    @Query(
        "UPDATE call_sessions SET endedAtUtcMs = :endedAtUtcMs, endReason = :endReason " +
            "WHERE sessionId = :sessionId AND endedAtUtcMs IS NULL"
    )
    suspend fun finishSession(sessionId: String, endedAtUtcMs: Long, endReason: String)

    @Query("UPDATE call_sessions SET uploadState = :state WHERE sessionId = :sessionId")
    suspend fun updateUploadState(sessionId: String, state: String)

    @Query("DELETE FROM call_sessions WHERE startedAtUtcMs < :cutoffUtcMs AND uploadState = 'UPLOADED'")
    suspend fun deleteSessionsOlderThan(cutoffUtcMs: Long)

    @Query("DELETE FROM call_sessions")
    suspend fun clearSessions()

    @Transaction
    suspend fun insertSampleAndIncrement(entity: CallCellSampleEntity) {
        insertSample(entity)
        incrementSampleCount(entity.sessionId)
    }
}
