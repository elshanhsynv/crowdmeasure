package com.example.crowdmeasure.workers

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.crowdmeasure.data.prefs.WorkerStatusStore
import com.example.crowdmeasure.domain.repo.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

class WorkScheduler @Inject constructor(
    private val workManagerProvider: Provider<WorkManager>,
    private val statusStore: WorkerStatusStore
) {
    private val workManager: WorkManager get() = workManagerProvider.get()

    companion object {
        const val AUTO_RUN_NAME = "auto_run_measurement"
        const val AUTO_RUN_KICKOFF_NAME = "auto_run_measurement_kickoff"
        const val AUTO_RUN_DEBUG_ONCE_NAME = "auto_run_measurement_debug_once"
        const val UPLOAD_NAME = "upload_pending_measurements"
        const val CALL_UPLOAD_NAME = "upload_pending_calls"
        const val CALL_UPLOAD_KICKOFF_NAME = "upload_pending_calls_kickoff"
        const val MAINTENANCE_NAME = "maintenance_cleanup"
        const val RESCHEDULE_NAME = "reschedule_background_work"
        private const val MIN_PERIODIC_MINUTES = 20L
        private const val UPLOAD_PERIODIC_MINUTES = 60L
        private const val UPLOAD_FLEX_MINUTES = 15L
        private const val MIN_FLEX_MINUTES = 5L
        const val TAG_RESCHEDULE = "reschedule"
        const val TAG_AUTORUN = "autorun"
        const val TAG_UPLOAD = "upload"
        const val TAG_CALL_UPLOAD = "call_upload"
        const val TAG_MAINTENANCE = "maintenance"
        private val ACTIVE_STATES = setOf(
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.RUNNING,
            WorkInfo.State.BLOCKED
        )
    }

    fun observeAutoRunWorkInfo() =
        workManager.getWorkInfosForUniqueWorkFlow(AUTO_RUN_NAME).map { it.firstOrNull() }

    fun observeUploadWorkInfo() =
        workManager.getWorkInfosForUniqueWorkFlow(UPLOAD_NAME).map { it.firstOrNull() }

    fun scheduleMaintenanceDaily() {
        val req = PeriodicWorkRequestBuilder<MaintenanceWorker>(
            24,
            TimeUnit.HOURS,
            6,
            TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(TAG_MAINTENANCE)
            .build()

        workManager.enqueueUniquePeriodicWork(
            MAINTENANCE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }

    /**
     * Single source of truth:
     * - schedules maintenance always
     * - schedules periodic auto-run only when allowed
     * - optionally enqueues an immediate one-time kickoff to match user expectation
     */
    suspend fun rescheduleFromSettings(
        settings: AppSettings,
        kickoffOnceIfAllowed: Boolean
    ) {
        scheduleMaintenanceDaily()

        if (settings.firestoreUploadsEnabled) {
            scheduleUploadPeriodic()
            scheduleCallUploadPeriodic()
        } else {
            cancelUpload()
            cancelCallUpload()
        }

        val allowed = settings.autoRunEnabled
        if (!allowed) {
            cancelAutoRun()
            return
        }

        scheduleAutoRun(
            intervalMinutes = settings.autoRunIntervalMinutes.toLong(),
            wifiOnly = settings.collectOnlyWifi
        )

        if (kickoffOnceIfAllowed) {
            kickoffAutoRunOnce(settings.collectOnlyWifi)
        }
    }

    suspend fun scheduleAutoRun(intervalMinutes: Long, wifiOnly: Boolean = false) {
        val safeMinutes = intervalMinutes.coerceAtLeast(MIN_PERIODIC_MINUTES)
        val safeFlexMinutes = (safeMinutes / 3L).coerceAtLeast(MIN_FLEX_MINUTES)

        val last = statusStore.autoRunStatus.first()
        val scheduleUnchanged =
            last.lastScheduleMinutes == safeMinutes.toInt() &&
                    last.lastScheduleWifiOnly == wifiOnly
        if (scheduleUnchanged && isWorkActive(AUTO_RUN_NAME)) return

        val req = PeriodicWorkRequestBuilder<AutoRunWorker>(
            safeMinutes,
            TimeUnit.MINUTES,
            safeFlexMinutes,
            TimeUnit.MINUTES
        )
            .setConstraints(autoRunConstraints(wifiOnly = wifiOnly, requireBatteryNotLow = true))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .setInputData(workDataOf(AutoRunWorker.KEY_TRIGGER_SOURCE to AutoRunWorker.TRIGGER_PERIODIC))
            .addTag(TAG_AUTORUN)
            .build()

        workManager.enqueueUniquePeriodicWork(
            AUTO_RUN_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )

        statusStore.rememberSchedule(safeMinutes.toInt(), wifiOnly)
    }

    fun kickoffAutoRunOnce(wifiOnly: Boolean = false) {
        val req = OneTimeWorkRequestBuilder<AutoRunWorker>()
            .setConstraints(autoRunConstraints(wifiOnly = wifiOnly))
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(AutoRunWorker.KEY_TRIGGER_SOURCE to AutoRunWorker.TRIGGER_KICKOFF))
            .addTag("${TAG_AUTORUN}_kickoff")
            .build()

        workManager.enqueueUniqueWork(
            AUTO_RUN_KICKOFF_NAME,
            ExistingWorkPolicy.KEEP,
            req
        )
    }

    fun cancelAutoRun() {
        workManager.cancelUniqueWork(AUTO_RUN_NAME)
        workManager.cancelUniqueWork(AUTO_RUN_KICKOFF_NAME)
    }

    fun scheduleUploadPeriodic() {
        val req = PeriodicWorkRequestBuilder<UploadWorker>(
            UPLOAD_PERIODIC_MINUTES,
            TimeUnit.MINUTES,
            UPLOAD_FLEX_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(uploadConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .setInputData(workDataOf(UploadWorker.KEY_TRIGGER_SOURCE to UploadWorker.TRIGGER_PERIODIC))
            .addTag(TAG_UPLOAD)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UPLOAD_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }

    fun cancelUpload() {
        workManager.cancelUniqueWork(UPLOAD_NAME)
    }

    fun scheduleCallUploadPeriodic() {
        val req = PeriodicWorkRequestBuilder<CallUploadWorker>(
            UPLOAD_PERIODIC_MINUTES,
            TimeUnit.MINUTES,
            UPLOAD_FLEX_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(uploadConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(CallUploadWorker.KEY_TRIGGER_SOURCE to CallUploadWorker.TRIGGER_PERIODIC)
            )
            .addTag(TAG_CALL_UPLOAD)
            .build()

        workManager.enqueueUniquePeriodicWork(
            CALL_UPLOAD_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }

    fun kickoffCallUploadOnce() {
        val req = OneTimeWorkRequestBuilder<CallUploadWorker>()
            .setConstraints(uploadConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    CallUploadWorker.KEY_TRIGGER_SOURCE to CallUploadWorker.TRIGGER_CALL_ENDED
                )
            )
            .addTag("${TAG_CALL_UPLOAD}_kickoff")
            .build()

        workManager.enqueueUniqueWork(
            CALL_UPLOAD_KICKOFF_NAME,
            ExistingWorkPolicy.KEEP,
            req
        )
    }

    fun cancelCallUpload() {
        workManager.cancelUniqueWork(CALL_UPLOAD_NAME)
        workManager.cancelUniqueWork(CALL_UPLOAD_KICKOFF_NAME)
    }

    fun enqueueRescheduleWorker() {
        val req = OneTimeWorkRequestBuilder<WorkRescheduleWorker>()
            .setConstraints(Constraints.NONE)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    WorkRescheduleWorker.KEY_TRIGGER_SOURCE to WorkRescheduleWorker.TRIGGER_APP_START
                )
            )
            .addTag(TAG_RESCHEDULE)
            .build()

        workManager.enqueueUniqueWork(
            RESCHEDULE_NAME,
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun runAutoRunOnceNowDebug(ignoreConstraints: Boolean = true) {
        val constraints = if (ignoreConstraints) Constraints.NONE
        else Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val req = OneTimeWorkRequestBuilder<AutoRunWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
            .setInputData(workDataOf(AutoRunWorker.KEY_TRIGGER_SOURCE to AutoRunWorker.TRIGGER_DEBUG))
            .addTag("${TAG_AUTORUN}_debug_once")
            .build()

        workManager.enqueueUniqueWork(
            AUTO_RUN_DEBUG_ONCE_NAME,
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    private suspend fun isWorkActive(uniqueName: String): Boolean {
        val workInfo = workManager.getWorkInfosForUniqueWorkFlow(uniqueName)
            .first()
            .firstOrNull()
        return workInfo?.state in ACTIVE_STATES
    }

    private fun autoRunConstraints(
        wifiOnly: Boolean = false,
        requireBatteryNotLow: Boolean = false
    ): Constraints {
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        return Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(requireBatteryNotLow)
            .build()
    }

    private fun uploadConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

}
