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
import com.crowdmeasure.sdk.model.CarrierInfo
import com.crowdmeasure.sdk.model.CellInfo
import com.crowdmeasure.sdk.model.DataUsageInfo
import com.crowdmeasure.sdk.model.Location
import com.crowdmeasure.sdk.model.TransportType
import com.crowdmeasure.sdk.calls.CallStore
import com.crowdmeasure.sdk.calls.CallUploadState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.util.UUID

@Serializable
private data class StoredCallSample(
    val cell: CellInfo,
    val location: Location? = null,
    val dataUsage: DataUsageInfo? = null,
    val transportType: TransportType? = null,
)

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
        intervalSeconds: Int,
        transportType: TransportType?
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
            uploadState = "PENDING",
            carriersJson = null,
        )
        dao.insertSession(entity)
        entity.toDomain(latestSample = null)
    }

    override suspend fun insertSample(
        sessionId: String,
        sampledAtUtcMs: Long,
        elapsedMs: Long,
        cellInfo: CellInfo,
        location: Location?,
        dataUsage: DataUsageInfo?,
        transportType: TransportType?,
    ) = withContext(io) {
        val serving = cellInfo.serving
        val carriersJson = encodeCarriers(cellInfo.simCarriers)
        dao.insertSampleAndIncrement(
            CallCellSampleEntity(
                sessionId = sessionId,
                sampledAtUtcMs = sampledAtUtcMs,
                elapsedMs = elapsedMs,
                cellJson = Converters.json.encodeToString(
                    StoredCallSample(cellInfo.withoutCarriers(), location, dataUsage, transportType)
                ),
                rat = cellInfo.rat,
                nrState = cellInfo.nrState.name,
                dbm = serving?.dbm,
                rsrpDbm = serving?.rsrpDbm,
                rsrqDb = serving?.rsrqDb,
                sinrDb = serving?.sinrDb,
                pci = serving?.pci,
                tac = serving?.tac,
                band = serving?.band
            ),
            carriersJson,
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
            val samplesBySession = samples.mapNotNull { it.toDomainOrNull() }.groupBy { it.sessionId }
            val latestBySession = samplesBySession
                .mapValues { (_, sessionSamples) -> sessionSamples.maxByOrNull { it.sampledAtUtcMs } }
            val carriersBySession = samples.mapNotNull { it.carriersOrNull() }.toMap()

            sessions.map { session ->
                session.toDomain(
                    latestSample = latestBySession[session.sessionId],
                    fallbackCarriers = carriersBySession[session.sessionId],
                    transportType = samplesBySession[session.sessionId].sessionTransport(),
                )
            }
        }

    override fun observeSessions(limit: Int): Flow<List<CallSession>> = observeRecentSessions(limit)

    override fun observeSamples(sessionId: String): Flow<List<CallCellSample>> =
        dao.observeSamples(sessionId).map { samples ->
            samples.mapNotNull { it.toDomainOrNull() }
        }

    suspend fun getRecentSessionsForExport(limit: Int): List<CallSessionExport> = withContext(io) {
        dao.getRecentSessions(limit).map { session ->
            val samples = dao.getSamples(session.sessionId)
            val domainSamples = samples.mapNotNull { it.toDomainOrNull() }
            CallSessionExport(
                session = session.toDomain(
                    latestSample = domainSamples.maxByOrNull { it.sampledAtUtcMs },
                    fallbackCarriers = samples.firstCarriersOrNull(),
                    transportType = domainSamples.sessionTransport(),
                ),
                samples = domainSamples,
            )
        }
    }

    override suspend fun getRecentSessions(limit: Int): List<CallSessionExport> =
        getRecentSessionsForExport(limit)

    override suspend fun getUploadCandidates(limit: Int): List<CallSessionExport> = withContext(io) {
        dao.getUploadCandidates(limit).map { session ->
            val samples = dao.getSamples(session.sessionId)
            val domainSamples = samples.mapNotNull { it.toDomainOrNull() }
            CallSessionExport(
                session.toDomain(
                    latestSample = domainSamples.maxByOrNull { it.sampledAtUtcMs },
                    fallbackCarriers = samples.firstCarriersOrNull(),
                    transportType = domainSamples.sessionTransport(),
                ),
                domainSamples,
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

    private fun CallSessionEntity.toDomain(
        latestSample: CallCellSample?,
        fallbackCarriers: List<CarrierInfo>? = null,
        transportType: TransportType? = null,
    ): CallSession =
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
            simCarriers = decodeCarriers(carriersJson).ifEmpty { fallbackCarriers.orEmpty() },
            latestSample = latestSample,
            transportType = transportType ?: latestSample?.transportType,
        )

    private fun CallCellSampleEntity.toDomainOrNull(): CallCellSample? =
        runCatching {
            val stored = decodeStoredSample(cellJson)
            CallCellSample(
                id = id,
                sessionId = sessionId,
                sampledAtUtcMs = sampledAtUtcMs,
                elapsedMs = elapsedMs,
                cell = stored.cell.withoutCarriers(),
                rat = rat,
                nrState = nrState,
                dbm = dbm,
                rsrpDbm = rsrpDbm,
                rsrqDb = rsrqDb,
                sinrDb = sinrDb,
                pci = pci,
                tac = tac,
                band = band,
                location = stored.location,
                dataUsage = stored.dataUsage,
                transportType = stored.transportType,
            )
        }.getOrNull()

    private fun CallCellSampleEntity.carriersOrNull(): Pair<String, List<CarrierInfo>>? =
        decodeStoredSample(cellJson).cell.simCarriers.takeIf { it.isNotEmpty() }?.let { sessionId to it }

    private fun List<CallCellSampleEntity>.firstCarriersOrNull(): List<CarrierInfo>? =
        firstNotNullOfOrNull { it.carriersOrNull()?.second }

    private fun List<CallCellSample>?.sessionTransport(): TransportType? {
        val real = this.orEmpty().mapNotNull { it.transportType }.filter { it != TransportType.NONE }.toSet()
        return when {
            real.size > 1 -> TransportType.MIXED
            real.size == 1 -> real.first()
            this.orEmpty().any { it.transportType == TransportType.NONE } -> TransportType.NONE
            else -> null
        }
    }

    private fun decodeStoredSample(value: String): StoredCallSample =
        runCatching { Converters.json.decodeFromString(StoredCallSample.serializer(), value) }
            .getOrElse {
                StoredCallSample(Converters.json.decodeFromString(CellInfo.serializer(), value))
            }

    private fun encodeCarriers(value: List<CarrierInfo>): String? =
        value.takeIf { it.isNotEmpty() }?.let {
            Converters.json.encodeToString(ListSerializer(CarrierInfo.serializer()), it)
        }

    private fun decodeCarriers(value: String?): List<CarrierInfo> =
        value?.takeIf { it.isNotBlank() }?.let {
            runCatching {
                Converters.json.decodeFromString(ListSerializer(CarrierInfo.serializer()), it)
            }.getOrNull()
        }.orEmpty()

    private fun CellInfo.withoutCarriers(): CellInfo = copy(simCarriers = emptyList())
}
