# Nginx 反向代理配置

## 目录结构

```
nginx/
├── Dockerfile          # Nginx 镜像构建文件
├── nginx.conf          # Nginx 配置文件
└── ssl/                # SSL 证书目录（生产环境使用）
    ├── erp.crt
    └── erp.key
```

## 功能特性

### 1️⃣ 统一入口
- 所有外部请求通过 Nginx 的 80/443 端口
- 隐藏内部微服务架构（15+ 服务端口不再暴露）

### 2️⃣ 请求路由
- `/` → 前端静态资源（Vue 应用）
- `/api/**` → API Gateway（统一 API 入口）
- `/health` → 健康检查端点
- `/nginx-status` → Nginx 状态监控（内网访问）

### 3️⃣ 安全防护
- **API 限流**: 10 请求/秒，burst=20
- **登录限流**: 5 请求/秒，burst=5（防暴力破解）
- **IP 访问控制**: `/nginx-status` 仅内网访问

### 4️⃣ 性能优化
- **Gzip 压缩**: JS/CSS/JSON 等资源自动压缩
- **静态缓存**: 前端资源缓存 7 天
- **连接优化**: keepalive、tcp_nopush、tcp_nodelay

### 5️⃣ HTTPS 支持
- 配置文件已包含 HTTPS 模板（注释状态）
- 生产环境只需：
  1. 放置 SSL 证书到 `nginx/ssl/` 目录
  2. 取消 `nginx.conf` 中 HTTPS server 块的注释
  3. 开启 HTTP→HTTPS 重定向

## 本地开发

开发环境可以继续使用各服务的直接端口访问，生产环境使用 Nginx：

```bash
# 启动完整栈（包含 Nginx）
docker-compose up -d

# 访问地址
# 前端：http://localhost
# API: http://localhost/api/...
```

## 生产部署

### 获取 SSL 证书（Let's Encrypt 免费证书）

```bash
# 安装 certbot
docker run -it --rm \
  -v ./nginx/ssl:/etc/letsencrypt \
  certbot/certbot \
  certonly --standalone -d erp.yourdomain.com
```

### 启用 HTTPS

1. 编辑 `nginx.conf`，取消 HTTPS server 块的注释
2. 在 HTTP server 块中开启重定向：
   ```nginx
   return 301 https://$server_name$request_uri;
   ```

### 部署命令

```bash
# 重新构建并启动
docker-compose up -d --build nginx

# 查看日志
docker-compose logs -f nginx

# 检查状态
docker-compose ps
```

## 监控和诊断

```bash
# 查看 Nginx 访问日志
docker-compose logs -f nginx | grep access

# 查看错误日志
docker-compose logs -f nginx | grep error

# 检查 Nginx 状态（内网）
curl http://localhost/nginx-status

# 健康检查
curl http://localhost/health
```

## 扩展：负载均衡

当某个服务需要多实例部署时，在 `nginx.conf` 的 upstream 块中添加：

```nginx
upstream gateway {
    server gateway:8080;
    server gateway-2:8080;
    server gateway-3:8080;
}
```

然后在 `docker-compose.yml` 中添加对应的服务副本。

## 环境变量配置

生产环境建议使用环境变量管理敏感配置：

```nginx
# 在 nginx.conf 中使用
# ssl_certificate /etc/nginx/ssl/${SSL_CERT_PATH};
```

结合 Docker secrets 或环境变量文件使用。
