# Testing Strategy

> **Final Delivery Package**: See [Final Delivery Report](final-delivery-report.md) for quality gate summary, [Backend Test Matrix](backend-test-matrix.md) for full coverage details, and [Backend Testing Guide](backend-testing-guide.md) for test patterns and troubleshooting.

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
- 真实 MySQL 测试库 (`ai_coding_platform`)
- Flyway 默认禁用（test profile），schema 由 `spring.sql.init` 管理
- MOCK Model Gateway（不依赖真实 API Key）

### 测试覆盖矩阵

详见 [Backend Test Matrix](backend-test-matrix.md)。

| 测试类 | 类型 | 测试数 | 覆盖模块 |
|--------|------|--------|----------|
| `AuthIntegrationTest` | 集成 | 7 | Auth / JWT / 认证边界 |
| `JwtTokenProviderTest` | 单元 | 15 | JWT token 类型、验证、解析 |
| `ProjectIntegrationTest` | 集成 | 5 | 项目 CRUD / 权限 |
| `TaskOrchestratorIntegrationTest` | 集成 | 4 | 任务创建 / 执行 / 重复执行拦截 |
| `TaskStateMachineTest` | 单元 | 20 | 状态机合法/非法流转 / 边界 |
| `ChatIntegrationTest` | 集成 | 4 | 会话创建 / 消息发送 / SSE 状态 |
| `RagIntegrationTest` | 集成 | 4 | 知识库 / 文档上传 / chunk / 搜索 |
| `DocumentChunkServiceTest` | 单元 | 14 | Chunk 分片 / overlap / hash / token 估算 |
| `PromptSafetyServiceTest` | 单元 | 9 | Prompt 安全过滤 / 高危拦截 / 警告 |
| `ModelSecretMaskingServiceTest` | 单元 | 15 | API Key 掩码 / 日志脱敏 |
| `ModelPricingServiceTest` | 单元 | 12 | 成本估算 / 覆盖定价 / 所有模型 |
| `ModelGatewayIntegrationTest` | 集成 | 7 | MOCK Provider / 配置验证 |
| `AgentProjectConfigIntegrationTest` | 集成 | 23 | 项目 Agent 配置 CRUD / 启用停用 / 版本选择 / 模型配置校验 |
| `AgentVersionIntegrationTest` | 集成 | 10 | Agent 版本列表 / 版本详情 / 权限校验 / 版本归属校验 |
| `PrReviewApplicationServiceTest` | 单元 | 23 | JSON 解析 / 风险评估 / Prompt 构建 / 异常输出 |
| `GithubPropertiesTest` | 单元 | 4 | GitHub OAuth 配置检查 |

### 后端质量门

以下质量门在发布前**必须通过**：

| 门禁 | 类型 | 说明 |
|------|------|------|
| `mvn test` | **Blocking** | 所有后端测试必须通过 |
| `mvn compile` | **Blocking** | 编译通过 |
| `mvn package -DskipTests` | **Blocking** | 可打包 |

后端测试失败阻塞发布。`scripts/run-backend-checks.sh` 串行执行 compile → test → package，任一步失败即退出。

### 测试辅助工具

[`TestDataFactory.java`](../backend/src/test/java/com/aicoding/platform/support/TestDataFactory.java) — 测试数据工厂，生成唯一名称的测试数据。

[`TestJsonHelper.java`](../backend/src/test/java/com/aicoding/platform/support/TestJsonHelper.java)：
- `parse(json)` → `JsonNode`
- `getString(root, "data.id")` — 点号分隔路径提取
- `getLong(root, path)` / `getBool(root, path)`

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
| `auth.spec.ts` | 未登录跳转登录页、admin 登录成功、登出、错误密码提示、未登录根路径跳转 /public、验证码展示 |
| `project-task-chat.spec.ts` | 创建项目、创建+执行任务(COMPLETED)、Chat 会话创建+消息发送+SSE 流完成 |
| `knowledge-observability.spec.ts` | Knowledge Tab 导航、RAG 搜索、可观测性页面可访问 |
| `model-gateway.spec.ts` | 模型网关页面导航、Provider 区域可见 |
| `project-agent-config.spec.ts` | 项目 Agent 配置表格、启用弹窗(Model Config 下拉)、启用/停用、权限错误、JS 错误检测 |
| `agent-version.spec.ts` | Agent 版本抽屉、版本列表、systemPrompt/toolPolicy/executionPolicy 详情、启用弹窗版本选择 |

### 稳定选择器策略

严格遵守选择器优先级：

1. **`page.getByTestId()`** — 首选，所有关键元素必须有 `data-testid`
2. **`page.getByRole()`** — 标准 HTML 元素
3. **`page.getByLabel()`** — 表单输入
4. **`page.getByText()`** — 唯一可见文本
5. **`page.getByPlaceholder()`** — 输入占位符
6. **`page.locator('css')`** — 仅限结构性元素（如 `.app-shell`）

