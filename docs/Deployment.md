# 🚢 部署文档

## 部署方式总览

| 方式 | 适用场景 | 复杂度 |
|------|---------|--------|
| Docker Compose | 开发/测试 | ⭐ |
| Kubernetes | 生产环境 | ⭐⭐⭐ |
| 裸机部署 | 特殊需求 | ⭐⭐ |

---

## 方式一：Docker Compose 部署（推荐）

### 1. 轻量部署（开发/测试用）

```bash
cd OmniTradeERP
docker-compose -f docker-compose.lite.yml up -d
```

**包含服务：** Gateway + 核心服务 + MySQL + Redis

### 2. 最小化部署（生产最低配置）

```bash
docker-compose -f docker-compose.minimal.yml up -d
```

### 3. 完整部署（全部服务）

```bash
docker-compose -f docker-compose.full.yml up -d
```

**包含：** 全部15个微服务 + AI服务 + MySQL + Redis + ES + RocketMQ

### 4. 测试环境部署

```bash
./deploy-test.sh
```

---

## 方式二：Kubernetes 部署（生产环境）

### 前置要求
- Kubernetes 1.24+
- Helm 3.x
- 存储类（StorageClass）

### 部署步骤

```bash
cd k8s

# 使用默认配置部署
kubectl apply -f deployment-production.yml

# 或使用 Helm
helm install omnitrade ./charts/omnitrade-erp -f values-production.yaml
```

### 配置说明

| 文件 | 用途 |
|------|------|
| `deployment-dev.yml` | 开发环境 |
| `deployment-test.yml` | 测试环境 |
| `deployment-production.yml` | 生产环境 |
| `values-production.yaml` | Helm配置 |

---

## 方式三：裸机部署

### 系统要求
- CPU: 4核+
- 内存: 8GB+
- 磁盘: 100GB+

### 部署脚本

```bash
# 快速部署
./quick-start.sh

# 本地构建后部署
./local-build.sh && ./start.sh

# 智能部署（自动选择配置）
./smart-deploy.sh
```

---

## 环境变量配置

核心配置项：

```bash
# 数据库
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/omnitrade
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

# Nacos
SPRING_CLOUD_NACOS_SERVER_ADDR=localhost:8848

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# AI服务
AI_DEEPSEEK_API_KEY=your_api_key
AI_MODEL_NAME=deepseek-chat
```

---

## 服务访问

| 服务 | 地址 |
|------|------|
| API Gateway | http://localhost:8080 |
| Nacos 控制台 | http://localhost:8848/nacos (nacos/nacos) |
| 前端 Web | http://localhost:3000 |
| Sentinel | http://localhost:8080/sentinel |

---

## 健康检查

```bash
# 检查所有服务状态
curl http://localhost:8080/actuator/health

# 检查特定服务
curl http://localhost:8081/actuator/health
```

---

## 相关文档

- [快速开始](Quick-Start)
- [系统架构](Architecture)
- [AI功能介绍](AI-Features)