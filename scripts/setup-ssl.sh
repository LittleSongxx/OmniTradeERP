#!/bin/bash

# SSL 证书一键配置脚本
# 使用方法：./scripts/setup-ssl.sh your-domain.com your-email@example.com

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 参数检查
if [ $# -lt 2 ]; then
    echo -e "${RED}❌ 用法：$0 <域名> <邮箱> [DNS 服务商]${NC}"
    echo ""
    echo "示例:"
    echo "  $0 erp.example.com admin@example.com          # Standalone 模式（需要 80 端口）"
    echo "  $0 erp.example.com admin@example.com cloudflare  # Cloudflare DNS 验证"
    echo "  $0 erp.example.com admin@example.com aliyun     # 阿里云 DNS 验证"
    exit 1
fi

DOMAIN=$1
EMAIL=$2
DNS_PROVIDER=${3:-standalone}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SSL_DIR="$PROJECT_ROOT/nginx/ssl"

echo -e "${GREEN}🔐 SSL 证书配置脚本${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "域名：${YELLOW}$DOMAIN${NC}"
echo -e "邮箱：${YELLOW}$EMAIL${NC}"
echo -e "验证方式：${YELLOW}$DNS_PROVIDER${NC}"
echo ""

# 创建 SSL 目录
echo -e "${GREEN}[1/4]${NC} 创建 SSL 证书目录..."
mkdir -p "$SSL_DIR"
echo "✅ 目录已创建：$SSL_DIR"
echo ""

# 停止 Nginx（standalone 模式需要 80 端口）
if [ "$DNS_PROVIDER" = "standalone" ]; then
    echo -e "${GREEN}[2/4]${NC} 停止 Nginx 服务（释放 80 端口）..."
    cd "$PROJECT_ROOT"
    docker-compose stop nginx || true
    echo "✅ Nginx 已停止"
else
    echo -e "${GREEN}[2/4]${NC} 跳过 Nginx 停止（DNS 验证模式）..."
fi
echo ""

# 获取证书
echo -e "${GREEN}[3/4]${NC} 获取 SSL 证书..."
cd "$PROJECT_ROOT"

case $DNS_PROVIDER in
    cloudflare)
        if [ -z "$CF_DNS_API_TOKEN" ]; then
            echo -e "${RED}❌ 需要设置 Cloudflare API Token${NC}"
            echo "请设置环境变量：export CF_DNS_API_TOKEN=your_token"
            exit 1
        fi
        docker run -it --rm \
            -v "$SSL_DIR:/etc/letsencrypt" \
            -e CF_DNS_API_TOKEN="$CF_DNS_API_TOKEN" \
            certbot/certbot \
            certonly --dns-cloudflare \
            -d "$DOMAIN" \
            -d "www.$DOMAIN" \
            --email "$EMAIL" \
            --agree-tos \
            --no-eff-email \
            --non-interactive
        ;;
    
    aliyun)
        if [ -z "$ALIYUN_ACCESS_KEY_ID" ] || [ -z "$ALIYUN_ACCESS_KEY_SECRET" ]; then
            echo -e "${RED}❌ 需要设置阿里云 Access Key${NC}"
            echo "请设置环境变量:"
            echo "  export ALIYUN_ACCESS_KEY_ID=your_key_id"
            echo "  export ALIYUN_ACCESS_KEY_SECRET=your_key_secret"
            exit 1
        fi
        docker run -it --rm \
            -v "$SSL_DIR:/etc/letsencrypt" \
            -e ALIYUN_ACCESS_KEY_ID="$ALIYUN_ACCESS_KEY_ID" \
            -e ALIYUN_ACCESS_KEY_SECRET="$ALIYUN_ACCESS_KEY_SECRET" \
            certbot/certbot \
            certonly --dns-aliyun \
            -d "$DOMAIN" \
            -d "www.$DOMAIN" \
            --email "$EMAIL" \
            --agree-tos \
            --no-eff-email \
            --non-interactive
        ;;
    
    standalone|*)
        docker run -it --rm \
            -v "$SSL_DIR:/etc/letsencrypt" \
            -p 80:80 \
            certbot/certbot \
            certonly --standalone \
            -d "$DOMAIN" \
            -d "www.$DOMAIN" \
            --email "$EMAIL" \
            --agree-tos \
            --no-eff-email \
            --non-interactive
        ;;
esac

if [ $? -eq 0 ]; then
    echo "✅ 证书获取成功！"
else
    echo -e "${RED}❌ 证书获取失败${NC}"
    exit 1
fi
echo ""

# 更新 Nginx 配置
echo -e "${GREEN}[4/4]${NC} 更新 Nginx 配置文件..."

NGINX_CONF="$PROJECT_ROOT/nginx/nginx.conf"

# 备份原配置
cp "$NGINX_CONF" "$NGINX_CONF.bak"

# 替换域名
sed -i.bak "s/erp.yourdomain.com/$DOMAIN/g" "$NGINX_CONF"
sed -i.bak "s/your-domain.com/$DOMAIN/g" "$NGINX_CONF"

# 取消 HTTPS 配置注释（使用 awk 处理多行）
awk '
    /^    # HTTPS 服务器/ { in_https=1; next }
    /^    # }/ && in_https { in_https=0; print "    }"; next }
    in_https { sub(/^    # /, "    "); print; next }
    { print }
' "$NGINX_CONF" > "$NGINX_CONF.tmp" && mv "$NGINX_CONF.tmp" "$NGINX_CONF"

# 开启 HTTP→HTTPS 重定向
sed -i.bak 's/# return 301 https:\/\/\$server_name\$request_uri;/return 301 https:\/\/\$server_name\$request_uri;/' "$NGINX_CONF"

# 清理备份文件
rm -f "$NGINX_CONF.bak" "$NGINX_CONF.tmp"

echo "✅ Nginx 配置已更新"
echo ""

# 显示证书信息
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ SSL 证书配置完成！${NC}"
echo ""
echo "📁 证书文件位置：$SSL_DIR/live/$DOMAIN/"
echo ""
echo "📋 下一步操作："
echo ""
echo "  1. 启动服务（包含自动续期）:"
echo -e "     ${YELLOW}cd $PROJECT_ROOT${NC}"
echo -e "     ${YELLOW}docker-compose -f docker-compose.yml -f docker-compose.ssl.yml up -d${NC}"
echo ""
echo "  2. 查看 Nginx 日志:"
echo -e "     ${YELLOW}docker-compose logs -f nginx${NC}"
echo ""
echo "  3. 验证 HTTPS:"
echo -e "     ${YELLOW}curl -I https://$DOMAIN${NC}"
echo ""
echo "  4. 检查证书状态:"
echo -e "     ${YELLOW}docker-compose exec nginx openssl s_client -connect localhost:443 -servername $DOMAIN${NC}"
echo ""
echo -e "${YELLOW}⚠️  注意：证书有效期 90 天，已配置自动续期（每 12 小时检查）${NC}"
echo ""
