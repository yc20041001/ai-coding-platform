# Milestone 33C-Fix: E2E Captcha Environment Stabilization

## 1. 背景

Milestone 33C 已完成 Agent Version Management UI：

- 后端 Agent Version API 已完成。
- 前端 Agent Version Drawer 已完成。
- Project Agent 启用弹窗已支持选择 PUBLISHED 版本。
- 后端 `mvn test` 通过 205 个测试。
- 前端 typecheck / build 通过。

但 E2E 验证仍有环境问题：

```text
当前 Docker 后端容器默认启用 AUTH_CAPTCHA_ENABLED=true，
导致 Playwright 登录 helper 不能直接登录，E2E 登录失败。
```

这是环境配置问题，不是 33C 功能问题。若不收口，后续每个前端 E2E 都会重复遇到验证码阻塞。

## 2. 目标

将 E2E / Docker 测试环境中的认证安全开关固定下来：

1. E2E 后端容器默认关闭验证码。
2. E2E 后端容器默认关闭登录失败锁定。
3. 保持生产 / 本地正常运行时验证码默认开启。
4. 更新脚本或 Compose 配置，避免手工 docker run 传一长串环境变量。
5. 重新跑完整 E2E，确认 33C 版本管理链路通过。

## 3. 严格边界

本阶段只修 E2E 环境配置。

必须遵守：

1. 不改验证码业务逻辑。
2. 不删除验证码功能。
3. 不让生产环境默认关闭验证码。
4. 不改 Auth / JWT / Security 架构。
5. 不跳过 E2E 登录。
6. 不删除 E2E 断言。
7. 不改 Agent Version 业务逻辑。
8. 不改 Project Agent Config 业务逻辑。
9. 不接真实模型 API。

允许做：

- 修改 Docker Compose app/test 环境变量。
- 修改 E2E 启动脚本。
- 修改 README / testing docs。
- 修改 Playwright 配置中的 baseURL 或环境说明。
- 新增轻量脚本用于启动 E2E 后端。

## 4. 推荐修复点

请检查以下文件，按项目实际情况最小修改：

```text
deploy/docker-compose.app.yml
scripts/run-frontend-checks.sh
scripts/docker-smoke-test.sh
scripts/release-demo-check.sh
frontend/playwright.config.ts
docs/testing-strategy.md
README.md
```

### 4.1 Docker Compose E2E 环境

如果 `deploy/docker-compose.app.yml` 被 E2E 复用，backend service 建议增加：

```yaml
environment:
  AUTH_CAPTCHA_ENABLED: "false"
  AUTH_LOGIN_PROTECTION_ENABLED: "false"
```

注意：

- 这只适用于 local demo / E2E compose。
- 生产 compose `deploy/prod/docker-compose.prod.yml` 不应默认关闭验证码。

### 4.2 独立 E2E 后端脚本

如果不希望修改 app compose，可以新增脚本：

```text
scripts/start-e2e-backend.sh
```

职责：

- 删除旧测试容器。
- 使用最新 `ai-coding-platform-backend:local` 镜像启动后端。
- 自动配置：

```env
AUTH_CAPTCHA_ENABLED=false
AUTH_LOGIN_PROTECTION_ENABLED=false
SPRING_DATA_REDIS_HOST=ai-coding-platform-redis
SPRING_DATA_REDIS_PORT=6379
```

- 等待 `/actuator/health` UP。
- 输出后端地址。

示例：

```bash
docker rm -f ai-coding-platform-backend-test || true
docker run -d --name ai-coding-platform-backend-test \
  --network deploy_default \
  -p 9080:8080 \
  -e AUTH_CAPTCHA_ENABLED=false \
  -e AUTH_LOGIN_PROTECTION_ENABLED=false \
  -e SPRING_DATA_REDIS_HOST=ai-coding-platform-redis \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e REDIS_HOST=ai-coding-platform-redis \
  -e 'DB_URL=jdbc:mysql://ai-coding-platform-mysql:3306/ai_coding_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=platform123 \
  ai-coding-platform-backend:local
```

### 4.3 Frontend E2E Base URL

确认：

```text
frontend/playwright.config.ts
```

支持：

```env
PLAYWRIGHT_BASE_URL=http://localhost:5173
```

如果前端 dev server 代理 `/api` 到 `localhost:9080`，确认 `vite.config.ts` 或 `.env` 中 API base 对应测试后端。

### 4.4 文档更新

至少更新：

```text
docs/testing-strategy.md
```

说明：

- E2E 环境验证码关闭。
- 生产环境验证码默认开启。
- 关闭原因：E2E 登录 helper 不应依赖人工识别验证码。
- 验证码业务逻辑由后端单元/集成测试覆盖。

## 5. 验证要求

必须执行：

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如使用 Docker E2E 后端：

```bash
docker build -t ai-coding-platform-backend:local ./backend
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

期望：

```text
Backend tests: PASS
Frontend typecheck: PASS
Frontend build: PASS
E2E: all passed
```

## 6. 验收标准

功能验收：

- E2E 登录不再被验证码阻塞。
- Agent Version E2E 可以执行。
- Project Agent Config E2E 可以执行。
- 后端验证码业务测试仍然通过。

安全验收：

- 生产配置没有默认关闭验证码。
- `.env.production.example` 不建议关闭验证码。
- 文档明确 E2E 关闭验证码仅用于自动化测试。

回归验收：

- 33A / 33B / 33C E2E 都能跑。
- 后端 `mvn test` 通过。
- 前端 typecheck / build 通过。

## 7. 完成报告格式

完成后按以下格式输出：

1. 根因确认
2. 修改文件清单
3. E2E 后端环境配置说明
4. 安全边界说明
5. 后端测试结果
6. 前端 typecheck / build / E2E 结果
7. 手动验证结果
8. 已知限制
9. 是否可以进入 Milestone 33D

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 33C-Fix。

文档路径：

```text
docs/milestone-33c-fix-e2e-captcha-environment.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 Docker / E2E / frontend 配置。
2. 本阶段只修 E2E 环境验证码配置，不要改验证码业务逻辑。
3. 不要让生产环境默认关闭验证码。
4. 不要删除任何 E2E 断言。
5. 不要跳过登录测试。
6. 不要改 Agent Version 或 Project Agent Config 业务逻辑。
7. 如果需要新增脚本，脚本必须 set -euo pipefail。
8. 文档里要明确：E2E 关闭验证码，生产默认开启验证码。

需要完成：

1. 固定 E2E 后端环境变量：
   - `AUTH_CAPTCHA_ENABLED=false`
   - `AUTH_LOGIN_PROTECTION_ENABLED=false`
2. 根据项目实际情况，修改 compose 或新增 `scripts/start-e2e-backend.sh`。
3. 确认 Redis host 环境变量正确：
   - `SPRING_DATA_REDIS_HOST`
   - `SPRING_DATA_REDIS_PORT`
4. 更新 `docs/testing-strategy.md` 或 README 中的 E2E 说明。
5. 运行完整验证：

```bash
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

完成后按文档第 7 节格式输出报告。

现在开始实现，不要只给计划。
