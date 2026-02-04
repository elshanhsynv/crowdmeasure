package com.example.crowdmeasure.data.repo

import com.example.crowdmeasure.data.db.MeasurementDao
import com.example.crowdmeasure.data.db.RecordState
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.example.crowdmeasure.domain.repo.UploadRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject

class UploadRepositoryFirestore @Inject constructor(
    private val dao: MeasurementDao,
    private val prefs: AppPreferences,
    private val firestore: FirebaseFirestore,
) : UploadRepository {

    override suspend fun uploadPending(limit: Int): Result<Int> = runCatching {
        // Hard gate: require consent/collection enabled (privacy)
        val settings = prefs.settingsFirst()
        if (!settings.consentAccepted || !settings.collectionEnabled) {
            return@runCatching 0
        }

        prefs.ensureInstallId()
        val installId = prefs.settingsFirst().installId

        val pending = dao.getByState(RecordState.PENDING, limit)
        if (pending.isEmpty()) return@runCatching 0

        val batch = firestore.batch()

        pending.forEach { e ->
            val payloadMap = jsonToMap(e.json)

            val doc = firestore.collection("measurements").document(e.measurementId)
            batch.set(
                doc,
                mapOf(
                    "install_id" to installId,
                    "measurement_id" to e.measurementId,
                    "timestamp_utc_ms" to e.timestampUtcMs,
                    "transport" to e.transport,
                    "feedback_tag" to e.feedbackTag,
                    "uploaded_at_utc_ms" to System.currentTimeMillis(),
                    "payload" to payloadMap
                )
            )
        }

        batch.commit().await()

        dao.updateState(pending.map { it.measurementId }, RecordState.UPLOADED)
        pending.size
    }

    /**
     * Convert JSON string -> nested Map/Lists accepted by Firestore.
     * Firestore supports Map<String, Any?> with nested maps/lists/primitives.
     */
    private fun jsonToMap(json: String): Map<String, Any?> {
        val obj = JSONObject(json)
        return obj.toMap()
    }

    private fun JSONObject.toMap(): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        val keys = keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = get(k)
            out[k] = when (v) {
                JSONObject.NULL -> null
                is JSONObject -> v.toMap()
                is org.json.JSONArray -> v.toList()
                else -> v
            }
        }
        return out
    }

    private fun org.json.JSONArray.toList(): List<Any?> {
        val out = ArrayList<Any?>(length())
        for (i in 0 until length()) {
            val v = get(i)
            out.add(
                when (v) {
                    JSONObject.NULL -> null
                    is JSONObject -> v.toMap()
                    is org.json.JSONArray -> v.toList()
                    else -> v
                }
            )
        }
        return out
    }
}