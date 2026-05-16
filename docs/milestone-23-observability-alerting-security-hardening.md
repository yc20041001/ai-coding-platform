# Milestone 23: 生产监控、告警与安全加固

## 1. 背景

当前项目已经完成：

- Milestone 11：基础 Audit Log、Usage Metrics、System Overview。
- Milestone 18：Docker / CI/CD / Compose 基础。
- Milestone 21：单机云端演示部署、Nginx、production profile、部署脚本、备份脚本。
- Milestone 22：真实模型 Provider 和 GitHub OAuth 生产联调准备。

现在系统已经具备“可部署、可演示、可联调真实外部服务”的能力。下一步需要补齐上线后最关键的生产运维能力：

> 当服务出问题时能发现、能定位、能止血、能恢复；当密钥或 token 有泄露风险时能扫描；当模型或 GitHub 外部服务异常时能告警；当成本或失败率异常时能被看见。

Milestone 23 的目标是：

> 在不改变业务逻辑、不引入复杂平台依赖的前提下，为单机 Docker Compose 生产演示环境补齐监控指标、健康检查、日志扫描、安全检查、告警规则文档和运维 Runbook。

## 2. 严格边界

执行本阶段必须遵守：

1. 不改业务逻辑。
2. 不改核心 API 行为。
3. 不改数据库业务表结构，除非为监控/安全小范围补充必要字段并说明原因。
4. 不提交真实密钥、token、API Key、OAuth Secret。
5. 不提交 `.env.production`。
6. 不引入 Kubernetes、Helm、Terraform。
7. 不强绑定云厂商。
8. 不要求必须部署 Prometheus/Grafana/ELK/Loki，但可以提供可选配置或文档。
9. 不破坏本地开发模式。
10. 不破坏已有 Docker Compose 和 GitHub Actions。
11. 不把日志、备份、dump 文件提交到 Git。
12. 不关闭认证、CORS、Prompt Safety 或审计。

## 3. 总目标

实现 5 个能力：

1. 生产健康检查
   - 应用健康。
   - 数据库健康。
   - 模型网关健康。
   - GitHub OAuth / API 配置健康。
   - Chat SSE 基础健康。

2. 生产指标与告警规则
   - HTTP 错误率。
   - JVM 内存。
   - DB 连接。
   - Model Gateway 失败率、fallback 率、token / cost 异常。
   - GitHub API 失败率。
   - Chat SSE 错误。
   - 登录失败。

3. 日志与 Trace 排障
   - traceId 全链路确认。
   - nginx access/error log。
   - backend structured log。
   - Docker logs 查询脚本。
   - 敏感信息脱敏扫描脚本。

4. 安全检查
   - 密钥扫描。
   - `.env.production` 未提交检查。
   - CORS 生产域名检查。
   - Security headers 检查。
   - Actuator 暴露检查。
   - GitHub token / Model API Key 不泄露检查。

5. 运维 Runbook
   - 服务不可用怎么办。
   - Chat SSE 异常怎么办。
   - 模型调用失败怎么办。
   - GitHub OAuth 失败怎么办。
   - 数据库恢复。
   - 回滚流程。
   - 成本异常处理。

## 4. 需要检查的现有文件

执行前先阅读：

```text
docs/milestone-11-devops-observability.md
docs/milestone-21-cloud-demo-production-deployment.md
docs/milestone-22-real-model-github-oauth-production-validation.md
docs/production-deployment-runbook.md
docs/model-provider-production-setup.md
docs/github-oauth-production-setup.md
backend/src/main/resources/application.yml
backend/src/main/resources/application-prod.yml
backend/src/main/java/com/aicoding/platform/common/config/TraceIdFilter.java
backend/src/main/java/com/aicoding/platform/common/exception/GlobalExceptionHandler.java
backend/src/main/java/com/aicoding/platform/security/config/SecurityConfig.java
backend/src/main/java/com/aicoding/platform/audit/application/AuditLogApplicationService.java
backend/src/main/java/com/aicoding/platform/observability/application/ModelUsageApplicationService.java
backend/src/main/java/com/aicoding/platform/observability/application/SystemOverviewApplicationService.java
backend/src/main/java/com/aicoding/platform/modelgateway/application/ModelRequestLogService.java
backend/src/main/java/com/aicoding/platform/github/application/GithubClient.java
deploy/prod/docker-compose.prod.yml
deploy/prod/nginx.http.conf
deploy/prod/nginx.https.conf.example
.env.production.example
```

