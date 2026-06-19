package com.example.crowdmeasure.data.repo

import com.example.crowdmeasure.data.db.CallCellSampleEntity
import com.example.crowdmeasure.data.db.CallSamplingDao
import com.example.crowdmeasure.data.db.CallSessionEntity
import com.example.crowdmeasure.data.db.Converters
import com.crowdmeasure.sdk.model.CarrierInfo
import com.crowdmeasure.sdk.model.CellInfo
import com.crowdmeasure.sdk.model.CellRadioSnapshot
import com.crowdmeasure.sdk.model.Location
import com.example.crowdmeasure.domain.model.CallSource
import com.example.crowdmeasure.domain.model.CallType
import com.crowdmeasure.sdk.model.NrState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CallSamplingRepositoryImplTest {

    private val dao = FakeCallSamplingDao()
    private val repository = CallSamplingRepositoryImpl(dao, Dispatchers.Unconfined)

    @Test
    fun startSession_reusesActiveSession() = runBlocking {
        val first = repository.startSession(
            callType = CallType.INCOMING,
            callSource = CallSource.CELLULAR,
            intervalSeconds = 30
        )
        val second = repository.startSession(
            callType = CallType.INCOMING,
            callSource = CallSource.CELLULAR,
            intervalSeconds = 30
        )

        assertEquals(first.sessionId, second.sessionId)
        assertEquals(CallType.INCOMING, first.callType)
        assertEquals(CallSource.CELLULAR, first.callSource)
        assertEquals(1, dao.sessions.size)
    }

    @Test
    fun startSession_replacesActiveSessionWhenSourceChanges() = runBlocking {
        val cellular = repository.startSession(
            callType = CallType.OUTGOING,
            callSource = CallSource.CELLULAR,
            intervalSeconds = 30
        )
        val whatsapp = repository.startSession(
            callType = CallType.UNKNOWN,
            callSource = CallSource.WHATSAPP_VOICE,
            intervalSeconds = 30
        )

        assertEquals(2, dao.sessions.size)
        assertEquals("replaced_by_whatsapp_voice", dao.sessions.first().endReason)
        assertEquals(CallSource.WHATSAPP_VOICE, whatsapp.callSource)
        assertEquals(whatsapp.sessionId, dao.getActiveSession()?.sessionId)
        assertEquals(false, cellular.sessionId == whatsapp.sessionId)
    }

    @Test
    fun insertSample_persistsSampleAndIncrementsSessionCount() = runBlocking {
        val session = repository.startSession(
            callType = CallType.INCOMING,
            callSource = CallSource.CELLULAR,
            intervalSeconds = 30
        )

        repository.insertSample(
            sessionId = session.sessionId,
            sampledAtUtcMs = 1_000L,
            elapsedMs = 0L,
            cellInfo = testCellInfo(),
            location = Location(40.4093, 49.8671, 12f),
        )

        val sessions = repository.observeRecentSessions().first()
        val samples = repository.observeSamples(session.sessionId).first()

        assertEquals(1, sessions.single().sampleCount)
        assertEquals("LTE", sessions.single().latestSample?.rat)
        assertEquals(1, samples.size)
        assertEquals(-91, samples.single().dbm)
        assertEquals(40.4093, samples.single().location?.lat)
    }

    @Test
    fun observeSamples_readsLegacyCellOnlyRows() = runBlocking {
        val session = repository.startSession(
            callType = CallType.INCOMING,
            callSource = CallSource.CELLULAR,
            intervalSeconds = 30
        )
        dao.insertSample(
            CallCellSampleEntity(
                sessionId = session.sessionId,
                sampledAtUtcMs = 1_000L,
                elapsedMs = 0L,
                cellJson = Converters.json.encodeToString(CellInfo.serializer(), testCellInfo()),
                rat = "LTE",
                nrState = "NONE",
                dbm = -91,
                rsrpDbm = -95,
                rsrqDb = -12,
                sinrDb = 14,
                pci = 20,
                tac = 10,
                band = 3,
            )
        )

        val sample = repository.observeSamples(session.sessionId).first().single()

        assertEquals("LTE", sample.rat)
        assertNull(sample.location)
    }

    @Test
    fun finishSession_closesOnlyActiveSession() = runBlocking {
        val session = repository.startSession(
            callType = CallType.INCOMING,
            callSource = CallSource.CELLULAR,
            intervalSeconds = 30
        )

        repository.finishSession(
            sessionId = session.sessionId,
            endedAtUtcMs = 2_000L,
            endReason = "call_ended"
        )

        val closed = dao.sessions.single()
        assertEquals(2_000L, closed.endedAtUtcMs)
        assertEquals("call_ended", closed.endReason)
        assertNull(dao.getActiveSession())
    }

    @Test
    fun finishActiveSession_closesStaleSession() = runBlocking {
        repository.startSession(
            callType = CallType.UNKNOWN,
            callSource = CallSource.VOIP_GENERIC,
            intervalSeconds = 30
        )

        repository.finishActiveSession(endedAtUtcMs = 3_000L, endReason = "service_restarted")

        assertNull(dao.getActiveSession())
        assertEquals("service_restarted", dao.sessions.single().endReason)
    }

    @Test
    fun reclassifySession_updatesActiveSessionWithoutSplittingIt() = runBlocking {
        val session = repository.startSession(
            callType = CallType.OUTGOING,
            callSource = CallSource.CELLULAR,
            intervalSeconds = 30
        )

        repository.reclassifySession(
            sessionId = session.sessionId,
            callType = CallType.UNKNOWN,
            callSource = CallSource.VOIP_GENERIC
        )

        assertEquals(1, dao.sessions.size)
        assertEquals(CallType.UNKNOWN.name, dao.sessions.single().callType)
        assertEquals(CallSource.VOIP_GENERIC.name, dao.sessions.single().callSource)
    }

    @Test
    fun deleteOlderThan_removesOnlyUploadedOldSessions() = runBlocking {
        val session = repository.startSession(
            callType = CallType.INCOMING,
            callSource = CallSource.CELLULAR,
            intervalSeconds = 30
        )
        dao.finishSession(session.sessionId, endedAtUtcMs = 2_000L, endReason = "call_ended")
        dao.updateUploadState(session.sessionId, state = "UPLOADED")

        repository.deleteOlderThan(Long.MAX_VALUE)

        assertEquals(0, dao.sessions.size)
    }

    private fun testCellInfo(): CellInfo =
        CellInfo(
            simCarriers = listOf(
                CarrierInfo(
                    carrierName = "Test",
                    mcc = "001",
                    mnc = "01",
                    simOperatorId = "00101",
                    simOperatorName = "Test",
                    countryIso = "az",
                    duplexMode = "FDD",
                    subscriptionId = 1,
                    simSlotIndex = 0,
                    isDefaultData = true,
                    isActiveData = true
                )
            ),
            collectedSubscriptionId = 1,
            collectedSimSlotIndex = 0,
            rat = "LTE",
            nrState = NrState.NONE,
            dataNetworkType = "LTE",
            voiceNetworkType = "LTE",
            roaming = false,
            serving = CellRadioSnapshot(
                timestampOffsetMs = 0L,
                cellId = 1,
                cid = null,
                nci = null,
                lac = null,
                tac = 10,
                pci = 20,
                psc = null,
                bsic = null,
                band = 3,
                arfcn = 1_800,
                uarfcn = null,
                nrarfcn = null,
                rsrpDbm = -95,
                rsrqDb = -12,
                sinrDb = 14,
                rssiDbm = -70,
                cqi = null,
                asuLevel = 40,
                dbm = -91,
                timingAdvance = 1,
                ssRsrpDbm = null,
                ssRsrqDb = null,
                ssSinrDb = null,
                csiRsrpDbm = null,
                csiRsrqDb = null,
                csiSinrDb = null,
                bandwidthMhz = 20,
                mimoLayers = null
            ),
            neighbors = emptyList(),
        )
}

