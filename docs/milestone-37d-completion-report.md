# Milestone 37D: Tool Execution Metrics Dashboard — 完成报告

## 1. 总览

| 项目 | 值 |
|------|-----|
| 里程碑 | 37D — Tool Execution Metrics Dashboard |
| 分支 | main |
| 完成日期 | 2026-05-21 |
| 后端新增文件 | 7 |
| 前端新增/修改文件 | 3 |
| 后端测试 | 24 个集成测试，全部通过 |
| E2E 测试 | 7 个新增用例 |

## 2. 完成功能

### 2.1 后端聚合服务

新增 5 个 DTO 和一个 Application Service，基于 `tool_execution_job` 表提供 30 天内指标聚合：

| 端点 | 权限 | 功能 |
|------|------|------|
| `GET /api/observability/tool-executions/metrics` | ADMIN | 全局工具执行指标 |
| `GET /api/projects/{projectId}/observability/tool-executions/metrics` | MAINTAINER+ | 项目级工具执行指标 |
| `GET /api/projects/{projectId}/observability/tool-executions/problem-jobs` | MAINTAINER+ | 问题作业列表（支持 status/limit 参数） |

### 2.2 聚合维度

- **Summary** — 总量、各状态计数、成功率/失败率/重试率、平均耗时/最大耗时、总重试次数
- **Tool Metrics** — 按工具 key 分组统计，含 Top 错误码和 Top 失败阶段
- **Daily Trend** — 近 30 天逐日趋势，无数据日期自动补 0
- **Error Code Distribution** — 错误码聚合（按次数降序）
- **Failure Stage Distribution** — 失败阶段聚合（按次数降序）
- **Problem Jobs** — 列出 FAILED / RETRY_PENDING / DEAD_LETTERED 作业，不返回 payload

### 2.3 前端面板

- 新增 `ToolExecutionMetricsPanel.vue` — 包含 MetricTile 汇总卡片、工具维度 el-table、CSS 柱状趋势图、错误码/失败阶段分布
- 集成至 `ObservabilityPage.vue` — 位于模型用量面板与审计日志之间，错误隔离设计
- 全部使用 data-testid 属性，支持 E2E 测试
- 空数据/错误状态均有处理

### 2.4 测试覆盖

**后端集成测试 (24 tests)**:

| 类别 | 测试数 | 覆盖点 |
|------|--------|--------|
| 空数据 | 1 | 无数据时 summary 全部为 0，工具/错误码列表为空 |
| Summary | 3 | 混合状态计数、成功率/失败率计算、无 completed 时的边界 |
| Tool Metrics | 4 | 分组聚合、按总量降序排序、Top 错误码/失败阶段、平均耗时计算 |
| Daily Metrics | 2 | 30 天完整覆盖、按日期聚合 |
| 失败指标 | 3 | 错误码/失败阶段分组去重、无错误属性的作业不出现 |
| 项目级 | 1 | 跨项目隔离 |
| 重试指标 | 1 | 总重试次数、重试率、平均耗时 |
| 30 天过滤 | 1 | 超过 30 天的作业不纳入 |
| Problem Jobs | 4 | 默认 3 状态 + 按 status 过滤 + limit + payload 安全 |
| 鉴权 | 1 | 全局接口未登录返回 401 |
| 边界情况 | 2 | 全 0 时长、maxDurationMs |

**前端 E2E 测试 (7 cases)**:
- 访问 observability 页面
- Tool Worker Metrics 面板可见
- 汇总卡片区域渲染
- 工具维度表格或空态可见
- 失败指标区块可见
- 每日趋势区块可见
- 页面无 JS 错误

## 3. 新增文件清单

```
backend/src/main/java/com/aicoding/platform/observability/dto/
├── ToolExecutionSummaryResponse.java
├── ToolExecutionToolMetricResponse.java
├── ToolExecutionDailyMetricResponse.java
├── ToolExecutionFailureMetricResponse.java
├── ToolExecutionMetricsResponse.java

backend/src/main/java/com/aicoding/platform/observability/application/
└── ToolExecutionMetricsApplicationService.java

backend/src/main/java/com/aicoding/platform/observability/controller/
└── ToolExecutionMetricsController.java

backend/src/test/java/com/aicoding/platform/observability/
└── ToolExecutionMetricsIntegrationTest.java

frontend/src/modules/admin/components/
└── ToolExecutionMetricsPanel.vue
```

## 4. 修改文件清单

```
frontend/src/modules/admin/api.ts              — 新增 ToolExecutionMetrics 类型和 API
frontend/src/modules/admin/pages/ObservabilityPage.vue — 集成 ToolExecutionMetricsPanel
frontend/e2e/knowledge-observability.spec.ts   — 新增工具指标面板 E2E 用例
```

## 5. 设计决策

1. **不引入额外图表库** — 趋势图使用纯 CSS 柱状图，避免增加前端依赖
2. **问题列表不返回 payload** — `toSafeJobResponse()` 明确置空 `requestPayload` 和 `resultPayload`，避免敏感数据泄露
3. **错误隔离** — 指标接口失败不影响 observability 页面上其他面板
4. **30 天窗口硬编码** — 当前固定为 30 天，后续可改为可配参数
5. **`default-property-inclusion: non_null`** — Spring Jackson 全局配置，确保响应体最小化

## 6. 已知限制

1. **30 天窗口硬编码** — 当前固定查询最近 30 天数据，不支持自定义时间范围
2. **纯 DB 聚合** — 不涉及 Prometheus/Grafana/OLAP，大数据量下聚合性能可能下降
3. **无实时指标** — 数据仅反映请求时刻的快照，不做 WebSocket/SSE 推送
4. **无多维分析** — 不支持按项目 + 工具 + 时间等多维下钻
5. **无告警** — 失败率超过阈值时不触发通知

## 7. 是否可以进入 Milestone 37E

**是**。Milestone 37D 已完成全部要求：

- ✅ 全局 metrics API 可用
- ✅ 项目 metrics API 可用
- ✅ summary 字段完整
- ✅ toolKey 聚合可用
- ✅ daily trend 可用
- ✅ failure metrics 可用
- ✅ problem jobs 不返回 payload
- ✅ Observability 页面展示 Tool Worker Metrics
- ✅ API 失败不影响其他 Observable 区块
- ✅ 后端 `mvn test` 通过（549 tests, 0 failures）
- ✅ 前端 `npm run typecheck` 通过
- ✅ 前端 `npm run build` 通过
- ✅ E2E 测试已更新
