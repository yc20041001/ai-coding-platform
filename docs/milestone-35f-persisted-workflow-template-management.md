# Milestone 35F: Persisted Workflow Template Management

## 1. 背景

Milestone 35A-35E 已完成多智能体编排主链路：

- 35A: Multi-Agent Mock Orchestration
- 35B: Multi-Agent Message Passing
- 35C: Phase / Lane Parallel Mock Execution
- 35D: Workflow Strategy Template
- 35E: Human Approval Gate

当前 Workflow Strategy 仍然是代码内置模板：

```text
WorkflowStrategyCatalogService
  ├─ STANDARD_DELIVERY
  ├─ BACKEND_FOCUSED
  ├─ FRONTEND_FOCUSED
  └─ REVIEW_ONLY
```

这让系统已经能选择不同策略，但模板本身不可持久化、不可后台查看、不可启用/禁用，也无法为后续团队级模板管理打基础。

Milestone 35F 的目标是新增 **Persisted Workflow Template Management**：

```text
内置策略 seed 到数据库
后端运行时优先读取数据库模板
管理员可以查看 / 启用 / 停用模板
普通用户启动 Multi-Agent Run 时只能选择启用模板
```

本阶段不做拖拽编辑器，不做复杂节点编辑，不做模板版本发布流。

## 2. 总目标

实现数据库持久化的工作流模板管理基础能力：

1. 新增 workflow template 数据表。
2. 将 35D 的 4 个内置策略 seed 到数据库。
3. `GET /api/multi-agent-strategies` 从数据库读取启用模板。
4. Multi-Agent Run 启动时从数据库解析模板。
5. 管理员可查看所有模板。
6. 管理员可启用 / 停用模板。
7. 模板结构仍使用 JSON 保存，不做复杂编辑器。
8. 保留代码内置模板作为 fallback / seed source。
9. 前端 Model / Multi-Agent 页面支持模板管理入口或基础列表。

完成后，平台从：

```text
代码内置 Strategy
```

升级为：

```text
数据库持久化 Workflow Template
```

## 3. 严格边界

必须遵守：

1. 不做拖拽工作流编辑器。
2. 不做节点可视化编辑。
3. 不做模板版本发布 / 审批。
4. 不做团队级模板权限。
5. 不做 marketplace。
6. 不执行 shell。
7. 不执行 Git 写操作。
8. 不生成真实代码文件。
9. 不改真实工具执行能力。
10. 不破坏 35A-35E 已有 API。
11. 不破坏单 Agent 执行接口。
12. 不绕过 Task 状态机。
13. 前端保持当前中文暗色科技风 UI。

允许做：

- 新增模板表。
- 新增 seed migration。
- 新增 template entity / mapper / DTO / service / controller。
- 修改 `WorkflowStrategyCatalogService` 读取数据库。
- 管理员启用 / 停用模板。
- 前端展示模板列表和详情 JSON 预览。
- 前端 Multi-Agent Run strategy 下拉仍复用已有接口。

