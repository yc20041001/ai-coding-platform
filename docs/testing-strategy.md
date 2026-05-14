# Testing Strategy

## 测试分层

本项目采用三层测试策略：

| 层级 | 技术 | 范围 | 位置 |
|------|------|------|------|
| 后端集成测试 | JUnit 5 + Spring Boot Test + TestRestTemplate | Auth / Project / Task / Chat / RAG API | `backend/src/test/` |
| 前端 E2E | Playwright | 登录 / 项目 / 任务 / Chat / 知识库 / 可观测性 | `frontend/e2e/` |
| Smoke Test | Shell 脚本 (curl) | 全链路冒烟 | `scripts/` |

## 后端集成测试

### 技术栈

- JUnit 5 + Spring Boot Test
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `@ActiveProfiles("test")`
- `TestRestTemplate` — 真实 HTTP 请求
- 真实 MySQL 测试库 (`ai_coding_platform_test`)
- Flyway 自动迁移
- MOCK Model Gateway（不依赖真实 API Key）

### 测试配置

测试配置文件：[`backend/src/test/resources/application-test.yml`](../backend/src/test/resources/application-test.yml)

关键配置：
- 数据库：`${TEST_DB_URL:jdbc:mysql://127.0.0.1:3306/ai_coding_platform_test}`
- 模型网关：`MOCK`
- JWT Secret：环境变量注入，测试环境有默认值

### 测试基类

[`IntegrationTestBase.java`](../backend/src/test/java/com/aicoding/platform/support/IntegrationTestBase.java) 提供：

- 端口注入 (`@LocalServerPort`)
- `TestRestTemplate` 自动装配
- 懒加载 admin token（`AtomicReference` 缓存）
- 便捷方法：`post()`, `get()`, `put()`, `delete()` 自动带 Auth header
- `getNoAuth()` — 无认证请求
- `assertOk()`, `assertCode()` — 状态码断言

### 测试辅助工具

[`TestJsonHelper.java`](../backend/src/test/java/com/aicoding/platform/support/TestJsonHelper.java)：

- `parse(json)` → `JsonNode`
- `getString(root, "data.id")` — 点号分隔路径提取
- `getLong(root, path)` / `getBool(root, path)`

### 测试覆盖

| 测试类 | 测试数 | 覆盖内容 |
|--------|--------|----------|
| `AuthIntegrationTest` | 7 | 登录成功、获取当前用户、Token 刷新、错误密码、缺失邮箱、无认证拦截 |
| `ProjectIntegrationTest` | 5 | 创建项目、项目详情、项目概览、成员列表、无认证拦截 |
| `TaskOrchestratorIntegrationTest` | 4 | 创建任务(PENDING)、执行任务(COMPLETED)、日志/制品/执行记录/模型日志、重复执行拦截(CONFLICT)、无认证拦截 |
| `ChatIntegrationTest` | 4 | 创建会话、发送消息(用户+助手消息)、会话列表、无认证拦截 |
| `RagIntegrationTest` | 4 | 创建知识库、上传文档+自动分块、RAG 搜索命中、无认证拦截 |

### 运行

```bash
# 确保测试数据库存在
mysql -u root -e "CREATE DATABASE IF NOT EXISTS ai_coding_platform_test CHARACTER SET utf8mb4"

# 运行后端测试
cd backend
mvn test

# 或使用脚本
bash scripts/run-backend-checks.sh
```

## 前端 E2E 测试

### 技术栈

- Playwright (Chromium)
- `@playwright/test`

### 配置文件

[`frontend/playwright.config.ts`](../frontend/playwright.config.ts)：
- `baseURL: http://localhost:5173`
- `webServer` 自动启动前端 dev server
- `reuseExistingServer: true`
- `trace: retain-on-failure`
- `screenshot: only-on-failure`

### 测试覆盖

| 测试文件 | 覆盖内容 |
|----------|----------|
| `auth.spec.ts` | 未登录跳转登录页、admin 登录成功、登出、错误密码提示 |
| `project-task-chat.spec.ts` | 创建项目、创建+执行任务(COMPLETED)、Chat Tab + 会话创建 |
| `knowledge-observability.spec.ts` | 创建知识库、RAG 搜索、可观测性页面可访问 |

### 稳定选择器策略

- 优先使用 `data-testid` 属性：`[data-testid="login-email"]`
- 次要使用按钮文本：`button:has-text("新建项目")`
- 避免依赖动态内容：使用 `Date.now()` 生成唯一名称

### 运行

```bash
# 安装 Playwright（首次）
cd frontend
npm install -D @playwright/test
npx playwright install chromium

# 启动后端（另一个终端）
cd backend && mvn spring-boot:run

# 运行 E2E 测试
cd frontend && npm run test:e2e

# 或使用脚本
bash scripts/run-frontend-checks.sh
```

## Smoke Test

[`backend-unified-smoke-test.sh`](../scripts/backend-unified-smoke-test.sh) — 全链路冒烟测试，覆盖健康检查、认证、项目、Agent、RAG、Chat SSE、Task 执行、模型网关等。

[`frontend-smoke-test-plan.md`](../docs/frontend-smoke-test-plan.md) — 前端手动冒烟测试计划，12 个业务场景 + 5 项非功能检查。

## 一键检查脚本

| 脚本 | 内容 |
|------|------|
| `scripts/run-backend-checks.sh` | 编译 → 测试 → 打包 |
| `scripts/run-frontend-checks.sh` | 安装依赖 → 类型检查 → 构建 → E2E |
| `scripts/run-all-checks.sh` | 后端检查 → 前端检查，任一步失败立即退出 |

## CI 接入建议

```yaml
# GitHub Actions 示例
- name: Backend Tests
  run: |
    cd backend
    mvn clean compile
    mvn test
    mvn package -DskipTests

- name: Frontend Tests
  run: |
    cd frontend
    npm ci
    npm run typecheck
    npm run build
    npx playwright test
```

建议使用 MySQL 服务容器作为 CI 测试数据库，确保 Flyway 迁移可执行。
