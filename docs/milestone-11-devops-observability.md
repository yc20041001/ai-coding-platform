# Milestone 11: DevOps + Observability 基础模块实施文档

## 1. 背景与目标

当前后端已经完成从项目、任务、RAG、Agent Orchestrator 到真实/Mock Model Gateway 和 Chat SSE 的主链路，并通过统一 smoke 测试。

Milestone 11 的目标是：

> 让平台不仅“能跑”，而且“可排查、可审计、可统计、可交付”。

本阶段不继续堆叠 AI 能力，而是补齐企业级平台最基础的运维与可观测性能力：

- 审计日志
- 模型用量统计
- 任务/Agent/模型概览指标
- 健康检查增强
- 结构化错误日志
- 本地 Docker Compose 环境
- README 启动说明
- smoke test 标准化

## 2. 实施边界

### 2.1 本阶段要做

- 新增审计日志表 `audit_log`
- 关键业务操作写入审计日志
- 新增模型用量统计 API
- 新增系统概览 API
- 增强 actuator info/health
- 增强全局异常日志
- 增加请求 traceId 贯通说明
- 增加 Docker Compose 本地环境
- 增加 `.env.example`
- 增加后端启动 README
- 固化统一 smoke test 脚本说明

### 2.2 本阶段不做

- 不接 Prometheus / Grafana
- 不接 ELK / Loki
- 不做分布式链路追踪系统
- 不做 Kubernetes 部署
- 不做复杂限流系统
- 不做计费结算
- 不做多租户账单
- 不改造业务主流程
- 不改变已验证 API 行为

## 3. 约束要求

- 不破坏 Milestone 0-10 已验证通过的主链路
- 不改动业务核心逻辑，除非为了加审计或统计埋点
- 新增能力优先旁路实现
- 审计失败不应阻塞主业务
- 统计失败不应阻塞主业务
- 遵循现有项目规范：
  - Spring Boot 3.x
  - MyBatis-Plus
  - 无 Lombok
  - 构造器注入
  - 手写 getter/setter
  - ApiResponse
  - BizException
  - ErrorCode
- IDs 对外保持 String
- 权限校验复用当前 Security / ProjectPermissionService

## 4. 模块目标

实现 4 个基础能力：

### 4.1 Audit Log

- 记录关键操作
- 支持按项目、用户、操作类型、时间查询
- 记录 traceId、IP、User-Agent
- 审计写入失败不影响主业务

### 4.2 Usage Metrics

- 基于 `model_request_log` 汇总模型调用次数
- 统计 token 使用量
- 统计 fallback 次数
- 统计成功率

### 4.3 System Overview

- 项目数
- 任务数
- Agent 数
- 知识库数
- 文档数
- 模型调用数
- 今日 token 使用量

### 4.4 Local Delivery

- Docker Compose 启动 MySQL / Redis / RabbitMQ
- `.env.example`
- README 本地启动步骤
- smoke test 使用说明

## 5. 新增目录结构

```text
backend/src/main/java/com/aicoding/platform/
├── audit/
│   ├── application/
│   │   └── AuditLogApplicationService.java
│   ├── controller/
│   │   └── AuditLogController.java
│   ├── domain/
│   │   ├── AuditLogEntity.java
│   │   └── AuditActionType.java
│   ├── dto/
│   │   ├── AuditLogResponse.java
│   │   └── AuditLogQueryRequest.java
│   └── infrastructure/
│       └── AuditLogMapper.java
│
├── observability/
│   ├── application/
│   │   ├── ModelUsageApplicationService.java
│   │   └── SystemOverviewApplicationService.java
│   ├── controller/
│   │   ├── ModelUsageController.java
│   │   └── SystemOverviewController.java
│   └── dto/
│       ├── ModelUsageSummaryResponse.java
│       ├── ModelUsageDailyResponse.java
│       └── SystemOverviewResponse.java
│
backend/src/main/resources/db/migration/
└── V9__init_audit_log.sql

deploy/
└── docker-compose.yml

.env.example
README.md
```

## 6. 数据库设计

新增迁移：

```text
backend/src/main/resources/db/migration/V9__init_audit_log.sql
```

## 6.1 audit_log

