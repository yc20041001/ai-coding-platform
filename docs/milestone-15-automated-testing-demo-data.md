# Milestone 15: 自动化测试与演示数据固化实施文档

## 1. 背景与目标

Milestone 14 已完成前后端联调与发布准备，当前项目已经具备本地演示条件：

- 后端可启动、可编译、可测试、可打包
- 前端可启动、可 typecheck、可 build
- Auth / Project / Task / Chat SSE / RAG / Observability 主链路可联调
- README、环境变量模板、Smoke Test、发布准备清单已补齐

但当前验证仍偏人工和脚本化 smoke test，缺少系统化自动化测试与稳定演示数据。

Milestone 15 的目标是：

> 将“能演示”升级为“每次都能稳定演示”，通过后端集成测试、前端最小 E2E、演示数据脚本和一键检查脚本，固化项目质量与演示路径。

## 2. 实施边界

### 2.1 本阶段要做

- 增加后端集成测试基础能力
- 增加核心 API 集成测试
- 增加前端最小 Playwright E2E
- 固化演示数据初始化脚本
- 固化一键检查脚本
- 更新 README 与测试文档
- 确保本地环境可重复验证

### 2.2 本阶段不做

- 不新增业务模块
- 不重构已验证通过的业务逻辑
- 不接真实模型
- 不做复杂测试覆盖率门禁
- 不做完整端到端大规模测试矩阵
- 不做生产 CI/CD
- 不依赖外部 GitHub 仓库稳定性作为必选测试
- 不把真实密钥写入测试或脚本

## 3. 交付物

建议新增或修改：

```text
backend/src/test/java/com/aicoding/platform/
backend/src/test/resources/application-test.yml
backend/src/test/java/com/aicoding/platform/support/
backend/src/test/java/com/aicoding/platform/auth/AuthIntegrationTest.java
backend/src/test/java/com/aicoding/platform/project/ProjectIntegrationTest.java
backend/src/test/java/com/aicoding/platform/task/TaskOrchestratorIntegrationTest.java
backend/src/test/java/com/aicoding/platform/chat/ChatIntegrationTest.java
backend/src/test/java/com/aicoding/platform/rag/RagIntegrationTest.java

frontend/playwright.config.ts
frontend/e2e/auth.spec.ts
frontend/e2e/project-task-chat.spec.ts
frontend/e2e/knowledge-observability.spec.ts

scripts/dev-seed-demo-data.sh
scripts/dev-reset-db.sh
scripts/run-backend-checks.sh
scripts/run-frontend-checks.sh
scripts/run-all-checks.sh

docs/testing-strategy.md
docs/demo-data-guide.md
```

允许根据项目实际结构微调文件名。

## 4. 后端测试设计

### 4.1 测试技术栈

使用现有后端技术栈：

```text
JUnit 5
Spring Boot Test
MockMvc 或 TestRestTemplate
MyBatis-Plus
Flyway
MySQL 测试库
```

优先方案：

- 使用真实 MySQL 测试库，验证 Flyway、SQL、Mapper、事务和接口真实行为
- 使用 `application-test.yml` 隔离测试配置
- 使用 Mock Model Gateway，不接真实模型

### 4.2 测试配置

新增：

```text
backend/src/test/resources/application-test.yml
```

建议内容：

```yaml
spring:
  datasource:
    url: ${TEST_DB_URL:jdbc:mysql://127.0.0.1:3306/ai_coding_platform_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true}
    username: ${TEST_DB_USERNAME:aicoding}
    password: ${TEST_DB_PASSWORD:aicoding123}
  flyway:
    enabled: true
    clean-disabled: false

app:
  jwt:
    secret: ${JWT_SECRET:verification-test-secret-min-32bytes}
  model:
    provider: MOCK
```

注意：

- 测试库使用 `ai_coding_platform_test`
- 不要污染开发库 `ai_coding_platform`
- 如果需要重置库，必须只允许操作 `_test` 后缀数据库

### 4.3 测试辅助类

建议新增：

