package com.notificationfix

import android.content.Context
import android.os.PowerManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class KeepAliveWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        private const val WORK_NAME = "notification_fix_keep_alive"
        private const val PERIODIC_TAG = "notification_fix_periodic"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build()

            val work = PeriodicWorkRequestBuilder<KeepAliveWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints)
                .addTag(PERIODIC_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                work
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun enqueueOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build()

            val work = OneTimeWorkRequestBuilder<KeepAliveWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(work)
        }
    }

    override fun doWork(): Result {
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakelock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NotificationFix:WorkWakeLock"
        )
        wakelock.acquire(3000)
        try {
            Thread.sleep(1000)
        } catch (_: InterruptedException) {
        } finally {
            wakelock.release()
        }
        return Result.success()
    }
}
