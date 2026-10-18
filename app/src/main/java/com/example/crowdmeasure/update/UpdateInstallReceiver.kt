package com.example.crowdmeasure.update

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UpdateInstallReceiver : BroadcastReceiver() {
    @Inject lateinit var installEvents: UpdateInstallEvents
    @Inject lateinit var notifier: UpdateNotifier
    @Inject lateinit var analytics: UpdateAnalytics

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                } ?: return

                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Background activity launches are blocked, so fall back to a notification the user can tap.
                if (isAppInForeground()) {
                    context.startActivity(confirmation)
                } else {
                    notifier.notifyInstallConfirmationNeeded(confirmation)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> Unit

            else -> {
                val error = UpdateInstallFailedException(
                    status,
                    intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                )
                analytics.failed("commit", null, error)
                installEvents.reportFailure(error.message.orEmpty())
            }
        }
    }

    private fun isAppInForeground(): Boolean {
        val state = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(state)
        return state.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }
}
