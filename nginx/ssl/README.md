# SSL 证书配置指南

## 📋 证书获取方式

### 方式一：Let's Encrypt 免费证书（推荐）

使用 Certbot 自动获取和续期，完全免费，有效期 90 天（自动续期）。

### 方式二：商业证书

从阿里云/腾讯云等购买，有效期 1 年，需要手动续期。

---

## 🚀 使用 Certbot 获取证书

### 1. 创建证书目录

```bash
cd /Users/huanghuixiang/.openclaw/workspace/OmniTradeERP/nginx
mkdir -p ssl
```

### 2. 获取证书（ standalone 模式）

**前提条件：** 80 端口未被占用，需要先停止 Nginx

```bash
# 停止 Nginx（如果已运行）
docker-compose stop nginx

# 运行 Certbot
docker run -it --rm \
  -v ./ssl:/etc/letsencrypt \
  certbot/certbot \
  certonly --standalone \
  -d your-domain.com \
  -d www.your-domain.com \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email
```

### 3. 获取证书（dns 模式 - 推荐生产环境）

如果使用了 DNS 服务商，可以用 DNS 验证（无需占用 80 端口）：

```bash
# Cloudflare DNS
docker run -it --rm \
  -v ./ssl:/etc/letsencrypt \
  -e CF_DNS_API_TOKEN=your_cloudflare_token \
  certbot/certbot \
  certonly --dns-cloudflare \
  -d your-domain.com \
  -d www.your-domain.com \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email

# 阿里云 DNS
docker run -it --rm \
  -v ./ssl:/etc/letsencrypt \
  -e ALIYUN_ACCESS_KEY_ID=your_key_id \
  -e ALIYUN_ACCESS_KEY_SECRET=your_key_secret \
  certbot/certbot \
  certonly --dns-aliyun \
  -d your-domain.com \
  -d www.your-domain.com
```

### 4. 证书文件位置

成功后会生成：
```
ssl/
├── live/
│   └── your-domain.com/
│       ├── cert.pem      # 证书文件
│       ├── privkey.pem   # 私钥文件
│       ├── chain.pem     # 证书链
│       └── fullchain.pem # 完整证书链
├── archive/              # 历史证书归档
└── renewal/              # 续期配置
```

---

## 🔧 配置 Nginx 使用证书

### 1. 修改 nginx.conf

取消 HTTPS server 块的注释，并修改证书路径：

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;

    # SSL 证书配置
    ssl_certificate /etc/nginx/ssl/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/live/your-domain.com/privkey.pem;
    
    # ... 其他配置
}
```

### 2. 开启 HTTP→HTTPS 重定向

在 HTTP server 块中添加：

```nginx
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    
    # 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}
```

### 3. 重启 Nginx

```bash
docker-compose up -d --force-recreate nginx
```

---

## 🔄 自动续期配置

Let's Encrypt 证书有效期 90 天，建议 60 天时自动续期。

### 创建续期脚本

```bash
# 创建续期脚本
cat > ssl/renew.sh << 'EOF'
#!/bin/bash
set -e

cd /Users/huanghuixiang/.openclaw/workspace/OmniTradeERP

# 停止 Nginx（standalone 模式需要 80 端口）
docker-compose stop nginx

# 续期证书
docker run -it --rm \
  -v ./ssl:/etc/letsencrypt \
  certbot/certbot renew

# 重新加载 Nginx
docker-compose start nginx

echo "✅ 证书续期完成"
EOF

chmod +x ssl/renew.sh
```

### 添加定时任务（Cron）

```bash
# 编辑 crontab
crontab -e

# 添加每月 1 号凌晨 2 点执行
0 2 1 * * /Users/huanghuixiang/.openclaw/workspace/OmniTradeERP/nginx/ssl/renew.sh >> /var/log/certbot-renew.log 2>&1
```

### 或使用 Docker 自动续期容器

```yaml
# docker-compose.ssl.yml
version: '3.8'

services:
  certbot:
    image: certbot/certbot
    container_name: certbot
    volumes:
      - ./nginx/ssl:/etc/letsencrypt
    entrypoint: "/bin/sh -c 'trap exit TERM; while :; do certbot renew; sleep 12h & wait $${!}; done;'"
    restart: always
```

启动：
```bash
docker-compose -f docker-compose.yml -f docker-compose.ssl.yml up -d
```

---

## ✅ 验证配置

### 1. 检查证书信息

```bash
docker-compose exec nginx openssl s_client -connect localhost:443 -servername your-domain.com
```

### 2. 在线验证

访问 https://www.ssllabs.com/ssltest/ 输入你的域名

### 3. 检查续期状态

```bash
docker run -it --rm \
  -v ./ssl:/etc/letsencrypt \
  certbot/certbot certificates
```

---

## 🔒 安全加固建议

### 1. 限制 SSL/TLS 协议

```nginx
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';
ssl_prefer_server_ciphers on;
```

### 2. 添加 HSTS（强制 HTTPS）

```nginx
add_header Strict-Transport-Security "max-age=63072000" always;
```

### 3. 证书透明度

```nginx
ssl_stapling on;
ssl_stapling_verify on;
resolver 8.8.8.8 8.8.4.4 valid=300s;
resolver_timeout 5s;
```

---

## 🆘 常见问题

### Q: 证书获取失败？
A: 检查域名 DNS 解析是否正确，确保 80 端口可访问（standalone 模式）

### Q: 续期失败？
A: 检查 Certbot 日志：`docker logs certbot`

### Q: 如何切换证书？
A: 直接替换 `ssl/live/your-domain.com/` 下的文件，然后 `docker-compose restart nginx`

---

## 📞 支持

- Certbot 文档：https://certbot.eff.org/docs/
- Let's Encrypt：https://letsencrypt.org/
- SSL 配置生成器：https://ssl-config.mozilla.org/
