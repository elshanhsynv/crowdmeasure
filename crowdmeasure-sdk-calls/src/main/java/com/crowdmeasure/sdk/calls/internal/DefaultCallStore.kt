package com.crowdmeasure.sdk.calls.internal

import android.content.Context
import androidx.room.*
import com.crowdmeasure.sdk.calls.*
import com.crowdmeasure.sdk.model.CarrierInfo
import com.crowdmeasure.sdk.model.CellInfo
import com.crowdmeasure.sdk.model.DataUsageInfo
import com.crowdmeasure.sdk.model.Location
import com.crowdmeasure.sdk.model.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Entity(tableName = "call_sessions", indices = [Index("startedAtUtcMs")])
internal data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val startedAtUtcMs: Long,
    val endedAtUtcMs: Long?,
    val callType: String,
    val callSource: String,
    val sampleIntervalSeconds: Int,
    val sampleCount: Int,
    val endReason: String?,
    val uploadState: String,
    val carriersJson: String?,
)

@Entity(
    tableName = "call_cell_samples",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["sessionId"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId"), Index("sampledAtUtcMs")],
)
internal data class SampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val sampledAtUtcMs: Long,
    val elapsedMs: Long,
    val cellJson: String,
    val rat: String?,
    val nrState: String?,
    val dbm: Int?,
    val rsrpDbm: Int?,
    val rsrqDb: Int?,
    val sinrDb: Int?,
    val pci: Int?,
    val tac: Int?,
    val band: Int?,
)

@Dao
internal interface CallsDao {
    @Query("SELECT * FROM call_sessions WHERE endedAtUtcMs IS NULL ORDER BY startedAtUtcMs DESC LIMIT 1")
    suspend fun active(): SessionEntity?

    @Query("SELECT * FROM call_sessions ORDER BY startedAtUtcMs DESC LIMIT :limit")
    fun observeSessions(limit: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM call_sessions ORDER BY startedAtUtcMs DESC LIMIT :limit")
    suspend fun sessions(limit: Int): List<SessionEntity>

    @Query("SELECT * FROM call_sessions WHERE endedAtUtcMs IS NOT NULL AND uploadState IN ('PENDING','FAILED') ORDER BY startedAtUtcMs ASC LIMIT :limit")
    suspend fun uploadCandidates(limit: Int): List<SessionEntity>

    @Query("SELECT * FROM call_cell_samples WHERE sessionId = :id ORDER BY sampledAtUtcMs ASC")
    fun observeSamples(id: String): Flow<List<SampleEntity>>

    @Query("SELECT * FROM call_cell_samples WHERE sessionId = :id ORDER BY sampledAtUtcMs ASC")
    suspend fun samples(id: String): List<SampleEntity>

    @Query("SELECT * FROM call_cell_samples WHERE sessionId IN (SELECT sessionId FROM call_sessions ORDER BY startedAtUtcMs DESC LIMIT :limit) ORDER BY sampledAtUtcMs DESC")
    fun recentSamples(limit: Int): Flow<List<SampleEntity>>

    @Insert
    suspend fun insertSession(entity: SessionEntity)

    @Insert
    suspend fun insertSample(entity: SampleEntity)

    @Query("UPDATE call_sessions SET sampleCount = sampleCount + 1 WHERE sessionId = :id")
    suspend fun increment(id: String)

    @Query("UPDATE call_sessions SET carriersJson = :carriersJson WHERE sessionId = :id AND (carriersJson IS NULL OR carriersJson = '')")
    suspend fun setCarriersIfEmpty(id: String, carriersJson: String)

    @Transaction
    suspend fun insertAndIncrement(entity: SampleEntity, carriersJson: String?) {
        insertSample(entity)
        if (!carriersJson.isNullOrBlank()) setCarriersIfEmpty(entity.sessionId, carriersJson)
        increment(entity.sessionId)
    }

    @Query("UPDATE call_sessions SET endedAtUtcMs=:ended, endReason=:reason WHERE sessionId=:id AND endedAtUtcMs IS NULL")
    suspend fun finish(id: String, ended: Long, reason: String)

    @Query("UPDATE call_sessions SET endedAtUtcMs=:ended, endReason=:reason WHERE endedAtUtcMs IS NULL")
    suspend fun finishActive(ended: Long, reason: String)

    @Query("UPDATE call_sessions SET callType=:type, callSource=:source WHERE sessionId=:id AND endedAtUtcMs IS NULL")
    suspend fun reclassify(id: String, type: String, source: String)

    @Query("UPDATE call_sessions SET uploadState=:state WHERE sessionId IN (:ids)")
    suspend fun mark(ids: List<String>, state: String)

    @Query("DELETE FROM call_sessions WHERE startedAtUtcMs < :cutoff AND uploadState = 'UPLOADED'")
    suspend fun prune(cutoff: Long)

    @Query("DELETE FROM call_sessions")
    suspend fun clear()
}

@Database(entities = [SessionEntity::class, SampleEntity::class], version = 2, exportSchema = true)
internal abstract class CallsDatabase : RoomDatabase() {
    abstract fun dao(): CallsDao
}

@Serializable
internal data class StoredCallSample(
    val cell: CellInfo,
    val location: Location? = null,
    val dataUsage: DataUsageInfo? = null,
    val transportType: TransportType? = null,
)

internal class DefaultCallStore private constructor(private val dao: CallsDao) : CallStore {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override suspend fun getActiveSession() = dao.active()?.domain()
    override suspend fun startSession(
        callType: CallType,
        callSource: CallSource,
        intervalSeconds: Int,
        transportType: TransportType?
    ): CallSession {
        dao.active()?.let {
            dao.finish(
                it.sessionId,
                System.currentTimeMillis(),
                "replaced_by_${callSource.name.lowercase()}"
            )
        }
        val entity = SessionEntity(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            null,
            callType.name,
            callSource.name,
            intervalSeconds,
            0,
            null,
            CallUploadState.PENDING.name,
            null,
        )
        dao.insertSession(entity)
        return entity.domain()
    }