private class FakeCallSamplingDao : CallSamplingDao {
    val sessions = mutableListOf<CallSessionEntity>()
    private val samples = mutableListOf<CallCellSampleEntity>()
    private val sessionsFlow = MutableStateFlow<List<CallSessionEntity>>(emptyList())
    private val samplesFlow = MutableStateFlow<List<CallCellSampleEntity>>(emptyList())
    private var nextSampleId = 1L

    override suspend fun getActiveSession(): CallSessionEntity? =
        sessions.firstOrNull { it.endedAtUtcMs == null }

    override fun observeRecentSessions(limit: Int): Flow<List<CallSessionEntity>> =
        sessionsFlow.map { it.sortedByDescending(CallSessionEntity::startedAtUtcMs).take(limit) }

    override fun observeSamples(sessionId: String): Flow<List<CallCellSampleEntity>> =
        samplesFlow.map { all ->
            all.filter { it.sessionId == sessionId }.sortedBy(CallCellSampleEntity::sampledAtUtcMs)
        }

    override suspend fun getUploadCandidates(
        limit: Int,
        pendingState: String
    ): List<CallSessionEntity> =
        sessions.filter { it.endedAtUtcMs != null && it.uploadState == pendingState }
            .sortedBy(CallSessionEntity::startedAtUtcMs)
            .take(limit)

