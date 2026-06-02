package com.example.crowdmeasure.workers

import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

data class MaintenanceExecution(
    val outcome: Outcome,
    val code: String,
    val cause: Throwable? = null
) {
    enum class Outcome { SUCCESS, RETRY, FAILURE }
}

class MaintenanceWorkRepository @Inject constructor(
    private val measurementRepo: MeasurementRepository,
    private val sessionRepo: UserSessionRepository
) {
    suspend fun execute(nowUtcMs: Long, runAttemptCount: Int): MaintenanceExecution {
        return try {
            val settings = sessionRepo.settings.first()

            val days = settings.retentionDays.coerceIn(
                MIN_RETENTION_DAYS,
                MAX_RETENTION_DAYS
            )

            val cutoffUtcMs = nowUtcMs - (days.toLong() * MILLIS_PER_DAY)

            measurementRepo.deleteOlderThan(cutoffUtcMs)

            MaintenanceExecution(
                outcome = MaintenanceExecution.Outcome.SUCCESS,
                code = CODE_OK
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val retry = WorkRetryClassifier.shouldRetry(error, runAttemptCount)

            MaintenanceExecution(
                outcome = if (retry) {
                    MaintenanceExecution.Outcome.RETRY
                } else {
                    MaintenanceExecution.Outcome.FAILURE
                },
                code = CODE_CLEANUP_FAILED,
                cause = error
            )
        }
    }

    companion object {
        const val CODE_OK = "cleanup_ok"
        const val CODE_CLEANUP_FAILED = "cleanup_failed"

        private const val MIN_RETENTION_DAYS = 1
        private const val MAX_RETENTION_DAYS = 90
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}

