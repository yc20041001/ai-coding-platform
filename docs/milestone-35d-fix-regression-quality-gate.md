# Milestone 35D-Fix: Regression Quality Gate Closure

## 1. 背景

Milestone 35D 已完成 Workflow Strategy Template：

- `WorkflowStrategyCatalogService`
- `GET /api/multi-agent-strategies`
- 4 个内置 strategy：
  - `STANDARD_DELIVERY`
  - `BACKEND_FOCUSED`
  - `FRONTEND_FOCUSED`
  - `REVIEW_ONLY`
- Legacy strategy 映射：
  - `DEFAULT_MOCK → STANDARD_DELIVERY`
  - `PHASED_PARALLEL_MOCK → STANDARD_DELIVERY`
- 前端 Multi-Agent 面板支持 strategy 下拉与模板预览。

35D 局部验证结果良好：

- Multi-Agent Orchestration 相关后端测试通过。
- 前端 typecheck / build 通过。
- E2E 测试已新增并可运行。

但 35D 报告中仍有一个全量回归问题：

```text
AgentProjectConfigIntegrationTest.shouldDisableAgentSuccessfully
duplicate user key
```

该问题虽然与 35D 功能无关，但会影响全项目质量门，因此需要单独做一个小修复轮。

## 2. 总目标

Milestone 35D-Fix 的目标是：

1. 修复 `AgentProjectConfigIntegrationTest` 中的 duplicate user key。
2. 检查测试数据创建方式，避免固定用户 ID / 邮箱 / 用户名导致冲突。
3. 跑全量后端测试。
4. 跑前端 typecheck / build。
5. 启动 E2E 后端环境。
6. 跑全量 E2E。
7. 确认 35D 质量门全量收口。

本阶段不新增业务功能，只做测试稳定性和质量门收口。

## 3. 严格边界

必须遵守：

1. 不改 35D 功能逻辑，除非测试暴露真实 bug。
2. 不改 Multi-Agent Strategy 行为。
3. 不改已有 API contract。
4. 不改前端 UI 功能。
5. 不跳过失败测试。
6. 不删除测试断言。
7. 不用 `@Disabled` 规避问题。
8. 不通过放宽生产代码约束来适配测试。
9. 不重置用户本地数据库。
10. 不执行破坏性数据库操作。

允许做：

- 修改测试数据工厂。
- 修改集成测试中的用户创建方式。
- 使用时间戳 / UUID 生成唯一邮箱和用户名。
- 在测试前清理测试自己创建的数据。
- 将重复的 test user 创建逻辑抽到 helper。
- 修复 E2E 稳定性等待条件。
- 更新测试文档。

## 4. 重点问题定位

### 4.1 已知失败

失败测试：

```text
AgentProjectConfigIntegrationTest.shouldDisableAgentSuccessfully
```

错误：

```text
duplicate user key
```

可能原因：

1. 测试中固定插入相同 user id。
2. 测试中固定插入相同 email。
3. 测试中固定插入相同 username。
4. `@BeforeEach` 每次创建第二用户但没有清理。
5. 测试数据库共享 dev/test 数据，历史残留导致唯一索引冲突。

### 4.2 推荐修复方向

不要使用固定用户字段：

```java
String suffix = String.valueOf(System.nanoTime());
user.setEmail("agent-test-" + suffix + "@example.com");
user.setUsername("agent-test-" + suffix);
```

如果项目已有 `TestDataFactory`，优先复用或增强。

推荐 helper：

```java
private Long createUniqueTestUser(String roleCode) {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    // insert user
    // insert user_role
    // return userId
}
```

注意：

- 不要硬编码 userId。
- 不要硬编码 email。
- 不要硬编码 username。
- 如果必须指定 role，按现有 role 表查询或复用现有 helper。

## 5. 后端修复要求

### 5.1 检查文件

重点检查：

```text
backend/src/test/java/com/aicoding/platform/agent/AgentProjectConfigIntegrationTest.java
backend/src/test/java/com/aicoding/platform/support/TestDataFactory.java
backend/src/test/java/com/aicoding/platform/support/IntegrationTestBase.java
```

同时搜索固定测试用户：

```bash
rg -n "test.*@|example.com|setEmail|setUsername|setId|userId|user_role" backend/src/test/java
```

### 5.2 修复策略

至少完成：

1. `AgentProjectConfigIntegrationTest` 中第二用户创建改为唯一。
2. 如果有固定 user_role 插入，也必须避免重复。
3. 如果存在测试共享 helper，统一修复 helper。
4. 确保测试可重复运行：

```bash
cd backend
mvn -Dtest=AgentProjectConfigIntegrationTest test
mvn -Dtest=AgentProjectConfigIntegrationTest test
```

连续运行两次都必须通过。

### 5.3 全量回归

必须执行：

```bash
cd backend
mvn test
```

目标：

