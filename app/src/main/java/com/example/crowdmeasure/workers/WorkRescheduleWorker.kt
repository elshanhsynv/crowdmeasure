// workers/WorkRescheduleWorker.kt
package com.example.crowdmeasure.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class WorkRescheduleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sessionRepo: UserSessionRepository,
    private val scheduler: WorkScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val s = sessionRepo.settings.first()
            scheduler.rescheduleFromSettings(settings = s, kickoffOnceIfAllowed = false)
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
