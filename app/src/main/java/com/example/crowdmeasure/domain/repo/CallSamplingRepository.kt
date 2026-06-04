package com.example.crowdmeasure.domain.repo

import com.example.crowdmeasure.domain.model.CallCellSample
import com.example.crowdmeasure.domain.model.CallSession
import com.example.crowdmeasure.domain.model.CallSessionExport
import com.example.crowdmeasure.domain.model.CallSource
import com.example.crowdmeasure.domain.model.CallType
import com.example.crowdmeasure.domain.model.CellInfo
import kotlinx.coroutines.flow.Flow

interface CallSamplingRepository {
    suspend fun startSession(
        callType: CallType,
        callSource: CallSource,
        intervalSeconds: Int = 30
    ): CallSession
    suspend fun insertSample(
        sessionId: String,
        sampledAtUtcMs: Long,
        elapsedMs: Long,
        cellInfo: CellInfo,
    )

    suspend fun finishSession(sessionId: String, endedAtUtcMs: Long, endReason: String)
    fun observeRecentSessions(limit: Int = 50): Flow<List<CallSession>>
    fun observeSamples(sessionId: String): Flow<List<CallCellSample>>
    suspend fun getRecentSessionsForExport(limit: Int): List<CallSessionExport>
    suspend fun deleteOlderThan(cutoffUtcMs: Long)
    suspend fun clearCallSamplingData()
}
