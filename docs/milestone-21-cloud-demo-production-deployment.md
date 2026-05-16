# Milestone 21: 真实部署 / 云端演示环境

## 1. 背景

当前项目已经完成：

- 后端主链路：Auth、Project、Member、Repository、Task、Agent、Chat SSE、RAG、Model Gateway、GitHub PR Review、Audit、Observability。
- 前端控制台：Vue 3 + Element Plus + 动态科技风 UI，覆盖 Dashboard、Projects、Tasks、Chat、Knowledge、Repository、Members、GitHub、Model Gateway、Observability。
- 测试与发布基础：后端集成测试、前端 Playwright E2E、Dockerfile、Docker Compose、GitHub Actions、Release QA。
- Milestone 20A / 20B 已完成 UI 视觉升级与一致性收口。

Milestone 18 已经提供本地 Docker Compose 演示基础。本阶段要把它推进到真实云端演示环境：

> 让项目可以部署到一台云服务器，通过域名 + HTTPS 访问前端，前端通过 `/api` 反向代理后端，支持 Chat SSE，使用生产化环境变量和基础运维脚本完成启动、检查、备份与回滚准备。

本阶段不是 Kubernetes、不是多机高可用、不是复杂 CI/CD 发布平台。目标是 **单机 Docker Compose 生产演示环境**。

## 2. 总目标

实现 5 个能力：

1. 生产 Compose 编排
   - 使用独立 `prod` 配置，不污染本地开发 Compose。
   - 后端、前端、MySQL、Redis、RabbitMQ 可在云服务器启动。
   - 所有密钥通过 `.env.production` 注入。
   - 数据使用命名 volume 或清晰的数据目录。

2. Nginx / HTTPS 反向代理
   - 支持域名访问前端。
   - `/api/` 代理到后端。
   - SSE 不被 buffering 截断。
   - 支持 Certbot 或 Cloudflare Tunnel/Proxy 的接入说明。

3. 生产配置加固
   - 强制说明 `JWT_SECRET`、`DB_PASSWORD`、模型 Key、GitHub OAuth Secret 不能使用默认值。
   - CORS 限定生产域名。
   - Actuator 只暴露必要端点。
   - 日志和健康检查可用。

4. 部署脚本与 Smoke Test
   - 一键拉取 / 构建 / 启动。
   - 一键健康检查。
   - 一键查看日志。
   - 一键备份 MySQL。
   - 一键 smoke test 生产域名。

5. 文档化上线流程
   - 云服务器准备清单。
   - DNS / HTTPS 配置。
   - 首次部署步骤。
   - 更新部署步骤。
   - 回滚和排障步骤。

## 3. 严格边界

执行 Milestone 21 时必须遵守：

1. 不改业务逻辑。
2. 不改后端 API 行为。
3. 不改数据库表结构，除非发现明确部署配置 bug。
4. 不提交真实 `.env.production`。
5. 不提交真实密钥、真实 API Key、OAuth Secret、生产数据库密码。
6. 不写死个人电脑路径。
7. 不绑定单一云厂商 SDK。
8. 不引入 Kubernetes、Helm、Terraform。
9. 不改变当前本地开发启动方式。
10. 不破坏 Milestone 18 的本地 Docker Compose。
11. 不把 `dist/`、`target/`、`node_modules/`、数据库 dump、日志文件提交。

## 4. 建议新增 / 修改文件

### 4.1 生产 Compose

新增：

```text
deploy/prod/docker-compose.prod.yml
deploy/prod/nginx.conf
deploy/prod/README.md
```

`deploy/prod/docker-compose.prod.yml` 建议包含：

```text
mysql
redis
rabbitmq
backend
frontend
nginx
```

要求：

- `mysql` 不直接暴露公网端口，除非通过 `127.0.0.1:3317:3306` 仅本机调试。
- `backend` 不直接暴露公网端口，走 `nginx /api`。
- `frontend` 可不直接暴露公网端口，走 `nginx /`。
- `nginx` 暴露 `80` 和 `443`。
- 所有服务设置 `restart: unless-stopped`。
- MySQL、Redis、RabbitMQ 使用命名 volume。
- backend healthcheck 使用 `/actuator/health`。
- frontend/nginx healthcheck 使用 `/` 或 `/healthz`。
- 生产环境变量从 `.env.production` 读取。

