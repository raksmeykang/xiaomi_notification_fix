package com.notificationfix

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var statusNotification: TextView
    private lateinit var statusBattery: TextView
    private lateinit var statusService: TextView
    private lateinit var switchNotification: SwitchMaterial
    private lateinit var switchBattery: SwitchMaterial
    private lateinit var switchService: SwitchMaterial
    private lateinit var btnNotification: Button
    private lateinit var btnBattery: Button
    private lateinit var btnBatterySaver: Button
    private lateinit var btnDevOptions: Button
    private lateinit var btnStartService: Button
    private lateinit var btnRefresh: Button
    private lateinit var textDeviceInfo: TextView
    private lateinit var appButtonsContainer: LinearLayout

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateNotificationStatus(granted)
    }

    private val batteryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateAllStatuses() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setClickListeners()
        populateAppButtons()
        textDeviceInfo.text = XiaomiUtils.getDeviceInfo()
        updateAllStatuses()
    }

    override fun onResume() {
        super.onResume()
        updateAllStatuses()
    }

    private fun initViews() {
        statusNotification = findViewById(R.id.status_notification)
        statusBattery = findViewById(R.id.status_battery)
        statusService = findViewById(R.id.status_service)
        switchNotification = findViewById(R.id.switch_notification)
        switchBattery = findViewById(R.id.switch_battery)
        switchService = findViewById(R.id.switch_service)
        btnNotification = findViewById(R.id.btn_notification)
        btnBattery = findViewById(R.id.btn_battery)
        btnBatterySaver = findViewById(R.id.btn_battery_saver)
        btnDevOptions = findViewById(R.id.btn_dev_options)
        btnStartService = findViewById(R.id.btn_start_service)
        btnRefresh = findViewById(R.id.btn_refresh)
        textDeviceInfo = findViewById(R.id.text_device_info)
        appButtonsContainer = findViewById(R.id.app_buttons_container)
    }

    private fun setClickListeners() {
        btnNotification.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (PermissionHelper.hasNotificationPermission(this)) {
                    XiaomiUtils.openNotificationSettings(this, packageName)
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                XiaomiUtils.openNotificationSettings(this, packageName)
            }
        }
        btnBattery.setOnClickListener {
            XiaomiUtils.openBatteryOptimizationSettings(this, packageName)
        }
        btnBatterySaver.setOnClickListener { XiaomiUtils.openBatterySaverSettings(this) }
        btnDevOptions.setOnClickListener { XiaomiUtils.openDeveloperOptions(this) }
        btnStartService.setOnClickListener { toggleService() }
        btnRefresh.setOnClickListener { updateAllStatuses() }
    }

    private fun populateAppButtons() {
        val inflater = layoutInflater
        for (app in XiaomiUtils.COMMON_APPS) {
            val row = inflater.inflate(R.layout.item_app_config, appButtonsContainer, false)
            val btn = row.findViewById<Button>(R.id.btn_app_config)
            btn.text = app.name
            btn.setOnClickListener {
                XiaomiUtils.openAppSettings(this, app.packageName)
            }
            appButtonsContainer.addView(row)
        }
    }

    private fun updateAllStatuses() {
        updateNotificationStatus(PermissionHelper.hasNotificationPermission(this))
        updateBatteryStatus()
        updateServiceStatus()
    }

    private fun updateNotificationStatus(granted: Boolean) {
        statusNotification.text = if (granted) "Granted" else "Not granted"
        switchNotification.isChecked = granted
    }

    private fun updateBatteryStatus() {
        val exempt = PermissionHelper.hasBatteryExemption(this)
        statusBattery.text = if (exempt) "No restrictions" else "Optimizing battery"
        switchBattery.isChecked = exempt
    }

    private fun updateServiceStatus() {
        val running = isServiceRunning(NotificationFixService::class.java)
        statusService.text = if (running) "Running + alarms active" else "Stopped"
        switchService.isChecked = running
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        return try {
            val manager = getSystemService(android.app.ActivityManager::class.java)
            @Suppress("DEPRECATION")
            manager.getRunningServices(Integer.MAX_VALUE)
                .any { serviceClass.name == it.service.className }
        } catch (_: Exception) {
            false
        }
    }

    private fun toggleService() {
        val intent = Intent(this, NotificationFixService::class.java)
        if (isServiceRunning(NotificationFixService::class.java)) {
            stopService(intent)
            KeepAliveWorker.cancel(this)
            AlarmReceiver.cancelAlarm(this)
            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !PermissionHelper.hasNotificationPermission(this)
            ) {
                Toast.makeText(this, "Grant notification permission first", Toast.LENGTH_SHORT).show()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
            startForegroundService(intent)
            KeepAliveWorker.schedule(this)
            AlarmReceiver.scheduleAlarm(this)
            Toast.makeText(this, "Service started — notifications should arrive now", Toast.LENGTH_LONG).show()
        }
        lifecycleScope.launch {
            delay(500)
            updateServiceStatus()
        }
    }
}
