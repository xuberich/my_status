# 📱 Phone Status - 实时手机状态监控

一个实时同步手机状态到网页的"视奸"项目，支持查看当前应用、电量、位置、音乐播放等信息。

## 🎯 项目特点

- **实时同步**：手机状态每 60 秒自动上报
- **趣味数据**：今日屏幕时长、游戏时长、步数、心情检测
- **访客统计**：在线人数、今日/总访客数
- **现代 UI**：亮色主题 + 响应式设计
- **隐私保护**：位置仅显示市级

## 📦 项目结构

```text
phone-status/
├── frontend/           # 前端网页（单 HTML 文件）
│   └── index.html     # 亮色主题 + Arctic Frost 配色
├── worker/            # Cloudflare Worker 后端 API
│   ├── src/
│   │   └── index.ts   # TypeScript 源码
│   └── wrangler.toml  # CF 配置
└── android/           # Android 数据采集 App
    └── app/
        ├── src/main/
        │   ├── java/top/dwill/phonestatus/
        │   │   ├── MainActivity.kt          # 主界面
        │   │   ├── StatusService.kt         # 后台服务
        │   │   ├── StatusCollector.kt       # 数据收集
        │   │   └── MusicNotificationListener.kt  # 音乐监听
        │   └── res/
        │       ├── layout/
        │       │   └── activity_main.xml    # 亮色 Material Design UI
        │       └── drawable/                # 按钮/卡片样式
        └── build.gradle.kts
```

## 🚀 部署状态

### 当前部署（Cloudflare）

- **前端**: `https://status.dwill.top` (CF Pages)
- **后端**: `https://phone-status-api-v2.zhangtao3788.workers.dev` (CF Workers)
- **存储**: Cloudflare KV (免费版 1000 次写入/天)

**注意**: 国内访问需要代理/VPN

### 配置信息

```text
Worker URL: https://phone-status-api-v2.zhangtao3788.workers.dev/update
API Key: dwill-status-2026-x7k9m2p4q8r1
KV Namespace ID: a108bc314d6e4935983e7016c2f79c83
Pages Project: phone-status
```

## 📊 功能列表

### 前端展示

- ✅ 当前使用应用（中文名称映射）
- ✅ 在线状态（5 分钟内活跃）
- ✅ 心情检测（游戏中/工作中/摸鱼/夜猫子）
- ✅ 今日屏幕时长
- ✅ 今日游戏时长
- ✅ 今日步数
- ✅ 电量 + 充电状态 + 温度
- ✅ 网络类型（WiFi/流量）+ WiFi 名称
- ✅ 位置（市级，如"徐州市"）
- ✅ 正在播放的音乐
- ✅ 设备型号（vivo X200 Pro）
- ✅ 今日最常用 Top 3 应用
- ✅ 访客统计（在线/今日/总计）
- ✅ 最近访客列表（城市 + 浏览器）

### Android App 功能

- ✅ 后台常驻服务
- ✅ 前台通知保活
- ✅ 自动采集数据：
  - 当前应用（UsageStats）
  - 电池信息（电量/充电/温度）
  - 网络状态（WiFi/蓝牙）
  - 位置信息（GPS）
  - 音乐播放（NotificationListener）
  - 屏幕使用时长
  - 游戏时长统计
  - 步数（传感器）
- ✅ 可配置 API 地址、密钥、上报间隔
- ✅ 状态签名（自定义消息）

## 🎨 UI 设计

### 前端（Arctic Frost 亮色主题）

- **配色**:
  - 主背景: `#f8fafc` (浅灰)
  - 卡片: `#ffffff` (纯白)
  - 主色调: `#3b82f6` (蓝色)
  - 强调色: `#10b981` (绿色)
- **特性**:
  - 响应式布局（320px - 1280px）
  - 骨架屏加载动画
  - 卡片悬浮阴影
  - 平滑过渡动画
  - Inter 字体（Google Fonts）

### Android App（Material Design 亮色）

- **配色**:
  - 背景: `#F5F7FA`
  - 卡片: `#FFFFFF`
  - 启动按钮: `#10B981` (绿色)
  - 停止按钮: `#EF4444` (红色)
  - 保存按钮: `#3B82F6` (蓝色)
