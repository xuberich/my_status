# ☁️ Cloudflare Workers 部署方案 (Serverless Edition)

如果你不想在家里开启服务器（NAS/PC），可以使用基于 Cloudflare Workers + KV 的“零成本、零维护”方案。

## 🏛️ 架构简述
- **Storage**: Cloudflare KV (存储状态 JSON)
- **Logic**: Cloudflare Workers (处理 GET/POST 请求)
- **Frontend**: Cloudflare Pages (部署前端静态页面)

## 🚀 部署步骤

### 1. 准备 KV 命名空间
1. 登录 Cloudflare 控制台。
2. 进入 **Workers 和 Pages** -> **KV**。
3. 创建一个命名空间，例如：`PHONE_STATUS_KV`。

### 2. 部署 Worker
1. 进入 `phone-status/worker` 目录。
2. 修改 `wrangler.toml` 中的 `kv_namespaces` 绑定 ID。
3. 执行部署（需安装 `wrangler`）：
   ```bash
   npx wrangler deploy
   ```
4. 记下你的 Worker URL（例如 `https://status-api.xxx.workers.dev`）。

### 3. 部署前端 (Pages)
1. 在 Cloudflare Pages 中创建一个新项目。
2. 关联你的 GitHub 仓库或直接上传 `phone-status/frontend` 文件夹。
3. 在前端环境变量中设置 `API_BASE` 指向你的 Worker URL。

## ⚖️ 优缺点对比

| 维度 | 自托管 (Docker) | Cloudflare |
| :--- | :--- | :--- |
| **隐私性** | 🔒 极高 (数据仅在自己硬盘) | ☁️ 一般 (数据存放在 CF) |
| **成本** | ⚡ 电费 + 域名 | 💰 免费 (额度内) |
| **稳定性** | 🏠 依赖家里网络/断电 | 🌐 全球稳定运行 |
| **延迟** | 📦 视网络环境而定 | ⚡ 极致低延迟 |