    override suspend fun getRecentSessions(limit: Int): List<CallSessionEntity> =
        sessions.sortedByDescending(CallSessionEntity::startedAtUtcMs).take(limit)

    override suspend fun getSamples(sessionId: String): List<CallCellSampleEntity> =
        samples.filter { it.sessionId == sessionId }.sortedBy(CallCellSampleEntity::sampledAtUtcMs)

    override fun observeRecentSamples(sessionLimit: Int): Flow<List<CallCellSampleEntity>> =
        samplesFlow

    override suspend fun insertSession(entity: CallSessionEntity) {
        sessions += entity
        emit()
    }

    override suspend fun updateSession(entity: CallSessionEntity) {
        val index = sessions.indexOfFirst { it.sessionId == entity.sessionId }
        if (index >= 0) sessions[index] = entity
        emit()
    }

    override suspend fun insertSample(entity: CallCellSampleEntity) {
        samples += entity.copy(id = nextSampleId++)
        emit()
    }

    override suspend fun incrementSampleCount(sessionId: String) {
        val index = sessions.indexOfFirst { it.sessionId == sessionId }
        if (index >= 0) {
            val session = sessions[index]
            sessions[index] = session.copy(sampleCount = session.sampleCount + 1)
        }
        emit()
    }

    override suspend fun finishSession(sessionId: String, endedAtUtcMs: Long, endReason: String) {
        val index = sessions.indexOfFirst { it.sessionId == sessionId && it.endedAtUtcMs == null }
        if (index >= 0) {
            sessions[index] = sessions[index].copy(
                endedAtUtcMs = endedAtUtcMs,
                endReason = endReason
            )
        }
        emit()
    }

    override suspend fun finishActiveSession(endedAtUtcMs: Long, endReason: String) {
        sessions.indices.forEach { index ->
            if (sessions[index].endedAtUtcMs == null) {
                sessions[index] = sessions[index].copy(
                    endedAtUtcMs = endedAtUtcMs,
                    endReason = endReason
                )
            }
        }
        emit()
    }

    override suspend fun reclassifySession(
        sessionId: String,
        callType: String,
        callSource: String
    ) {
        val index = sessions.indexOfFirst { it.sessionId == sessionId && it.endedAtUtcMs == null }
        if (index >= 0) {
            sessions[index] = sessions[index].copy(
                callType = callType,
                callSource = callSource
            )
        }
        emit()
    }

    override suspend fun updateUploadState(sessionId: String, state: String) {
        val index = sessions.indexOfFirst { it.sessionId == sessionId }
        if (index >= 0) {
            sessions[index] = sessions[index].copy(uploadState = state)
        }
        emit()
    }

    override suspend fun deleteSessionsOlderThan(cutoffUtcMs: Long) {
        val removed = sessions
            .filter { it.startedAtUtcMs < cutoffUtcMs && it.uploadState == "UPLOADED" }
            .map { it.sessionId }
            .toSet()
        sessions.removeAll { it.sessionId in removed }
        samples.removeAll { it.sessionId in removed }
        emit()
    }

    override suspend fun clearSessions() {
        sessions.clear()
        samples.clear()
        emit()
    }

    private fun emit() {
        sessionsFlow.value = sessions.toList()
        samplesFlow.value = samples.toList()
    }
}
