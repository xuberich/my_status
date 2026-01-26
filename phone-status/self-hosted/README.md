# 📱 EgoGaze - 自托管部署指南

本目录包含 EgoGaze 的 Docker 自托管版本，适用于私有服务器或 NAS 部署。

## 🏗️ 架构

```text
┌─────────────────────────────────────────────────────────┐
│                     Caddy (反向代理)                      │
│                  status.dwill.top:7777                   │
└─────────────────────┬───────────────────────────────────┘
                      │ 反代到 localhost:9529
                      ▼
┌─────────────────────────────────────────────────────────┐
│              Docker Container (Node.js)                  │
│  ┌─────────────────────────────────────────────────────┐│
│  │           Express Server (:9529)                    ││
│  │  ┌──────────────┐  ┌────────────────────┐          ││
│  │  │ 前端静态文件   │  │  后端 API          │          ││
│  │  │ /index.html  │  │  /api/status       │          ││
│  │  │ /assets/*    │  │  /api/update       │          ││
│  │  └──────────────┘  │  /api/visit        │          ││
│  │                    │  /api/visitors     │          ││
│  │                    └────────────────────┘          ││
│  └─────────────────────────────────────────────────────┘│
│                           │                              │
│                           ▼                              │
│  ┌─────────────────────────────────────────────────────┐│
│  │              JSON 文件存储 (./data/)                 ││
│  │    status.json (手机状态)  visitors.json (访客)      ││
│  └─────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
```

## 🚀 快速部署

### 1. 上传文件到服务器

```bash
# 将整个 phone-status 目录上传到服务器
scp -r phone-status/ user@server:/path/to/

# 或者用 Git
git clone https://github.com/your/repo.git
cd phone-status/self-hosted
```

### 2. 配置环境变量（可选）

```bash
cp .env.example .env
# 编辑 .env，修改 API_KEY
```

### 3. 构建并启动

```bash
cd phone-status/self-hosted

# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

### 4. 配置 Caddy 反向代理

在 Caddyfile 中添加：

```caddyfile
status.dwill.top:7777 {
    import cloudflare2
    reverse_proxy localhost:9529
}
```

重载 Caddy：

```bash
caddy reload
# 或
systemctl reload caddy
```

## 📱 配置 Android App

修改 App 中的 API 地址为：

```text
https://status.dwill.top:7777/api/update
```

API Key 保持不变（或使用你在 .env 中设置的）。

## 📂 目录结构

```text
self-hosted/
├── docker-compose.yml    # Docker 编排配置
├── Dockerfile            # 容器构建文件
├── .env.example          # 环境变量模板
├── .dockerignore         # Docker 忽略文件
├── README.md             # 本文档
└── server/               # 后端源码
    ├── package.json
    ├── tsconfig.json
    └── src/
        └── index.ts      # Express 服务器
```

## 🔧 常用命令

```bash
# 启动
docker-compose up -d

# 停止
docker-compose down

# 重建（代码更新后）
docker-compose build --no-cache
docker-compose up -d

# 查看日志
docker-compose logs -f

# 查看状态
docker-compose ps

# 进入容器
docker exec -it phone-status sh

# 查看数据
docker exec phone-status cat /app/data/status.json
```

## 📊 API 接口

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/test` | 测试接口 |
| POST | `/api/update` | 上报手机状态（需要 API Key） |
| GET | `/api/status` | 获取当前状态 |
| GET | `/api/visit` | 记录访客 |
| GET | `/api/visitors` | 获取访客统计 |

## ⚠️ 注意事项

1. **端口映射**：默认使用 9529 端口，可在 docker-compose.yml 中修改
2. **数据持久化**：数据保存在 Docker Volume `phone-status-data` 中
3. **时区**：已配置为 `Asia/Shanghai`
4. **健康检查**：容器会每 30 秒检查一次服务状态

## 🔄 与 CF 版本的区别

| 特性 | CF 版本 | 自托管版本 |
| ---- | ------- | ---------- |
| 存储 | Cloudflare KV | JSON 文件 |
| API 路径 | `/status` | `/api/status` |
| 写入限制 | 1000次/天 | 无限制 |
| 国内访问 | 需代理 | 直连 |
| 成本 | 免费 | 需要服务器 |

## 📄 许可证

MIT License
