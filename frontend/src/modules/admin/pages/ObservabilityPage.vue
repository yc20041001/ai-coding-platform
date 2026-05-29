<script setup lang="ts">
import { ref, onMounted } from 'vue'
import client from '@/shared/api/client'
import type { ApiResponse, PageResult } from '@/shared/api/types'
import { getOverview, getModelUsageSummary, type SystemOverview, type ModelUsageSummary, type AuditLogItem } from '@/modules/admin/api'
import ToolExecutionMetricsPanel from '@/modules/admin/components/ToolExecutionMetricsPanel.vue'
import ToolExecutionTraceDrawer from '@/modules/task/components/ToolExecutionTraceDrawer.vue'
import ToolIncidentPanel from '@/modules/admin/components/ToolIncidentPanel.vue'
import ToolAlertRulePanel from '@/modules/admin/components/ToolAlertRulePanel.vue'
import ToolEscalationPolicyPanel from '@/modules/admin/components/ToolEscalationPolicyPanel.vue'
import KnownIssueTemplatePanel from '@/modules/admin/components/KnownIssueTemplatePanel.vue'
import BetaTrialSessionPanel from '@/modules/admin/components/BetaTrialSessionPanel.vue'
import BetaTrialFeedbackPanel from '@/modules/admin/components/BetaTrialFeedbackPanel.vue'
import BetaEnvironmentReadinessPanel from '@/modules/admin/components/BetaEnvironmentReadinessPanel.vue'
import ModelCostDashboardPanel from '@/modules/admin/components/ModelCostDashboardPanel.vue'
import PrReviewQualityPanel from '@/modules/admin/components/PrReviewQualityPanel.vue'
import BetaReleaseGateDashboardPanel from '@/modules/admin/components/BetaReleaseGateDashboardPanel.vue'
import BetaReleaseDecisionPanel from '@/modules/admin/components/BetaReleaseDecisionPanel.vue'
import ReleaseReadinessDashboardPanel from '@/modules/admin/components/ReleaseReadinessDashboardPanel.vue'
import ReleaseRolloutPlanPanel from '@/modules/admin/components/ReleaseRolloutPlanPanel.vue'
import ReleaseVerificationPanel from '@/modules/admin/components/ReleaseVerificationPanel.vue'
import ReleaseRollbackDrillPanel from '@/modules/admin/components/ReleaseRollbackDrillPanel.vue'
import ReleaseAuditTimelinePanel from '@/modules/admin/components/ReleaseAuditTimelinePanel.vue'
import ReleasePostmortemReviewPanel from '@/modules/admin/components/ReleasePostmortemReviewPanel.vue'
import ReleaseEvidenceCenterPanel from '@/modules/admin/components/ReleaseEvidenceCenterPanel.vue'
import ReleaseSignoffPanel from '@/modules/admin/components/ReleaseSignoffPanel.vue'
import ReleaseExecutiveSummaryPanel from '@/modules/admin/components/ReleaseExecutiveSummaryPanel.vue'
import ReleasePortfolioDashboardPanel from '@/modules/admin/components/ReleasePortfolioDashboardPanel.vue'
import ReleaseGovernanceBaselinePanel from '@/modules/admin/components/ReleaseGovernanceBaselinePanel.vue'
import ReleaseRiskHeatmapPanel from '@/modules/admin/components/ReleaseRiskHeatmapPanel.vue'
import OrganizationTrialPolicyPanel from '@/modules/admin/components/OrganizationTrialPolicyPanel.vue'
import ReleaseGuardrailDashboardPanel from '@/modules/admin/components/ReleaseGuardrailDashboardPanel.vue'
import PortfolioDriftDashboardPanel from '@/modules/admin/components/PortfolioDriftDashboardPanel.vue'
import GovernanceRecommendationWorkflowPanel from '@/modules/admin/components/GovernanceRecommendationWorkflowPanel.vue'
import GovernanceWaiverPanel from '@/modules/admin/components/GovernanceWaiverPanel.vue'
import GovernanceWorkflowSummaryPanel from '@/modules/admin/components/GovernanceWorkflowSummaryPanel.vue'
import GovernanceSlaPolicyPanel from '@/modules/admin/components/GovernanceSlaPolicyPanel.vue'
import GovernanceEscalationPanel from '@/modules/admin/components/GovernanceEscalationPanel.vue'
import GovernanceOwnershipHealthPanel from '@/modules/admin/components/GovernanceOwnershipHealthPanel.vue'
import GovernanceCapacityForecastPanel from '@/modules/admin/components/GovernanceCapacityForecastPanel.vue'
import PredictiveRiskSignalPanel from '@/modules/admin/components/PredictiveRiskSignalPanel.vue'
import GovernanceBacklogHealthPanel from '@/modules/admin/components/GovernanceBacklogHealthPanel.vue'
import GovernanceSimulationScenarioPanel from '@/modules/admin/components/GovernanceSimulationScenarioPanel.vue'
import GovernanceSimulationComparisonPanel from '@/modules/admin/components/GovernanceSimulationComparisonPanel.vue'
import PolicyTuningSuggestionPanel from '@/modules/admin/components/PolicyTuningSuggestionPanel.vue'
import IncidentRootCauseEditor from '@/modules/admin/components/IncidentRootCauseEditor.vue'
import SimilarIncidentList from '@/modules/admin/components/SimilarIncidentList.vue'
import IncidentRetrospectiveEditor from '@/modules/admin/components/IncidentRetrospectiveEditor.vue'
import KnowledgeQualityReviewPanel from '@/modules/admin/components/KnowledgeQualityReviewPanel.vue'
import AuditLogFilters, { type AuditLogFilterValues } from '@/modules/admin/components/AuditLogFilters.vue'
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import { formatDateTime, formatNumber } from '@/shared/utils/format'
import { ElTag, ElButton, ElDescriptions, ElDescriptionsItem } from 'element-plus'
import { getIncident, listIncidentAlertDeliveries, retryAlertDelivery, listIncidentKnowledgeLinks, type ToolIncidentItem, type ToolAlertDeliveryItem, type IncidentKnowledgeLinkItem } from '@/modules/admin/api'
import { listProjects } from '@/modules/project/api'

