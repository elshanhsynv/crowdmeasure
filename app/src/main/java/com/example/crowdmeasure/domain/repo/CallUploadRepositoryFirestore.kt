package com.example.crowdmeasure.domain.repo

import android.os.Build
import com.example.crowdmeasure.data.db.CallCellSampleEntity
import com.example.crowdmeasure.data.db.CallSamplingDao
import com.example.crowdmeasure.data.db.CallSessionEntity
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.example.crowdmeasure.domain.model.RecordState
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class CallUploadRepositoryFirestore @Inject constructor(
    private val dao: CallSamplingDao,
    private val prefs: AppPreferences,
    private val firestore: FirebaseFirestore
) : CallUploadRepository {

    override suspend fun uploadPending(limit: Int): Result<Int> = runCatching {
        val settings = prefs.settingsFirst()
        if (!settings.firestoreUploadsEnabled) return@runCatching 0

        prefs.ensureInstallId()
        val installId = prefs.settingsFirst().installId
        val sessions = dao.getUploadCandidates(limit)

        sessions.forEach { session ->
            uploadSession(session, installId)
            dao.updateUploadState(session.sessionId, RecordState.UPLOADED.name)
        }

        sessions.size
    }

    private suspend fun uploadSession(session: CallSessionEntity, installId: String) {
        val sessionDoc = firestore.collection(COLLECTION_CALLS).document(session.sessionId)
        sessionDoc.set(
            mapOf(
                "schema_version" to 1,
                "session_id" to session.sessionId,
                "install_id" to installId,
                "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                "started_at_utc_ms" to session.startedAtUtcMs,
                "ended_at_utc_ms" to session.endedAtUtcMs,
                "call_type" to session.callType,
                "call_source" to session.callSource,
                "sample_interval_seconds" to session.sampleIntervalSeconds,
                "sample_count" to session.sampleCount,
                "end_reason" to session.endReason,
                "uploaded_at" to FieldValue.serverTimestamp()
            )
        ).await()

        dao.getSamples(session.sessionId)
            .chunked(MAX_BATCH_WRITES)
            .forEach { samples ->
                val batch = firestore.batch()
                samples.forEach { sample ->
                    val sampleDoc = sessionDoc.collection(SUBCOLLECTION_SAMPLES)
                        .document(sample.id.toString())
                    batch.set(sampleDoc, sample.toPayload())
                }
                batch.commit().await()
            }
    }

    private fun CallCellSampleEntity.toPayload(): Map<String, Any?> =
        mapOf(
            "sample_id" to id,
            "sampled_at_utc_ms" to sampledAtUtcMs,
            "elapsed_ms" to elapsedMs,
            "rat" to rat,
            "nr_state" to nrState,
            "dbm" to dbm,
            "rsrp_dbm" to rsrpDbm,
            "rsrq_db" to rsrqDb,
            "sinr_db" to sinrDb,
            "pci" to pci,
            "tac" to tac,
            "band" to band,
            "cell" to JSONObject(cellJson).toMap()
        )

    private fun JSONObject.toMap(): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        keys().forEach { key ->
            result[key] = get(key).toFirestoreValue()
        }
        return result
    }

    private fun JSONArray.toList(): List<Any?> =
        List(length()) { index -> get(index).toFirestoreValue() }

    private fun Any?.toFirestoreValue(): Any? = when (this) {
        JSONObject.NULL -> null
        is JSONObject -> toMap()
        is JSONArray -> toList()
        else -> this
    }

    companion object {
        private const val COLLECTION_CALLS = "calls"
        private const val SUBCOLLECTION_SAMPLES = "samples"
        private const val MAX_BATCH_WRITES = 400
    }
}