```text
backend/src/test/java/com/aicoding/platform/support/IntegrationTestBase.java
backend/src/test/java/com/aicoding/platform/support/TestAuthHelper.java
backend/src/test/java/com/aicoding/platform/support/TestJsonHelper.java
```

职责：

- 启动 SpringBootTest
- 配置 `@ActiveProfiles("test")`
- 提供登录 admin 获取 token 方法
- 提供带 Authorization header 的请求封装
- 提供 JSON 解析工具
- 可选：每个测试前清理业务数据

## 5. 后端测试用例

### 5.1 AuthIntegrationTest

覆盖：

- admin 登录成功
- 登录返回 accessToken / refreshToken / user
- `/api/auth/me` 携带 accessToken 成功
- 无 token 访问 `/api/auth/me` 返回 UNAUTHORIZED
- refreshToken 可刷新 token
- accessToken 调 refresh 返回 UNAUTHORIZED

### 5.2 ProjectIntegrationTest

覆盖：

- 创建项目成功
- 创建项目后自动生成 OWNER 成员
- 查询项目列表包含新项目
- 查询项目详情返回 currentUserRole=OWNER
- 更新项目成功
- 删除/归档项目需要 OWNER 权限

### 5.3 TaskOrchestratorIntegrationTest

覆盖：

- 创建 Task 成功，初始状态 PENDING
- 执行 Task 成功
- Task 状态变为 COMPLETED
- 生成 Task Logs
- 生成 Task Artifacts
- 生成 Agent Execution
- 生成 Model Request Log
- 重复执行 COMPLETED Task 返回 CONFLICT

### 5.4 ChatIntegrationTest

覆盖：

- 创建 Chat Session 成功
- 发送消息成功
- 创建 USER message 和 AGENT STREAMING message
- 查询消息列表成功
- 可选：SSE stream 完成后 AGENT message 变为 COMPLETED
- 无 token 访问返回 UNAUTHORIZED

### 5.5 RagIntegrationTest

覆盖：

- 创建 Knowledge Base 成功
- 上传 Markdown 文档成功
- 自动生成 Document Chunk
- RAG Search 命中返回结果
- 不支持 PDF 上传返回 BAD_REQUEST
- 无项目权限返回 PROJECT_ACCESS_DENIED

## 6. 前端 E2E 设计

### 6.1 技术栈

使用：

```text
Playwright
Vue 3
Vite dev server
```

新增依赖：

```bash
cd frontend
npm install -D @playwright/test
npx playwright install chromium
```

### 6.2 Playwright 配置

新增：

```text
frontend/playwright.config.ts
```

建议：

- baseURL: `http://localhost:5173`
- webServer 可选：
  - frontend dev server
  - 或要求用户先启动前后端
- retries: CI 中 1，本地 0
- browser: chromium
- trace: retain-on-failure
- screenshot: only-on-failure

### 6.3 E2E 用例

#### auth.spec.ts

覆盖：

- 打开首页跳转登录页
- 输入 admin@example.com / Admin@123456
- 登录成功跳转 Dashboard
- 点击退出回到登录页

#### project-task-chat.spec.ts

覆盖：

- 创建项目
- 进入项目详情
- 创建任务
- 执行任务
- 打开任务详情
- 查看 Logs / Artifacts / Executions
- 进入 Chat Tab
- 创建 Chat Session
- 发送消息
- 观察流式回复完成

#### knowledge-observability.spec.ts

覆盖：

- 进入 Knowledge Tab
- 创建 Knowledge Base
- 上传 Markdown 文档
- 执行 RAG Search
- 进入 Observability 页面
- 查看 Overview / Audit Logs

### 6.4 E2E 注意事项

- 使用稳定选择器，必要时新增 `data-testid`
- 不依赖文本模糊匹配过多 UI 细节
- 避免依赖外部网络
- 避免测试 Repository clone/pull 作为必选项
- 每个测试生成唯一项目名，例如 `E2E Project ${Date.now()}`

## 7. 演示数据固化

### 7.1 目标

提供一套可重复生成的演示数据：

- Demo Project
- Demo Knowledge Base
- Demo Markdown Document
- Demo Chat Session
- Demo Task
- Demo Task Execution
- Demo Audit Logs