审计日志表。

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PRIMARY KEY | 主键 |
| project_id | BIGINT NULL | 项目 ID |
| user_id | BIGINT NULL | 操作用户 |
| username | VARCHAR(128) NULL | 用户名 |
| action_type | VARCHAR(64) NOT NULL | 操作类型 |
| resource_type | VARCHAR(64) NULL | 资源类型 |
| resource_id | BIGINT NULL | 资源 ID |
| description | VARCHAR(512) NULL | 描述 |
| request_method | VARCHAR(16) NULL | HTTP 方法 |
| request_path | VARCHAR(512) NULL | 请求路径 |
| ip_address | VARCHAR(64) NULL | IP |
| user_agent | VARCHAR(512) NULL | User-Agent |
| trace_id | VARCHAR(64) NULL | Trace ID |
| success | TINYINT NOT NULL DEFAULT 1 | 是否成功 |
| error_message | TEXT NULL | 错误信息 |
| create_time | DATETIME NOT NULL | 创建时间 |

索引：

```sql
idx_audit_project_time(project_id, create_time)
idx_audit_user_time(user_id, create_time)
idx_audit_action(action_type)
idx_audit_resource(resource_type, resource_id)
idx_audit_trace(trace_id)
```

建表建议：

```sql
CREATE TABLE audit_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键 ID',
    project_id BIGINT NULL COMMENT '项目 ID',
    user_id BIGINT NULL COMMENT '用户 ID',
    username VARCHAR(128) NULL COMMENT '用户名',
    action_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    resource_type VARCHAR(64) NULL COMMENT '资源类型',
    resource_id BIGINT NULL COMMENT '资源 ID',
    description VARCHAR(512) NULL COMMENT '描述',
    request_method VARCHAR(16) NULL COMMENT 'HTTP 方法',
    request_path VARCHAR(512) NULL COMMENT '请求路径',
    ip_address VARCHAR(64) NULL COMMENT 'IP 地址',
    user_agent VARCHAR(512) NULL COMMENT 'User-Agent',
    trace_id VARCHAR(64) NULL COMMENT 'Trace ID',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功',
    error_message TEXT NULL COMMENT '错误信息',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_audit_project_time (project_id, create_time),
    KEY idx_audit_user_time (user_id, create_time),
    KEY idx_audit_action (action_type),
    KEY idx_audit_resource (resource_type, resource_id),
    KEY idx_audit_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
```

无物理外键。

## 7. Audit Domain 设计

## 7.1 AuditActionType

新增枚举：

```java
public enum AuditActionType {
    AUTH_LOGIN,
    PROJECT_CREATE,
    PROJECT_UPDATE,
    PROJECT_DELETE,
    MEMBER_INVITE,
    REPOSITORY_BIND,
    TASK_CREATE,
    TASK_EXECUTE,
    TASK_CANCEL,
    AGENT_CREATE,
    CHAT_SEND,
    RAG_DOCUMENT_UPLOAD,
    RAG_SEARCH,
    MODEL_CALL,
    SYSTEM_OPERATION
}
```

## 7.2 AuditLogEntity

对应 `audit_log`。

要求：

- `@TableName("audit_log")`
- `@TableId(type = IdType.ASSIGN_ID)`
- `@TableField(fill = FieldFill.INSERT)` 标注 `createTime`
- 不继承 BaseEntity
- 不使用 Lombok
- 手写 getter/setter

## 8. Audit Service 设计

## 8.1 AuditLogApplicationService

职责：

- 写审计日志
- 查询审计日志
- 提供安全失败策略

方法：

```java
void record(AuditLogEntity log);
void recordSuccess(Long projectId, Long resourceId, String actionType, String resourceType, String description);
void recordFailure(Long projectId, Long resourceId, String actionType, String resourceType, String description, String errorMessage);
PageResult<AuditLogResponse> list(AuditLogQueryRequest request);
```

要求：

- `record*` 方法内部 catch 所有异常
- 审计写入失败只打印 warn 日志，不抛出到业务层
- 自动补充当前用户、traceId、请求路径、IP、User-Agent

## 8.2 审计埋点范围

优先在以下操作完成后写审计：

| 模块 | 操作 | actionType |
|---|---|---|
| Auth | 登录成功 | AUTH_LOGIN |
| Project | 创建项目 | PROJECT_CREATE |
| Task | 创建任务 | TASK_CREATE |
| Orchestrator | 执行任务 | TASK_EXECUTE |
| Chat | 发送消息 | CHAT_SEND |
| RAG | 上传文档 | RAG_DOCUMENT_UPLOAD |
| RAG | 检索 | RAG_SEARCH |
| ModelGateway | 模型调用 | MODEL_CALL |

本阶段可以先覆盖：

- TASK_EXECUTE
- CHAT_SEND
- RAG_DOCUMENT_UPLOAD
- MODEL_CALL

避免一次性改太多已验证模块。

## 9. Audit API 设计

## 9.1 AuditLogController