## 5. 建议新增 / 修改文件

### 5.1 文档

新增：

```text
docs/production-observability-runbook.md
docs/production-alerting-rules.md
docs/production-security-hardening-checklist.md
docs/incident-response-runbook.md
```

修改：

```text
docs/production-deployment-runbook.md
docs/deployment-guide.md
README.md
```

### 5.2 脚本

新增：

```text
scripts/prod-health-check.sh
scripts/prod-security-check.sh
scripts/prod-log-scan.sh
scripts/prod-alert-check.sh
scripts/prod-diagnostics.sh
```

脚本要求：

- 使用 `set -euo pipefail`。
- 不打印密钥。
- 支持传入 base URL。
- 输出清晰 `PASS` / `WARN` / `FAIL` / `SKIP`。
- 可在本地和云服务器运行。

### 5.3 后端可选增强

可按需新增：

```text
backend/src/main/java/com/aicoding/platform/observability/controller/HealthCheckController.java
backend/src/main/java/com/aicoding/platform/observability/dto/ExternalHealthResponse.java
backend/src/main/java/com/aicoding/platform/observability/application/ExternalHealthApplicationService.java
```

但如果已有 actuator 和 observability API 已足够，可以不新增后端代码，只通过脚本和文档完成。

限制：

- 不要改变现有 `/api/observability/*` 响应结构，除非新增字段且保持兼容。
- 不要暴露敏感配置。
- 不要把 provider API key、GitHub token、DB password 放进 health response。

## 6. 生产健康检查要求

### 6.1 prod-health-check.sh

新增：

```text
scripts/prod-health-check.sh
```

用法：

```bash
bash scripts/prod-health-check.sh https://example.com
bash scripts/prod-health-check.sh http://localhost
```

至少检查：

1. Frontend 首页返回 200。
2. `/api/auth/login` 可登录。
3. `/api/auth/me` 返回当前用户。
4. `/api/projects` 可访问。
5. `/api/agents` 可访问。
6. `/api/observability/overview` 可访问（ADMIN token）。
7. `/api/observability/model-usage/summary` 可访问。
8. `/api/github/oauth/status` 可访问或返回可解释的未配置状态。
9. `/api/model-gateway/providers` 可访问。
10. Chat session 创建 + send message 基础检查（可选，因为会产生数据）。

要求：

- 登录失败必须 FAIL。
- 基础 API 不通必须 FAIL。
- GitHub 未配置可以 WARN / SKIP。
- 真实模型未配置可以 WARN / SKIP。
- 输出 traceId（如果响应中有）便于排查。

### 6.2 Docker 内部健康检查

文档必须说明：

```bash
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production ps
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production logs backend --tail=200
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production exec backend wget -qO- http://localhost:8080/actuator/health
```

## 7. 告警规则要求

新增：

```text
docs/production-alerting-rules.md
```

至少覆盖：

### 7.1 服务可用性

| 告警 | 条件 | 严重级别 | 建议动作 |
|---|---|---|---|
| Frontend Down | 首页连续 3 次非 2xx | P1 | 检查 nginx/frontend |
| Backend Down | `/actuator/health` 非 UP | P1 | 检查 backend logs |
| MySQL Down | backend health DB down | P1 | 检查 mysql container / volume |
| Nginx 5xx | 5xx 比例 > 5% 持续 5 分钟 | P1 | 检查 upstream |

### 7.2 Model Gateway