- **特性**:
  - CardView 卡片设计
  - 阴影和圆角
  - 大点击区域（56dp）
  - 实时数据刷新（3 秒）

## 🔧 技术栈

### 前端

- 纯 HTML + CSS + JavaScript（无框架）
- CSS 变量系统
- Fetch API
- 响应式设计

### 后端

- Cloudflare Workers（TypeScript）
- Cloudflare KV（键值存储）
- OpenStreetMap Nominatim（地理编码）

### Android

- Kotlin
- Material Components
- CardView
- UsageStatsManager（应用使用统计）
- NotificationListenerService（音乐监听）
- LocationManager（GPS）
- SensorManager（步数）

## 📝 API 接口

### POST /update

上报手机状态

**Headers**:

```text
Authorization: Bearer {API_KEY}
Content-Type: application/json
```

**Body**:

```json
{
  "app": "com.tencent.mobileqq",
  "appName": "QQ",
  "battery": 70,
  "charging": false,
  "batteryTemp": 29.7,
  "wifi": true,
  "wifiName": "MyWiFi",
  "bluetooth": true,
  "screen": true,
  "networkType": "WiFi",
  "lat": 34.0,
  "lon": 117.0,
  "music": "歌曲名",
  "musicArtist": "歌手",
  "steps": 5000,
  "screenTime": 300,
  "gameTime": 60,
  "mood": "normal",
  "isLateNight": false,
  "statusMessage": "今天心情不错",
  "topApps": [
    {"name": "QQ", "minutes": 120},
    {"name": "微信", "minutes": 90}
  ],
  "device": "vivo X200 Pro"
}
```

### GET /status

获取当前状态

**Response**:

```json
{
  "app": "com.tencent.mobileqq",
  "appName": "QQ",
  "battery": 70,
  "location": "徐州市",
  "timestamp": 1768389474252,
  ...
}
```

### GET /visit

记录访客（自动调用）

### GET /visitors

获取访客统计

**Response**:

```json
{
  "online": 1,
  "today": 5,
  "total": 25,
  "recentVisitors": [...]
}
```

## 🛠️ 部署指南

### 1. 部署 Worker

```bash
cd phone-status/worker
npm install
npx wrangler deploy
```

### 2. 部署前端

```bash
cd phone-status
npx wrangler pages deploy frontend --project-name=phone-status
```

### 3. 编译 Android App

1. 用 Android Studio 打开 `phone-status/android`
2. 修改 `MainActivity.kt` 中的默认 API 地址和密钥
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. 安装到手机：`phone-status/android/app/build/outputs/apk/debug/app-debug.apk`

### 4. 配置 Android App

1. 打开 App，填写 API 地址和密钥
2. 点击"授予必要权限"，授予：
   - 位置权限（精确位置）
   - 使用情况访问权限
   - 通知监听权限（音乐播放）
   - 蓝牙权限
   - 身体活动权限（步数）
3. 点击"启动服务"

## ⚠️ 已知问题

1. **国内访问需要代理**: CF Workers 在国内被墙，需要 VPN
2. **KV 写入限制**: 免费版每天 1000 次，建议上报间隔 ≥ 90 秒
3. **步数可能不准**: 国产手机后台传感器访问受限
4. **音乐监听不稳定**: 部分音乐 App 不发送通知
5. **Google Fonts 被墙**: 前端字体加载可能失败

## 🔜 待办事项

- [ ] 迁移到自托管服务器（避免墙）
- [ ] 支持多用户（每人独立 ID）
- [ ] Docker Compose 一键部署
- [ ] 使用 Redis 替代 KV
- [ ] 添加数据可视化图表
- [ ] 支持历史数据查询
- [ ] PWA 支持（离线访问）
- [ ] 深色模式切换

## 📄 许可证

MIT License

## 👤 作者

- B 站: [Le_Ho](https://space.bilibili.com/631853247)
- 域名: `status.dwill.top`

---

**最后更新**: 2026-01-21
