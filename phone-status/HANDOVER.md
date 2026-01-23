# 📱 Phone Status 项目交接文档

> 这份文档帮助你快速了解项目现状，无缝接手开发工作。

## 项目概述

这是一个**手机状态监控系统**，用于在网页上实时展示手机的状态信息（正在使用的 App、播放的音乐、电量、位置等）。

**架构**: Android 客户端 → 自托管服务器 → 前端展示页面

## 技术栈

| 组件 | 技术 |
|------|------|
| Android 客户端 | Kotlin, FusedLocationProviderClient, MediaSessionManager |
| 服务端 | Node.js + Express + TypeScript |
| 前端 | 原生 HTML/CSS/JS |
| 部署 | Docker + Caddy 反代 |

## 项目结构

```
phone-status/
├── android/                    # Android 客户端
│   └── app/src/main/java/top/dwill/phonestatus/
│       ├── MainActivity.kt     # 主界面，配置服务器地址、API Key、高德 Key
│       ├── StatusService.kt    # 前台服务，定时触发状态收集和上报
│       └── StatusCollector.kt  # 核心！收集所有状态数据
│
├── self-hosted/               # 自托管服务端
│   ├── server/
│   │   └── src/index.ts       # Express 服务器，端口 9529
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── caddy-config.txt       # Caddy 反代配置参考
│
└── frontend/
    └── index.html             # 前端展示页面
```

## 已完成功能 ✅

### Android 客户端
- [x] 后台前台服务 (foregroundServiceType="dataSync")
- [x] 前台应用检测 (使用 `queryEvents` 而非 `queryUsageStats`，更精准)
- [x] 正在播放检测 (MediaSessionManager，支持音乐/视频标题、艺术家)
- [x] 电量状态 (电量百分比 + 充电状态)
- [x] GPS 定位 (FusedLocationProviderClient，含 lastLocation 兜底)
- [x] 反向地理编码 (Android Geocoder 优先，高德 API 备用)
- [x] 可配置服务器地址、API Key、高德 Key

### 服务端
- [x] 状态接收和存储 (JSON 文件)
- [x] API Key 验证
- [x] 访客统计 (IP 去重，隐私保护)
- [x] 静态前端文件服务
- [x] Docker 部署支持

### 前端
- [x] 状态卡片展示
- [x] 自动刷新
- [x] 访客计数显示

## 关键代码位置

### 位置获取流程 (StatusCollector.kt)
```
getLocation() 方法 (~第400行起):
1. GPS 获取经纬度 (getCurrentLocation → lastLocation 兜底)
2. Android Geocoder 反向地理编码 (优先，不需要 API Key)
3. 高德 API (仅当 Geocoder 失败时调用)
```

### 前台应用检测 (StatusCollector.kt)
```
getForegroundApp() 方法:
- 使用 UsageStatsManager.queryEvents() 
- 查找最近的 MOVE_TO_FOREGROUND 事件
- 比 queryUsageStats 更精准
```

### 服务端 API (server/src/index.ts)
```
POST /api/status     - 上报状态 (需要 x-api-key)
GET  /api/status     - 获取状态
GET  /api/visitors   - 获取访客数
POST /api/visitor    - 记录访客
```

## ⚠️ 重要：网络环境说明

用户的服务器运行 **Clash TUN 代理**，出口是**新加坡节点**，无法直接访问中国大陆 API（如高德）。

**解决方案**: 所有需要访问国内 API 的操作都在 **Android 手机端** 完成（手机是国内网络），服务端只做数据存储。

这就是为什么位置解析在手机端做，而不是服务端。

## 开发命令

### 启动本地服务器
```bash
cd phone-status/self-hosted/server
npm run dev
# 服务运行在 http://localhost:9529
```

### 构建 Android 客户端
在 Android Studio 中打开 `phone-status/android` 目录，直接 Build & Run。

### Docker 部署
```bash
cd phone-status/self-hosted
docker-compose up -d
```

## 配置说明

### Android 客户端配置 (SharedPreferences "config")
- `server_url`: 服务器地址 (如 https://status.example.com)
- `api_key`: API 密钥
- `amap_key`: 高德地图 API Key (可选，Geocoder 失败时备用)

### 服务端环境变量
- `PORT`: 端口，默认 9529
- `API_KEY`: API 密钥，默认 "dwill-status-key-2024"

## 访客统计机制

- 基于 IP 地址去重 (同一 IP 只计数一次)
- IP 存储时做隐私处理 (只存前6位 + ***)
- 数据存储在 `server/data/visitors.json`

## 待办/可扩展功能

- [ ] 位置缓存优化 (减少 API 调用，距离 < 5km 不更新)
- [ ] 历史记录/活动时间线
- [ ] 多设备支持
- [ ] 自定义主题
- [ ] 通知推送

## 调试技巧

### 查看 Android 日志
```bash
adb logcat | grep StatusCollector
```

关键日志 TAG:
- `StatusCollector` - 状态收集
- `StatusService` - 服务生命周期

### 测试 API
```bash
# 获取状态
curl http://localhost:9529/api/status

# 上报状态 (测试)
curl -X POST http://localhost:9529/api/status \
  -H "Content-Type: application/json" \
  -H "x-api-key: dwill-status-key-2024" \
  -d '{"app":"测试","battery":100}'
```

---

**最后更新**: 2026-01-23

有问题直接看代码，关键逻辑都有注释。祝开发顺利！ 🚀