| 告警 | 条件 | 严重级别 |
|---|---|---|
| Model Failure Rate High | 最近 15 分钟 success=false 比例 > 20% | P2 |
| Model Fallback Rate High | fallback_used 比例 > 30% | P2 |
| Model Cost Spike | 今日 estimated_cost 超过阈值 | P2 |
| Model Timeout Spike | TIMEOUT 错误连续出现 | P2 |
| Prompt Safety Block Spike | safety block 激增 | P3 |

### 7.3 GitHub

| 告警 | 条件 | 严重级别 |
|---|---|---|
| GitHub Auth Error | AUTH error 连续出现 | P2 |
| GitHub Rate Limit | rate limit error 出现 | P2 |
| PR Review Failure | review job FAILED 比例 > 20% | P2 |

### 7.4 Auth / Security

| 告警 | 条件 | 严重级别 |
|---|---|---|
| Login Failure Spike | 登录失败次数突增 | P2 |
| Unauthorized Spike | 401/403 比例突增 | P3 |
| Secret Leak Pattern | 日志出现 `sk-` / `ghp_` / `github_pat_` | P0 |
| CORS Misconfiguration | 非预期 Origin 被允许 | P1 |

## 8. 日志扫描要求

### 8.1 prod-log-scan.sh

新增：

```text
scripts/prod-log-scan.sh
```

用法：

```bash
bash scripts/prod-log-scan.sh
bash scripts/prod-log-scan.sh backend
```

检查内容：

1. API key 泄露模式：
   - `sk-`
   - `ghp_`
   - `github_pat_`
   - `Bearer <long token>`
   - `OPENAI_API_KEY=...`
   - `CLAUDE_API_KEY=...`
   - `GITHUB_CLIENT_SECRET=...`
2. Error 高频：
   - `Exception`
   - `ERROR`
   - `Stacktrace`
   - `SQLSyntaxErrorException`
   - `CommunicationsException`
3. Model / GitHub 专项：
   - `AUTH_ERROR`
   - `RATE_LIMIT`
   - `TIMEOUT`
   - `fallback`
   - `GitHub API`

要求：

- 发现疑似密钥泄露必须 FAIL。
- 普通 ERROR 输出 WARN，并展示最近几行。
- 不把完整 token 再输出到控制台；如必须显示，必须 mask。

## 9. 安全检查要求

### 9.1 prod-security-check.sh

新增：

```text
scripts/prod-security-check.sh
```

用法：

```bash
bash scripts/prod-security-check.sh https://example.com
bash scripts/prod-security-check.sh http://localhost
```

至少检查：

1. `.env.production` 不在 git tracked files。
2. `backups/` 不在 git tracked files。
3. 当前工作区无 `.env.production` 被 staged。
4. 仓库中无明显真实密钥模式。
5. HTTP security headers：
   - `X-Content-Type-Options`
   - `X-Frame-Options`
   - `Referrer-Policy`
6. CORS：
   - 生产域名允许。
   - 非预期 Origin 不应得到宽松 `Access-Control-Allow-Origin: *`。
7. Actuator：
   - 外部不可直接访问详细 health details。
   - `/api/actuator` 不应暴露完整敏感信息。
8. API 未登录访问：
   - `/api/projects` 返回 401。
9. Prompt / token 泄露：
   - Model Gateway response 不含 API Key。
   - GitHub OAuth status 不含 accessToken。

### 9.2 Security Checklist 文档

新增：

```text
docs/production-security-hardening-checklist.md
```

必须包含：

- Secrets。
- CORS。
- Auth / JWT。
- GitHub OAuth。
- Model Provider。
- Nginx headers。
- Actuator。
- Logs。
- Backup。
- Incident response。

## 10. 诊断脚本要求

### 10.1 prod-diagnostics.sh

新增：

```text
scripts/prod-diagnostics.sh
```

用途：

一键采集生产故障排查信息，但不包含密钥。

输出建议：

