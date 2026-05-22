# Milestone 37D: Tool Execution Metrics Dashboard

## 1. 背景

Milestone 37A-37C 已经完成异步工具执行基础设施：

- 37A: Async Worker Queue with Redis / RabbitMQ
- 37B: Read-only Code Search Index
- 37C: Worker DLQ / Retry Backoff

当前系统已经具备：

```text
Tool Execution Job
  -> Async Worker
  -> Retry Backoff
  -> DLQ
  -> Manual Retry
  -> Timeout Recovery
```

但这些执行质量数据还没有集中展示。运维和管理员无法快速看到：

- 工具执行总量
- 成功率 / 失败率
- DLQ 数量
- 平均耗时
- 哪些工具最容易失败
- 哪些项目工具执行异常
- 最近 30 天趋势

Milestone 37D 的目标是新增 **Tool Execution Metrics Dashboard**：

```text
tool_execution_job / tool_sandbox_execution
  -> DB aggregation
  -> Observability API
  -> Admin dashboard panels
```

本阶段不引入 Prometheus / Grafana，仅基于数据库聚合实现平台内置可观测性。

## 2. 总目标

实现工具执行指标看板：

1. 后端新增工具执行指标聚合服务。
2. 支持全局 summary。
3. 支持项目级 summary。
4. 支持按工具维度统计。
5. 支持最近 30 天 daily trend。
6. 支持 DLQ / retry / failed job 列表入口。
7. 前端 Observability 页面新增 Tool Worker 面板。
8. 前端 Project Observability 或 Project Detail 可展示项目级工具指标。
9. 补齐后端测试和前端 E2E。

完成后，系统从：

```text
Worker can recover from failure
```

升级为：

```text
Worker health can be observed and diagnosed
```

## 3. 严格边界

必须遵守：

1. 不引入 Prometheus。
2. 不引入 Grafana。
3. 不新增外部监控服务。
4. 不做实时 WebSocket。
5. 不做 SSE 实时指标。
6. 不做复杂 OLAP。
7. 不改 Worker 执行语义。
8. 不破坏 37A-37C 已有 API。
9. 不破坏已有 Observability 页面。
10. 不暴露敏感 payload。
11. 前端保持中文暗色科技风 UI。

允许做：

- MyBatis-Plus / SQL 聚合。
- 简单 DTO。
- Admin Observability 面板。
- 项目级指标 API。
- 最近 30 天趋势。

## 4. 数据来源

主要表：

```text
tool_execution_job
tool_sandbox_execution
tool_catalog
project
```

指标基于：

- `tool_execution_job.status`
- `tool_execution_job.tool_key`
- `tool_execution_job.duration_ms`
- `tool_execution_job.retry_count`
- `tool_execution_job.error_code`
- `tool_execution_job.failure_stage`
- `tool_execution_job.create_time`
- `tool_sandbox_execution.status`
- `tool_sandbox_execution.execution_mode`

本阶段不新增表。

## 5. 指标定义

### 5.1 Summary Metrics

字段：

| 字段 | 说明 |
|---|---|
| totalJobs | 总 Job 数 |
| pendingJobs | PENDING 数 |
| runningJobs | RUNNING 数 |
| completedJobs | COMPLETED 数 |
| failedJobs | FAILED 数 |
| retryPendingJobs | RETRY_PENDING 数 |
| canceledJobs | CANCELED 数 |
| deadLetteredJobs | DEAD_LETTERED 数 |
| successRate | completed / total |
| failureRate | (failed + deadLettered) / total |
| retryRate | retryCount > 0 的 job / total |
| avgDurationMs | completed job 平均耗时 |
| maxDurationMs | 最大耗时 |
| totalRetries | retry_count 总和 |

### 5.2 Tool Metrics

按 toolKey 聚合：

- toolKey
- totalJobs
- completedJobs
- failedJobs
- deadLetteredJobs
- successRate
- avgDurationMs
- totalRetries
- topErrorCode
- topFailureStage

### 5.3 Daily Metrics

最近 30 天：

- date
- totalJobs
- completedJobs
- failedJobs
- deadLetteredJobs
- retryPendingJobs
- avgDurationMs

### 5.4 Failure Metrics

