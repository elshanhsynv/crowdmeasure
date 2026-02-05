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
            scheduler.scheduleMaintenanceDaily()
            val allowed = s.consentAccepted && s.collectionEnabled && s.autoRunEnabled
            if (allowed) {
                scheduler.scheduleAutoRun(s.autoRunIntervalMinutes.toLong(), s.collectOnlyWifi)
            } else {
                scheduler.cancelAutoRun()
            }
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}
