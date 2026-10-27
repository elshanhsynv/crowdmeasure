package com.example.crowdmeasure.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.crowdmeasure.MainActivity
import com.example.crowdmeasure.R
import com.example.crowdmeasure.data.prefs.ContinuousSessionPrefs
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.presentation.util.AppLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Runs a continuous collection session: measurements repeat until the end time passes, the user
 * stops it, consent is withdrawn, or measurements keep failing. By default the next measurement
 * starts as soon as the previous one finishes; a non-zero gap pauses between them.
 *
 * Runs as expedited work and promotes itself to a location foreground service so collection
 * continues while the app is in the background.
 */
@HiltWorker
class ContinuousSessionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val measurements: MeasurementRepository,
    private val session: UserSessionRepository,
    private val prefs: ContinuousSessionPrefs,
) : CoroutineWorker(appContext, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo()

    override suspend fun doWork(): Result {
        val endsAtUtcMs = inputData.getLong(KEY_ENDS_AT_UTC_MS, 0L)
        if (endsAtUtcMs <= System.currentTimeMillis()) return Result.success()
        val gapMs = inputData.getInt(KEY_GAP_SECONDS, 0) * 1_000L

        return try {
            setForeground(foregroundInfo())
            collectUntil(endsAtUtcMs, gapMs)
            AppLog.i(TAG, "Continuous session finished.")
            Result.success()
        } catch (e: CancellationException) {
            AppLog.i(TAG, "Continuous session stopped.")
            throw e
        } catch (e: Exception) {
            AppLog.e(TAG, "Continuous session could not run: ${e.message}", e)
            prefs.recordError(e.message ?: "The session could not start.")
            Result.failure()
        }
    }

    private suspend fun collectUntil(endsAtUtcMs: Long, gapMs: Long) {
        var consecutiveFailures = 0
        while (true) {
            val remainingMs = endsAtUtcMs - System.currentTimeMillis()
            if (remainingMs <= 0) return
            if (!session.settings.first().consentGateDismissed) {
                AppLog.i(TAG, "Consent is not accepted; ending the session.")
                return
            }

            // A measurement still running when the timer ends is cancelled rather than saved late.
            val outcome = withTimeoutOrNull(remainingMs) { measurements.runSingleMeasurement() }
                ?: return

            outcome.fold(
                onSuccess = {
                    consecutiveFailures = 0
                    prefs.recordCollected()
                },
                onFailure = { error ->
                    consecutiveFailures++
                    AppLog.e(TAG, "Continuous measurement failed: ${error.message}", error)
                    prefs.recordError(error.message ?: "Measurement failed.")
                },
            )
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                AppLog.e(TAG, "Ending the session after $consecutiveFailures failed measurements in a row.")
                return
            }

            setForeground(foregroundInfo())
            if (gapMs > 0) delay(minOf(gapMs, endsAtUtcMs - System.currentTimeMillis()))
        }
    }

    private suspend fun foregroundInfo(): ForegroundInfo {
        ensureChannel()
        val progress = prefs.progress.first()
        val endsAt = progress.endsAtUtcMs ?: inputData.getLong(KEY_ENDS_AT_UTC_MS, 0L)
        val text = "Ends at ${formatClock(endsAt)} · ${progress.collectedCount} collected"

        val openApp = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.crowdmeasure)
            .setContentTitle("Continuous measurement running")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(
                0,
                "Stop",
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id),
            )
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Continuous collection", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun formatClock(utcMs: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(utcMs))

    companion object {
        const val WORK_NAME = "crowdmeasure.continuous_session"
        const val KEY_ENDS_AT_UTC_MS = "ends_at_utc_ms"
        const val KEY_GAP_SECONDS = "gap_seconds"
        private const val TAG = "ContinuousSession"
        private const val CHANNEL_ID = "continuous_sessions"
        private const val NOTIFICATION_ID = 4001
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }
}
