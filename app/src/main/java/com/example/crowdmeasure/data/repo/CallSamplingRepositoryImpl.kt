package com.example.crowdmeasure.data.repo

import com.example.crowdmeasure.data.db.CallCellSampleEntity
import com.example.crowdmeasure.data.db.CallSamplingDao
import com.example.crowdmeasure.data.db.CallSessionEntity
import com.example.crowdmeasure.data.db.Converters
import com.example.crowdmeasure.domain.model.CallCellSample
import com.example.crowdmeasure.domain.model.CallSession
import com.example.crowdmeasure.domain.model.CellInfo
import com.example.crowdmeasure.domain.repo.CallSamplingRepository
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
) : CallSamplingRepository {

    override suspend fun startSession(intervalSeconds: Int): CallSession = withContext(io) {
        val active = dao.getActiveSession()
        if (active != null) return@withContext active.toDomain(latestSample = null)

        val entity = CallSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            startedAtUtcMs = System.currentTimeMillis(),
            endedAtUtcMs = null,
            sampleIntervalSeconds = intervalSeconds,
            sampleCount = 0,
            endReason = null
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

    override fun observeRecentSessions(limit: Int): Flow<List<CallSession>> =
        dao.observeRecentSessions(limit).combine(dao.observeRecentSamples(limit)) { sessions, samples ->
            val latestBySession = samples
                .mapNotNull { it.toDomainOrNull() }
                .groupBy { it.sessionId }
                .mapValues { (_, sessionSamples) -> sessionSamples.maxByOrNull { it.sampledAtUtcMs } }

            sessions.map { session ->
                session.toDomain(latestSample = latestBySession[session.sessionId])
            }
        }

    override fun observeSamples(sessionId: String): Flow<List<CallCellSample>> =
        dao.observeSamples(sessionId).map { samples ->
            samples.mapNotNull { it.toDomainOrNull() }
        }

    override suspend fun deleteOlderThan(cutoffUtcMs: Long) = withContext(io) {
        dao.deleteSessionsOlderThan(cutoffUtcMs)
    }

    override suspend fun clearCallSamplingData() = withContext(io) {
        dao.clearSessions()
    }

    private fun CallSessionEntity.toDomain(latestSample: CallCellSample?): CallSession =
        CallSession(
            sessionId = sessionId,
            startedAtUtcMs = startedAtUtcMs,
            endedAtUtcMs = endedAtUtcMs,
            sampleIntervalSeconds = sampleIntervalSeconds,
            sampleCount = sampleCount,
            endReason = endReason,
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