接口：

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/audit/logs` | ADMIN | 查询审计日志 |
| GET | `/api/projects/{projectId}/audit/logs` | OWNER | 查询项目审计日志 |

查询参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| page | Integer | 页码 |
| pageSize | Integer | 每页数量 |
| userId | String | 用户 ID |
| actionType | String | 操作类型 |
| resourceType | String | 资源类型 |
| resourceId | String | 资源 ID |
| startTime | String | 开始时间 |
| endTime | String | 结束时间 |

响应：

```json
{
  "records": [
    {
      "id": "205xxx",
      "projectId": "205xxx",
      "userId": "100001",
      "username": "admin",
      "actionType": "TASK_EXECUTE",
      "resourceType": "TASK",
      "resourceId": "205xxx",
      "description": "Execute task with Agent Orchestrator",
      "traceId": "abc123",
      "success": true,
      "createTime": "2026-05-13T10:00:00"
    }
  ],
  "page": 1,
  "pageSize": 10,
  "total": 1
}
```

## 10. Observability API 设计

## 10.1 ModelUsageController

接口：

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/observability/model-usage/summary` | ADMIN | 全局模型用量汇总 |
| GET | `/api/projects/{projectId}/observability/model-usage/summary` | OWNER | 项目模型用量汇总 |
| GET | `/api/projects/{projectId}/observability/model-usage/daily` | OWNER | 项目每日模型用量 |

### ModelUsageSummaryResponse

字段：

| 字段 | 类型 |
|---|---|
| requestCount | Long |
| successCount | Long |
| failureCount | Long |
| successRate | BigDecimal |
| promptTokens | Long |
| completionTokens | Long |
| totalTokens | Long |
| avgLatencyMs | BigDecimal |
| mockCount | Long |
| realProviderCount | Long |

数据来源：

- `model_request_log`

### ModelUsageDailyResponse

字段：

| 字段 | 类型 |
|---|---|
| date | String |
| requestCount | Long |
| totalTokens | Long |
| successCount | Long |
| failureCount | Long |

## 10.2 SystemOverviewController

接口：

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/observability/overview` | ADMIN | 系统概览 |
| GET | `/api/projects/{projectId}/observability/overview` | OWNER | 项目概览 |

### SystemOverviewResponse

字段：

| 字段 | 类型 |
|---|---|
| projectCount | Long |
| userCount | Long |
| taskCount | Long |
| runningTaskCount | Long |
| completedTaskCount | Long |
| agentCount | Long |
| knowledgeBaseCount | Long |
| documentCount | Long |
| chatMessageCount | Long |
| modelRequestCount | Long |
| todayModelRequestCount | Long |
| todayTokenUsage | Long |

## 11. Actuator 增强

修改 `application.yml`：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized
  info:
    env:
      enabled: true

info:
  app:
    name: AI Coding Platform
    version: 0.0.1-SNAPSHOT
    description: Enterprise AI Coding Collaboration Platform
```

要求：

- `/actuator/health` 继续可访问
- `/actuator/info` 可返回应用信息
- 不暴露敏感配置

## 12. 全局异常日志增强

修改：

```text
common/exception/GlobalExceptionHandler.java
```

要求：

- BizException：warn 级别，打印 code/message/traceId/path
- ValidationException：warn 级别
- AuthenticationException：warn 或 debug
- AccessDeniedException：warn
- Exception：error 级别，打印堆栈
- 不在响应里返回堆栈
- 不打印 API Key

日志格式建议：

```text
request failed, traceId={}, path={}, code={}, message={}
```

## 13. Docker Compose

新增：

```text
deploy/docker-compose.yml
```

包含：

- MySQL 8
- Redis 7
- RabbitMQ 3 management

示例端口：

| 服务 | 端口 |
|---|---|
| MySQL | 3307:3306 |
| Redis | 6379:6379 |
| RabbitMQ | 5672:5672 |
| RabbitMQ Management | 15672:15672 |

MySQL：

- database: `ai_coding_platform`
- user: `root`
- password: `platform123`

## 14. .env.example

新增：

```text
.env.example
```

内容：

```bash
DB_URL=jdbc:mysql://127.0.0.1:3307/ai_coding_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
DB_USERNAME=root
DB_PASSWORD=platform123
JWT_SECRET=verification-test-secret-min-32bytes
MODEL_GATEWAY_PROVIDER=MOCK

OPENAI_ENABLED=false
OPENAI_API_KEY=
OPENAI_MODEL=gpt-4.1-mini

DEEPSEEK_ENABLED=false
DEEPSEEK_API_KEY=
DEEPSEEK_MODEL=deepseek-chat

QWEN_ENABLED=false
QWEN_API_KEY=
QWEN_MODEL=qwen-plus
```

## 15. README 本地启动说明

新增或更新：

```text
README.md
```

