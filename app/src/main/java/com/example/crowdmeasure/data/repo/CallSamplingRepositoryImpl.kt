package com.example.crowdmeasure.data.repo

import com.example.crowdmeasure.data.db.CallCellSampleEntity
import com.example.crowdmeasure.data.db.CallSamplingDao
import com.example.crowdmeasure.data.db.CallSessionEntity
import com.example.crowdmeasure.data.db.Converters
import com.example.crowdmeasure.domain.model.CallCellSample
import com.example.crowdmeasure.domain.model.CallSession
import com.example.crowdmeasure.domain.model.CallSessionExport
import com.example.crowdmeasure.domain.model.CallSource
import com.example.crowdmeasure.domain.model.CallType
import com.yourcompany.crowdmeasure.sdk.model.CellInfo
import com.yourcompany.crowdmeasure.sdk.calls.CallStore
import com.yourcompany.crowdmeasure.sdk.calls.CallUploadState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.util.UUID

class CallSamplingRepositoryImpl(
    private val dao: CallSamplingDao,
    private val io: CoroutineDispatcher
) : CallStore {

    override suspend fun getActiveSession(): CallSession? = withContext(io) {
        dao.getActiveSession()?.toDomain(latestSample = null)
    }

    override suspend fun startSession(
        callType: CallType,
        callSource: CallSource,
        intervalSeconds: Int
    ): CallSession = withContext(io) {
        val active = dao.getActiveSession()
        if (active != null) {
            if (active.callSource == callSource.name) {
                return@withContext active.toDomain(latestSample = null)
            }
            dao.finishSession(
                sessionId = active.sessionId,
                endedAtUtcMs = System.currentTimeMillis(),
                endReason = "replaced_by_${callSource.name.lowercase()}"
            )
        }

        val entity = CallSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            startedAtUtcMs = System.currentTimeMillis(),
            endedAtUtcMs = null,
            callType = callType.name,
            callSource = callSource.name,
            sampleIntervalSeconds = intervalSeconds,
            sampleCount = 0,
            endReason = null,
            uploadState = "PENDING"
        )
        dao.insertSession(entity)
        entity.toDomain(latestSample = null)
    }

    override suspend fun insertSample(
        sessionId: String,
        sampledAtUtcMs: Long,
        elapsedMs: Long,
        cellInfo: CellInfo
    ) = withContext(io) {
        val serving = cellInfo.serving
        dao.insertSampleAndIncrement(
            CallCellSampleEntity(
                sessionId = sessionId,
                sampledAtUtcMs = sampledAtUtcMs,
                elapsedMs = elapsedMs,
                cellJson = Converters.json.encodeToString(cellInfo),
                rat = cellInfo.rat,
                nrState = cellInfo.nrState.name,
                dbm = serving?.dbm,
                rsrpDbm = serving?.rsrpDbm,
                rsrqDb = serving?.rsrqDb,
                sinrDb = serving?.sinrDb,
                pci = serving?.pci,
                tac = serving?.tac,
                band = serving?.band
            )
        )
    }

    override suspend fun finishSession(
        sessionId: String,
        endedAtUtcMs: Long,
        endReason: String
    ) = withContext(io) {
        dao.finishSession(sessionId, endedAtUtcMs, endReason)
    }

    override suspend fun finishActiveSession(
        endedAtUtcMs: Long,
        endReason: String
    ) = withContext(io) {
        dao.finishActiveSession(endedAtUtcMs, endReason)
    }

    override suspend fun reclassifySession(
        sessionId: String,
        callType: CallType,
        callSource: CallSource
    ) = withContext(io) {
        dao.reclassifySession(sessionId, callType.name, callSource.name)
    }

    fun observeRecentSessions(limit: Int = 50): Flow<List<CallSession>> =
        dao.observeRecentSessions(limit).combine(dao.observeRecentSamples(limit)) { sessions, samples ->
            val latestBySession = samples
                .mapNotNull { it.toDomainOrNull() }
                .groupBy { it.sessionId }
                .mapValues { (_, sessionSamples) -> sessionSamples.maxByOrNull { it.sampledAtUtcMs } }

            sessions.map { session ->
                session.toDomain(latestSample = latestBySession[session.sessionId])
            }
        }

    override fun observeSessions(limit: Int): Flow<List<CallSession>> = observeRecentSessions(limit)

    override fun observeSamples(sessionId: String): Flow<List<CallCellSample>> =
        dao.observeSamples(sessionId).map { samples ->
            samples.mapNotNull { it.toDomainOrNull() }
        }

    suspend fun getRecentSessionsForExport(limit: Int): List<CallSessionExport> = withContext(io) {
        dao.getRecentSessions(limit).map { session ->
            CallSessionExport(
                session = session.toDomain(latestSample = null),
                samples = dao.getSamples(session.sessionId).mapNotNull { it.toDomainOrNull() }
            )
        }
    }

    override suspend fun getRecentSessions(limit: Int): List<CallSessionExport> =
        getRecentSessionsForExport(limit)

    override suspend fun getUploadCandidates(limit: Int): List<CallSessionExport> = withContext(io) {
        dao.getUploadCandidates(limit).map { session ->
            CallSessionExport(
                session.toDomain(latestSample = null),
                dao.getSamples(session.sessionId).mapNotNull { it.toDomainOrNull() },
            )
        }
    }

    override suspend fun markUploaded(sessionIds: List<String>) = withContext(io) {
        sessionIds.forEach { dao.updateUploadState(it, CallUploadState.UPLOADED.name) }
    }

    override suspend fun markFailed(sessionIds: List<String>) = withContext(io) {
        sessionIds.forEach { dao.updateUploadState(it, CallUploadState.FAILED.name) }
    }

    override suspend fun deleteOlderThan(cutoffUtcMs: Long) = withContext(io) {
        dao.deleteSessionsOlderThan(cutoffUtcMs)
    }

    suspend fun clearCallSamplingData() = withContext(io) {
        dao.clearSessions()
    }

    override suspend fun deleteAll() = clearCallSamplingData()

    private fun CallSessionEntity.toDomain(latestSample: CallCellSample?): CallSession =
        CallSession(
            sessionId = sessionId,
            startedAtUtcMs = startedAtUtcMs,
            endedAtUtcMs = endedAtUtcMs,
            callType = runCatching { CallType.valueOf(callType) }.getOrDefault(CallType.UNKNOWN),
            callSource = runCatching { CallSource.valueOf(callSource) }.getOrDefault(CallSource.UNKNOWN),
            sampleIntervalSeconds = sampleIntervalSeconds,
            sampleCount = sampleCount,
            endReason = endReason,
            uploadState = runCatching { CallUploadState.valueOf(uploadState) }.getOrDefault(CallUploadState.PENDING),
            latestSample = latestSample
        )

    private fun CallCellSampleEntity.toDomainOrNull(): CallCellSample? =
        runCatching {
            CallCellSample(
                id = id,
                sessionId = sessionId,
                sampledAtUtcMs = sampledAtUtcMs,
                elapsedMs = elapsedMs,
                cell = Converters.json.decodeFromString(CellInfo.serializer(), cellJson),
                rat = rat,
                nrState = nrState,
                dbm = dbm,
                rsrpDbm = rsrpDbm,
                rsrqDb = rsrqDb,
                sinrDb = sinrDb,
                pci = pci,
                tac = tac,
                band = band
            )
        }.getOrNull()
}
