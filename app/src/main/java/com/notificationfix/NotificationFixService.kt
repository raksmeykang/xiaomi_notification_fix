package com.notificationfix

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class NotificationFixService : Service() {

    companion object {
        private const val CHANNEL_ID = "notification_fix_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        KeepAliveWorker.schedule(this)
        AlarmReceiver.scheduleAlarm(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        KeepAliveWorker.enqueueOneTime(this)
        return START_STICKY
    }

    override fun onDestroy() {
        KeepAliveWorker.cancel(this)
        AlarmReceiver.cancelAlarm(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Notification Fix",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Prevents device deep doze for notification delivery"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Notification Fix Active")
            .setContentText("Keeping device awake for notifications")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