    override suspend fun insertSample(
        sessionId: String,
        sampledAtUtcMs: Long,
        elapsedMs: Long,
        cellInfo: CellInfo,
        location: Location?,
        dataUsage: DataUsageInfo?,
        transportType: TransportType?,
    ) {
        val serving = cellInfo.serving
        val carriersJson = encodeCarriers(cellInfo.simCarriers)
        dao.insertAndIncrement(
            SampleEntity(
                0,
                sessionId,
                sampledAtUtcMs,
                elapsedMs,
                json.encodeToString(
                    StoredCallSample.serializer(),
                    StoredCallSample(cellInfo.withoutCarriers(), location, dataUsage, transportType)
                ),
                cellInfo.rat,
                cellInfo.nrState.name,
                serving?.dbm,
                serving?.rsrpDbm,
                serving?.rsrqDb,
                serving?.sinrDb,
                serving?.pci,
                serving?.tac,
                serving?.band
            ),
            carriersJson,
        )
    }

    override suspend fun finishSession(sessionId: String, endedAtUtcMs: Long, endReason: String) =
        dao.finish(sessionId, endedAtUtcMs, endReason)

    override suspend fun finishActiveSession(endedAtUtcMs: Long, endReason: String) =
        dao.finishActive(endedAtUtcMs, endReason)

    override suspend fun reclassifySession(
        sessionId: String,
        callType: CallType,
        callSource: CallSource
    ) = dao.reclassify(sessionId, callType.name, callSource.name)

    override fun observeSessions(limit: Int) =
        dao.observeSessions(limit).combine(dao.recentSamples(limit)) { sessions, samples ->
            val samplesBySession = samples.mapNotNull { it.domainOrNull() }.groupBy { it.sessionId }
            val latest = samplesBySession.mapValues { it.value.maxByOrNull(CallCellSample::sampledAtUtcMs) }
            val sampleCarriers = samples.mapNotNull { it.carriersOrNull() }.toMap()
            sessions.map {
                it.domain(
                    latest[it.sessionId],
                    sampleCarriers[it.sessionId],
                    samplesBySession[it.sessionId].sessionTransport(),
                )
            }
        }

    override fun observeSamples(sessionId: String) =
        dao.observeSamples(sessionId).map { list -> list.mapNotNull { it.domainOrNull() } }