```text
diagnostics/
  diagnostics_YYYYmmdd_HHMMSS/
    docker-ps.txt
    docker-stats.txt
    backend-health.json
    backend-logs-tail.txt
    nginx-logs-tail.txt
    disk-usage.txt
    memory.txt
    smoke-test.txt
```

要求：

- 自动 mask 敏感内容。
- `diagnostics/` 加入 `.gitignore`。
- 不导出数据库内容。
- 不导出 `.env.production` 明文。

## 11. 可选 Prometheus / Grafana 文档

本阶段不强制接入 Prometheus。

但 `docs/production-observability-runbook.md` 可包含可选方案：

1. Spring Boot Actuator metrics。
2. `micrometer-registry-prometheus` 后续接入说明。
3. Docker Compose 增加 Prometheus / Grafana 的后续方向。
4. 推荐 dashboards：
   - JVM memory。
   - HTTP latency。
   - HTTP 4xx / 5xx。
   - Model Gateway requests。
   - Token / cost。
   - GitHub API errors。

不要在本阶段强行引入新依赖，除非项目已经具备。

## 12. 运维 Runbook 要求

新增：

```text
docs/production-observability-runbook.md
docs/incident-response-runbook.md
```

### 12.1 production-observability-runbook.md

必须包含：

1. 监控入口。
2. 健康检查命令。
3. 日志查看命令。
4. 模型用量查看。
5. 审计日志查看。
6. GitHub OAuth 状态查看。
7. 数据库状态查看。
8. 备份状态查看。
9. 常见指标解释。
10. 告警规则链接。

### 12.2 incident-response-runbook.md

必须包含：

1. Frontend 访问失败。
2. Backend 502 / 5xx。
3. MySQL down。
4. 登录失败。
5. Chat SSE 异常。
6. Model Provider 失败。
7. Model cost spike。
8. GitHub OAuth callback 失败。
9. PR Review 失败。
10. 发现疑似 secret leak。
11. 回滚流程。
12. 数据库恢复流程。

## 13. 自动化验证

必须执行：

```bash
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

回到项目根目录执行：

```bash
bash scripts/prod-health-check.sh http://localhost || true
bash scripts/prod-security-check.sh http://localhost || true
bash scripts/prod-log-scan.sh || true
bash scripts/prod-alert-check.sh || true
```

如果 Docker / production Compose 可用：

```bash
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production config
bash scripts/prod-diagnostics.sh
```

## 14. 浏览器 / 手动验收

如果生产环境或本地 Docker 环境可用，检查：

1. 首页可访问。
2. 登录成功。
3. Dashboard 正常。
4. Observability 页面正常。
5. Model Usage 面板正常。
6. Audit Logs 可查询。
7. Chat SSE 正常。
8. GitHub 未配置时提示清晰。
9. Model Gateway 未配置真实 key 时 MOCK 正常。
10. Logout 正常。

## 15. 不要做的事

不要：

- 添加真实密钥。
- 暴露完整 actuator details 到公网。
- 把 `.env.production`、日志、诊断包、备份提交。
- 为了通过脚本关闭认证。
- 添加 GitHub 写操作。
- 引入复杂监控平台依赖作为硬要求。
- 大规模重构 observability 模块。

## 16. 完成报告格式

完成后按以下格式输出：

```text
Milestone 23 完成报告

1. 新增/修改文件清单
2. 生产健康检查脚本说明
3. 告警规则文档说明
4. 日志扫描与敏感信息检查
5. 安全加固检查
6. 诊断脚本说明
7. Observability / Incident Runbook 说明
8. 可选 Prometheus / Grafana 后续方案
9. 自动化测试结果
10. 生产 / 本地验证结果
11. 已知限制
12. 是否可以进入 Milestone 24：产品化 Demo 与真实用户试用
```

## 17. Claude 执行提示词

将下面内容复制给 Claude 执行：

```text
请根据项目中的文档执行 Milestone 23。

文档路径：
docs/milestone-23-observability-alerting-security-hardening.md

