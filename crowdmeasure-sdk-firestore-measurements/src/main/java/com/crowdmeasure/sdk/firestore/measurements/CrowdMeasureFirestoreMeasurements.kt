package com.crowdmeasure.sdk.firestore.measurements

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.crowdmeasure.sdk.upload.MeasurementUploadError
import com.crowdmeasure.sdk.upload.MeasurementUploadItem
import com.crowdmeasure.sdk.upload.MeasurementUploader
import com.crowdmeasure.sdk.upload.MeasurementUploaderResult
import com.crowdmeasure.sdk.upload.UploadBatchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CrowdMeasureFirestoreMeasurements {
    fun create(firestore: FirebaseFirestore): MeasurementUploader =
        FirestoreMeasurementUploader(firestore)
}

internal class FirestoreMeasurementUploader(
    private val firestore: FirebaseFirestore,
) : MeasurementUploader {
    override suspend fun upload(items: List<MeasurementUploadItem>): MeasurementUploaderResult {
        if (items.isEmpty()) return MeasurementUploaderResult.Success(UploadBatchResult())
        return try {
            val batch = firestore.batch()
            items.forEach { item ->
                batch.set(
                    firestore.collection(COLLECTION).document(item.measurement.meta.measurementId),
                    FirestoreMeasurementPayload.create(item),
                )
            }
            batch.commit().await()
            MeasurementUploaderResult.Success(
                UploadBatchResult(uploadedIds = items.mapTo(linkedSetOf()) { it.measurement.meta.measurementId })
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            MeasurementUploaderResult.Failure(error.toUploadError())
        }
    }

    private fun Throwable.toUploadError(): MeasurementUploadError =
        if (this is FirebaseNetworkException ||
            this is FirebaseFirestoreException && code in TRANSIENT_CODES
        ) MeasurementUploadError.TransientFailure(this) else MeasurementUploadError.BackendRejected(this)

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

internal object FirestoreMeasurementPayload {
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
