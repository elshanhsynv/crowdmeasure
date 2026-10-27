package com.example.crowdmeasure.data.repo

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OutOfQuotaPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.crowdmeasure.data.prefs.ContinuousSessionPrefs
import com.example.crowdmeasure.domain.model.ArmedContinuousSession
import com.example.crowdmeasure.domain.model.ContinuousSessionStatus
import com.example.crowdmeasure.domain.repo.ContinuousSessionRepository
import com.example.crowdmeasure.workers.ContinuousSessionWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class ContinuousSessionRepositoryImpl(
    context: Context,
    private val prefs: ContinuousSessionPrefs,
) : ContinuousSessionRepository {

    private val workManager = WorkManager.getInstance(context)

    override fun observeStatus(): Flow<ContinuousSessionStatus> = combine(
        workManager.getWorkInfosForUniqueWorkFlow(ContinuousSessionWorker.WORK_NAME),
        prefs.progress,
    ) { infos, progress ->
        ContinuousSessionStatus(
            running = infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING },
            endsAtUtcMs = progress.endsAtUtcMs,
            collectedCount = progress.collectedCount,
            lastError = progress.lastError,
        )
    }

    override fun observeArmed(): Flow<ArmedContinuousSession?> = prefs.armed

    override suspend fun armedSession(): ArmedContinuousSession? = prefs.armedSession()

    override suspend fun start(durationMinutes: Int, gapSeconds: Int) {
        if (observeStatus().first().running) return

        val endsAtUtcMs = System.currentTimeMillis() + durationMinutes * 60_000L
        prefs.beginSession(endsAtUtcMs)

        val request = OneTimeWorkRequestBuilder<ContinuousSessionWorker>()
            .setInputData(
                workDataOf(
                    ContinuousSessionWorker.KEY_ENDS_AT_UTC_MS to endsAtUtcMs,
                    ContinuousSessionWorker.KEY_GAP_SECONDS to gapSeconds,
                )
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueueUniqueWork(
            ContinuousSessionWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override suspend fun stop() {
        workManager.cancelUniqueWork(ContinuousSessionWorker.WORK_NAME)
    }

    override suspend fun arm(durationMinutes: Int, gapSeconds: Int, afterMeasurementId: String?) {
        prefs.arm(durationMinutes, gapSeconds, afterMeasurementId)
    }

    override suspend fun disarm() {
        prefs.disarm()
    }
}