执行要求：
1. 先完整阅读该文档，再检查 Milestone 11、21、22 的相关代码和文档。
2. 本阶段是生产监控、告警、安全加固和运维 Runbook，不要改业务逻辑。
3. 不要改核心 API 行为，不要改数据库业务表结构。
4. 不要提交真实密钥、token、API Key、OAuth Secret、.env.production、日志、备份、诊断包。
5. 不要引入 Kubernetes、Helm、Terraform，也不要绑定特定云厂商。
6. 不要强制引入 Prometheus/Grafana/ELK/Loki；可以写可选接入文档。
7. 保持本地开发、Docker Compose、CI/CD、MOCK 模式全部可用。

需要实现：
1. 新增 docs/production-observability-runbook.md。
2. 新增 docs/production-alerting-rules.md。
3. 新增 docs/production-security-hardening-checklist.md。
4. 新增 docs/incident-response-runbook.md。
5. 新增 scripts/prod-health-check.sh。
6. 新增 scripts/prod-security-check.sh。
7. 新增 scripts/prod-log-scan.sh。
8. 新增 scripts/prod-alert-check.sh。
9. 新增 scripts/prod-diagnostics.sh。
10. 更新 docs/production-deployment-runbook.md、docs/deployment-guide.md、README.md，加入监控、告警、安全检查入口。
11. 更新 .gitignore，确保 diagnostics/、logs/、backups/、*.sql、*.dump 不被提交。
12. 检查 TraceId、GlobalExceptionHandler、ModelRequestLog、GithubClient、AuditLog 是否已具备脱敏和排障信息，如发现小问题可做小修复，但不要重构。

重点要求：
1. prod-health-check.sh 至少检查首页、登录、me、projects、agents、observability overview、model usage、github status、model providers。
2. prod-security-check.sh 至少检查 secret patterns、.env.production 未跟踪、安全响应头、CORS、actuator、未登录 401、GitHub token 不返回前端。
3. prod-log-scan.sh 必须扫描 sk-、ghp_、github_pat_、Bearer token、OPENAI_API_KEY、CLAUDE_API_KEY、GITHUB_CLIENT_SECRET 等泄露模式；发现疑似密钥泄露必须 FAIL。
4. prod-alert-check.sh 输出基于当前 API / logs / model usage 的 PASS/WARN/FAIL，不要求接入真实告警平台。
5. prod-diagnostics.sh 采集 docker ps/stats、health、logs tail、disk、memory、smoke test 输出，并自动 mask 敏感内容。
6. 所有脚本必须 set -euo pipefail，不得打印密钥。
7. 没有运行中的生产环境时，脚本应输出清晰 SKIP/WARN，而不是误报代码失败。

完成后必须执行：
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1

回到项目根目录执行：
bash scripts/prod-health-check.sh http://localhost || true
bash scripts/prod-security-check.sh http://localhost || true
bash scripts/prod-log-scan.sh || true
bash scripts/prod-alert-check.sh || true

如果 Docker / production Compose 可用，额外执行：
docker compose -f deploy/prod/docker-compose.prod.yml --env-file .env.production config
bash scripts/prod-diagnostics.sh || true

安全检查必须执行：
git status --short
rg "sk-|ghp_|github_pat_|OPENAI_API_KEY=.*[A-Za-z0-9]|CLAUDE_API_KEY=.*[A-Za-z0-9]|GITHUB_CLIENT_SECRET=.*[A-Za-z0-9]" .

完成后按以下格式输出：
1. 新增/修改文件清单
2. 生产健康检查脚本说明
3. 告警规则文档说明
4. 日志扫描与敏感信息检查
5. 安全加固检查
6. 诊断脚本说明
7. Observability / Incident Runbook 说明
8. 可选 Prometheus / Grafana 后续方案
9. 自动化测试结果
10. 生产 / 本地验证结果
11. 已知限制
12. 是否可以进入 Milestone 24：产品化 Demo 与真实用户试用

现在开始实现，不要只给计划。
```
