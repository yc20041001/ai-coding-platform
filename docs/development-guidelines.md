# AI Coding Platform 开发规范

## 1. 目标与适用范围

本文档定义 AI Coding Platform 的团队开发规范，适用于后端、前端、数据库、API、AI Agent、测试、Git 协作和文档维护。

目标：

- 保持代码结构清晰、命名统一、职责边界稳定。
- 降低多人协作冲突和后续维护成本。
- 保障权限、安全、审计、AI 工具调用等关键链路可控。
- 让需求、架构、数据库、API 和代码实现保持一致。

适用目录：

- `backend/`
- `frontend/`
- `docs/`
- `infra/`
- `deploy/`
- `scripts/`

## 2. 通用开发原则

- 先读文档，再写代码。开发前优先查看 `docs/requirements.md`、`docs/system-architecture.md`、`docs/module-breakdown.md`、`docs/database-design.md`、`docs/api-design.md`。
- 保持模块边界。禁止跨模块直接访问对方数据库 Mapper 或内部实现。
- 小步提交。每次提交只解决一个明确问题。
- 优先实现 P0 闭环，再扩展 P1/P2 能力。
- 所有项目级资源必须校验 `projectId` 和当前用户项目角色。
- 所有 AI 执行、工具调用、Git 写操作必须可追踪、可审计。
- 敏感信息不得出现在代码、日志、提交记录和文档示例中。

## 3. 后端开发规范

### 3.1 技术栈

- Java 17。
- Spring Boot 3.x。
- Spring Security。
- MyBatis-Plus。
- MySQL 8。
- Redis。
- RabbitMQ。
- LangChain4j / Spring AI。

### 3.2 包结构规范

后端按模块化单体组织：

```text
backend/src/main/java/com/aicoding/platform/
  common/
  security/
  auth/
  project/
  member/
  repository/
  task/
  chat/
  agent/
  knowledge/
  ai/
  audit/
  notification/
```

业务模块内部使用统一分层：

```text
module/
  controller/        # REST Controller
  application/       # 应用服务，编排业务流程
  domain/            # 领域模型、领域服务、枚举
  infrastructure/    # Mapper、外部系统、消息队列、存储适配
  dto/               # Request、Response、Command、DTO
```

### 3.3 分层职责

| 层 | 职责 | 禁止事项 |
| --- | --- | --- |
| controller | 参数接收、鉴权注解、调用应用服务 | 写业务逻辑、直接访问 Mapper |
| application | 编排业务流程、事务控制、跨模块 Facade 调用 | 堆积复杂领域规则 |
| domain | 领域对象、枚举、状态机、领域规则 | 依赖 Spring Web、Mapper |
| infrastructure | 数据库、Redis、MQ、外部 API 适配 | 向上暴露数据库细节 |
| dto | API 请求响应对象 | 放业务逻辑 |

### 3.4 命名规范

类命名：

| 类型 | 示例 |
| --- | --- |
| Controller | `ProjectController` |
| Application Service | `ProjectApplicationService` |
| Domain Service | `ProjectDomainService` |
| Entity | `ProjectEntity` |
| Mapper | `ProjectMapper` |
| Request | `CreateProjectRequest` |
| Response | `ProjectDetailResponse` |
| Command | `CreateProjectCommand` |
| Enum | `TaskStatus` |

方法命名：

- 查询单个：`getProjectDetail`
- 分页查询：`pageProjects`
- 创建：`createProject`
- 更新：`updateProject`
- 删除或归档：`archiveProject`
- 状态流转：`startTask`、`cancelTask`、`retryTask`
- 权限校验：`checkProjectPermission`

### 3.5 Controller 规范

- Controller 只做参数校验、权限声明、调用应用服务。
- URL、请求体、响应体必须与 `docs/api-design.md` 保持一致。
- 不在 Controller 中写事务。
- 不在 Controller 中直接调用 Mapper。
- 所有接口返回统一响应结构。

示例：

```java
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectApplicationService projectApplicationService;

    public ProjectController(ProjectApplicationService projectApplicationService) {
        this.projectApplicationService = projectApplicationService;
    }

    @PostMapping
    public ApiResponse<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ApiResponse.ok(projectApplicationService.createProject(request));
    }
}
```