注意：

- 如果当前 `frontend` 镜像内已经有 nginx，也可以选择只使用 `frontend` 的 nginx 代理 `/api`，但生产域名和 HTTPS 更推荐单独外层 nginx。
- 如果采用单独外层 nginx，内部 frontend 可以只暴露 `80` 到 Compose 网络，不映射宿主机。

### 4.2 生产环境变量模板

新增：

```text
.env.production.example
```

至少包含：

```text
COMPOSE_PROJECT_NAME=ai-coding-platform-prod

APP_DOMAIN=example.com
APP_BASE_URL=https://example.com
API_BASE_URL=https://example.com/api

MYSQL_DATABASE=ai_coding_platform
MYSQL_ROOT_PASSWORD=CHANGE_ME_STRONG_PASSWORD
DB_URL=jdbc:mysql://mysql:3306/ai_coding_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=CHANGE_ME_STRONG_PASSWORD

JWT_SECRET=CHANGE_ME_AT_LEAST_32_CHARS_RANDOM
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xms256m -Xmx768m -XX:+UseG1GC

MODEL_GATEWAY_PROVIDER=MOCK
OPENAI_API_KEY=
CLAUDE_API_KEY=
DEEPSEEK_API_KEY=
QWEN_API_KEY=
GEMINI_API_KEY=

GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=
GITHUB_REDIRECT_URI=https://example.com/api/github/oauth/callback

RABBITMQ_DEFAULT_USER=platform
RABBITMQ_DEFAULT_PASS=CHANGE_ME_RABBITMQ_PASSWORD
```

要求：

- 必须明确 `CHANGE_ME` 不可直接用于生产。
- 不要提供真实值。
- `.gitignore` 必须包含 `.env.production`。

### 4.3 Spring 生产配置

可新增：

```text
backend/src/main/resources/application-prod.yml
```

只放生产 profile 差异配置：

- 日志级别。
- Actuator 暴露端点。
- CORS allowed origins。
- server forward headers。
- 错误响应策略。

建议：

```yaml
server:
  forward-headers-strategy: framework

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: never

logging:
  level:
    root: INFO
    com.aicoding.platform: INFO
```

如果当前 CORS 在 Java Config 中写死 `localhost:*`，需要改为读取环境变量或配置项：

```text
APP_CORS_ALLOWED_ORIGINS=https://example.com
```

但注意：只能改配置能力，不要改变认证和业务逻辑。

### 4.4 Nginx 配置

新增：

```text
deploy/prod/nginx.conf
```

要求：

- HTTP `80` 可用于 ACME challenge 或跳转 HTTPS。
- HTTPS `443` 代理前端和后端。
- `/` 代理前端容器。
- `/api/` 代理 backend 容器。
- SSE 支持：
  - `proxy_buffering off;`
  - `proxy_cache off;`
  - `proxy_read_timeout 3600s;`
  - `proxy_send_timeout 3600s;`
- 安全头：
  - `X-Content-Type-Options nosniff`
  - `X-Frame-Options SAMEORIGIN`
  - `Referrer-Policy strict-origin-when-cross-origin`
  - 可选 `Content-Security-Policy`，但不要过严导致前端失效。

如果不直接配置 HTTPS 证书，可提供两个版本：

```text
deploy/prod/nginx.http.conf
deploy/prod/nginx.https.conf.example
```

或者在 README 里说明：

- Cloudflare Proxy 模式：服务器只开 80。
- Certbot 模式：服务器开 80/443，证书挂载到 nginx。

### 4.5 部署脚本

新增：

```text
scripts/prod-deploy.sh
scripts/prod-smoke-test.sh
scripts/prod-logs.sh
scripts/prod-backup-mysql.sh
scripts/prod-restore-mysql.sh
```

要求：

- 脚本必须从项目根目录运行。
- 使用 `set -euo pipefail`。
- 检查 `.env.production` 是否存在。
- 不打印敏感环境变量。
- `prod-deploy.sh` 支持：
  - build
  - up
  - pull（如果使用 GHCR 镜像）
  - restart
  - status
- `prod-smoke-test.sh` 支持传入域名：

```bash
bash scripts/prod-smoke-test.sh https://example.com
```

至少检查：