## 4. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V19__init_workflow_template_tables.sql
```

如果 V19 已存在，请顺延到下一个版本号。

### 4.1 workflow_template

```sql
CREATE TABLE IF NOT EXISTS workflow_template (
    id BIGINT PRIMARY KEY,
    template_key VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'MULTI_AGENT',
    status VARCHAR(32) NOT NULL,
    built_in TINYINT NOT NULL DEFAULT 0,
    template_json JSON NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_workflow_template_key (template_key),
    INDEX idx_workflow_template_status (status),
    INDEX idx_workflow_template_category (category),
    INDEX idx_workflow_template_builtin (built_in)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流模板表';
```

字段说明：

| 字段 | 说明 |
|---|---|
| template_key | 策略 key，例如 STANDARD_DELIVERY |
| name | 展示名称 |
| description | 展示说明 |
| category | 当前固定 MULTI_AGENT |
| status | ENABLED / DISABLED |
| built_in | 是否内置模板 |
| template_json | phases / steps / approvalGates 结构 |

### 4.2 Seed 数据

迁移中插入 4 个内置模板：

- `STANDARD_DELIVERY`
- `BACKEND_FOCUSED`
- `FRONTEND_FOCUSED`
- `REVIEW_ONLY`

建议 `template_json` 结构：

```json
{
  "strategyKey": "STANDARD_DELIVERY",
  "phases": [
    {
      "phaseOrder": 1,
      "phaseKey": "PLANNING",
      "title": "架构规划",
      "steps": [
        {
          "stepOrder": 1,
          "stepType": "ARCHITECTURE_ANALYSIS",
          "agentCode": "architect-agent",
          "laneKey": "architect",
          "title": "架构分析"
        }
      ]
    }
  ],
  "approvalGates": [
    {
      "gateKey": "IMPLEMENTATION_PLAN_APPROVAL",
      "title": "实施方案审批",
      "description": "请确认多智能体生成的实施方案是否可以进入审查与总结阶段。",
      "afterPhaseOrder": 2
    }
  ]
}
```

## 5. 枚举设计

新增：

```text
WorkflowTemplateStatus.java
```

```java
public enum WorkflowTemplateStatus {
    ENABLED,
    DISABLED
}
```

可选新增：

```text
WorkflowTemplateCategory.java
```

```java
public enum WorkflowTemplateCategory {
    MULTI_AGENT
}
```

如果不新增 category enum，也可使用 String。

## 6. Entity / Mapper / DTO

### 6.1 Entity

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/domain/WorkflowTemplateEntity.java
```

要求：

- `@TableName("workflow_template")`
- `@TableId(type = IdType.ASSIGN_ID)`
- `@TableField(fill = FieldFill.INSERT)` createTime
- `@TableField(fill = FieldFill.INSERT_UPDATE)` updateTime
- 不继承 BaseEntity
- 手写 getter/setter

字段：

```java
private Long id;
private String templateKey;
private String name;
private String description;
private String category;
private String status;
private Integer builtIn;
private String templateJson;
private LocalDateTime createTime;
private LocalDateTime updateTime;
```

### 6.2 Mapper

新增：

```text
WorkflowTemplateMapper.java
```

```java
public interface WorkflowTemplateMapper extends BaseMapper<WorkflowTemplateEntity> {
}
```

### 6.3 DTO

新增：

```text
WorkflowTemplateResponse.java
UpdateWorkflowTemplateStatusRequest.java
```

`WorkflowTemplateResponse` 字段：

```java
private String id;
private String templateKey;
private String name;
private String description;
private String category;
private String status;
private Boolean builtIn;
private String templateJson;
private WorkflowStrategyResponse strategy;
private LocalDateTime createTime;
private LocalDateTime updateTime;
```

`UpdateWorkflowTemplateStatusRequest` 字段：

```java
private String status;
```

## 7. 后端 Service 设计

### 7.1 WorkflowTemplateApplicationService

新增：

```text
backend/src/main/java/com/aicoding/platform/orchestration/application/WorkflowTemplateApplicationService.java
```

职责：

1. 管理员查询模板列表。
2. 查询模板详情。
3. 启用 / 停用模板。
4. 将 `WorkflowTemplateEntity.templateJson` 解析为 `WorkflowStrategyResponse`。
5. 校验模板 JSON 结构。

方法建议：

```java
List<WorkflowTemplateResponse> listTemplates(String status);
WorkflowTemplateResponse getTemplate(Long templateId);
WorkflowTemplateResponse updateStatus(Long templateId, String status);
WorkflowStrategyTemplate parseTemplate(WorkflowTemplateEntity entity);
```

### 7.2 WorkflowStrategyCatalogService 改造

当前 `WorkflowStrategyCatalogService` 使用内置模板。

35F 要求：

1. `listStrategies()` 优先从 `workflow_template` 查询 `status=ENABLED`。
2. 如果数据库无模板，则 fallback 到内置模板。
3. `resolveTemplate(strategyKey)`：
   - legacy key 先 normalize。
   - 查询数据库中 `template_key = normalizedKey` 且 `status=ENABLED`。
   - 找不到则查内置 fallback。
   - 如果仍不存在则 BAD_REQUEST。
4. 内置模板仍保留作为 fallback。

### 7.3 JSON 解析

推荐使用 Jackson `ObjectMapper`。

要求：

- templateJson 解析失败时，该模板在 listStrategies 中跳过或返回错误标识。
- resolveTemplate 遇到解析失败，应返回 `BAD_REQUEST` 或 `INTERNAL_ERROR`。
- built-in seed JSON 必须稳定通过测试。

## 8. 后端 API 设计

### 8.1 用户查询可用 Strategy

已有：

```http
GET /api/multi-agent-strategies
```

行为变化：

- 返回 `status=ENABLED` 的数据库模板。
- 如果数据库暂无模板，则返回内置 fallback。
- 响应结构保持兼容。

### 8.2 管理员查询所有模板

新增：

```http
GET /api/workflow-templates?status=
```

权限：

```text
ADMIN
```

### 8.3 管理员查询模板详情

新增：

```http
GET /api/workflow-templates/{templateId}
```

权限：

```text
ADMIN
```

### 8.4 管理员启用 / 停用模板

新增：

```http
PUT /api/workflow-templates/{templateId}/status
```

权限：

```text
ADMIN
```

请求：

```json
{
  "status": "DISABLED"
}
```

规则：

- status 只能为 `ENABLED` / `DISABLED`。
- 停用模板后：
  - `/api/multi-agent-strategies` 不再返回该模板。
  - 使用该 strategy 启动 run 返回 `BAD_REQUEST`。
- 不允许删除 built-in 模板，本阶段不做删除接口。

## 9. 前端实现

### 9.1 API client

修改：

```text
frontend/src/modules/task/api.ts
```

或新增：

```text
frontend/src/modules/workflow/api.ts
```

推荐新增模块：

```text
frontend/src/modules/workflow/api.ts
```

类型：

```ts
export interface WorkflowTemplateResponse {
  id: string
  templateKey: string
  name: string
  description?: string
  category: string
  status: 'ENABLED' | 'DISABLED'
  builtIn: boolean
  templateJson: string
  strategy?: WorkflowStrategyResponse
  createTime?: string
  updateTime?: string
}
```

函数：

```ts
export function listWorkflowTemplates(status?: string)
export function getWorkflowTemplate(templateId: string)
export function updateWorkflowTemplateStatus(templateId: string, status: string)
```

### 9.2 管理页面

新增页面：

```text
frontend/src/modules/workflow/pages/WorkflowTemplatePage.vue
```

路由：

```text
/workflow-templates
```

权限：

- 前端菜单只对 ADMIN 显示。
- 后端仍是最终权限源。

页面内容：

1. 模板列表表格：
   - name
   - templateKey
   - status
   - builtIn
   - phaseCount
   - stepCount
   - updateTime
2. 操作：
   - 查看详情
   - 启用
   - 停用
3. 详情 Drawer：
   - strategy 概览
   - phase / step 结构
   - approval gates
   - raw JSON 预览

### 9.3 MultiAgentRunPanel 兼容

无需大改。

要求：

- strategy 下拉继续调用 `GET /api/multi-agent-strategies`。
- 如果某模板被停用，前端下拉自动不显示。
- 如果当前 run 使用的模板后来被停用，run detail 仍显示已保存的 strategyName / strategyDescription。

## 10. 权限设计

| API | 权限 |
|---|---|
| GET `/api/multi-agent-strategies` | 登录用户 |
| GET `/api/workflow-templates` | ADMIN |
| GET `/api/workflow-templates/{templateId}` | ADMIN |
| PUT `/api/workflow-templates/{templateId}/status` | ADMIN |

前端：

- ADMIN 菜单显示「工作流模板」。
- 非 ADMIN 不显示入口。

## 11. 后端测试要求

新增或扩展：

```text
WorkflowTemplateIntegrationTest.java
```

至少覆盖：

1. Flyway seed 后有 4 个 built-in 模板。
2. `GET /api/multi-agent-strategies` 返回启用模板。
3. ADMIN 可查询模板列表。
4. 非 ADMIN 查询模板列表返回 FORBIDDEN 或 PROJECT_ACCESS_DENIED，按现有 ErrorCode 约定。
5. 未登录查询模板列表返回 UNAUTHORIZED。
6. ADMIN 可查询模板详情。
7. ADMIN 可停用模板。
8. 停用后 `/api/multi-agent-strategies` 不返回该模板。
9. 停用后使用该 strategy 启动 run 返回 BAD_REQUEST。
10. ADMIN 可重新启用模板。
11. 启用后可再次用于启动 run。
12. legacy `DEFAULT_MOCK` 仍映射可用模板。
13. 数据库无模板时 fallback 到内置模板。
14. templateJson 解析失败时 resolve 返回明确错误。

后端质量门：

```bash
cd backend
mvn test
```

## 12. 前端 E2E 要求

新增：

```text
frontend/e2e/workflow-template.spec.ts
```

至少覆盖：

1. ADMIN 登录后可看到「工作流模板」入口。
2. 打开 `/workflow-templates` 页面。
3. 模板列表显示 4 个内置模板。
4. 打开模板详情 Drawer。
5. 停用 BACKEND_FOCUSED。
6. 回到 Multi-Agent Run 面板，strategy 下拉不显示 BACKEND_FOCUSED。
7. 重新启用 BACKEND_FOCUSED。
8. strategy 下拉再次显示 BACKEND_FOCUSED。
9. 页面无 JS error。

前端质量门：

```bash
cd frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果需要 E2E 后端：

```bash
bash scripts/start-e2e-backend.sh
```

## 13. 手动验证清单

1. 登录 admin。
2. 打开「工作流模板」页面。
3. 确认 4 个 built-in 模板存在。
4. 查看 `STANDARD_DELIVERY` 详情。
5. 确认 phases / steps / approval gates 展示正确。
6. 停用 `BACKEND_FOCUSED`。
7. 打开 Task Detail → 多智能体。
8. 确认 strategy 下拉不再显示 `BACKEND_FOCUSED`。
9. 重新启用 `BACKEND_FOCUSED`。
10. 确认 strategy 下拉恢复显示。
11. 使用 `BACKEND_FOCUSED` 启动 run。
12. 确认 run 正常进入 approval gate。

## 14. 完成报告格式

完成后按以下格式输出：

```markdown
Milestone 35F 完成报告

1. 新增 / 修改文件清单
2. Workflow Template 数据库设计说明
3. Seed 内置模板说明
4. WorkflowStrategyCatalogService 改造说明
5. 后端模板管理 API 实现说明
6. 前端工作流模板页面实现说明
7. MultiAgentRunPanel 策略下拉兼容说明
8. 权限控制说明
9. 后端测试覆盖说明
10. 前端 typecheck / build / E2E 结果
11. 手动验证结果
12. 已知限制
13. 是否可以进入 Milestone 36A
```

## 15. 已知限制

35F 完成后仍不包含：

- 自定义创建模板。
- 编辑模板 JSON。
- 模板版本管理。
- 模板发布审批。
- 拖拽工作流编辑器。
- 条件分支。
- 工具节点。
- 团队级模板权限。

后续建议：

```text
36A: Safe Tool Execution Sandbox
36B: Code Change Proposal Flow
36C: Human Approved Git Patch Proposal
36D: Workflow Template Editor
```

---

# Claude 执行提示词

请根据项目中的文档执行 Milestone 35F。

文档路径：

```text
docs/milestone-35f-persisted-workflow-template-management.md
```

执行要求：

1. 先完整阅读该文档，再检查当前 backend / frontend 代码结构。
2. 本阶段是在 Milestone 35A-35E 基础上新增 Persisted Workflow Template Management。
3. 不要重写已有 Run / Step / Message / Phase / Lane / Strategy / Approval Gate 能力。
4. 不要破坏已有 multi-agent run / detail / messages / phases / strategies / approval API。
5. 不要破坏已有单 Agent `POST /api/tasks/{taskId}/execute`。
6. 不做拖拽工作流编辑器。
7. 不做模板 JSON 编辑。
8. 不做模板版本发布流程。
9. 不执行 shell。
10. 不执行 Git 写操作。
11. 不生成真实代码文件。
12. 不引入队列，不引入工作流引擎。
13. 不更换技术栈，不更换 UI 框架。
14. 复用现有 Spring Boot 3.x、MyBatis-Plus、ApiResponse、BizException、ErrorCode、构造器注入、无 Lombok、手写 getter/setter。
15. 复用现有 ADMIN 权限判断方式。
16. 前端保持当前中文暗色科技风，复用 StatusPulse、GlowButton、MarkdownRenderer、SectionRail 等现有组件。
17. 所有新增 API 的 ID 对外保持 String。
18. 所有新增测试必须跟随现有测试风格。

需要实现：

1. 新增 `V19__init_workflow_template_tables.sql`，如果 V19 已存在则顺延版本号。
2. 新增 `workflow_template` 表。
3. Seed 4 个内置模板：
   - STANDARD_DELIVERY
   - BACKEND_FOCUSED
   - FRONTEND_FOCUSED
   - REVIEW_ONLY
4. 新增 `WorkflowTemplateStatus` 枚举。
5. 新增 `WorkflowTemplateEntity`。
6. 新增 `WorkflowTemplateMapper`。
7. 新增 `WorkflowTemplateResponse`。
8. 新增 `UpdateWorkflowTemplateStatusRequest`。
9. 新增 `WorkflowTemplateApplicationService`。
10. 改造 `WorkflowStrategyCatalogService`：
    - listStrategies 从数据库读取 ENABLED 模板
    - resolveTemplate 从数据库读取 ENABLED 模板
    - 数据库无模板时 fallback 到内置模板
    - legacy strategy 继续兼容
11. 新增 `GET /api/workflow-templates?status=`。
12. 新增 `GET /api/workflow-templates/{templateId}`。
13. 新增 `PUT /api/workflow-templates/{templateId}/status`。
14. 前端新增 `frontend/src/modules/workflow/api.ts`。
15. 前端新增 `WorkflowTemplatePage.vue`。
16. 前端新增 `/workflow-templates` 路由。
17. 前端 ADMIN 菜单增加「工作流模板」入口。
18. 模板列表支持启用 / 停用。
19. 模板详情 Drawer 展示 strategy / phases / steps / approval gates / raw JSON。
20. MultiAgentRunPanel 的 strategy 下拉继续通过 `/api/multi-agent-strategies` 获取，因此停用模板后自动不显示。
21. 新增后端集成测试。
22. 新增前端 E2E 测试。

完成后必须执行：

```bash
cd backend
mvn test

cd ../frontend
npm run typecheck
npm run build
npm run test:e2e -- --workers=1
```

如果 E2E 需要后端测试环境，请先执行：

```bash
bash scripts/start-e2e-backend.sh
```

完成后按以下格式输出：

1. 新增 / 修改文件清单
2. Workflow Template 数据库设计说明
3. Seed 内置模板说明
4. WorkflowStrategyCatalogService 改造说明
5. 后端模板管理 API 实现说明
6. 前端工作流模板页面实现说明
7. MultiAgentRunPanel 策略下拉兼容说明
8. 权限控制说明
9. 后端测试覆盖说明
10. 前端 typecheck / build / E2E 结果
11. 手动验证结果
12. 已知限制
13. 是否可以进入 Milestone 36A

现在开始实现，不要只给计划。
