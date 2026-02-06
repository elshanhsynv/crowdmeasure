// workers/WorkScheduler.kt
package com.example.crowdmeasure.workers

import androidx.work.*
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
        const val MAINTENANCE_NAME = "maintenance_cleanup"
        const val RESCHEDULE_NAME = "reschedule_background_work"

        private const val MIN_PERIODIC_MINUTES = 15L

        const val TAG_RESCHEDULE = "reschedule"
        const val TAG_AUTORUN = "autorun"
        const val TAG_MAINTENANCE = "maintenance"
    }

    fun observeAutoRunWorkInfo() =
        workManager.getWorkInfosForUniqueWorkFlow(AUTO_RUN_NAME).map { it.firstOrNull() }

    fun observeMaintenanceWorkInfo() =
        workManager.getWorkInfosForUniqueWorkFlow(MAINTENANCE_NAME).map { it.firstOrNull() }

    fun scheduleMaintenanceDaily() {
        val req = PeriodicWorkRequestBuilder<MaintenanceWorker>(24, TimeUnit.HOURS)
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
            ExistingPeriodicWorkPolicy.UPDATE,
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

        val allowed =
            settings.consentAccepted && settings.collectionEnabled && settings.autoRunEnabled
        if (!allowed) {
            cancelAutoRun()
            // also cancel kickoff if previously enqueued
            workManager.cancelUniqueWork(AUTO_RUN_KICKOFF_NAME)
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

    suspend fun scheduleAutoRun(intervalMinutes: Long, wifiOnly: Boolean) {
        val safeMinutes = intervalMinutes.coerceAtLeast(MIN_PERIODIC_MINUTES).toInt()

        val last = statusStore.autoRunStatus.first()
        if (last.lastScheduleMinutes == safeMinutes && last.lastScheduleWifiOnly == wifiOnly) return

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val req = PeriodicWorkRequestBuilder<AutoRunWorker>(safeMinutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(TAG_AUTORUN)
            .build()

        workManager.enqueueUniquePeriodicWork(
            AUTO_RUN_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )

        statusStore.rememberSchedule(safeMinutes, wifiOnly)
    }

    fun kickoffAutoRunOnce(wifiOnly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val req = OneTimeWorkRequestBuilder<AutoRunWorker>()
            .setConstraints(constraints)
            .addTag("${TAG_AUTORUN}_kickoff")
            .build()

        workManager.enqueueUniqueWork(
            "auto_run_measurement_kickoff",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun cancelAutoRun() {
        workManager.cancelUniqueWork(AUTO_RUN_NAME)
    }

    fun enqueueRescheduleWorker() {
        val req = OneTimeWorkRequestBuilder<WorkRescheduleWorker>()
            .setConstraints(Constraints.NONE)
            .addTag(TAG_RESCHEDULE)
            .build()

        workManager.enqueueUniqueWork(
            RESCHEDULE_NAME,
            ExistingWorkPolicy.KEEP,
            req
        )
    }

    fun runAutoRunOnceNowDebug(ignoreConstraints: Boolean = true) {
        val constraints = if (ignoreConstraints) Constraints.NONE
        else Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val req = OneTimeWorkRequestBuilder<AutoRunWorker>()
            .setConstraints(constraints)
            .addTag("${TAG_AUTORUN}_debug_once")
            .build()

        workManager.enqueue(req)
    }
}


