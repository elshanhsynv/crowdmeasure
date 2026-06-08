package com.example.crowdmeasure.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class BootAndUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val req = OneTimeWorkRequestBuilder<WorkRescheduleWorker>()
            .setConstraints(Constraints.NONE)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    WorkRescheduleWorker.KEY_TRIGGER_SOURCE to
                        WorkRescheduleWorker.TRIGGER_BOOT_RECEIVER
                )
            )
            .addTag(WorkScheduler.TAG_RESCHEDULE)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WorkScheduler.RESCHEDULE_NAME,
            ExistingWorkPolicy.KEEP,
            req
        )

    }
}
