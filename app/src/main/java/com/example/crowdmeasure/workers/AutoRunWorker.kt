package com.example.crowdmeasure.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.crowdmeasure.data.prefs.WorkerStatusStore
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UploadRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class AutoRunWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sessionRepo: UserSessionRepository,
    private val measurementRepo: MeasurementRepository,
    private val uploadRepo: UploadRepository,
    private val statusStore: WorkerStatusStore
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        statusStore.markAutoRunStart(now)

        try {
            val settings = sessionRepo.settings.first()

            // Hard privacy gate
            val allowed = settings.autoRunEnabled && settings.consentAccepted && settings.collectionEnabled
            if (!allowed) {
                statusStore.markAutoRunEnd(
                    nowUtcMs = System.currentTimeMillis(),
                    result = "SUCCESS",
                    error = "gate_blocked",
                    uploadedCount = 0,
                    measurementId = null
                )
                return Result.success()
            }

            // === Min-interval gate (prevents early/duplicate runs) ===
            // Use scheduled interval; fall back to settings; clamp to WM min.
            val scheduledMinutes = statusStore.autoRunStatus.first().lastScheduleMinutes
                .takeIf { it > 0 }
                ?: settings.autoRunIntervalMinutes

            val intervalMinutes = scheduledMinutes.coerceAtLeast(15)
            val intervalMs = intervalMinutes * 60_000L
            val lastOk = statusStore.getLastSuccessUtcMs()

            // Tolerance: if OS fires a bit earlier, skip (prevents "twice in an hour")
            val toleranceMs = 60_000L // 1 minute

            if (lastOk > 0 && (now - lastOk) < (intervalMs - toleranceMs)) {
                statusStore.markAutoRunEnd(
                    nowUtcMs = System.currentTimeMillis(),
                    result = "SUCCESS",
                    error = "skipped_recent_run",
                    uploadedCount = 0,
                    measurementId = null
                )
                return Result.success()
            }

            // 1) Run measurement
            val measurement = measurementRepo.runSingleMeasurement().getOrElse {
                statusStore.markAutoRunEnd(
                    nowUtcMs = System.currentTimeMillis(),
                    result = "RETRY",
                    error = "measurement_failed",
                    uploadedCount = 0,
                    measurementId = null
                )
                return Result.retry()
            }

            // 2) Insert into DB
            runCatching { measurementRepo.insert(measurement) }.onFailure {
                statusStore.markAutoRunEnd(
                    nowUtcMs = System.currentTimeMillis(),
                    result = "RETRY",
                    error = "db_insert_failed",
                    uploadedCount = 0,
                    measurementId = null
                )
                return Result.retry()
            }

            // 3) Upload (best-effort)
            var uploaded = 0
            var uploadError: String? = null

            if (settings.firestoreUploadsEnabled) {
                uploadRepo.uploadPending(limit = 50).fold(
                    onSuccess = { uploaded = it },
                    onFailure = { uploadError = "upload_failed" }
                )
            }

            statusStore.setLastSuccessUtcMs(System.currentTimeMillis())

            statusStore.markAutoRunEnd(
                nowUtcMs = System.currentTimeMillis(),
                result = "SUCCESS",
                error = uploadError,
                uploadedCount = uploaded,
                measurementId = null
            )
            return Result.success()
        } catch (_: Throwable) {
            statusStore.markAutoRunEnd(
                nowUtcMs = System.currentTimeMillis(),
                result = "RETRY",
                error = "unexpected_error",
                uploadedCount = 0,
                measurementId = null
            )
            return Result.retry()
        }
    }
}