```text
GET /actuator health via /api/actuator/health 或内部 backend health
GET /
POST /api/auth/login
GET /api/auth/me
GET /api/projects
GET /api/agents
```

注意：

- 如果生产不公开 `/api/actuator/health`，smoke test 可以通过 `docker compose exec backend wget` 在服务器内部检查。

### 4.6 文档

新增：

```text
docs/production-deployment-runbook.md
```

内容包括：

1. 服务器要求。
2. 域名和 DNS。
3. Docker / Docker Compose 安装。
4. 首次部署。
5. HTTPS 方案。
6. 环境变量配置。
7. 启动、停止、更新、回滚。
8. MySQL 备份和恢复。
9. 日志查看。
10. 常见问题。

同时更新：

```text
README.md
docs/deployment-guide.md
```

只添加指向生产部署 Runbook 的入口，不要把 README 写得过长。

## 5. 生产配置重点

### 5.1 CORS

当前开发环境允许 `localhost:*`。生产环境必须支持限定域名。

建议：

```text
APP_CORS_ALLOWED_ORIGINS=https://example.com
```

后端读取配置：

```yaml
app:
  cors:
    allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:http://localhost:*}
```

Java Config 中解析为 list 或 pattern。

要求：

- 本地开发仍可用 localhost。
- 生产 profile 使用真实域名。
- 不要为了方便在生产中使用 `*`。

### 5.2 JWT Secret

生产必须要求：

- 至少 32 字符。
- 建议：

```bash
openssl rand -base64 32
```

如果当前代码允许空 secret fallback，生产 Runbook 必须明确禁止。可在 `prod` profile 下增加启动校验，但不得破坏 dev 模式。

### 5.3 Actuator

生产建议：

- 公开外部只允许 `/actuator/health` 或不公开 actuator。
- 内部 Compose healthcheck 可访问 backend 容器的 `/actuator/health`。
- 不公开详细 health details。

### 5.4 日志

建议：

- 后端 stdout 输出给 Docker logs。
- 可选挂载 `./logs/backend:/app/logs`。
- 不记录 API Key、Token、OAuth Secret。
- 保留 traceId。

### 5.5 数据库备份

至少提供：

```bash
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production exec mysql \
  mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" ai_coding_platform > backups/ai_coding_platform_YYYYmmdd_HHMMSS.sql
```

脚本应自动创建 `backups/`，文件不提交。

`.gitignore` 需要包含：

```text
backups/
*.sql
*.dump
```

## 6. 验收标准

### 6.1 本地静态检查

必须通过：

```bash
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
```

### 6.2 Docker 配置检查

至少运行：

```bash
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production.example config
```

如果 `.env.production.example` 中包含 `CHANGE_ME` 导致某些服务无法实际启动，配置渲染也必须通过。

### 6.3 本地生产模式演练

可以使用本地域名或 localhost 演练：

```bash
cp .env.production.example .env.production
# 修改 JWT_SECRET、DB_PASSWORD 等 CHANGE_ME
bash scripts/prod-deploy.sh up --build
bash scripts/prod-smoke-test.sh http://localhost
```

### 6.4 云服务器手动验收

部署到云服务器后检查：

1. 域名打开前端。
2. HTTPS 证书有效。
3. 登录成功。
4. Dashboard 加载。
5. Project list 加载。
6. Chat SSE 流式回复正常。
7. Knowledge / RAG search 正常。
8. Task execute 正常。
9. Model Gateway 默认 MOCK 可用。
10. GitHub OAuth 未配置时给出清晰提示，不影响系统。
11. `/api` 代理无 502。
12. 刷新任意前端路由不 404。
13. Docker 容器重启后服务恢复。
14. MySQL 数据 volume 持久化。
15. `prod-backup-mysql.sh` 可生成备份。

## 7. 不要做的事

不要：

- 直接把服务器 IP、域名、真实密码写进仓库。
- 提交 `.env.production`。
- 提交 SQL 备份。
- 改业务代码来适配部署。
- 为了部署关闭认证。
- 在 nginx 中禁用所有安全限制。
- 在生产中开放 MySQL 到公网。
- 引入 Kubernetes / Helm / Terraform。

## 8. 完成报告格式

完成后按以下格式输出：