**禁止使用：**
- Element Plus 内部 CSS 类名（`.el-input__inner`, `.el-button--primary`）
- `.nth()` 不加稳定过滤的链式调用
- 固定 `waitForTimeout`（除非明确注解原因）

### data-testid 命名规范

| 元素类型 | 命名模式 | 示例 |
|---------|---------|------|
| 操作按钮 | `btn-<action>-<entity>` | `btn-create-project`, `btn-submit-task` |
| 输入框 | `input-<entity>-<field>` | `input-project-name`, `input-task-title` |
| 对话框 | `dialog-<action>-<entity>` | `dialog-create-project`, `dialog-execute-task` |
| 表格 | `<entity>-table` | `project-table`, `task-table` |
| 列表容器 | `<entity>-<container-type>` | `chat-session-list` |
| 下拉选择 | `select-<entity>-<field>` | `select-task-type` |

### 等待策略

| 场景 | 推荐策略 | 超时 |
|------|---------|------|
| API 写操作完成 | `waitForResponse(url pattern + method + status)` | 15-30s |
| 路由跳转完成 | `expect(page).toHaveURL(/pattern/)` | 8-10s |
| 对话框打开 | `expect(getByTestId('dialog-...')).toBeVisible()` | 5s |
| 对话框关闭 | `expect(getByTestId('dialog-...')).not.toBeVisible()` | 8s |
| 表格数据更新 | `expect(getByTestId('...-table')).toContainText(name)` | 8s |
| SSE 流完成 | `expect(streaming-indicator).not.toBeVisible()` | 30s |

### 测试数据隔离

- 所有测试数据使用唯一后缀：`const SUFFIX = Date.now().toString()`
- 不依赖 Demo 数据（Demo AI Workspace 等）必须存在
- 测试前通过 admin 登录即可，不依赖预置项目/任务/会话

### E2E 后端环境配置

E2E 测试需要后端关闭验证码和登录保护，原因：

- Playwright 登录 helper 不应依赖人工识别验证码。
- 登录失败锁定（`AUTH_LOGIN_PROTECTION_ENABLED`）在 E2E 重试或频繁运行时可能误锁测试账号。
- 验证码和登录保护的业务逻辑已由后端单元/集成测试覆盖，E2E 无需再测。

**安全边界**：
- 此配置仅用于本地/CI 自动化测试。
- **生产部署配置（`deploy/prod/docker-compose.prod.yml`）保持验证码默认开启**。
- `.env.example` 中验证码默认值为 `true`，不更改。
- E2E 后端容器仅绑定到本地 9080 端口，不对外暴露。

使用专用脚本启动 E2E 后端：

```bash
# 构建镜像并启动 E2E 后端容器
bash scripts/start-e2e-backend.sh
```

该脚本自动配置：
- `AUTH_CAPTCHA_ENABLED=false`
- `AUTH_LOGIN_PROTECTION_ENABLED=false`
- Redis / MySQL 连接指向 Docker Compose 网络中的服务容器
- 等待 `/actuator/health` 返回 UP 后提示就绪

### 运行

```bash
# 安装 Playwright（首次）
cd frontend
npm install -D @playwright/test
npx playwright install chromium

# 启动 E2E 后端（使用 Docker）
bash scripts/start-e2e-backend.sh

# 或启动后端（本地开发）
cd backend && mvn spring-boot:run

# 运行 E2E 测试
cd frontend && npm run test:e2e -- --workers=1

# 或使用脚本
bash scripts/run-frontend-checks.sh
```

### 发布质量门

以下质量门在发布前**必须通过**：

| 门禁 | 类型 | 说明 |
|------|------|------|
| TypeCheck | Blocking | `cd frontend && npm run typecheck` |
| Build | Blocking | `cd frontend && npm run build` |
| E2E (Run 1) | Blocking | 13/13 通过，`--workers=1` |
| E2E (Run 2) | Blocking | 13/13 通过（稳定性验证） |
| Bundle Check | Warning | `scripts/frontend-bundle-check.sh` — 超过预算 WARN，不阻塞 |

E2E 测试失败阻塞发布。以下情况阻塞发布：
- `auth.spec.ts` 任何用例失败
- `project-task-chat.spec.ts` 任何用例失败
- 其他 spec 失败需评估是否为环境问题

Bundle 体积检查当前为 Warning 级别，不阻塞发布。后续 Milestone 将逐步收紧为 Blocking。

详见：
- [E2E Stability Guide](e2e-stability-guide.md)
- [Frontend Performance Budget](frontend-performance-budget.md)
- [Bundle Analysis Report](bundle-analysis-report.md)

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
