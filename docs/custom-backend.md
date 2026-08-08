# Custom Backend Integration

The SDK is not tied to Firebase. Firebase is only one uploader implementation. A host app can upload to any backend by implementing:

- `MeasurementUploader` for measurement uploads.
- `CallUploader` for call-session uploads.

The SDK still owns local queueing, WorkManager scheduling, retry timing, and upload state. The custom uploader owns HTTP/auth/payload mapping.

## Recommended Backend Shape

Use two HTTPS endpoints:

```text
POST /v1/crowdmeasure/measurements:batch
POST /v1/crowdmeasure/calls:batch
```

Make both endpoints idempotent. Re-uploading the same `measurementId` or `sessionId` should be safe and should return success for already stored records.

Recommended response shape:

```json
{
  "uploadedIds": ["m-1", "m-2"],
  "retryableIds": ["m-3"],
  "rejectedIds": []
}
```

Meaning:

- `uploadedIds`: backend stored the item, or already had it.
- `retryableIds`: temporary backend problem for this item; SDK keeps it pending.
- `rejectedIds`: permanent problem for this item; SDK marks it failed.

If the whole request fails because of no network, timeout, HTTP 429, or HTTP 5xx, return a transient failure from the uploader. If the backend rejects the whole batch with HTTP 400/401/403/422, return a backend rejection.

## Measurement Backend API

This is a minimal HTTP client using OkHttp and kotlinx.serialization, which are already used in the project.

```kotlin
import com.crowdmeasure.sdk.model.Measurement
import com.crowdmeasure.sdk.calls.CallCellSample
import com.crowdmeasure.sdk.calls.CallSession
import com.crowdmeasure.sdk.calls.CallUploadItem
import com.crowdmeasure.sdk.upload.MeasurementUploadItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MyBackendApi(
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val tokenProvider: suspend () -> String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val mediaType = "application/json".toMediaType()

    suspend fun uploadMeasurements(
        items: List<MeasurementUploadItem>
    ): BackendBatchResult = post(
        path = "/v1/crowdmeasure/measurements:batch",
        body = MeasurementBatchRequest(
            records = items.map {
                MeasurementRecord(
                    measurementId = it.measurement.meta.measurementId,
                    installId = it.installId,
                    measurement = it.measurement,
                )
            },
        ),
    )

    suspend fun uploadCalls(
        items: List<CallUploadItem>
    ): BackendBatchResult = post(
        path = "/v1/crowdmeasure/calls:batch",
        body = CallBatchRequest(
            records = items.map {
                CallRecord(
                    sessionId = it.session.sessionId,
                    installId = it.installId,
                    deviceModel = it.deviceModel,
                    session = it.session,
                    samples = it.samples,
                )
            },
        ),
    )

    private suspend inline fun <reified T> post(
        path: String,
        body: T,
    ): BackendBatchResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .header("Authorization", "Bearer ${tokenProvider()}")
            .header("Content-Type", "application/json")
            .post(json.encodeToString(body).toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            when {
                response.isSuccessful -> {
                    val text = response.body?.string().orEmpty()
                    json.decodeFromString<BackendBatchResult>(text)
                }
                response.code == 408 || response.code == 429 || response.code in 500..599 -> {
                    throw RetryableBackendException("HTTP ${response.code}")
                }
                else -> {
                    throw PermanentBackendException("HTTP ${response.code}: ${response.body?.string().orEmpty()}")
                }
            }
        }
    }
}

@Serializable
data class MeasurementBatchRequest(
    val records: List<MeasurementRecord>,
)

@Serializable
data class MeasurementRecord(
    val measurementId: String,
    val installId: String,
    val measurement: Measurement,
)

@Serializable
data class CallBatchRequest(
    val records: List<CallRecord>,
)

@Serializable
data class CallRecord(
    val sessionId: String,
    val installId: String,
    val deviceModel: String,
    val session: CallSession,
    val samples: List<CallCellSample>,
)

@Serializable
data class BackendBatchResult(
    val uploadedIds: Set<String> = emptySet(),
    val retryableIds: Set<String> = emptySet(),
    val rejectedIds: Set<String> = emptySet(),
)

class RetryableBackendException(message: String) : IOException(message)
class PermanentBackendException(message: String) : RuntimeException(message)
```

Then adapt it to the SDK contract:

