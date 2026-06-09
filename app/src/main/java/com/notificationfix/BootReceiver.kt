package com.notificationfix

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            KeepAliveWorker.schedule(context)
            val serviceIntent = Intent(context, NotificationFixService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
