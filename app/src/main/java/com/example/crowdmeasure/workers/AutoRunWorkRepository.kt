package com.example.crowdmeasure.workers

import com.example.crowdmeasure.data.prefs.WorkerStatusStore
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UploadRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import kotlin.math.max

data class AutoRunExecution(
    val outcome: Outcome,
    val code: String,
    val uploadedCount: Int = 0,
    val measurementId: String? = null,
    val cause: Throwable? = null
) {
    enum class Outcome { SUCCESS, RETRY, FAILURE }
}

class AutoRunWorkRepository @Inject constructor(
    private val sessionRepo: UserSessionRepository,
    private val measurementRepo: MeasurementRepository,
    private val uploadRepo: UploadRepository,
    private val statusStore: WorkerStatusStore
) {
    suspend fun execute(nowUtcMs: Long, runAttemptCount: Int): AutoRunExecution {
        if (!AutoRunExecutionLock.mutex.tryLock()) {
            return AutoRunExecution(
                outcome = AutoRunExecution.Outcome.SUCCESS,
                code = CODE_SKIPPED_CONCURRENT_RUN
            )
        }

        return try {
            executeExclusive(nowUtcMs, runAttemptCount)
        } finally {
            AutoRunExecutionLock.mutex.unlock()
        }
    }

    private suspend fun executeExclusive(nowUtcMs: Long, runAttemptCount: Int): AutoRunExecution {
        val settings = sessionRepo.settings.first()
        val allowed = settings.autoRunEnabled
        if (!allowed) {
            return AutoRunExecution(
                outcome = AutoRunExecution.Outcome.SUCCESS,
                code = CODE_GATE_BLOCKED
            )
        }

        val intervalMinutes = settings.autoRunIntervalMinutes.coerceAtLeast(MIN_PERIODIC_MINUTES)
        val intervalMs = intervalMinutes * 60_000L
        val lastSuccessFromStatus = statusStore.getLastSuccessUtcMs()
        val lastPersistedMeasurementUtcMs =
            measurementRepo.getLastN(limit = 1).firstOrNull()?.meta?.timestampUtcMs ?: 0L
        val effectiveLastSuccessUtcMs = max(lastSuccessFromStatus, lastPersistedMeasurementUtcMs)

        if (effectiveLastSuccessUtcMs > 0L &&
            (nowUtcMs - effectiveLastSuccessUtcMs) < (intervalMs - RECENT_RUN_TOLERANCE_MS)
        ) {
            return AutoRunExecution(
                outcome = AutoRunExecution.Outcome.SUCCESS,
                code = CODE_SKIPPED_RECENT_RUN
            )
        }

        val measurement = measurementRepo.runSingleMeasurement().getOrElse { error ->
            return retryOrFailure(CODE_MEASUREMENT_FAILED, error, runAttemptCount)
        }

        try {
            measurementRepo.insert(measurement)
        } catch (error: Exception) {
            return retryOrFailure(CODE_DB_INSERT_FAILED, error, runAttemptCount)
        }

        runCatching { statusStore.setLastSuccessUtcMs(nowUtcMs) }
            .onFailure { WorkerLog.w(TAG, "failed to persist lastSuccess timestamp", it) }

        var uploadedCount = 0
        var uploadError: Throwable? = null
        if (settings.firestoreUploadsEnabled) {
            uploadRepo.uploadPending(limit = UPLOAD_LIMIT).fold(
                onSuccess = { uploadedCount = it },
                onFailure = { uploadError = it }
            )
        }

        if (uploadError != null) {
            WorkerLog.w(TAG, "pending upload failed; keeping run successful", uploadError)
        }

        return AutoRunExecution(
            outcome = AutoRunExecution.Outcome.SUCCESS,
            code = if (uploadError == null) CODE_OK else CODE_UPLOAD_FAILED,
            uploadedCount = uploadedCount,
            measurementId = measurement.meta.measurementId,
            cause = uploadError
        )
    }

    private fun retryOrFailure(
        code: String,
        error: Throwable,
        runAttemptCount: Int
    ): AutoRunExecution {
        val retry = WorkRetryClassifier.shouldRetry(error, runAttemptCount)
        return AutoRunExecution(
            outcome = if (retry) AutoRunExecution.Outcome.RETRY else AutoRunExecution.Outcome.FAILURE,
            code = code,
            cause = error
        )
    }

    companion object {
        const val CODE_OK = "ok"
        const val CODE_GATE_BLOCKED = "gate_blocked"
        const val CODE_SKIPPED_RECENT_RUN = "skipped_recent_run"
        const val CODE_SKIPPED_CONCURRENT_RUN = "skipped_concurrent_run"
        const val CODE_MEASUREMENT_FAILED = "measurement_failed"
        const val CODE_DB_INSERT_FAILED = "db_insert_failed"
        const val CODE_UPLOAD_FAILED = "upload_failed"
        private const val TAG = "AutoRunWorkRepository"
        private const val MIN_PERIODIC_MINUTES = 15
        private const val RECENT_RUN_TOLERANCE_MS = 60_000L
        private const val UPLOAD_LIMIT = 50
    }
}

private object AutoRunExecutionLock {
    val mutex = Mutex()
}

