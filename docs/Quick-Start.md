# 🚀 快速开始

## 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 18+
- Docker & Docker Compose

## 方式一：Docker 一键启动

```bash
# 克隆项目
git clone https://github.com/nplszfl/OmniTradeERP.git
cd OmniTradeERP

# 启动全部服务（推荐配置）
docker-compose -f docker-compose.yml up -d

# 或使用轻量配置
docker-compose -f docker-compose.lite.yml up -d
```

访问 `http://localhost:8080` 即可使用。

## 方式二：本地开发启动

### 后端启动

```bash
# 编译项目
./mvnw clean package -DskipTests

# 启动所有后端服务
./start.sh
```

### 前端启动

```bash
cd erp-web
npm install
npm run dev
```

## 方式三：Kubernetes 部署

```bash
cd k8s
kubectl apply -f deployment-production.yml
```

## 验证部署

```bash
# 检查服务状态
curl http://localhost:8080/actuator/health

# 查看所有服务
docker-compose ps
```

## 下一步

- 📖 查看 [完整安装指南](Installation)
- 🏗️ 了解 [系统架构](Architecture)
- 🤖 探索 [AI功能](AI-Features)