### 3.6 事务规范

- 事务只放在 application 层。
- 查询方法默认不加事务，复杂一致性查询可使用只读事务。
- MQ 消息发送与数据库状态变更要考虑一致性，必要时使用本地消息表或事务后事件。
- 不在循环中执行大量独立数据库写入。
- 不在事务中执行耗时外部调用，例如模型调用、Git 操作、文件解析。

### 3.7 异常规范

使用统一业务异常：

```java
throw new BizException(ErrorCode.PROJECT_ACCESS_DENIED);
```

异常分类：

| 类型 | HTTP 状态 | 示例 |
| --- | --- | --- |
| 参数错误 | 400 | `BAD_REQUEST` |
| 未登录 | 401 | `UNAUTHORIZED` |
| 无权限 | 403 | `FORBIDDEN` |
| 资源不存在 | 404 | `NOT_FOUND` |
| 状态冲突 | 409 | `CONFLICT` |
| 参数校验失败 | 422 | `VALIDATION_ERROR` |
| AI 调用失败 | 502 | `AI_PROVIDER_ERROR` |
| AI 调用超时 | 504 | `AI_PROVIDER_TIMEOUT` |

### 3.8 日志规范

- 使用结构化日志，必须包含 `traceId`。
- 关键业务日志包含 `userId`、`projectId`、`taskId`。
- 禁止打印明文 Token、API Key、密码、OAuth Token。
- AI Prompt 和模型响应日志只能保存摘要或脱敏内容。
- Git 写操作、工具调用、权限变更必须写审计日志。

日志等级：

| 等级 | 使用场景 |
| --- | --- |
| DEBUG | 本地调试、详细上下文 |
| INFO | 关键业务流程状态 |
| WARN | 可恢复异常、降级、重试 |
| ERROR | 不可恢复异常、任务失败 |

### 3.9 权限规范

- 所有登录接口以外的 API 必须认证。
- 所有包含 `projectId` 的接口必须校验项目成员关系。
- 项目资源查询必须带项目边界。
- Admin 接口必须校验平台角色。
- Git 写操作、Agent 工具调用、模型 Key 配置必须有审计记录。

## 4. 前端开发规范

### 4.1 技术栈

- Vue 3。
- TypeScript。
- Vite。
- Pinia。
- Vue Router。
- Element Plus。
- Monaco Editor。
- Markdown Renderer。

### 4.2 目录规范

```text
frontend/src/
  app/              # 路由、布局、守卫、全局 Store
  shared/           # 通用组件、API Client、工具、类型
  modules/          # 业务模块
  styles/           # 全局样式
  assets/           # 静态资源
```

业务模块建议结构：

```text
modules/project/
  api/
  components/
  pages/
  stores/
  types/
  utils/
```

### 4.3 Vue 组件规范

- 使用 Composition API。
- 组件名使用 PascalCase，例如 `ProjectListPage.vue`。
- 页面组件放 `pages/`。
- 可复用业务组件放 `components/`。
- 通用组件放 `shared/components/`。
- 单个组件过大时拆分子组件，避免一个文件承担过多职责。

### 4.4 TypeScript 规范

- 禁止使用隐式 `any`。
- API 请求和响应必须定义类型。
- 枚举值尽量与后端枚举字符串保持一致。
- 通用类型放 `shared/types/`。
- 模块私有类型放 `modules/<module>/types/`。

### 4.5 API 调用规范

- 所有 HTTP 调用统一通过 `shared/api` 封装。
- 业务模块 API 放在模块目录下，例如 `modules/project/api/projectApi.ts`。
- 不在 Vue 组件中直接拼接复杂 URL。
- 统一处理 401、403、500、业务错误码。
- 分页参数统一使用 `page`、`pageSize`、`sort`。

### 4.6 状态管理规范

- Pinia 只保存跨页面或全局状态。
- 页面局部状态优先使用组件内 `ref`、`reactive`。
- 登录用户、权限、项目上下文可放全局 Store。
- 大量列表数据优先由页面请求维护，不滥用全局 Store。

### 4.7 UI 规范

