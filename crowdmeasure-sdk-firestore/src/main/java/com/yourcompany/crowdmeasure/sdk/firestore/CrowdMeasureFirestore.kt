package com.yourcompany.crowdmeasure.sdk.firestore

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.yourcompany.crowdmeasure.sdk.calls.CallSamplingError
import com.yourcompany.crowdmeasure.sdk.calls.CallUploadBatchResult
import com.yourcompany.crowdmeasure.sdk.calls.CallUploadItem
import com.yourcompany.crowdmeasure.sdk.calls.CallUploader
import com.yourcompany.crowdmeasure.sdk.calls.CallUploaderResult
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadError
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadItem
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploader
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploaderResult
import com.yourcompany.crowdmeasure.sdk.upload.UploadBatchResult
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CrowdMeasureFirestore {
    fun create(firestore: FirebaseFirestore): MeasurementUploader =
        FirestoreMeasurementUploader(firestore)

    fun createCallUploader(firestore: FirebaseFirestore): CallUploader =
        FirestoreCallUploader(firestore)
}

internal class FirestoreCallUploader(private val firestore: FirebaseFirestore) : CallUploader {
    override suspend fun upload(items: List<CallUploadItem>): CallUploaderResult = try {
        items.forEach { item ->
            val session = item.session
            val ref = firestore.collection("calls").document(session.sessionId)
            ref.set(CallFirestorePayloadFactory.session(item)).await()
            item.samples.chunked(400).forEach { samples ->
                val batch = firestore.batch()
                samples.forEach { sample ->
                    batch.set(ref.collection("samples").document(sample.id.toString()), CallFirestorePayloadFactory.sample(sample))
                }
                batch.commit().await()
            }
        }
        CallUploaderResult.Success(CallUploadBatchResult(items.map { it.session.sessionId }))
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

internal object CallFirestorePayloadFactory {
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
            "uploaded_at" to uploadedAt,
        )

    fun sample(value: com.yourcompany.crowdmeasure.sdk.calls.CallCellSample): Map<String, Any?> =
        mapOf(
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
            "cell" to FirestorePayloadFactory.toFirestoreValue(value.cell),
        )
}

internal class FirestoreMeasurementUploader(
    private val firestore: FirebaseFirestore,
) : MeasurementUploader {
    override suspend fun upload(items: List<MeasurementUploadItem>): MeasurementUploaderResult {
        if (items.isEmpty()) return MeasurementUploaderResult.Success(UploadBatchResult(emptyList()))
        return try {
            val batch = firestore.batch()
            items.forEach { item ->
                val payload = try {
                    FirestorePayloadFactory.create(item)
                } catch (error: Exception) {
                    return MeasurementUploaderResult.Failure(
                        MeasurementUploadError.SerializationFailure(error)
                    )
                }
                batch.set(
                    firestore.collection(COLLECTION).document(item.measurement.meta.measurementId),
                    payload,
                )
            }
            batch.commit().await()
            MeasurementUploaderResult.Success(
                UploadBatchResult(items.map { it.measurement.meta.measurementId })
            )
        } catch (error: Exception) {
            MeasurementUploaderResult.Failure(error.toUploadError())
        }
    }

    private fun Throwable.toUploadError(): MeasurementUploadError {
        if (this is FirebaseNetworkException) return MeasurementUploadError.TransientFailure(this)
        if (this is FirebaseFirestoreException && code in TRANSIENT_CODES) {
            return MeasurementUploadError.TransientFailure(this)
        }
        return MeasurementUploadError.BackendRejected(this)
    }

    private companion object {
        const val COLLECTION = "measurements"
        val TRANSIENT_CODES = setOf(
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.INTERNAL,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
            FirebaseFirestoreException.Code.UNAVAILABLE,
        )
    }
}

internal object FirestorePayloadFactory {
    private val json = Json { explicitNulls = false }
    private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")

    fun create(item: MeasurementUploadItem, nowUtcMs: Long = System.currentTimeMillis()): Map<String, Any?> {
        val measurement = item.measurement
        return mapOf(
            "install_id" to item.installId,
            "measurement_id" to measurement.meta.measurementId,
            "transport" to measurement.environment.network.transport.name,
            "run_date" to format(measurement.meta.timestampUtcMs),
            "uploaded_at" to format(nowUtcMs),
            "payload" to json.parseToJsonElement(json.encodeToString(measurement)).toFirestoreValue(),
        )
    }

    fun toFirestoreValue(value: com.yourcompany.crowdmeasure.sdk.model.CellInfo): Any? =
        json.parseToJsonElement(json.encodeToString(value)).toFirestoreValue()

    private fun format(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(formatter)

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