```text
Milestone 21 完成报告

1. 新增/修改文件清单
2. 生产 Compose 编排说明
3. Nginx / HTTPS / SSE 代理配置说明
4. 环境变量与密钥处理
5. Spring prod profile / CORS / Actuator 加固
6. 部署脚本说明
7. 备份 / 恢复 / 日志脚本说明
8. Runbook 文档说明
9. 本地构建与测试结果
10. Docker compose config / 本地演练结果
11. 云服务器部署验收结果（如已执行）
12. 已知限制
13. 是否可以进入 Milestone 22：真实模型与 GitHub OAuth 生产联调
```

## 9. Claude 执行提示词

将下面内容复制给 Claude 执行：

```text
请根据项目中的文档执行 Milestone 21。

文档路径：
docs/milestone-21-cloud-demo-production-deployment.md

执行要求：
1. 先完整阅读该文档，再检查当前 deploy、Dockerfile、README、application.yml、SecurityConfig/CORS 相关代码。
2. 本阶段是在 Milestone 18 Docker 基础上做真实云端演示环境，不要重写 Docker 体系。
3. 不要改业务逻辑，不要改后端接口，不要改数据库表结构。
4. 不要提交真实密钥、真实 API Key、OAuth Secret、生产数据库密码。
5. 不要提交 .env.production、SQL dump、日志、dist、target、node_modules。
6. 不要引入 Kubernetes、Helm、Terraform，也不要绑定特定云厂商。
7. 保持本地开发 Compose 和现有 CI/CD 不回退。
8. 重点完成单机 Docker Compose 生产演示部署能力。

需要实现：
1. 新增 deploy/prod/docker-compose.prod.yml。
2. 新增 deploy/prod/nginx.conf 或 nginx.http.conf + nginx.https.conf.example。
3. 新增 .env.production.example，包含生产部署需要的全部变量，并用 CHANGE_ME 占位。
4. 如需要，新增 backend/src/main/resources/application-prod.yml。
5. 如当前 CORS 只支持 localhost，请改为配置化，支持 APP_CORS_ALLOWED_ORIGINS，同时保持本地 localhost 可用。
6. 新增 scripts/prod-deploy.sh。
7. 新增 scripts/prod-smoke-test.sh。
8. 新增 scripts/prod-logs.sh。
9. 新增 scripts/prod-backup-mysql.sh。
10. 新增 scripts/prod-restore-mysql.sh。
11. 新增 docs/production-deployment-runbook.md。
12. 更新 README.md 和 docs/deployment-guide.md，加入生产部署入口。
13. 更新 .gitignore，确保 .env.production、backups/、*.sql、*.dump、logs/ 不被提交。

重点要求：
1. Nginx 必须支持 Vue Router history fallback。
2. /api/ 必须反向代理 backend。
3. SSE 必须关闭 proxy buffering/cache，并设置足够长的 read timeout。
4. backend 不应直接暴露公网端口，生产流量走 nginx。
5. MySQL 不应暴露公网端口。
6. backend healthcheck 使用 /actuator/health。
7. 生产 JWT_SECRET 必须在文档中要求强随机值。
8. Actuator 生产环境不暴露详细 health details。
9. 部署脚本不得打印敏感变量。
10. Smoke test 至少覆盖首页、登录、me、projects、agents。

完成后必须执行：
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build

回到项目根目录执行：
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production.example config

如果本机 Docker 可用，并且你能安全创建 .env.production，请进行本地生产模式演练：
cp .env.production.example .env.production
# 将 CHANGE_ME 替换为本地强随机测试值
bash scripts/prod-deploy.sh up --build
bash scripts/prod-smoke-test.sh http://localhost

完成后按以下格式输出：
1. 新增/修改文件清单
2. 生产 Compose 编排说明
3. Nginx / HTTPS / SSE 代理配置说明
4. 环境变量与密钥处理
5. Spring prod profile / CORS / Actuator 加固
6. 部署脚本说明
7. 备份 / 恢复 / 日志脚本说明
8. Runbook 文档说明
9. 本地构建与测试结果
10. Docker compose config / 本地演练结果
11. 云服务器部署验收结果（如已执行）
12. 已知限制
13. 是否可以进入 Milestone 22：真实模型与 GitHub OAuth 生产联调

现在开始实现，不要只给计划。
```