按 errorCode / failureStage 聚合：

- errorCode
- count
- latestTime

- failureStage
- count
- latestTime

## 6. 后端 DTO

新增：

```text
backend/src/main/java/com/aicoding/platform/observability/dto/ToolExecutionSummaryResponse.java
backend/src/main/java/com/aicoding/platform/observability/dto/ToolExecutionToolMetricResponse.java
backend/src/main/java/com/aicoding/platform/observability/dto/ToolExecutionDailyMetricResponse.java
backend/src/main/java/com/aicoding/platform/observability/dto/ToolExecutionFailureMetricResponse.java
backend/src/main/java/com/aicoding/platform/observability/dto/ToolExecutionMetricsResponse.java
```

### 6.1 ToolExecutionMetricsResponse

组合响应：

```java
public class ToolExecutionMetricsResponse {
    private ToolExecutionSummaryResponse summary;
    private List<ToolExecutionToolMetricResponse> tools;
    private List<ToolExecutionDailyMetricResponse> daily;
    private List<ToolExecutionFailureMetricResponse> errorCodes;
    private List<ToolExecutionFailureMetricResponse> failureStages;
}
```

所有数字使用 Long / BigDecimal / Double，避免 int 溢出。

## 7. 后端服务

新增：

```text
backend/src/main/java/com/aicoding/platform/observability/application/ToolExecutionMetricsApplicationService.java
```

方法：

```java
public ToolExecutionMetricsResponse getGlobalMetrics()
public ToolExecutionMetricsResponse getProjectMetrics(Long projectId)
public List<ToolExecutionJobResponse> listProblemJobs(Long projectId, String status, Integer limit)
```

权限：

| 方法 | 权限 |
|---|---|
| getGlobalMetrics | ADMIN |
| getProjectMetrics | ADMIN 或项目 MAINTAINER+ |
| listProblemJobs | ADMIN 或项目 MAINTAINER+ |

### 7.1 查询实现建议

可以使用：

- MyBatis-Plus QueryWrapper
- Java stream 聚合
- 简单 mapper SQL

考虑项目规模当前较小，允许先查最近 30 天 / 最近 N 条数据后内存聚合。

建议限制：

- global metrics 默认聚合最近 30 天。
- problem jobs 默认 limit=50，最大 200。

## 8. 后端 API

新增 Controller：

```text
backend/src/main/java/com/aicoding/platform/observability/controller/ToolExecutionMetricsController.java
```

端点：

| Method | Endpoint | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/observability/tool-executions/metrics` | ADMIN | 全局工具执行指标 |
| GET | `/api/projects/{projectId}/observability/tool-executions/metrics` | ADMIN or MAINTAINER+ | 项目工具执行指标 |
| GET | `/api/projects/{projectId}/observability/tool-executions/problem-jobs?status=&limit=` | ADMIN or MAINTAINER+ | 项目问题 Job |

响应示例：

```json
{
  "code": "OK",
  "data": {
    "summary": {
      "totalJobs": 120,
      "completedJobs": 110,
      "deadLetteredJobs": 2,
      "successRate": 0.9167,
      "avgDurationMs": 42
    },
    "tools": [],
    "daily": [],
    "errorCodes": [],
    "failureStages": []
  }
}
```

## 9. 前端 API

修改或新增：

```text
frontend/src/modules/admin/api.ts
```

新增类型：

```ts
export interface ToolExecutionSummary {
  totalJobs: number
  pendingJobs: number
  runningJobs: number
  completedJobs: number
  failedJobs: number
  retryPendingJobs: number
  canceledJobs: number
  deadLetteredJobs: number
  successRate: number
  failureRate: number
  retryRate: number
  avgDurationMs: number
  maxDurationMs: number
  totalRetries: number
}

export interface ToolExecutionToolMetric {
  toolKey: string
  totalJobs: number
  completedJobs: number
  failedJobs: number
  deadLetteredJobs: number
  successRate: number
  avgDurationMs: number
  totalRetries: number
  topErrorCode: string | null
  topFailureStage: string | null
}

export interface ToolExecutionDailyMetric {
  date: string
  totalJobs: number
  completedJobs: number
  failedJobs: number
  deadLetteredJobs: number
  retryPendingJobs: number
  avgDurationMs: number
}