至少包含：

- 项目简介
- 技术栈
- 本地依赖
- Docker Compose 启动
- 环境变量配置
- 后端启动
- smoke test 执行
- 常见问题

核心命令：

```bash
docker compose -f deploy/docker-compose.yml up -d

cp .env.example .env
source .env

cd backend
mvn spring-boot:run

scripts/backend-unified-smoke-test.sh
```

## 16. Smoke Test 固化

当前已有：

```text
scripts/backend-unified-smoke-test.sh
docs/unified-backend-regression-test-plan.md
```

Milestone 11 要求：

- README 中说明如何运行
- smoke 脚本保持可重复执行
- 脚本失败时输出明确失败步骤
- 不依赖真实模型 API Key
- 默认使用 Mock Provider

## 17. 权限设计

| API | 权限 |
|---|---|
| GET `/api/audit/logs` | ADMIN |
| GET `/api/projects/{projectId}/audit/logs` | OWNER |
| GET `/api/observability/model-usage/summary` | ADMIN |
| GET `/api/projects/{projectId}/observability/model-usage/summary` | OWNER |
| GET `/api/projects/{projectId}/observability/model-usage/daily` | OWNER |
| GET `/api/observability/overview` | ADMIN |
| GET `/api/projects/{projectId}/observability/overview` | OWNER |

如果当前项目还没有统一 ADMIN 注解，可参考已有 Agent 创建接口里的 admin 判断方式。

## 18. 错误处理

| 场景 | 错误码 |
|---|---|
| 未登录 | UNAUTHORIZED |
| 无权限 | FORBIDDEN 或 PROJECT_ACCESS_DENIED |
| project 不存在 | NOT_FOUND |
| 查询参数非法 | BAD_REQUEST |
| 系统异常 | INTERNAL_ERROR |

审计写入失败：

- 只记录 warn
- 不影响业务响应

统计查询失败：

- 返回 INTERNAL_ERROR

## 19. 验收标准

### 19.1 编译测试

必须通过：

```bash
cd backend
mvn clean compile
mvn test
```

### 19.2 Docker Compose

```bash
docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml ps
```

期望：

- MySQL healthy/running
- Redis running
- RabbitMQ running

### 19.3 后端启动

```bash
source .env
cd backend
mvn spring-boot:run
```

期望：

- 应用启动成功
- Flyway V9 执行成功
- `/actuator/health` UP
- `/actuator/info` 有应用信息

### 19.4 Smoke Test

```bash
scripts/backend-unified-smoke-test.sh
```

期望：

- 全部 PASS

### 19.5 审计日志验证

执行 task / chat / rag 操作后：

```http
GET /api/audit/logs
Authorization: Bearer <admin token>
```

期望：

- 返回审计记录
- 包含 TASK_EXECUTE / CHAT_SEND / RAG_DOCUMENT_UPLOAD / MODEL_CALL 至少部分记录
- traceId 非空

### 19.6 模型用量验证

```http
GET /api/observability/model-usage/summary
Authorization: Bearer <admin token>
```

期望：

- requestCount > 0
- totalTokens > 0
- successCount > 0

### 19.7 系统概览验证

```http
GET /api/observability/overview
Authorization: Bearer <admin token>
```

期望：

- projectCount > 0
- taskCount > 0
- modelRequestCount > 0

## 20. 回归验证

必须确认：

- Auth 登录不受影响
- Project 创建不受影响
- RAG 上传/搜索不受影响
- Chat SSE 不受影响
- Task execute 不受影响
- Model Gateway fallback 不受影响
- Smoke test 仍然通过

## 21. 完成报告模板

完成后请按以下格式输出：

```markdown
# Milestone 11 完成报告

## 1. 新增/修改文件清单

...

## 2. 数据库表和索引清单

...

## 3. Audit Log 实现说明

...

## 4. Observability API 实现说明

...

## 5. Actuator 增强说明

...

## 6. Docker Compose / .env / README 说明

...

## 7. Smoke Test 固化结果

...

## 8. mvn clean compile / mvn test 结果

...

## 9. 手动接口验证结果

...

## 10. 回归验证结果

...

## 11. 是否可以进入 Milestone 12

...
```

## 22. Milestone 12 预告

如果 Milestone 11 验证通过，下一阶段建议进入：

```text
Milestone 12: 前端 Vue 3 企业级控制台基础工程
```

建议范围：

- Vite + Vue 3 + TypeScript 初始化
- Element Plus
- Pinia
- Axios API client
- 登录页
- 主布局
- 项目列表
- 项目详情
- Chat 页面
- Task 页面
- Agent 页面
- Knowledge Base 页面
- 基础权限路由