const overview = ref<SystemOverview | null>(null)
const modelUsage = ref<ModelUsageSummary | null>(null)
const overviewError = ref(false)

const auditLogs = ref<AuditLogItem[]>([])
const loadingOverview = ref(false)
const loadingUsage = ref(false)
const loadingAudit = ref(false)
const auditPage = ref(1)
const auditTotal = ref(0)
const auditFilters = ref<AuditLogFilterValues>({})
const opsProjectId = ref<string | null>(null)

// Trace drawer
const traceDrawerExecId = ref<string | undefined>(undefined)
const traceDrawerVisible = ref(false)

function openTraceDrawer(executionId?: string | null) {
  if (executionId) {
    traceDrawerExecId.value = executionId
    traceDrawerVisible.value = true
  }
}

// Incident detail
const selectedIncidentId = ref<string | null>(null)
const selectedIncident = ref<ToolIncidentItem | null>(null)
const incidentDeliveries = ref<ToolAlertDeliveryItem[]>([])
const incidentDetailLoading = ref(false)
const incidentRetrying = ref<string | null>(null)
const incidentKnowledgeLinks = ref<IncidentKnowledgeLinkItem[]>([])
const rcaRefreshKey = ref(0)

// Beta trial state
const betaSelectedSessionId = ref<string | null>(null)
const betaPanelRefreshKey = ref(0)

// Rollout state
const selectedRolloutPlanId = ref<string | null>(null)
const rolloutPanelRefreshKey = ref(0)

// Rollout Audit state (39B)
const rolloutAuditRefreshKey = ref(0)

async function viewIncident(id: string) {
  selectedIncidentId.value = id
  incidentDetailLoading.value = true
  try {
    const [incidentRes, deliveriesRes, linksRes] = await Promise.all([
      getIncident(id),
      listIncidentAlertDeliveries(id),
      listIncidentKnowledgeLinks(id),
    ])
    selectedIncident.value = incidentRes.data.data
    incidentDeliveries.value = deliveriesRes.data.data
    incidentKnowledgeLinks.value = linksRes.data.data
  } catch {
    selectedIncident.value = null
    incidentDeliveries.value = []
    incidentKnowledgeLinks.value = []
  } finally {
    incidentDetailLoading.value = false
  }
}

