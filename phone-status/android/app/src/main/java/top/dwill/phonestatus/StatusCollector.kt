package top.dwill.phonestatus

import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import kotlin.coroutines.resume

object StatusCollector {

    // 步数计数器
    var todaySteps: Int = 0
    private var initialSteps: Int = -1

    // 游戏包名列表
    private val gamePackages = setOf(
        "com.tencent.tmgp.sgame", "com.tencent.tmgp.pubgmhd", "com.tencent.ig",
        "com.miHoYo.Yuanshen", "com.miHoYo.hkrpg", "com.miHoYo.zzz",
        "com.netease.onmyoji", "com.netease.dwrg", "com.YoStarJP.BlueArchive",
        "com.nexon.bluearchive", "com.tencent.lolm"
    )

    // 工作/学习App
    private val workPackages = setOf(
        "com.microsoft.office.word", "com.microsoft.office.excel", "com.microsoft.office.powerpoint",
        "com.tencent.wework", "com.alibaba.android.rimet", "com.ss.android.lark",
        "com.github.android", "com.csdn.csdnplus"
    )

    private val appNameMap = mapOf(
        "com.tencent.mm" to "微信",
        "com.tencent.mobileqq" to "QQ",
        "com.sina.weibo" to "微博",
        "com.zhihu.android" to "知乎",
        "com.xingin.xhs" to "小红书",
        "org.telegram.messenger" to "Telegram",
        "com.discord" to "Discord",
        "com.tencent.tmgp.sgame" to "王者荣耀",
        "com.tencent.tmgp.pubgmhd" to "和平精英",
        "com.miHoYo.Yuanshen" to "原神",
        "com.miHoYo.hkrpg" to "崩坏：星穹铁道",
        "tv.danmaku.bili" to "哔哩哔哩",
        "com.ss.android.ugc.aweme" to "抖音",
        "com.netease.cloudmusic" to "网易云音乐",
        "com.tencent.qqmusic" to "QQ音乐",
        "com.microsoft.emmx" to "Edge",
        "com.android.chrome" to "Chrome",
        "com.quark.browser" to "夸克",
        "com.taobao.taobao" to "淘宝",
        "com.taobao.idlefish" to "闲鱼",
        "com.jingdong.app.mall" to "京东",
        "com.xunmeng.pinduoduo" to "拼多多",
        "com.sankuai.meituan" to "美团",
        "com.eg.android.AlipayGphone" to "支付宝",
        "com.android.settings" to "设置",
        "com.android.vending" to "Play商店",
        "com.bbk.launcher2" to "桌面",
        "com.vivo.launcher" to "桌面",
        "top.dwill.phonestatus" to "状态同步"
    )

    fun initStepCounter(context: Context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sm.registerListener(object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val totalSteps = event.values[0].toInt()
                    if (initialSteps < 0) initialSteps = totalSteps
                    todaySteps = totalSteps - initialSteps
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun getCurrentApp(context: Context): String {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 60000
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        if (stats.isNullOrEmpty()) return "unknown"
        val recentApp = stats.maxByOrNull { it.lastTimeUsed }
        return recentApp?.packageName ?: "unknown"
    }

    fun getAppDisplayName(packageName: String): String {
        return appNameMap[packageName] ?: packageName.substringAfterLast(".")
    }

    // 获取今日屏幕使用时长（分钟）
    fun getTodayScreenTime(context: Context): Int {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        if (stats.isNullOrEmpty()) return 0
        
        var totalTime = 0L
        for (stat in stats) {
            totalTime += stat.totalTimeInForeground
        }
        return (totalTime / 60000).toInt()
    }

    // 获取今日游戏时长（分钟）
    fun getTodayGameTime(context: Context): Int {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        if (stats.isNullOrEmpty()) return 0
        
        var gameTime = 0L
        for (stat in stats) {
            if (gamePackages.contains(stat.packageName)) {
                gameTime += stat.totalTimeInForeground
            }
        }
        return (gameTime / 60000).toInt()
    }

    // 获取今日Top3应用
    fun getTodayTopApps(context: Context): List<Pair<String, Int>> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        if (stats.isNullOrEmpty()) return emptyList()
        
        val ignoredPackages = setOf("com.bbk.launcher2", "com.vivo.launcher", "com.android.systemui")
        
        // 先按显示名称分组合并时长，避免同一个App多个进程重复显示
        val appTimeMap = mutableMapOf<String, Long>()
        for (stat in stats) {
            if (stat.totalTimeInForeground > 60000 && !ignoredPackages.contains(stat.packageName)) {
                val displayName = getAppDisplayName(stat.packageName)
                appTimeMap[displayName] = (appTimeMap[displayName] ?: 0L) + stat.totalTimeInForeground
            }
        }
        
        return appTimeMap.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { Pair(it.key, (it.value / 60000).toInt()) }
    }

    // 判断当前状态（摸鱼/工作/游戏/休息）
    fun getCurrentMood(currentApp: String): String {
        return when {
            gamePackages.contains(currentApp) -> "gaming"
            workPackages.contains(currentApp) -> "working"
            currentApp.contains("launcher") || currentApp == "unknown" -> "idle"
            currentApp.contains("bili") || currentApp.contains("aweme") || 
            currentApp.contains("weibo") || currentApp.contains("xhs") -> "slacking"
            else -> "normal"
        }
    }

    // 判断是否熬夜（23点-5点使用手机）
    fun isLateNight(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 23 || hour < 5
    }

    fun getBatteryLevel(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun isCharging(context: Context): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.isCharging
    }

    fun getBatteryTemperature(context: Context): Float {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return temp / 10f
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getWifiName(context: Context): String? {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wm.connectionInfo
            info.ssid?.replace("\"", "")?.takeIf { it != "<unknown ssid>" }
        } catch (e: Exception) { null }
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        return try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter?.isEnabled == true
        } catch (e: SecurityException) { false }
    }

    fun isScreenOn(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isInteractive
    }

    fun getNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "无网络"
        val caps = cm.getNetworkCapabilities(network) ?: return "无网络"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "流量"
            else -> "其他"
        }
    }

    fun getVolume(context: Context): Int {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) (current * 100 / max) else 0
    }

    fun getBrightness(context: Context): Int {
        return try {
            val brightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            (brightness * 100 / 255)
        } catch (e: Exception) { -1 }
    }

    fun getStorageInfo(): String {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes / (1024 * 1024 * 1024)
            val free = stat.availableBytes / (1024 * 1024 * 1024)
            "${total - free}/${total}GB"
        } catch (e: Exception) { "" }
    }

    fun getMemoryInfo(context: Context): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val total = memInfo.totalMem / (1024 * 1024 * 1024)
            val free = memInfo.availMem / (1024 * 1024 * 1024)
            "${total - free}/${total}GB"
        } catch (e: Exception) { "" }
    }

    suspend fun getLocation(context: Context): Pair<Double, Double>? {
        return try {
            val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            suspendCancellableCoroutine { cont ->
                try {
                    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                        .addOnSuccessListener { location ->
                            if (location != null) cont.resume(Pair(location.latitude, location.longitude))
                            else cont.resume(null)
                        }
                        .addOnFailureListener { cont.resume(null) }
                } catch (e: SecurityException) { cont.resume(null) }
            }
        } catch (e: Exception) { null }
    }

    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }
}