### 7.2 dev-seed-demo-data.sh

新增：

```text
scripts/dev-seed-demo-data.sh
```

职责：

1. 登录 admin
2. 创建或复用 Demo Project
3. 创建或复用 Demo Knowledge Base
4. 上传 Demo Markdown 文档
5. 创建 Chat Session
6. 发送 Chat Message，可选触发 SSE
7. 创建 Task
8. 执行 Task
9. 输出 demo projectId、taskId、chatSessionId、knowledgeBaseId

要求：

- 尽量幂等
- 不重复创建过多脏数据
- 输出清晰
- 失败时打印 HTTP 响应

### 7.3 dev-reset-db.sh

新增：

```text
scripts/dev-reset-db.sh
```

职责：

- 仅在本地开发环境使用
- 删除并重建开发库或测试库
- 重新启动后端由 Flyway 自动迁移

安全要求：

- 必须检测数据库名包含 `ai_coding_platform` 或 `_test`
- 不允许对未知数据库执行 drop
- 执行前打印警告
- 默认需要用户确认，或提供 `--yes`

## 8. 一键检查脚本

### 8.1 run-backend-checks.sh

新增：

```text
scripts/run-backend-checks.sh
```

执行：

```bash
cd backend
mvn clean compile
mvn test
mvn package -DskipTests
```

### 8.2 run-frontend-checks.sh

新增：

```text
scripts/run-frontend-checks.sh
```

执行：

```bash
cd frontend
npm install
npm run typecheck
npm run build
npm run test:e2e
```

如未安装 Playwright 浏览器，给出清晰提示。

### 8.3 run-all-checks.sh

新增：

```text
scripts/run-all-checks.sh
```

执行：

1. 后端 compile/test/package
2. 前端 typecheck/build
3. 后端 smoke test
4. 可选前端 E2E

要求：

- 任一步失败立即退出
- 输出分段清晰
- 最后打印总结果

## 9. 文档更新

### 9.1 docs/testing-strategy.md

新增内容：

- 测试分层
- 后端集成测试说明
- 前端 E2E 说明
- Smoke Test 说明
- 测试数据库说明
- 本地执行方式
- CI 接入建议

### 9.2 docs/demo-data-guide.md

新增内容：

- Demo 数据说明
- 如何初始化 Demo 数据
- Demo 账号
- Demo Project 内容
- Demo Knowledge 文档内容
- Demo 演示路径
- 如何清理 Demo 数据

### 9.3 README.md

补充：

- 测试命令
- 一键检查脚本
- Demo 数据脚本
- Playwright 安装说明

## 10. 验证命令

后端：

```bash
cd backend
mvn clean compile
mvn test
mvn package -DskipTests
```

前端：

```bash
cd frontend
npm install
npm run typecheck
npm run build
npm run test:e2e
```

脚本：

```bash
scripts/run-backend-checks.sh
scripts/run-frontend-checks.sh
scripts/run-all-checks.sh
scripts/dev-seed-demo-data.sh
```

## 11. 验收标准

### 11.1 必须通过

- 后端 `mvn clean compile` 成功
- 后端 `mvn test` 成功，新增集成测试通过
- 后端 `mvn package -DskipTests` 成功
- 前端 `npm run typecheck` 成功
- 前端 `npm run build` 成功
- 前端 Playwright 最小 E2E 成功
- `scripts/dev-seed-demo-data.sh` 可生成演示数据
- `scripts/run-all-checks.sh` 可完成主检查流程
- README 包含测试与演示数据说明

### 11.2 不允许出现

- 测试依赖真实模型 API Key
- 测试依赖外部 GitHub 仓库稳定性
- 测试脚本写死本机私有路径
- 脚本误删非项目数据库
- E2E 大面积依赖不稳定 UI 文案
- Demo 数据脚本重复制造大量脏数据
- 测试绕过真实权限链路

## 12. 完成报告模板

