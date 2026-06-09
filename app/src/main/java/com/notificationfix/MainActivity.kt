package com.notificationfix

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var statusAutostart: TextView
    private lateinit var statusBattery: TextView
    private lateinit var statusNotification: TextView
    private lateinit var statusOverlay: TextView
    private lateinit var statusService: TextView
    private lateinit var switchAutostart: SwitchMaterial
    private lateinit var switchBattery: SwitchMaterial
    private lateinit var switchNotification: SwitchMaterial
    private lateinit var switchOverlay: SwitchMaterial
    private lateinit var switchService: SwitchMaterial
    private lateinit var btnAutostart: Button
    private lateinit var btnBattery: Button
    private lateinit var btnNotification: Button
    private lateinit var btnOverlay: Button
    private lateinit var btnBatterySaver: Button
    private lateinit var btnStartService: Button
    private lateinit var btnRefresh: Button
    private lateinit var textDeviceInfo: TextView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateNotificationStatus(granted)
    }

    private val batteryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateAllStatuses() }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateAllStatuses() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setClickListeners()
        updateDeviceInfo()
        updateAllStatuses()
    }

    override fun onResume() {
        super.onResume()
        updateAllStatuses()
    }

    private fun initViews() {
        statusAutostart = findViewById(R.id.status_autostart)
        statusBattery = findViewById(R.id.status_battery)
        statusNotification = findViewById(R.id.status_notification)
        statusOverlay = findViewById(R.id.status_overlay)
        statusService = findViewById(R.id.status_service)
        switchAutostart = findViewById(R.id.switch_autostart)
        switchBattery = findViewById(R.id.switch_battery)
        switchNotification = findViewById(R.id.switch_notification)
        switchOverlay = findViewById(R.id.switch_overlay)
        switchService = findViewById(R.id.switch_service)
        btnAutostart = findViewById(R.id.btn_autostart)
        btnBattery = findViewById(R.id.btn_battery)
        btnNotification = findViewById(R.id.btn_notification)
        btnOverlay = findViewById(R.id.btn_overlay)
        btnBatterySaver = findViewById(R.id.btn_battery_saver)
        btnStartService = findViewById(R.id.btn_start_service)
        btnRefresh = findViewById(R.id.btn_refresh)
        textDeviceInfo = findViewById(R.id.text_device_info)
    }

    private fun setClickListeners() {
        btnAutostart.setOnClickListener {
            XiaomiUtils.openAutostartSettings(this)
            lifecycleScope.launch {
                delay(1500)
                updateAllStatuses()
            }
        }
        btnBattery.setOnClickListener {
            XiaomiUtils.openBatteryOptimizationSettings(this)
        }
        btnNotification.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (PermissionHelper.hasNotificationPermission(this)) {
                    XiaomiUtils.openNotificationSettings(this)
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                XiaomiUtils.openNotificationSettings(this)
            }
        }
        btnOverlay.setOnClickListener {
            XiaomiUtils.openOverlayPermissionSettings(this)
        }
        btnBatterySaver.setOnClickListener {
            XiaomiUtils.openBatterySaverSettings(this)
        }
        btnStartService.setOnClickListener {
            toggleService()
        }
        btnRefresh.setOnClickListener { updateAllStatuses() }
    }

    private fun updateDeviceInfo() {
        val device = "${Build.MANUFACTURER} ${Build.MODEL}"
        val os = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val miui = XiaomiUtils.getMIUIVersion()
        val info = buildString {
            append(device)
            append("\n$os")
            if (miui.isNotEmpty()) append("\n$miui")
        }
        textDeviceInfo.text = info
    }

    private fun updateAllStatuses() {
        updateAutostartStatus()
        updateBatteryStatus()
        updateNotificationStatus(PermissionHelper.hasNotificationPermission(this))
        updateOverlayStatus()
        updateServiceStatus()
    }

    private fun updateAutostartStatus() {
        val enabled = XiaomiUtils.isXiaomi()
        statusAutostart.text = if (enabled) {
            "Open autostart settings (check manually)"
        } else {
            "Not required on this device"
        }
        switchAutostart.isChecked = enabled
    }

    private fun updateBatteryStatus() {
        val exempt = PermissionHelper.hasBatteryExemption(this)
        statusBattery.text = if (exempt) "Battery optimization disabled" else "Battery optimization active"
        switchBattery.isChecked = exempt
    }

    private fun updateNotificationStatus(granted: Boolean) {
        statusNotification.text = if (granted) "Notification permission granted" else "Notification permission not granted"
        switchNotification.isChecked = granted
    }

    private fun updateOverlayStatus() {
        val granted = PermissionHelper.hasOverlayPermission(this)
        statusOverlay.text = if (granted) "Overlay permission granted" else "Overlay permission not granted"
        switchOverlay.isChecked = granted
    }

    private fun updateServiceStatus() {
        val running = isServiceRunning(NotificationFixService::class.java)
        statusService.text = if (running) "Background service running" else "Background service stopped"
        switchService.isChecked = running
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(android.app.ActivityManager::class.java)
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) return true
        }
        return false
    }

    private fun toggleService() {
        val intent = Intent(this, NotificationFixService::class.java)
        if (isServiceRunning(NotificationFixService::class.java)) {
            stopService(intent)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !PermissionHelper.hasNotificationPermission(this)
            ) {
                Toast.makeText(this, "Please grant notification permission first", Toast.LENGTH_SHORT).show()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
            startForegroundService(intent)
            Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show()
        }
        lifecycleScope.launch {
            delay(500)
            updateServiceStatus()
        }
    }
}