- 企业后台界面以清晰、稳定、信息密度适中为主。
- 表格页必须支持加载态、空状态、错误状态。
- 危险操作必须二次确认，例如删除、归档、Push、PR、取消任务。
- AI 流式输出必须支持 loading、失败、重试、中断状态。
- 代码、Diff、Patch 使用 Monaco Editor 或专用 Diff 组件展示。

## 5. API 开发规范

- API 以 `docs/api-design.md` 为准。
- 新增或修改接口时，必须同步更新 API 文档。
- 请求字段必须明确必填、可选、枚举和默认值。
- 响应 ID 统一返回字符串。
- 分页响应统一 `records`、`page`、`pageSize`、`total`、`hasNext`。
- 错误响应统一 `code`、`message`、`details`、`traceId`、`timestamp`。
- 创建任务、Commit、Push、PR 等接口应支持 `Idempotency-Key`。
- 不兼容变更必须先评估前端影响。

## 6. 数据库开发规范

- 数据库设计以 `docs/database-design.md` 为准。
- 表名使用小写下划线。
- 字段名使用小写下划线。
- 主键统一 `BIGINT id`。
- 业务表包含通用字段：`create_time`、`update_time`、`create_by`、`update_by`、`deleted`、`version`。
- 状态字段使用 `VARCHAR(32)`。
- 不使用数据库物理外键，应用层维护引用完整性。
- 关联字段必须建索引。
- 新增表和字段必须通过迁移脚本完成。
- 删除字段、修改字段类型、清理历史数据必须单独评估。

迁移脚本命名建议：

```text
V1__init_user_and_auth_tables.sql
V2__init_project_tables.sql
V3__init_repository_and_agent_tables.sql
```

## 7. AI 模块开发规范

### 7.1 Agent 规范

- Agent 定义必须包含：类型、名称、系统 Prompt、模型配置、工具权限、执行策略。
- Agent 配置变更必须版本化。
- 任务执行时必须绑定 Agent 版本。
- Agent 不得直接访问数据库、文件系统、Git 或外部 API。
- Agent 所有外部能力必须通过 Tool Runtime 调用。

### 7.2 Prompt 规范

- Prompt 模板必须集中管理。
- Prompt 变量必须显式声明。
- 重要 Prompt 需要版本号。
- 输出结构可控的场景优先使用 JSON Schema。
- RAG 内容必须作为非可信上下文处理。
- Prompt 中必须注入权限、安全和工具调用边界。

### 7.3 Tool Runtime 规范

- 工具必须注册名称、类型、输入 Schema、输出 Schema、权限要求。
- 工具调用必须记录 `tool_call_log`。
- 高风险工具需要审批：写文件、Commit、Push、PR、部署。
- 工具必须设置超时时间。
- 工具失败必须返回结构化错误。

### 7.4 RAG 规范

- 所有检索必须携带 `projectId`。
- Chunk 必须保存来源、路径、行号、文件类型和 Metadata。
- AI 输出引用知识库内容时必须返回引用来源。
- 删除文档时必须异步清理 Chunk、向量和对象存储文件。

## 8. Git 协作规范

### 8.1 分支命名

```text
main                         # 主分支
develop                      # 开发分支
feature/<module>-<summary>   # 功能分支
fix/<module>-<summary>       # 缺陷修复
docs/<summary>               # 文档变更
chore/<summary>              # 工程配置
```

示例：

```text
feature/project-api
fix/task-status-transition
docs/api-design
```

### 8.2 Commit 规范

使用 Conventional Commits：

```text
<type>(<scope>): <subject>
```

类型：

| type | 说明 |
| --- | --- |
| feat | 新功能 |
| fix | 缺陷修复 |
| docs | 文档 |
| refactor | 重构 |
| test | 测试 |
| chore | 构建、依赖、脚手架 |
| ci | CI/CD |
| perf | 性能优化 |

示例：

```text
feat(project): add project create API
docs(api): add task API contract
fix(auth): handle expired token response
```

### 8.3 Pull Request 规范

PR 必须包含：

- 变更说明。
- 关联需求或任务。
- 测试结果。
- 风险说明。
- 截图或接口示例，前端/API 变更时需要。

