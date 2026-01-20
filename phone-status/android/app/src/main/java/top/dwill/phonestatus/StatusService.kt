package top.dwill.phonestatus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class StatusService : Service() {

    companion object {
        const val CHANNEL_ID = "phone_status_channel"
        const val NOTIFICATION_ID = 1
        var isRunning = false
        var currentMusic: String? = null
        var currentMusicArtist: String? = null
        var lastUploadResult: String = "未上报"
        var statusMessage: String = "" // 状态签名
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private var intervalMs = 60000L

    private val uploadRunnable = object : Runnable {
        override fun run() {
            scope.launch { uploadStatus() }
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        StatusCollector.initStepCounter(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        intervalMs = (prefs.getInt("interval", 60) * 1000).toLong()
        statusMessage = prefs.getString("status_message", "") ?: ""
        startForeground(NOTIFICATION_ID, createNotification())
        handler.post(uploadRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(uploadRunnable)
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "状态同步服务", NotificationManager.IMPORTANCE_LOW)
        channel.description = "保持后台运行以同步手机状态"
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("状态同步运行中")
            .setContentText("正在同步手机状态到网页")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private suspend fun uploadStatus() {
        try {
            val prefs = getSharedPreferences("config", MODE_PRIVATE)
            val apiUrl = prefs.getString("api_url", "") ?: return
            val apiKey = prefs.getString("api_key", "") ?: return
            if (apiUrl.isEmpty() || apiKey.isEmpty()) return

            val packageName = StatusCollector.getCurrentApp(this)
            val topApps = StatusCollector.getTodayTopApps(this)
            
            var lat: Double? = null
            var lon: Double? = null
            try {
                val location = StatusCollector.getLocation(this)
                lat = location?.first
                lon = location?.second
            } catch (e: Exception) {}

            val status = mutableMapOf<String, Any?>(
                "app" to packageName,
                "appName" to StatusCollector.getAppDisplayName(packageName),
                "battery" to StatusCollector.getBatteryLevel(this),
                "charging" to StatusCollector.isCharging(this),
                "batteryTemp" to StatusCollector.getBatteryTemperature(this),
                "wifi" to StatusCollector.isWifiConnected(this),
                "wifiName" to StatusCollector.getWifiName(this),
                "bluetooth" to StatusCollector.isBluetoothEnabled(this),
                "screen" to StatusCollector.isScreenOn(this),
                "networkType" to StatusCollector.getNetworkType(this),
                "device" to StatusCollector.getDeviceName(),
                "music" to currentMusic,
                "musicArtist" to currentMusicArtist,
                "volume" to StatusCollector.getVolume(this),
                "brightness" to StatusCollector.getBrightness(this),
                "storage" to StatusCollector.getStorageInfo(),
                "memory" to StatusCollector.getMemoryInfo(this),
                // 新增趣味数据
                "steps" to StatusCollector.todaySteps,
                "screenTime" to StatusCollector.getTodayScreenTime(this),
                "gameTime" to StatusCollector.getTodayGameTime(this),
                "mood" to StatusCollector.getCurrentMood(packageName),
                "isLateNight" to StatusCollector.isLateNight(),
                "statusMessage" to statusMessage,
                "topApps" to topApps.map { mapOf("name" to it.first, "minutes" to it.second) }
            )

            if (lat != null && lon != null) {
                status["lat"] = lat
                status["lon"] = lon
            }

            val json = gson.toJson(status)
            Log.d("StatusService", "Uploading: $json")

            val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            lastUploadResult = if (response.code == 200) {
                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                "✓ 上报成功 $time"
            } else {
                "✗ 失败(${response.code})"
            }
            response.close()
        } catch (e: Exception) {
            lastUploadResult = "✗ ${e.message?.take(30) ?: "未知错误"}"
            Log.e("StatusService", "Upload failed", e)
        }
    }
}
