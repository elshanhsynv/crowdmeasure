package com.example.crowdmeasure.domain.repo

import com.example.crowdmeasure.data.db.MeasurementDao
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.example.crowdmeasure.domain.model.RecordState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class UploadRepositoryFirestore @Inject constructor(
    private val dao: MeasurementDao,
    private val prefs: AppPreferences,
    private val firestore: FirebaseFirestore,
) : UploadRepository {

    override suspend fun uploadPending(limit: Int): Result<Int> = runCatching {
        val settings = prefs.settingsFirst()
        if (!settings.firestoreUploadsEnabled) return@runCatching 0

        prefs.ensureInstallId()
        val installId = prefs.settingsFirst().installId

        val pending = dao.getUploadCandidates( limit)
        if (pending.isEmpty()) return@runCatching 0

        val batch = firestore.batch()

        val currentTimeMillis = System.currentTimeMillis()
        val instant = Instant.ofEpochMilli(currentTimeMillis)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
        val humanReadableTime = zonedDateTime.format(formatter)

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
                    "uploaded_at" to humanReadableTime,
                    "payload" to payloadMap
                )
            )
        }

        batch.commit().await()


        dao.updateState(pending.map { it.measurementId }, RecordState.UPLOADED.name)
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
                is JSONArray -> v.toList()
                else -> v
            }
        }
        return out
    }

    private fun JSONArray.toList(): List<Any?> {
        val out = ArrayList<Any?>(length())
        for (i in 0 until length()) {
            val v = get(i)
            out.add(
                when (v) {
                    JSONObject.NULL -> null
                    is JSONObject -> v.toMap()
                    is JSONArray -> v.toList()
                    else -> v
                }
            )
        }
        return out
    }
}