PR 合并前检查：

- 代码能编译。
- 测试通过。
- 无敏感信息。
- 文档已同步。
- 数据库迁移脚本可回滚或可前向修复。

## 9. 测试规范

### 9.1 后端测试

测试类型：

- 单元测试：领域规则、状态机、工具类。
- 应用服务测试：核心业务流程。
- Controller 测试：接口参数、权限、响应结构。
- 集成测试：数据库、Redis、RabbitMQ、外部 API Mock。

重点覆盖：

- 登录与权限。
- 项目成员权限。
- 任务状态流转。
- Agent 工具权限。
- Git 写操作审批。
- RAG 项目隔离。

### 9.2 前端测试

测试类型：

- 组件测试。
- API Mock 测试。
- 路由守卫测试。
- 关键流程 E2E 测试。

重点覆盖：

- 登录流程。
- 项目创建。
- 成员管理。
- AI Chat 流式输出。
- 任务创建、取消、重试。
- 知识库上传和索引状态。

### 9.3 验收要求

P0 功能至少具备：

- 核心业务单元测试。
- 关键接口集成测试。
- 前端主流程手工验收记录。

## 10. 安全规范

- 密码必须加密存储。
- GitHub Token、模型 API Key 必须加密存储。
- 日志中禁止出现密钥、Token、密码。
- 所有项目资源必须校验项目成员权限。
- 所有 Git 写操作必须记录审计日志。
- Prompt 注入防护必须在 Agent 系统 Prompt 和 Tool Runtime 双层实现。
- 文件上传必须限制类型、大小和解析超时。
- 防止 XSS：前端渲染 Markdown 时必须启用安全过滤。
- 防止 SQL 注入：禁止字符串拼接 SQL。

## 11. 配置规范

- 本地配置使用 `application-local.yml`，不得提交真实密钥。
- 生产配置通过环境变量、密钥管理系统或部署平台注入。
- 配置项命名应按模块分组。

示例：

```yaml
ai:
  model:
    default-provider: openai
  task:
    max-concurrency: 3
    timeout-seconds: 600
github:
  oauth:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
```

## 12. 文档规范

- 需求变更更新 `docs/requirements.md`。
- 架构变更更新 `docs/system-architecture.md`。
- 模块边界变更更新 `docs/module-breakdown.md`。
- 数据库变更更新 `docs/database-design.md`。
- API 变更更新 `docs/api-design.md`。
- 目录结构变更更新 `docs/project-structure.md`。
- 开发规范变更更新本文档。

文档提交建议：

```text
docs(<area>): update <topic>
```

示例：

```text
docs(database): add task artifact table
docs(api): update repository PR endpoint
```

## 13. Code Review 规范

Review 重点：

- 是否符合模块边界。
- 是否存在权限绕过。
- 是否有敏感信息泄露。
- 是否有事务范围过大。
- 是否有跨项目数据访问风险。
- 是否同步更新文档和测试。
- AI 工具调用是否经过 Tool Runtime。
- Git 写操作是否有审批和审计。

Review 不应纠缠：

- 无实际收益的个人风格偏好。
- 与当前变更无关的大规模重构。
- 已由格式化工具处理的细节。

## 14. 发布前检查清单

后端：

- 编译通过。
- 单元测试通过。
- 数据库迁移脚本执行成功。
- 关键接口返回结构符合 API 文档。
- 日志无敏感信息。

前端：

- 构建通过。
- 关键页面无明显控制台错误。
- 登录、项目、Chat、任务主流程可用。
- 空状态、加载态、错误态可用。

AI 能力：

- 模型配置可用。
- Prompt 输出符合预期。
- 工具调用有审计。
- 任务失败可查看原因。
- RAG 检索不越权。

文档：

- API、数据库、模块设计已同步。
- 变更说明清晰。

## 15. 当前限制

- 当前仓库只有目录和文档骨架，尚未生成 Spring Boot 和 Vue 工程配置。
- 具体格式化工具、Lint 规则、CI 检查脚本需要在工程初始化后补充。
- OpenAPI YAML、数据库迁移 SQL 和前端类型生成规则后续单独维护。