```kotlin
import com.crowdmeasure.sdk.upload.MeasurementUploadError
import com.crowdmeasure.sdk.upload.MeasurementUploadItem
import com.crowdmeasure.sdk.upload.MeasurementUploader
import com.crowdmeasure.sdk.upload.MeasurementUploaderResult
import com.crowdmeasure.sdk.upload.UploadBatchResult
import kotlinx.coroutines.CancellationException
import java.io.IOException

class MyBackendMeasurementUploader(
    private val api: MyBackendApi,
) : MeasurementUploader {
    override suspend fun upload(items: List<MeasurementUploadItem>): MeasurementUploaderResult {
        return try {
            val result = api.uploadMeasurements(items)
            MeasurementUploaderResult.Success(
                UploadBatchResult(
                    uploadedIds = result.uploadedIds,
                    retryableIds = result.retryableIds,
                    rejectedIds = result.rejectedIds,
                )
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: RetryableBackendException) {
            MeasurementUploaderResult.Failure(MeasurementUploadError.TransientFailure(error))
        } catch (error: IOException) {
            MeasurementUploaderResult.Failure(MeasurementUploadError.TransientFailure(error))
        } catch (error: Exception) {
            MeasurementUploaderResult.Failure(MeasurementUploadError.BackendRejected(error))
        }
    }
}
```

Install it:

```kotlin
val api = MyBackendApi(
    baseUrl = "https://api.example.com",
    client = OkHttpClient(),
    tokenProvider = { sessionRepository.currentAccessToken() },
)

val uploads = CrowdMeasureUploads.install(
    context = applicationContext,
    sdk = sdk,
    uploader = MyBackendMeasurementUploader(api),
)

uploads.enable(intervalMinutes = 60, wifiOnly = true)
```

## Call Backend API

Call uploads use a separate contract because the payload is a session plus many samples.

`MyBackendApi.uploadCalls()` above posts call sessions to `/v1/crowdmeasure/calls:batch`.

Uploader adapter:

```kotlin
import com.crowdmeasure.sdk.calls.CallSamplingError
import com.crowdmeasure.sdk.calls.CallUploadItem
import com.crowdmeasure.sdk.calls.CallUploadBatchResult
import com.crowdmeasure.sdk.calls.CallUploader
import com.crowdmeasure.sdk.calls.CallUploaderResult
import kotlinx.coroutines.CancellationException
import java.io.IOException

class MyBackendCallUploader(
    private val api: MyBackendApi,
) : CallUploader {
    override suspend fun upload(items: List<CallUploadItem>): CallUploaderResult {
        return try {
            val result = api.uploadCalls(items)
            CallUploaderResult.Success(
                CallUploadBatchResult(
                    uploadedSessionIds = result.uploadedIds,
                    retryableSessionIds = result.retryableIds,
                    rejectedSessionIds = result.rejectedIds,
                )
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: RetryableBackendException) {
            CallUploaderResult.Failure(CallSamplingError.TransientFailure(error))
        } catch (error: IOException) {
            CallUploaderResult.Failure(CallSamplingError.TransientFailure(error))
        } catch (error: Exception) {
            CallUploaderResult.Failure(CallSamplingError.BackendRejected(error))
        }
    }
}
```

Install call uploads:

```kotlin
val callUploads = CrowdMeasureCallUploads.install(
    context = applicationContext,
    calls = calls,
    config = CallUploadConfig(
        uploader = MyBackendCallUploader(api),
    ),
)

callUploads.enable(intervalMinutes = 60, wifiOnly = true)
```

## Backend Requirements To Give The Server Team

- Accept HTTPS only.
- Authenticate every request. Bearer token is the simplest Android integration.
- Support batches of at least 50 measurements and 50 call sessions.
- Make writes idempotent by `measurementId` and `sessionId`.
- Return per-record uploaded/retryable/rejected IDs.
- Return 2xx for partial success responses.
- Use 408, 429, or 5xx for retryable whole-request failures.
- Use 400, 401, 403, or 422 for permanent whole-request failures.
- Do not require devices to reach an internal-only server directly. Put a public authenticated ingestion API, gateway, or queue in front of it.

## Security Notes

Do not hardcode long-lived secrets in the app. Use user/session tokens, Firebase App Check, device attestation, or short-lived upload tokens depending on the host app. The SDK does not own authentication.

If the backend is internal-only, use one of these:

- Public API gateway that validates auth and forwards internally.
- Cloud queue/pub-sub topic that internal services consume.
- Firestore as temporary ingestion buffer, with internal sync later.
- VPN/private network only if every production device is guaranteed to have it.

## Failure Mapping

| Backend result | Uploader result | SDK behavior |
|---|---|---|
| Stored item | `uploadedIds` | Mark uploaded |
| Duplicate already stored | `uploadedIds` | Mark uploaded |
| Per-item temporary failure | `retryableIds` | Keep pending |
| Per-item invalid payload | `rejectedIds` | Mark failed |
| Network timeout/no connection | `TransientFailure` | WorkManager retries |
| HTTP 408/429/5xx | `TransientFailure` | WorkManager retries |
| HTTP 400/401/403/422 | `BackendRejected` | Mark batch failed |

Always rethrow coroutine cancellation. Do not convert `CancellationException` into a retry.
