package io.github.moecax.enable.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import io.github.moecax.enable.R
import io.github.moecax.enable.activities.MainActivity
import io.github.moecax.enable.utils.UpdateChecker
import java.util.concurrent.TimeUnit

/**
 * Periodic (every 2 days) background check against GitHub Releases.
 * Network-constrained, no wake locks, no foreground service — relies on
 * WorkManager/JobScheduler's own Doze-aware batching for battery
 * efficiency. See docs/superpowers/specs/2026-08-27-update-checker-design.md.
 */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "update_check"
        private const val CHANNEL_ID = "update_channel"
        private const val NOTIFICATION_ID = 2001

        /**
         * Enqueues the 2-day periodic check if the feature is enabled on a
         * non-dev build, otherwise cancels it. Safe to call repeatedly
         * (e.g. on every app start, and whenever the settings toggle changes).
         */
        fun scheduleOrCancel(context: Context) {
            val workManager = WorkManager.getInstance(context)
            if (UpdateChecker.isDevBuild() || !UpdateChecker.isEnabled(context)) {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }

            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(2, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override fun doWork(): Result {
        if (UpdateChecker.isDevBuild() || !UpdateChecker.isEnabled(applicationContext)) {
            return Result.success()
        }

        val isNewer = try {
            UpdateChecker.checkNow(applicationContext)
        } catch (e: Exception) {
            // Deliberately no retry: the next periodic run (2 days later)
            // tries again naturally, respecting the rate-limit intent.
            return Result.failure()
        }

        if (isNewer) {
            val tag = UpdateChecker.cachedTag(applicationContext)
            if (tag != null && !UpdateChecker.alreadyNotified(applicationContext, tag)) {
                postNotification(tag)
                UpdateChecker.markNotified(applicationContext, tag)
            }
        }

        return Result.success()
    }

    private fun postNotification(tag: String) {
        val context = applicationContext
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.update_notification_title))
            .setContentText(context.getString(R.string.update_notification_text, tag))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
