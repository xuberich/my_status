package top.dwill.phonestatus

import android.Manifest
import android.app.AppOpsManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var etApiUrl: EditText
    private lateinit var etApiKey: EditText
    private lateinit var etInterval: EditText
    private lateinit var etStatusMessage: EditText
    private lateinit var btnSave: CardView
    private lateinit var btnStart: CardView
    private lateinit var btnStop: CardView
    private lateinit var btnPermissions: CardView
    private lateinit var statusDot: View
    private lateinit var tvServiceStatus: TextView
    private lateinit var tvLastUpload: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        etApiUrl = findViewById(R.id.etApiUrl)
        etApiKey = findViewById(R.id.etApiKey)
        etInterval = findViewById(R.id.etInterval)
        etStatusMessage = findViewById(R.id.etStatusMessage)
        btnSave = findViewById(R.id.btnSave)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnPermissions = findViewById(R.id.btnPermissions)
        statusDot = findViewById(R.id.statusDot)
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        tvLastUpload = findViewById(R.id.tvLastUpload)

        loadConfig()
        updateStatus()

        btnSave.setOnClickListener { saveConfig() }
        btnStart.setOnClickListener { startService() }
        btnStop.setOnClickListener { stopService() }
        btnPermissions.setOnClickListener { requestAllPermissions() }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private fun loadConfig() {
        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        etApiUrl.setText(prefs.getString("api_url", "https://phone-status-api-v2.zhangtao3788.workers.dev/update"))
        etApiKey.setText(prefs.getString("api_key", "dwill-status-2026-x7k9m2p4q8r1"))
        etInterval.setText(prefs.getInt("interval", 60).toString())
        etStatusMessage.setText(prefs.getString("status_message", ""))
    }

    private fun saveConfig() {
        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        prefs.edit().apply {
            putString("api_url", etApiUrl.text.toString())
            putString("api_key", etApiKey.text.toString())
            putInt("interval", etInterval.text.toString().toIntOrNull() ?: 60)
            putString("status_message", etStatusMessage.text.toString())
            apply()
        }
        Toast.makeText(this, "✓ 配置已保存", Toast.LENGTH_SHORT).show()
    }

    private fun startService() {
        saveConfig()
        val intent = Intent(this, StatusService::class.java)
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "✓ 服务已启动", Toast.LENGTH_SHORT).show()
        updateStatus()
    }

    private fun stopService() {
        val intent = Intent(this, StatusService::class.java)
        stopService(intent)
        Toast.makeText(this, "✓ 服务已停止", Toast.LENGTH_SHORT).show()
        updateStatus()
    }

    private fun updateStatus() {
        // 更新状态指示点和服务状态文本
        if (StatusService.isRunning) {
            statusDot.setBackgroundResource(R.drawable.status_dot_online)
            tvServiceStatus.text = "服务运行中"
            tvServiceStatus.setTextColor(0xFF10B981.toInt())
            tvLastUpload.text = StatusService.lastUploadResult.ifEmpty { "等待上报..." }
        } else {
            statusDot.setBackgroundResource(R.drawable.status_dot_offline)
            tvServiceStatus.text = "服务未运行"
            tvServiceStatus.setTextColor(0xFF6B7280.toInt())
            tvLastUpload.text = "点击上方按钮启动服务"
        }

        // 更新实时数据
        val sb = StringBuilder()
        
        // 权限状态
        sb.appendLine("📋 权限状态")
        sb.appendLine("├─ 位置权限: ${if (checkLocationPermission()) "✓" else "✗"}")
        sb.appendLine("├─ 使用情况: ${if (checkUsageStatsPermission()) "✓" else "✗"}")
        sb.appendLine("├─ 通知监听: ${if (checkNotificationPermission()) "✓" else "✗"}")
        sb.appendLine("└─ 蓝牙权限: ${if (checkBluetoothPermission()) "✓" else "✗"}")
        sb.appendLine()
        
        // 实时数据
        sb.appendLine("📊 实时数据")
        sb.appendLine("├─ 电量: ${getBatteryLevel()}% ${if (isCharging()) "⚡充电中" else ""}")
        sb.appendLine("├─ 电池温度: ${getBatteryTemperature()}°C")
        sb.appendLine("├─ WiFi: ${if (isWifiConnected()) "✓ 已连接" else "✗ 未连接"}")
        sb.appendLine("├─ 蓝牙: ${if (isBluetoothEnabled()) "✓ 已开启" else "✗ 已关闭"}")
        sb.appendLine("└─ 当前应用: ${StatusCollector.getCurrentApp(this)}")
        
        tvStatus.text = sb.toString()
    }

    private fun requestAllPermissions() {
        // 位置权限
        if (!checkLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
            )
        }

        // 蓝牙权限 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !checkBluetoothPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                101
            )
        }

        // 通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    102
                )
            }
        }

        // 身体活动权限 (Android 10+) - 用于步数
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    103
                )
            }
        }

        // 使用情况访问权限
        if (!checkUsageStatsPermission()) {
            Toast.makeText(this, "请授予使用情况访问权限", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // 通知监听权限
        if (!checkNotificationPermission()) {
            Toast.makeText(this, "请授予通知访问权限（用于获取音乐播放状态）", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == 
            PackageManager.PERMISSION_GRANTED
    }

    private fun checkBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == 
                PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun checkNotificationPermission(): Boolean {
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return listeners?.contains(packageName) == true
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isCharging(): Boolean {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.isCharging
    }

    private fun getBatteryTemperature(): Float {
        val intent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return temp / 10f
    }

    private fun isWifiConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isBluetoothEnabled(): Boolean {
        return try {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter?.isEnabled == true
        } catch (e: SecurityException) {
            false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateStatus()
    }
}
