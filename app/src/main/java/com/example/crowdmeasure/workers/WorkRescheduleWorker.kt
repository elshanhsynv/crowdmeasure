// workers/WorkRescheduleWorker.kt
package com.example.crowdmeasure.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException

@HiltWorker
class WorkRescheduleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sessionRepo: UserSessionRepository,
    private val scheduler: WorkScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val trigger = inputData.getString(KEY_TRIGGER_SOURCE) ?: TRIGGER_UNKNOWN
        WorkerLog.i(TAG, "start attempt=$runAttemptCount trigger=$trigger")
        return try {
            val s = sessionRepo.settings.first()
            scheduler.rescheduleFromSettings(settings = s, kickoffOnceIfAllowed = false)
            Result.success()
        } catch (cancelled: CancellationException) {
            WorkerLog.w(TAG, "cancelled", cancelled)
            throw cancelled
        } catch (error: Exception) {
            val retry = WorkRetryClassifier.shouldRetry(error, runAttemptCount)
            if (retry) {
                WorkerLog.w(TAG, "reschedule retry", error)
                Result.retry()
            } else {
                WorkerLog.e(TAG, "reschedule failed", error)
                Result.failure(workDataOf(KEY_ERROR_CODE to CODE_RESCHEDULE_FAILED))
            }
        }
    }

    companion object {
        private const val TAG = "WorkRescheduleWorker"
        private const val CODE_RESCHEDULE_FAILED = "reschedule_failed"

        const val KEY_TRIGGER_SOURCE = "trigger_source"
        const val KEY_ERROR_CODE = "error_code"
        const val TRIGGER_APP_START = "app_start"
        const val TRIGGER_BOOT_RECEIVER = "boot_or_update_receiver"
        const val TRIGGER_UNKNOWN = "unknown"
    }
}
