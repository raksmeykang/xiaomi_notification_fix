package com.notificationfix

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object XiaomiUtils {

    data class AppInfo(val name: String, val packageName: String)

    val COMMON_APPS = listOf(
        AppInfo("Telegram", "org.telegram.messenger"),
        AppInfo("Instagram", "com.instagram.android"),
        AppInfo("WhatsApp", "com.whatsapp"),
        AppInfo("Facebook", "com.facebook.katana"),
        AppInfo("Messenger", "com.facebook.orca"),
        AppInfo("Twitter / X", "com.twitter.android"),
        AppInfo("Snapchat", "com.snapchat.android"),
        AppInfo("Discord", "com.discord"),
        AppInfo("Signal", "org.thoughtcrime.securesms"),
        AppInfo("Viber", "com.viber.voip"),
        AppInfo("Line", "jp.naver.line.android"),
        AppInfo("WeChat", "com.tencent.mm"),
        AppInfo("Skype", "com.skype.raider"),
        AppInfo("Slack", "com.Slack"),
        AppInfo("Telegram X", "org.thunderdog.challegram"),
    )

    fun isXiaomi(): Boolean {
        return listOf("Xiaomi", "Redmi", "POCO").any {
            Build.MANUFACTURER.equals(it, ignoreCase = true) ||
                    Build.BRAND.equals(it, ignoreCase = true)
        }
    }

    fun isHyperOS(): Boolean {
        return getMIUIVersion().contains("HyperOS", ignoreCase = true)
    }

    fun getMIUIVersion(): String {
        return try {
            val cls = Class.forName("android.os.SystemProperties")
            val get = cls.getDeclaredMethod("get", String::class.java)
            get.invoke(null, "ro.miui.ui.version.name") as? String ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun openAppSettings(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openAutostartSettings(context: Context, packageName: String) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings(context, packageName)
        }
    }

    fun openBatteryOptimizationSettings(context: Context, packageName: String) {
        try {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings(context, packageName)
        }
    }

    fun openNotificationSettings(context: Context, packageName: String) {
        try {
            val intent = Intent().apply {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("android.provider.extra.APP_PACKAGE", packageName)
                putExtra("app_package", packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings(context, packageName)
        }
    }

    fun openDeveloperOptions(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openBatterySaverSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun getDeviceInfo(): String {
        val device = "${Build.MANUFACTURER} ${Build.MODEL}"
        val os = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val miui = getMIUIVersion()
        return buildString {
            append(device)
            append("\n$os")
            if (miui.isNotEmpty()) append("\n$miui")
        }
    }
}