export interface ToolExecutionMetrics {
  summary: ToolExecutionSummary
  tools: ToolExecutionToolMetric[]
  daily: ToolExecutionDailyMetric[]
  errorCodes: ToolExecutionFailureMetric[]
  failureStages: ToolExecutionFailureMetric[]
}
```

新增函数：

```ts
getGlobalToolExecutionMetrics()
getProjectToolExecutionMetrics(projectId: string)
getProjectProblemToolJobs(projectId: string, status?: string, limit?: number)
```

## 10. 前端组件

新增：

```text
frontend/src/modules/admin/components/ToolExecutionMetricsPanel.vue
```

展示：

1. Summary cards：
   - Total Jobs
   - Success Rate
   - Failed / DLQ
   - Avg Duration
   - Retry Pending
2. Tool metrics table：
   - toolKey
   - successRate
   - failed
   - deadLettered
   - avgDuration
   - topErrorCode
3. Daily trend：
   - 简单表格或轻量 CSS bars，不引入图表库。
4. Failure metrics：
   - errorCode list
   - failureStage list

data-testid：

- `tool-metrics-panel`
- `tool-metrics-summary`
- `tool-metrics-table`
- `tool-metrics-daily`
- `tool-metrics-failures`

## 11. Observability 页面集成

修改：

```text
frontend/src/modules/admin/pages/ObservabilityPage.vue
```

新增 Tool Worker 区块：

```text
Tool Worker Metrics
```

加载：

```ts
getGlobalToolExecutionMetrics()
```

要求：

- API 失败不影响其他 observability 面板。
- 使用 ErrorState / LoadingState。
- 无数据时显示 EmptyState。

## 12. Project Detail 可选集成

可选：

```text
frontend/src/modules/project/pages/ProjectDetailPage.vue
```

如果已有 Observability tab，可显示项目级 tool metrics。

为了控制范围，本阶段可以只做 Admin Observability 全局面板。

## 13. 安全与隐私

指标 API 不得返回：

- requestPayload
- resultPayload
- inputPayload
- outputPayload
- API Key
- token
- 文件内容
- diff 内容

只返回聚合数字和枚举字段。

problem jobs API 可以返回 job 元数据，但不得返回 payload。

## 14. 后端测试

新增：

```text
backend/src/test/java/com/aicoding/platform/observability/ToolExecutionMetricsIntegrationTest.java
```

测试不少于 20 个：

1. ADMIN 可查询全局 metrics。
2. 未登录查询全局 metrics 返回 UNAUTHORIZED。
3. 非 ADMIN 查询全局 metrics 返回 FORBIDDEN。
4. 项目 MAINTAINER 可查询项目 metrics。
5. 非项目成员查询项目 metrics 返回 PROJECT_ACCESS_DENIED。
6. summary totalJobs 正确。
7. completedJobs 正确。
8. failedJobs 正确。
9. deadLetteredJobs 正确。
10. retryPendingJobs 正确。
11. successRate 计算正确。
12. failureRate 计算正确。
13. avgDurationMs 计算正确。
14. tool metrics 按 toolKey 聚合。
15. topErrorCode 正确。
16. topFailureStage 正确。
17. daily metrics 最近 30 天。
18. problem jobs limit 生效。
19. problem jobs 不返回 payload。
20. 空数据返回 0 而不是 null。

全量后端质量门：

```bash
cd backend
mvn test
```

## 15. 前端 E2E

新增或修改：

```text
frontend/e2e/knowledge-observability.spec.ts
```

或新增：

```text
frontend/e2e/tool-execution-metrics.spec.ts
```

测试：

1. ADMIN 打开 Observability 页面。
2. Tool Worker Metrics 面板可见。
3. Summary cards 可见。
4. Tool metrics table 可见或 EmptyState 可见。
5. Failure metrics 区块可见。
6. 页面无 JS error。

运行：

```bash
bash scripts/start-e2e-backend.sh
cd frontend
npm run test:e2e -- --workers=1
```

## 16. 文档与报告

完成后新增：

```text
docs/milestone-37d-completion-report.md
```

报告必须包含：

1. 新增 / 修改文件清单
2. Tool Execution Metrics 指标说明
3. 后端聚合服务说明
4. 后端 API 清单
5. 前端 ToolExecutionMetricsPanel 说明
6. Observability 页面集成说明
7. 安全与隐私说明
8. 后端测试结果
9. 前端 typecheck / build / E2E 结果
10. 已知限制
11. 是否可以进入 Milestone 37E

## 17. 验收标准

必须满足：

- 全局 metrics API 可用。
- 项目 metrics API 可用。
- summary 字段完整。
- toolKey 聚合可用。
- daily trend 可用。
- failure metrics 可用。
- problem jobs 不返回 payload。
- Observability 页面展示 Tool Worker Metrics。
- API 失败不影响其他 Observability 区块。
- 后端 `mvn test` 通过。
- 前端 `npm run typecheck` 通过。
- 前端 `npm run build` 通过。
- E2E 通过或说明不可运行原因。

## 18. 已知非目标

本阶段不做：

- Prometheus
- Grafana
- WebSocket 实时指标
- SSE 实时指标
- 复杂图表库
- 告警通知
- 长期指标归档
- 多维 OLAP

这些可进入后续：

- 37E: Real Read-only Tool Adapter Hardening
- 38A: Code Search Semantic RAG
- 38B: Worker Autoscaling / DLQ Operations
- 38C: Prometheus / Grafana Optional Integration

## 19. Claude 执行提示词

把下面内容直接发给 Claude 执行：

```text
请根据项目文档执行 Milestone 37D。

