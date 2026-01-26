# 🛠️ 本地开发与调试指南 (Local Development)

如果你想在本地机器上运行并测试本项目，请遵循以下步骤。

## 1. 后端服务端 (Node.js Server)

### 环境依赖
- Node.js (v18+)
- npm 或 yarn

### 运行步骤
1. 进入服务端目录：
   ```bash
   cd phone-status/self-hosted/server
   ```
2. 安装依赖：
   ```bash
   npm install
   ```
3. 启动开发服务器：
   ```bash
   npm run dev
   ```
   默认地址为 `http://localhost:9529`。

## 2. 手机端连接 (Android Connectivity)

由于是本地开发，手机需要能访问到电脑的 IP。

### 确定内网 IP
在 Windows 上运行 `ipconfig`，找到你的局域网 IP（例如 `192.168.31.50`）。

### 手机配置
1. 在 Android App 中填写 API 地址：`http://192.168.31.50:9529/api/update`。
2. 确保手机和电脑在**同一个 WiFi** 模式下。
3. **注意：** 连手机热点时，电脑 IP 会变化，且部分手机系统会隔离热点内的 App 访问宿主机。

## 3. 防火墙配置 (重要)
Windows 默认可能拦截入站请求。
- 请进入“Windows 安全中心” -> “防火墙和网络保护”。
- 确保 Node.js 被允许通过防火墙。
- 或者手动开放 `9529` 端口。

## 4. 调试模式
App 推出了 `selfhostedDebug` 变体。在该模式下，App 磁贴和通知栏会显示更详细的传输日志，方便排查连接问题。