function handleRcaSaved() {
  rcaRefreshKey.value++
  if (selectedIncidentId.value) {
    listIncidentKnowledgeLinks(selectedIncidentId.value).then(res => {
      incidentKnowledgeLinks.value = res.data.data
    })
  }
}

function closeIncidentDetail() {
  selectedIncidentId.value = null
  selectedIncident.value = null
  incidentDeliveries.value = []
}

async function handleRetryDelivery(deliveryId: string) {
  incidentRetrying.value = deliveryId
  try {
    await retryAlertDelivery(deliveryId)
    if (selectedIncidentId.value) {
      await viewIncident(selectedIncidentId.value)
    }
  } catch (e: any) {
    console.error('retry failed', e)
  } finally {
    incidentRetrying.value = null
  }
}

function severityTag(severity: string) {
  const map: Record<string, 'danger' | 'warning' | 'info'> = { CRITICAL: 'danger', HIGH: 'danger', MEDIUM: 'warning', LOW: 'info', INFO: 'info' }
  return map[severity] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = { OPEN: '待处理', ACKNOWLEDGED: '已确认', RESOLVED: '已解决', WONT_FIX: '不修复', FALSE_POSITIVE: '误报' }
  return map[status] || status
}

function statusTag(status: string) {
  const map: Record<string, 'danger' | 'warning' | 'success' | 'info'> = { OPEN: 'danger', ACKNOWLEDGED: 'warning', RESOLVED: 'success', WONT_FIX: 'info', FALSE_POSITIVE: 'info' }
  return map[status] || 'info'
}

async function loadOverview() {
  loadingOverview.value = true
  overviewError.value = false
  try {
    const [oRes, mRes] = await Promise.all([getOverview(), getModelUsageSummary()])
    overview.value = oRes.data.data
    modelUsage.value = mRes.data.data
  } catch {
    overviewError.value = true
  } finally {
    loadingOverview.value = false
    loadingUsage.value = false
  }
}

async function loadAudit(page: number, filters: AuditLogFilterValues = {}) {
  auditPage.value = page
  loadingAudit.value = true
  try {
    const params: Record<string, unknown> = { page, pageSize: 10 }
    if (filters.userId) params.userId = filters.userId
    if (filters.actionType) params.actionType = filters.actionType
    if (filters.resourceType) params.resourceType = filters.resourceType
    if (filters.resourceId) params.resourceId = filters.resourceId
    if (filters.startTime) params.startTime = filters.startTime
    if (filters.endTime) params.endTime = filters.endTime

    const res = await client.get<ApiResponse<PageResult<AuditLogItem>>>('/api/audit/logs', { params })
    auditLogs.value = res.data.data.records
    auditTotal.value = res.data.data.total
  } catch { /* handled */ } finally {
    loadingAudit.value = false
  }
}

async function loadOpsProject() {
  try {
    const res = await listProjects(1, 1)
    opsProjectId.value = res.data.data.records[0]?.id || null
  } catch {
    opsProjectId.value = null
  }
}

function handleAuditSearch(filters: AuditLogFilterValues) {
  auditFilters.value = filters
  loadAudit(1, filters)
}

function handleAuditReset() {
  auditFilters.value = {}
  loadAudit(1)
}

onMounted(() => {
  loadOverview()
  loadOpsProject()
  loadAudit(1)
})
</script>

