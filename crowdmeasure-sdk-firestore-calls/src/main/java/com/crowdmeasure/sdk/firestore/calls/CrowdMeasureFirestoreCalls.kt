package com.crowdmeasure.sdk.firestore.calls

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.crowdmeasure.sdk.calls.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

object CrowdMeasureFirestoreCalls {
    fun create(firestore: FirebaseFirestore): CallUploader = FirestoreCallUploader(firestore)
}

internal class FirestoreCallUploader(private val firestore: FirebaseFirestore) : CallUploader {
    override suspend fun upload(items: List<CallUploadItem>): CallUploaderResult = try {
        items.forEach { item ->
            val ref = firestore.collection("calls").document(item.session.sessionId)
            ref.set(CallFirestorePayload.session(item)).await()
            item.samples.chunked(400).forEach { samples ->
                val batch = firestore.batch()
                samples.forEach { sample ->
                    batch.set(ref.collection("samples").document(sample.id.toString()), CallFirestorePayload.sample(sample))
                }
                batch.commit().await()
            }
        }
        CallUploaderResult.Success(CallUploadBatchResult(items.mapTo(linkedSetOf()) { it.session.sessionId }))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        val mapped = if (error is FirebaseNetworkException ||
            error is FirebaseFirestoreException && error.code in TRANSIENT_CODES
        ) CallSamplingError.TransientFailure(error) else CallSamplingError.BackendRejected(error)
        CallUploaderResult.Failure(mapped)
    }

    private companion object {
        val TRANSIENT_CODES = setOf(
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.INTERNAL,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
            FirebaseFirestoreException.Code.UNAVAILABLE,
        )
    }
}

internal object CallFirestorePayload {
    private val json = Json { explicitNulls = false }
    fun session(item: CallUploadItem, uploadedAt: Any = FieldValue.serverTimestamp()): Map<String, Any?> =
        mapOf(
            "schema_version" to 1,
            "session_id" to item.session.sessionId,
            "install_id" to item.installId,
            "device_model" to item.deviceModel,
            "started_at_utc_ms" to item.session.startedAtUtcMs,
            "ended_at_utc_ms" to item.session.endedAtUtcMs,
            "call_type" to item.session.callType.name,
            "call_source" to item.session.callSource.name,
            "sample_interval_seconds" to item.session.sampleIntervalSeconds,
            "sample_count" to item.session.sampleCount,
            "end_reason" to item.session.endReason,
            "sim_carriers" to json.parseToJsonElement(json.encodeToString(item.session.simCarriers)).toFirestoreValue(),
            "uploaded_at" to uploadedAt,
        )

    fun sample(value: CallCellSample): Map<String, Any?> = mapOf(
        "sample_id" to value.id,
        "sampled_at_utc_ms" to value.sampledAtUtcMs,
        "elapsed_ms" to value.elapsedMs,
        "rat" to value.rat,
        "nr_state" to value.nrState,
        "dbm" to value.dbm,
        "rsrp_dbm" to value.rsrpDbm,
        "rsrq_db" to value.rsrqDb,
        "sinr_db" to value.sinrDb,
        "pci" to value.pci,
        "tac" to value.tac,
        "band" to value.band,
        "location" to value.location?.let {
            mapOf(
                "lat" to it.lat,
                "lon" to it.lon,
                "accuracyMeters" to it.accuracyMeters,
            )
        },
        "data_usage" to value.dataUsage?.let {
            mapOf(
                "dl_mb" to it.dlMB,
                "ul_mb" to it.ulMB,
                "dl_kbps" to it.dlKbps,
                "ul_kbps" to it.ulKbps,
            )
        },
        "cell" to json.parseToJsonElement(json.encodeToString(value.cell)).toFirestoreValue(),
    )

    private fun JsonElement.toFirestoreValue(): Any? = when (this) {
        JsonNull -> null
        is JsonObject -> entries.associate { it.key to it.value.toFirestoreValue() }
        is JsonArray -> map { it.toFirestoreValue() }
        is JsonPrimitive -> when {
            isString -> content
            content == "true" || content == "false" -> content.toBoolean()
            else -> content.toLongOrNull()?.let {
                if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it
            } ?: content.toDoubleOrNull() ?: content
        }
    }
}
