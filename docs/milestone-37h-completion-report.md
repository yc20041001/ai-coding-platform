# Milestone 37H 完成报告

## 工具执行事件与告警路由 (Incident & Alert Routing)

### 完成日期
2026-05-22

### 实现概要
实现了完整的工具执行事件管理、告警规则配置和告警投递路由功能。

### 后端变更

#### 新文件

| 文件 | 说明 |
|------|------|
| `db/migration/V34__init_tool_incident_alert_tables.sql` | Flyway 迁移：3 张新表 |
| `domain/ToolIncidentStatus.java` | 事件状态枚举 |
| `domain/ToolIncidentSeverity.java` | 事件严重级别枚举 |
| `domain/ToolIncidentSourceType.java` | 事件来源类型枚举 |
| `domain/ToolAlertChannel.java` | 告警通道枚举 |
| `domain/ToolAlertDeliveryStatus.java` | 投递状态枚举 |
| `domain/ToolIncidentEntity.java` | 事件实体 |
| `domain/ToolAlertRuleEntity.java` | 告警规则实体 |
| `domain/ToolAlertDeliveryEntity.java` | 告警投递实体 |
| `infrastructure/ToolIncidentMapper.java` | 事件 Mapper |
| `infrastructure/ToolAlertRuleMapper.java` | 告警规则 Mapper |
| `infrastructure/ToolAlertDeliveryMapper.java` | 告警投递 Mapper |
| `dto/CreateToolIncidentRequest.java` | 创建事件请求 |
| `dto/UpdateToolIncidentRequest.java` | 更新事件请求 |
| `dto/ToolIncidentResponse.java` | 事件响应 |
| `dto/ToolIncidentSummaryResponse.java` | 事件摘要响应 |
| `dto/CreateToolAlertRuleRequest.java` | 创建告警规则请求 |
| `dto/UpdateToolAlertRuleRequest.java` | 更新告警规则请求 |
| `dto/ToolAlertRuleResponse.java` | 告警规则响应 |
| `dto/ToolAlertDeliveryResponse.java` | 告警投递响应 |
| `application/ToolIncidentService.java` | 事件服务 (CRUD/状态转换/同步问题 Job/摘要) |
| `application/ToolAlertRuleService.java` | 告警规则服务 (CRUD/匹配规则) |
| `application/ToolAlertDeliveryService.java` | 告警投递服务 (路由/列表/重试) |
| `controller/ToolIncidentAlertController.java` | 11 个 API 端点 |

#### 修改文件

| 文件 | 变更 |
|------|------|
| `test/resources/schema.sql` | 新增 3 张测试表 |

### 11 个 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/orchestration/incidents` | 创建事件 |
| PUT | `/api/orchestration/incidents/{id}` | 更新事件 |
| GET | `/api/orchestration/incidents/{id}` | 获取事件详情 |
| GET | `/api/projects/{projectId}/incidents` | 分页查询事件 |
| GET | `/api/projects/{projectId}/incidents/summary` | 事件摘要 |
| POST | `/api/projects/{projectId}/incidents/sync-problem-jobs` | 同步问题 Job |
| POST | `/api/orchestration/alert-rules` | 创建告警规则 |
| PUT | `/api/orchestration/alert-rules/{id}` | 更新告警规则 |
| GET | `/api/projects/{projectId}/alert-rules` | 查询告警规则 |
| GET | `/api/projects/{projectId}/alert-deliveries` | 查询投递记录 |
| POST | `/api/orchestration/alert-deliveries/{id}/retry` | 重试投递 |

### 核心业务逻辑

- **事件状态转换**: OPEN → 任意状态, ACKNOWLEDGED → 终态, 终态 → OPEN (重新打开)
- **自动时间戳**: acknowledgedAt/resolvedAt 在状态转换时自动设置
- **告警路由匹配**: 按 projectId + sourceType + minSeverity 匹配启用的规则
- **同步问题 Job**: 扫描 FAILED/RETRY_PENDING/DEAD_LETTERED 状态的 Tool Job，自动创建事件
- **幂等性**: 同 tool_job_id + source_type 非终态事件仅更新 lastSeenAt，终态事件跳过

### 前端变更

#### 新文件

| 文件 | 说明 |
|------|------|
| `admin/components/ToolIncidentPanel.vue` | 事件列表面板（摘要/过滤/操作） |
| `admin/components/ToolIncidentDialog.vue` | 创建事件对话框 |
| `admin/components/ToolAlertRulePanel.vue` | 告警规则管理面板 |
| `e2e/incident-alert-routing.spec.ts` | 8 个 E2E 测试 |

#### 修改文件

| 文件 | 变更 |
|------|------|
| `admin/api.ts` | 新增 Incident/AlertRule/AlertDelivery 类型和 API |
| `admin/pages/ObservabilityPage.vue` | 集成事件面板、告警规则面板、事件详情抽屉 |
| `task/components/ToolExecutionTraceDrawer.vue` | 添加"创建事件"按钮和对话框 |
| `task/components/ToolOperatorReviewDialog.vue` | 审查列表中添加"创建事件"入口 |

### 测试覆盖率

| 测试文件 | 测试数 |
|----------|--------|
| `ToolIncidentWorkflowIntegrationTest.java` | 38 个 |
| 前端 E2E | 8 个 |

#### 测试覆盖场景
- 事件 CRUD: 创建/查询/更新/列表（含过滤和分页）
- 所有严重级别和来源类型
- 状态转换验证（确认/解决/重新打开/不修复/误报）
- 无效状态转换拒绝
- 输入校验（空标题、无效枚举值）
- 告警规则 CRUD（含所有通道类型）
- 告警投递路由匹配（按来源和级别）
- 投递重试
- 同步问题 Job（空结果）
- 不破坏 36A-37G 已有 671 个测试

### 质量门禁
- Backend: `mvn test` — 709 tests, 0 failures ✓
- Frontend: `vue-tsc --noEmit` — 0 errors ✓
- Frontend: `npm run build` — 0 errors ✓