```markdown
# Milestone 15 完成报告

## 1. 新增/修改文件清单

| 文件 | 说明 |
|---|---|
|  |  |

## 2. 后端集成测试

| 测试类 | 覆盖内容 | 结果 |
|---|---|---|
| AuthIntegrationTest |  |  |
| ProjectIntegrationTest |  |  |
| TaskOrchestratorIntegrationTest |  |  |
| ChatIntegrationTest |  |  |
| RagIntegrationTest |  |  |

## 3. 前端 E2E 测试

| 测试文件 | 覆盖内容 | 结果 |
|---|---|---|
| auth.spec.ts |  |  |
| project-task-chat.spec.ts |  |  |
| knowledge-observability.spec.ts |  |  |

## 4. 演示数据脚本

| 脚本 | 结果 | 输出 |
|---|---|---|
| dev-seed-demo-data.sh |  |  |
| dev-reset-db.sh |  |  |

## 5. 一键检查脚本

| 脚本 | 结果 |
|---|---|
| run-backend-checks.sh |  |
| run-frontend-checks.sh |  |
| run-all-checks.sh |  |

## 6. 构建验证

| 命令 | 结果 |
|---|---|
| backend mvn clean compile |  |
| backend mvn test |  |
| backend mvn package -DskipTests |  |
| frontend npm run typecheck |  |
| frontend npm run build |  |
| frontend npm run test:e2e |  |

## 7. 修复的问题

- 

## 8. 已知限制

- 

## 9. 结论

是否具备稳定本地演示条件：

- [ ] 是
- [ ] 否
```

## 13. 给 Claude 的执行提示词

可以直接发送以下内容给 Claude：

```text
请根据项目中的文档执行 Milestone 15：自动化测试与演示数据固化。

文档路径：
docs/milestone-15-automated-testing-demo-data.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend、frontend、scripts、docs 目录结构。
2. 本阶段是在 Milestone 14 已完成前后端联调与发布准备基础上，补齐自动化测试、演示数据和一键检查脚本。
3. 不新增业务模块。
4. 不重构已验证通过的后端核心逻辑。
5. 不重写前端工程。
6. 不接真实模型，不依赖真实 API Key。
7. 不依赖外部 GitHub 仓库稳定性作为必选测试。
8. 不写死本机私有路径。
9. 不提交真实密钥。
10. 可以修复测试过程中发现的明确 bug，但必须说明原因和影响范围。
11. 后端测试应尽量走真实 API / 权限 / 状态流转，不要绕过业务链路。
12. 前端 E2E 使用稳定选择器，必要时可以给组件补 data-testid。
13. 演示数据脚本要尽量幂等，不要重复制造大量脏数据。
14. 数据库重置脚本必须有安全保护，只允许操作项目开发库或测试库。

需要实现：
1. 后端 application-test.yml。
2. 后端集成测试基类和测试辅助工具。
3. AuthIntegrationTest。
4. ProjectIntegrationTest。
5. TaskOrchestratorIntegrationTest。
6. ChatIntegrationTest。
7. RagIntegrationTest。
8. 前端 Playwright 配置。
9. 前端最小 E2E：登录、项目任务、Chat、Knowledge、Observability。
10. scripts/dev-seed-demo-data.sh。
11. scripts/dev-reset-db.sh。
12. scripts/run-backend-checks.sh。
13. scripts/run-frontend-checks.sh。
14. scripts/run-all-checks.sh。
15. docs/testing-strategy.md。
16. docs/demo-data-guide.md。
17. 更新 README.md，补充测试与演示数据说明。

完成后必须执行：
后端：
cd backend
mvn clean compile
mvn test
mvn package -DskipTests

前端：
cd frontend
npm run typecheck
npm run build
npm run test:e2e

脚本：
scripts/run-backend-checks.sh
scripts/run-frontend-checks.sh
scripts/dev-seed-demo-data.sh

完成后按以下格式输出：
1. 新增/修改文件清单
2. 后端集成测试实现与结果
3. 前端 E2E 实现与结果
4. 演示数据脚本实现与结果
5. 一键检查脚本实现与结果
6. 构建验证结果
7. 修复的问题与原因
8. 已知限制
9. 是否具备稳定本地演示条件
10. 是否可以进入下一阶段

现在开始执行，不要只给计划。
```