    override suspend fun getRecentSessions(limit: Int) = dao.sessions(limit).map { session ->
        val samples = dao.samples(session.sessionId)
        val domainSamples = samples.mapNotNull { it.domainOrNull() }
        CallSessionExport(
            session.domain(
                latest = domainSamples.maxByOrNull(CallCellSample::sampledAtUtcMs),
                fallbackCarriers = samples.firstCarriersOrNull(),
                transportType = domainSamples.sessionTransport(),
            ),
            domainSamples,
        )
    }

    override suspend fun getUploadCandidates(limit: Int) = dao.uploadCandidates(limit)
        .map { session ->
            val samples = dao.samples(session.sessionId)
            val domainSamples = samples.mapNotNull { it.domainOrNull() }
            CallSessionExport(
                session.domain(
                    latest = domainSamples.maxByOrNull(CallCellSample::sampledAtUtcMs),
                    fallbackCarriers = samples.firstCarriersOrNull(),
                    transportType = domainSamples.sessionTransport(),
                ),
                domainSamples,
            )
        }

    override suspend fun markUploaded(sessionIds: List<String>) {
        if (sessionIds.isNotEmpty()) dao.mark(sessionIds, CallUploadState.UPLOADED.name)
    }

    override suspend fun markFailed(sessionIds: List<String>) {
        if (sessionIds.isNotEmpty()) dao.mark(sessionIds, CallUploadState.FAILED.name)
    }

    override suspend fun deleteOlderThan(cutoffUtcMs: Long) = dao.prune(cutoffUtcMs)
    override suspend fun deleteAll() = dao.clear()

    private fun SessionEntity.domain(
        latest: CallCellSample? = null,
        fallbackCarriers: List<CarrierInfo>? = null,
        transportType: TransportType? = null,
    ) = CallSession(
        sessionId,
        startedAtUtcMs,
        endedAtUtcMs,
        enum(callType, CallType.UNKNOWN),
        enum(callSource, CallSource.UNKNOWN),
        sampleIntervalSeconds,
        sampleCount,
        endReason,
        enum(uploadState, CallUploadState.PENDING),
        decodeCarriers(carriersJson).ifEmpty { fallbackCarriers.orEmpty() },
        latest,
        transportType ?: latest?.transportType
    )

    private fun SampleEntity.domainOrNull() = runCatching {
        val stored = decodeStoredSample(cellJson)
        CallCellSample(
            id,
            sessionId,
            sampledAtUtcMs,
            elapsedMs,
            stored.cell.withoutCarriers(),
            rat,
            nrState,
            dbm,
            rsrpDbm,
            rsrqDb,
            sinrDb,
            pci,
            tac,
            band,
            stored.location,
            stored.dataUsage,
            stored.transportType,
        )
    }.getOrNull()

    private fun SampleEntity.carriersOrNull(): Pair<String, List<CarrierInfo>>? =
        decodeStoredSample(cellJson).cell.simCarriers.takeIf { it.isNotEmpty() }?.let { sessionId to it }

    private fun List<SampleEntity>.firstCarriersOrNull(): List<CarrierInfo>? =
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
        runCatching { json.decodeFromString(StoredCallSample.serializer(), value) }
            .getOrElse {
                StoredCallSample(json.decodeFromString(CellInfo.serializer(), value))
            }

    private fun encodeCarriers(value: List<CarrierInfo>): String? =
        value.takeIf { it.isNotEmpty() }?.let {
            json.encodeToString(ListSerializer(CarrierInfo.serializer()), it)
        }

    private fun decodeCarriers(value: String?): List<CarrierInfo> =
        value?.takeIf { it.isNotBlank() }?.let {
            runCatching { json.decodeFromString(ListSerializer(CarrierInfo.serializer()), it) }.getOrNull()
        }.orEmpty()

    private fun CellInfo.withoutCarriers(): CellInfo = copy(simCarriers = emptyList())

    private inline fun <reified T : Enum<T>> enum(value: String, fallback: T) =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_sessions ADD COLUMN carriersJson TEXT")
            }
        }

        fun create(context: Context, name: String): CallStore = DefaultCallStore(
            Room.databaseBuilder(context, CallsDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2)
                .build()
                .dao()
        )
    }
}