文档路径：
docs/milestone-37d-tool-execution-metrics-dashboard.md

执行要求：
1. 先完整阅读该文档，再检查当前 backend/frontend 代码结构。
2. 本阶段是在 Milestone 37C Worker DLQ / Retry Backoff 基础上，新增 Tool Execution Metrics Dashboard。
3. 不要引入 Prometheus / Grafana。
4. 不要新增外部监控服务。
5. 不要返回 payload、文件内容、diff 内容、API Key、token 等敏感信息。
6. 不要改变 Worker 执行语义。
7. 不要破坏 36A-37C 已有 API。
8. 不要破坏已有 Observability 页面。
9. 遵循现有规范：Spring Boot 3.x、MyBatis-Plus、无 Lombok、构造器注入、手写 getter/setter、ApiResponse、BizException、ErrorCode。
10. IDs 对外保持 String。
11. 前端保持中文暗色科技风 UI，复用现有组件。

需要实现：
1. 新增 ToolExecutionMetricsApplicationService。
2. 新增 ToolExecutionMetricsController。
3. 新增 ToolExecutionSummaryResponse / ToolExecutionToolMetricResponse / ToolExecutionDailyMetricResponse / ToolExecutionFailureMetricResponse / ToolExecutionMetricsResponse。
4. 实现全局 metrics API：
   - GET /api/observability/tool-executions/metrics
5. 实现项目 metrics API：
   - GET /api/projects/{projectId}/observability/tool-executions/metrics
6. 实现 problem jobs API：
   - GET /api/projects/{projectId}/observability/tool-executions/problem-jobs?status=&limit=
7. 前端 admin/api.ts 增加 metrics 类型和 API。
8. 新增 ToolExecutionMetricsPanel.vue。
9. ObservabilityPage.vue 集成 Tool Worker Metrics 区块。
10. 后端测试不少于 20 个。
11. 前端 E2E 覆盖 Observability Tool Metrics 面板。
12. 新增 docs/milestone-37d-completion-report.md。

完成后必须执行：
cd backend && mvn test
cd frontend && npm run typecheck
cd frontend && npm run build
bash scripts/start-e2e-backend.sh
cd frontend && npm run test:e2e -- --workers=1

完成后按以下格式输出：
1. 新增 / 修改文件清单
2. Tool Execution Metrics 指标说明
3. 后端聚合服务说明
4. 后端 API 清单
5. 前端 ToolExecutionMetricsPanel 说明
6. Observability 页面集成说明
7. 安全与隐私说明
8. 后端测试结果
9. 前端 typecheck / build / E2E 结果
10. 已知限制
11. 是否可以进入 Milestone 37E

现在开始实现，不要只给计划。
```