```text
BUILD SUCCESS
0 failures
0 errors
```

## 6. 前端 / E2E 收口要求

35D 已新增 Strategy 相关 E2E。

35D-Fix 需要执行全量 E2E：

```bash
cd frontend
npm run typecheck
npm run build
```

如果后端 E2E 环境未启动：

```bash
bash scripts/start-e2e-backend.sh
```

然后：

```bash
cd frontend
npm run test:e2e -- --workers=1
```

要求：

```text
全量 E2E 通过
```

如果还有 flaky：

1. 不允许删除测试。
2. 优先修复等待条件。
3. 使用稳定选择器 `data-testid`。
4. 等 API 响应时使用 `waitForResponse`。
5. 避免基于固定时间的硬等待，除非是短暂 UI 渲染缓冲且有注释。

## 7. 额外检查

### 7.1 端口和代理

确认前端 E2E 代理指向正确后端。

检查：

```text
frontend/vite.config.ts
```

本地开发建议：

```ts
target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080'
```

或 E2E 使用脚本显式设置。

### 7.2 Captcha

E2E 后端必须关闭验证码：

```text
AUTH_CAPTCHA_ENABLED=false
AUTH_LOGIN_PROTECTION_ENABLED=false
```

确认：

```text
scripts/start-e2e-backend.sh
```

仍然设置这些变量。

### 7.3 Redis

如果后端引入 Redis health check，E2E 后端必须配置 Redis：

```text
SPRING_DATA_REDIS_HOST
SPRING_DATA_REDIS_PORT
```

或测试 profile 中确保 Redis 不阻断。

## 8. 验证命令

完整命令：

```bash
cd backend
mvn -Dtest=AgentProjectConfigIntegrationTest test
mvn -Dtest=AgentProjectConfigIntegrationTest test
mvn test

cd ../frontend
npm run typecheck
npm run build

cd ..
bash scripts/start-e2e-backend.sh

cd frontend
npm run test:e2e -- --workers=1
```

## 9. 完成报告格式

完成后按以下格式输出：

```markdown
Milestone 35D-Fix 完成报告

1. 问题根因
2. 新增 / 修改文件清单
3. 后端测试数据唯一性修复说明
4. AgentProjectConfigIntegrationTest 连续运行结果
5. 后端全量 mvn test 结果
6. 前端 typecheck / build 结果
7. E2E 后端环境说明
8. 前端全量 E2E 结果
9. 仍存在的 flaky 或风险
10. 是否可以进入 Milestone 35E
```

## 10. 已知限制

本阶段只做回归收口，不实现新功能。

不会包含：

- Human Approval Gate。
- Workflow Template 持久化。
- 工作流编辑器。
- 工具调用。
- 真实代码修改。

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 35D-Fix。

文档路径：

```text
docs/milestone-35d-fix-regression-quality-gate.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 测试结构。
2. 本阶段只做 35D 后的回归质量门收口，不新增业务功能。
3. 优先修复 `AgentProjectConfigIntegrationTest.shouldDisableAgentSuccessfully` 的 duplicate user key。
4. 不要跳过测试。
5. 不要删除测试断言。
6. 不要使用 `@Disabled`。
7. 不要通过放宽生产代码约束来适配测试。
8. 不要重置用户本地数据库。
9. 不执行破坏性数据库操作。
10. 不改变 Multi-Agent Strategy 行为。
11. 不改变已有 API contract。
12. 如需修改 E2E，只修复稳定性问题，优先使用 `data-testid` 和 `waitForResponse`。

需要实现：

1. 定位 duplicate user key 根因。
2. 修复 `AgentProjectConfigIntegrationTest` 测试数据创建逻辑。
3. 如果测试 helper 存在固定用户数据，统一改为唯一值。
4. 确保 `AgentProjectConfigIntegrationTest` 可连续运行两次。
5. 跑后端全量测试。
6. 跑前端 typecheck / build。
7. 启动 E2E 后端环境。
8. 跑前端全量 E2E。
9. 如发现 flaky，修复等待条件，不删除测试。

必须执行：

```bash
cd backend
mvn -Dtest=AgentProjectConfigIntegrationTest test
mvn -Dtest=AgentProjectConfigIntegrationTest test
mvn test

cd ../frontend
npm run typecheck
npm run build

cd ..
bash scripts/start-e2e-backend.sh

cd frontend
npm run test:e2e -- --workers=1
```

完成后按以下格式输出：

1. 问题根因
2. 新增 / 修改文件清单
3. 后端测试数据唯一性修复说明
4. AgentProjectConfigIntegrationTest 连续运行结果
5. 后端全量 mvn test 结果
6. 前端 typecheck / build 结果
7. E2E 后端环境说明
8. 前端全量 E2E 结果
9. 仍存在的 flaky 或风险
10. 是否可以进入 Milestone 35E

现在开始修复，不要只给计划。
