# ==================== 构建阶段 ====================
FROM node:20-alpine AS builder

WORKDIR /app

# 复制后端代码
COPY server/package*.json ./
RUN npm ci

COPY server/ ./
RUN npm run build

# ==================== 运行阶段 ====================
FROM node:20-alpine

WORKDIR /app

# 安装生产依赖
COPY server/package*.json ./
RUN npm ci --only=production && npm cache clean --force

# 复制编译后的代码
COPY --from=builder /app/dist ./dist

# 复制前端静态文件
COPY frontend/ ./frontend/

# 创建数据目录
RUN mkdir -p /app/data

# 环境变量
ENV NODE_ENV=production
ENV PORT=9529
ENV DATA_DIR=/app/data
ENV FRONTEND_DIR=/app/frontend

# 暴露端口
EXPOSE 9529

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9529/api/test || exit 1

# 启动命令
CMD ["node", "dist/index.js"]
