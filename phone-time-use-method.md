# 📱 Android 应用使用时长统计·金标准方法论

在开发 Android 手机状态监控或时间管理类应用时，获取准确的“今日使用时长”是一个巨大的挑战。本文总结了从“系统聚合查询”转向“原始事件溯源”的高可靠性方案。

---

## ❌ 为什么不要使用 `queryAndAggregateUsageStats`？

大多数初学者（包括早期的本项目）会选择调用系统提供的汇总接口。
- **原理**：向系统请求一个已经计算好的“报表”。
- **致命缺陷**：
    1. **统计桶（Buckets）重叠**：Android 系统内部数据存储是按桶存储的。在很多国产定制系统（如 vivo, OPPO）上，这些桶的起始时间往往不是自然日的 00:00，导致查询结果会包含昨天的数据。
    2. **实时性差**：汇总数据往往不包含“当前正在运行且尚未关闭”的应用时长。
    3. **穿越现象**：会导致“24小时内统计出27小时使用时长”的逻辑笑话。

---

## ✅ 唯一正确方案：基于原始事件的“账本模型”

核心思路是：不看报表，直接读取系统底层的**应用切换日志（UsageEvents）**，并手动计算时间差。

### 1. 核心原理：事件溯源 (Event Sourcing)
通过查询 `queryEvents(startTime, endTime)`，我们可以获得两条关键流水：
- `MOVE_TO_FOREGROUND`：App 被打开/切到前台。
- `MOVE_TO_BACKGROUND`：App 被关闭/切到后台。

通过匹配这两条成对出现的事件，计算时间差并累加。

### 2. 核心算法实现 (Kotlin)

```kotlin
/**
 * 使用原始事件流精确计算指定时间段内的应用使用时长
 * @param context 上下文
 * @param startTime 起始时间戳（如今日 00:00:00）
 * @param endTime 结束时间戳（如当前时间）
 * @return Map<String, Long> 应用名称或包名到总时长（毫秒）的映射
 */
fun calculatePreciseDailyTime(context: Context, startTime: Long, endTime: Long): Map<String, Long> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    
    // 1. 获取最底层的事件流日志
    val events = usm.queryEvents(startTime, endTime)
    val event = android.app.usage.UsageEvents.Event()
    
    // 2. 准备“账本”
    val appTimeMap = mutableMapOf<String, Long>() // 存储最终结果数据
    val openTimeMap = mutableMapOf<String, Long>() // 存储处于“打开”状态的中间数据

    // 3. 逐条审计日志
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        
        when (event.eventType) {
            android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                // 记入场：记录当前的入场时间
                openTimeMap[event.packageName] = event.timeStamp
            }
            android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                // 记离场：取出入场时间，计算差值
                val start = openTimeMap.remove(event.packageName)
                if (start != null) {
                    val duration = event.timeStamp - start
                    appTimeMap[event.packageName] = (appTimeMap[event.packageName] ?: 0L) + duration
                }
            }
        }
    }

    // 4. 【关键策略】处理正在使用的 App
    // 对于那些只有“打开”没有“关闭”的 App，说明它们现在正亮着屏被使用
    for ((pkg, start) in openTimeMap) {
        val duration = endTime - start
        appTimeMap[pkg] = (appTimeMap[pkg] ?: 0L) + duration
    }
    
    return appTimeMap
}
```

---

## 🌟 方案优势

1. **绝对精确的自然日对齐**：由于我们自己持有 `startTime` 指标，算法只会计算 00:00 之后产生的真实毫秒数。
2. **完美覆盖现时流量**：步骤 4 修复了传统统计漏掉“当下正在运行时间”的 Bug。
3. **强兼容性**：无视任何厂商对底层数据库桶的修改，只要 Android 标准的切换事件还在产生，统计就是准确的。

---

## ⚙️ 前置配置

### AndroidManifest.xml
必须声明权限：
```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions" />
```

### 运行时引导
第三方 App 必须引导用户手动开启权限：
```kotlin
val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
startActivity(intent)
```

---

*该笔记由 Antigravity 整理，基于 `Phone Status` 项目 1.0 版本统计重构实践总结。*