<template>
  <div class="page-container">
    <DynamicWorkspace
      title="可观测性"
      subtitle="系统遥测与审计日志"
      eyebrow="监控"
    >
      <template #actions>
        <StatusPulse
          :status="overview ? 'Live' : 'Loading...'"
          :tone="overview ? 'success' : 'warning'"
        />
      </template>

      <NeonDivider tone="primary" style="margin-bottom:20px" />

      <ErrorState v-if="overviewError" title="加载失败" message="无法获取系统概览数据" retry-text="重试" @retry="loadOverview" style="margin-bottom:20px" />

      <TechPanel v-if="!overviewError" title="系统总览" glow style="margin-bottom:20px" v-loading="loadingOverview">
        <div class="card-grid" v-if="overview">
          <MetricTile :value="overview.projectCount" label="项目" />
          <MetricTile :value="overview.userCount" label="用户" accent="accent" />
          <MetricTile :value="overview.taskCount" label="任务" accent="success" />
          <MetricTile :value="overview.runningTaskCount" label="运行中" accent="warning" />
          <MetricTile :value="overview.completedTaskCount" label="已完成" accent="success" />
          <MetricTile :value="overview.agentCount" label="智能体" accent="accent" />
          <MetricTile :value="overview.knowledgeBaseCount" label="知识库" accent="warning" />
          <MetricTile :value="overview.documentCount" label="文档" />
          <MetricTile :value="overview.modelRequestCount" label="模型调用" accent="accent" />
          <MetricTile :value="overview.todayModelRequestCount" label="今日调用" accent="success" />
          <MetricTile :value="formatNumber(overview.todayTokenUsage)" label="今日 Token" accent="accent" />
          <MetricTile :value="overview.chatMessageCount" label="对话消息" accent="warning" />
        </div>
      </TechPanel>

      <TechPanel v-if="!overviewError" title="模型用量" glow style="margin-bottom:20px" v-loading="loadingUsage">
        <div class="card-grid" v-if="modelUsage">
          <MetricTile :value="modelUsage.requestCount" label="请求数" />
          <MetricTile :value="modelUsage.successCount" label="成功" accent="success" />
          <MetricTile :value="modelUsage.failureCount" label="失败" accent="danger" />
          <MetricTile :value="(modelUsage.successRate * 100).toFixed(1) + '%'" label="成功率" accent="accent" />
          <MetricTile :value="formatNumber(modelUsage.totalTokens)" label="Token 总量" accent="accent" />
          <MetricTile :value="modelUsage.avgLatencyMs.toFixed(0) + 'ms'" label="平均延迟" accent="warning" />
          <MetricTile :value="modelUsage.mockCount" label="模拟调用" />
          <MetricTile :value="modelUsage.realProviderCount" label="真实调用" accent="success" />
        </div>
      </TechPanel>

      <TechPanel v-if="!overviewError" glow style="margin-bottom:20px">
        <ToolExecutionMetricsPanel />
      </TechPanel>

      <TechPanel v-if="!overviewError" glow style="margin-bottom:20px" data-testid="incident-section">
        <ToolIncidentPanel
          v-if="opsProjectId"
          :project-id="opsProjectId"
          @view-incident="viewIncident"
        />
        <EmptyState v-else description="暂无可用于事件面板的项目" />
      </TechPanel>

      <TechPanel v-if="!overviewError" glow style="margin-bottom:20px" data-testid="alert-rule-section">
        <ToolAlertRulePanel v-if="opsProjectId" :project-id="opsProjectId" />
        <EmptyState v-else description="暂无可用于告警规则的项目" />
      </TechPanel>

      <TechPanel v-if="!overviewError" glow style="margin-bottom:20px" data-testid="escalation-policy-section">
        <ToolEscalationPolicyPanel v-if="opsProjectId" :project-id="opsProjectId" />
        <EmptyState v-else description="暂无可用于升级策略的项目" />
      </TechPanel>

      <TechPanel v-if="!overviewError" glow style="margin-bottom:20px" data-testid="known-issue-template-section">
        <KnownIssueTemplatePanel v-if="opsProjectId" :project-id="opsProjectId" />
        <EmptyState v-else description="暂无可用于已知问题模板的项目" />
      </TechPanel>

      <TechPanel v-if="!overviewError" glow style="margin-bottom:20px" data-testid="quality-review-section">
        <KnowledgeQualityReviewPanel v-if="opsProjectId" :project-id="opsProjectId" />
        <EmptyState v-else description="暂无可用于知识质量审查的项目" />
      </TechPanel>

      <!-- Beta Trial Sections -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">⚡ Beta 外部试用反馈循环</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="beta-session-section">
            <BetaTrialSessionPanel
              :key="'session-' + betaPanelRefreshKey"
              :project-id="opsProjectId"
              @dashboard-refresh="betaPanelRefreshKey++"
              @select-session="betaSelectedSessionId = $event"
            />
          </TechPanel>

          <TechPanel v-if="betaSelectedSessionId" glow style="margin-bottom:20px" data-testid="beta-feedback-section">
            <BetaTrialFeedbackPanel
              :key="'fb-' + betaPanelRefreshKey"
              :session-id="betaSelectedSessionId"
              @dashboard-refresh="betaPanelRefreshKey++"
            />
          </TechPanel>
          <EmptyState v-else description="请选择一个 Beta 试用会话来查看反馈" />
        </div>
        <div class="beta-panel-col">
          <TechPanel v-if="opsProjectId" glow style="margin-bottom:20px" data-testid="beta-readiness-section">
            <BetaEnvironmentReadinessPanel
              :key="'readiness-' + betaPanelRefreshKey"
              :project-id="opsProjectId"
              :session-id="betaSelectedSessionId"
              @dashboard-refresh="betaPanelRefreshKey++"
            />
          </TechPanel>
        </div>
      </div>

      <!-- Model Cost & PR Review Quality Sections -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">模型成本与 PR 评审质量</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="model-cost-section">
            <ModelCostDashboardPanel :project-id="opsProjectId" />
          </TechPanel>
        </div>
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="pr-quality-section">
            <PrReviewQualityPanel :project-id="opsProjectId" />
          </TechPanel>
        </div>
      </div>

      <!-- Beta Release Gate Sections -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">Beta 发布门禁与 Go/No-Go 决策</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="beta-release-gate-section">
            <BetaReleaseGateDashboardPanel :project-id="opsProjectId" />
          </TechPanel>
        </div>
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="beta-release-decision-section">
            <BetaReleaseDecisionPanel :project-id="opsProjectId" />
          </TechPanel>
        </div>
      </div>

      <!-- Release Rollout Sections (39A) -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">Beta → Production 发布 rollout</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel v-if="opsProjectId" glow style="margin-bottom:20px" data-testid="rollout-dashboard-section">
            <ReleaseReadinessDashboardPanel
              :key="'rd-' + rolloutPanelRefreshKey"
              :project-id="opsProjectId"
            />
          </TechPanel>
          <TechPanel v-if="opsProjectId" glow style="margin-bottom:20px" data-testid="rollout-plan-section">
            <ReleaseRolloutPlanPanel
              :key="'rp-' + rolloutPanelRefreshKey"
              :project-id="opsProjectId"
              @plan-selected="selectedRolloutPlanId = $event"
              @dashboard-refresh="rolloutPanelRefreshKey++"
            />
          </TechPanel>
        </div>
        <div class="beta-panel-col">
          <TechPanel v-if="opsProjectId" glow style="margin-bottom:20px" data-testid="rollout-verification-section">
            <ReleaseVerificationPanel
              :key="'rv-' + rolloutPanelRefreshKey"
              :project-id="opsProjectId"
              :plan-id="selectedRolloutPlanId"
              @dashboard-refresh="rolloutPanelRefreshKey++"
            />
          </TechPanel>
        </div>
      </div>

      <!-- Release Rollback & Audit Sections (39B) -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">回滚演练 & 发布审计</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel v-if="opsProjectId" glow style="margin-bottom:20px" data-testid="rollback-drill-section">
            <ReleaseRollbackDrillPanel
              :key="'rdrill-' + rolloutAuditRefreshKey"
              :project-id="opsProjectId"
              :plan-id="selectedRolloutPlanId"
              @dashboard-refresh="rolloutAuditRefreshKey++"
            />
          </TechPanel>
          <TechPanel v-if="opsProjectId" glow style="margin-bottom:20px" data-testid="postmortem-review-section">
            <ReleasePostmortemReviewPanel
              :key="'pm-' + rolloutAuditRefreshKey"
              :project-id="opsProjectId"
              :plan-id="selectedRolloutPlanId"
              @dashboard-refresh="rolloutAuditRefreshKey++"
            />
          </TechPanel>
        </div>
        <div class="beta-panel-col">
          <TechPanel v-if="opsProjectId" glow style="margin-bottom:20px" data-testid="audit-timeline-section">
            <ReleaseAuditTimelinePanel
              :key="'at-' + rolloutAuditRefreshKey"
              :project-id="opsProjectId"
              :plan-id="selectedRolloutPlanId"
            />
          </TechPanel>
        </div>
      </div>

      <!-- Release Evidence & Summary Sections (39C) -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">发布证据中心与执行摘要</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel v-if="opsProjectId" glow style="margin-bottom:20px" data-testid="evidence-center-section">
            <ReleaseEvidenceCenterPanel
              :project-id="opsProjectId"
              :plan-id="selectedRolloutPlanId"
            />
          </TechPanel>
          <TechPanel v-if="opsProjectId" glow style="margin-bottom:20px" data-testid="signoff-section">
            <ReleaseSignoffPanel
              :project-id="opsProjectId"
              :plan-id="selectedRolloutPlanId"
            />
          </TechPanel>
        </div>
        <div class="beta-panel-col">
          <TechPanel v-if="opsProjectId" glow style="margin-bottom:20px" data-testid="executive-summary-section">
            <ReleaseExecutiveSummaryPanel
              :project-id="opsProjectId"
              :plan-id="selectedRolloutPlanId"
            />
          </TechPanel>
        </div>
      </div>

      <!-- Multi-Project Release Governance Sections (40A) -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">多项目发布治理</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="portfolio-dashboard-section">
            <ReleasePortfolioDashboardPanel :project-id="opsProjectId" />
          </TechPanel>
        </div>
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="governance-baseline-section">
            <ReleaseGovernanceBaselinePanel />
          </TechPanel>
        </div>
      </div>

      <div v-if="opsProjectId" style="margin-bottom:20px">
        <TechPanel glow data-testid="risk-heatmap-section">
          <ReleaseRiskHeatmapPanel />
        </TechPanel>
      </div>

      <!-- Organization Governance Sections (40B) -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">组织级治理</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="org-policy-section">
            <OrganizationTrialPolicyPanel />
          </TechPanel>
        </div>
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="guardrail-dashboard-section">
            <ReleaseGuardrailDashboardPanel />
          </TechPanel>
        </div>
      </div>

      <div v-if="opsProjectId" style="margin-bottom:20px">
        <TechPanel glow data-testid="drift-dashboard-section">
          <PortfolioDriftDashboardPanel />
        </TechPanel>
      </div>

      <!-- Governance Workflow Sections (40C) -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">治理工作流</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="workflow-summary-section">
            <GovernanceWorkflowSummaryPanel />
          </TechPanel>
        </div>
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="recommendation-workflow-section">
            <GovernanceRecommendationWorkflowPanel />
          </TechPanel>
        </div>
      </div>

      <div v-if="opsProjectId" style="margin-bottom:20px">
        <TechPanel glow data-testid="waiver-section">
          <GovernanceWaiverPanel />
        </TechPanel>
      </div>


      <!-- Governance Operations Sections (41A) -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">治理运营</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="sla-policy-section">
            <GovernanceSlaPolicyPanel />
          </TechPanel>
        </div>
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="escalation-section">
            <GovernanceEscalationPanel />
          </TechPanel>
        </div>
      </div>

      <div v-if="opsProjectId" style="margin-bottom:20px">
        <TechPanel glow data-testid="ownership-health-section">
          <GovernanceOwnershipHealthPanel />
        </TechPanel>
      </div>

      <!-- Governance Forecast Sections (41B) -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">治理预测</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="capacity-forecast-section">
            <GovernanceCapacityForecastPanel />
          </TechPanel>
        </div>
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="risk-signal-section">
            <PredictiveRiskSignalPanel />
          </TechPanel>
        </div>
      </div>

      <div v-if="opsProjectId" style="margin-bottom:20px">
        <TechPanel glow data-testid="backlog-health-section">
          <GovernanceBacklogHealthPanel />
        </TechPanel>
      </div>

      <!-- Governance Simulation Sections (41C) -->
      <NeonDivider v-if="opsProjectId" tone="accent" style="margin-bottom:20px" />
      <div v-if="opsProjectId" class="beta-section-header" style="margin-bottom:12px">
        <span class="beta-section-title">治理模拟</span>
      </div>

      <div v-if="opsProjectId" class="beta-panels-row">
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="simulation-scenario-section">
            <GovernanceSimulationScenarioPanel />
          </TechPanel>
        </div>
        <div class="beta-panel-col">
          <TechPanel glow style="margin-bottom:20px" data-testid="simulation-comparison-section">
            <GovernanceSimulationComparisonPanel />
          </TechPanel>
        </div>
      </div>

      <div v-if="opsProjectId" style="margin-bottom:20px">
        <TechPanel glow data-testid="tuning-suggestion-section">
          <PolicyTuningSuggestionPanel />
        </TechPanel>
      </div>

      <TechPanel v-loading="loadingAudit" title="审计日志">
        <AuditLogFilters @search="handleAuditSearch" @reset="handleAuditReset" />
        <el-table :data="auditLogs" size="small" style="width:100%">
          <el-table-column prop="actionType" label="操作" width="140" />
          <el-table-column label="资源" width="140">
            <template #default="{ row }">{{ row.resourceType }} #{{ row.resourceId }}</template>
          </el-table-column>
          <el-table-column prop="username" label="用户" width="100" />
          <el-table-column label="成功" width="80">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="auditTotal > 0" v-model:current-page="auditPage"
          :page-size="10" :total="auditTotal"
          layout="total, prev, pager, next" size="small"
          style="margin-top:16px;justify-content:flex-end"
          @current-change="loadAudit"
        />
        <EmptyState v-if="!loadingAudit && auditLogs.length === 0" description="暂无审计日志" />
      </TechPanel>
    </DynamicWorkspace>
  </div>

  <!-- Tool Execution Trace Drawer -->
  <ToolExecutionTraceDrawer
    v-model="traceDrawerVisible"
    :execution-id="traceDrawerExecId"
  />

  <!-- Incident Detail Drawer -->
  <el-drawer
    :model-value="!!selectedIncidentId"
    @update:model-value="closeIncidentDetail"
    title="事件详情"
    size="50%"
    :before-close="closeIncidentDetail"
    data-testid="incident-detail-drawer"
  >
    <div v-if="incidentDetailLoading" class="op-loading">加载中...</div>
    <template v-else-if="selectedIncident">
      <div class="op-detail-header">
        <h3 class="op-detail-title">{{ selectedIncident.title }}</h3>
        <div class="op-detail-tags">
          <ElTag :type="severityTag(selectedIncident.severity)" size="small" effect="dark">{{ selectedIncident.severity }}</ElTag>
          <ElTag :type="statusTag(selectedIncident.status)" size="small" effect="dark">{{ statusText(selectedIncident.status) }}</ElTag>
          <ElTag size="small">{{ selectedIncident.sourceType }}</ElTag>
        </div>
      </div>

      <ElDescriptions :column="2" border size="small" style="margin-bottom:16px">
        <ElDescriptionsItem label="ID">{{ selectedIncident.id }}</ElDescriptionsItem>
        <ElDescriptionsItem label="项目 ID">{{ selectedIncident.projectId }}</ElDescriptionsItem>
        <ElDescriptionsItem label="首次出现">{{ formatDateTime(selectedIncident.firstSeenAt) }}</ElDescriptionsItem>
        <ElDescriptionsItem label="最后出现">{{ formatDateTime(selectedIncident.lastSeenAt) }}</ElDescriptionsItem>
        <ElDescriptionsItem v-if="selectedIncident.summary" label="摘要" :span="2">{{ selectedIncident.summary }}</ElDescriptionsItem>
        <ElDescriptionsItem v-if="selectedIncident.resolution" label="解决方案" :span="2">{{ selectedIncident.resolution }}</ElDescriptionsItem>
        <ElDescriptionsItem v-if="selectedIncident.assigneeId" label="处理人">{{ selectedIncident.assigneeId }}</ElDescriptionsItem>
        <ElDescriptionsItem v-if="selectedIncident.toolExecutionId" label="Tool 执行 ID">{{ selectedIncident.toolExecutionId }}</ElDescriptionsItem>
        <ElDescriptionsItem v-if="selectedIncident.toolJobId" label="Tool Job ID">{{ selectedIncident.toolJobId }}</ElDescriptionsItem>
        <ElDescriptionsItem v-if="selectedIncident.operatorReviewId" label="Operator 审查 ID">{{ selectedIncident.operatorReviewId }}</ElDescriptionsItem>
      </ElDescriptions>

      <!-- Root Cause Analysis -->
      <div class="op-section-title" data-testid="rca-section">根因分析</div>
      <IncidentRootCauseEditor
        :key="'rca-' + rcaRefreshKey"
        :incident-id="selectedIncident.id"
        @saved="handleRcaSaved"
      />

      <!-- Knowledge Links -->
      <div class="op-section-title" data-testid="knowledge-links-section">知识关联</div>
      <div v-if="incidentKnowledgeLinks.length === 0" class="op-empty">暂无知识关联</div>
      <div v-for="link in incidentKnowledgeLinks" :key="link.id" class="op-delivery-item">
        <div class="op-delivery-row">
          <ElTag size="small" effect="dark">{{ link.linkType }}</ElTag>
          <span class="op-delivery-channel">{{ link.title }}</span>
          <span class="op-delivery-time">{{ formatDateTime(link.createTime) }}</span>
        </div>
      </div>

      <!-- Similar Incidents -->
      <div class="op-section-title" data-testid="similar-incidents-section">相似事件</div>
      <SimilarIncidentList
        :key="'sim-' + selectedIncident.id"
        :incident-id="selectedIncident.id"
      />

      <!-- Retrospective Editor -->
      <div class="op-section-title" data-testid="retrospective-section">事后回顾</div>
      <IncidentRetrospectiveEditor
        :key="'retro-' + selectedIncident.id"
        :incident-id="selectedIncident.id"
        @saved="handleRcaSaved"
      />

      <!-- Alert Deliveries -->
      <div class="op-section-title">告警投递记录</div>
      <div v-if="incidentDeliveries.length === 0" class="op-empty">暂无投递记录</div>
      <div v-for="del in incidentDeliveries" :key="del.id" class="op-delivery-item">
        <div class="op-delivery-row">
          <ElTag size="small" :type="del.status === 'DELIVERED' ? 'success' : del.status === 'FAILED' ? 'danger' : 'warning'" effect="dark">
            {{ del.status }}
          </ElTag>
          <span class="op-delivery-channel">{{ del.channel }}</span>
          <span v-if="del.routeTarget" class="op-delivery-target">{{ del.routeTarget }}</span>
          <span class="op-delivery-time">{{ formatDateTime(del.createTime) }}</span>
          <ElButton
            v-if="del.status === 'FAILED' || del.status === 'PENDING'"
            size="small"
            type="primary"
            link
            :loading="incidentRetrying === del.id"
            @click="handleRetryDelivery(del.id)"
            data-testid="op-retry-btn"
          >重试</ElButton>
        </div>
        <div v-if="del.errorMessage" class="op-delivery-error">{{ del.errorMessage }}</div>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.op-loading, .op-empty {
  padding: 40px;
  text-align: center;
  color: var(--app-text-muted);
  font-size: 14px;
}

.op-detail-header {
  margin-bottom: 16px;
}

.op-detail-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text-bright);
  margin: 0 0 8px;
}

.op-detail-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.op-section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--app-text-soft);
  margin: 0 0 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.op-delivery-item {
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 8px;
}

.op-delivery-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.op-delivery-channel {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-soft);
}

.op-delivery-target {
  font-size: 12px;
  color: var(--app-text-muted);
  font-family: 'SF Mono', 'Cascadia Code', monospace;
}

.op-delivery-time {
  font-size: 11px;
  color: var(--app-text-muted);
  margin-left: auto;
}

.op-delivery-error {
  font-size: 11px;
  color: var(--el-color-danger);
  margin-top: 4px;
}

.beta-section-header {
  display: flex;
  align-items: center;
}
.beta-section-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--app-accent);
  text-transform: uppercase;
  letter-spacing: 1px;
}
.beta-panels-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}
.beta-panel-col {
  display: flex;
  flex-direction: column;
}
@media (max-width: 1200px) {
  .beta-panels-row { grid-template-columns: 1fr; }
}
</style>
