<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  getModelCostDashboard,
  refreshModelCost,
  scanModelCostAlerts,
  listModelCostAlerts,
  updateModelCostAlertStatus,
  exportModelCostReport,
  type ModelCostDashboard,
  type ModelCostAlertItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { formatNumber } from '@/shared/utils/format'
import { ElTag, ElButton, ElDialog, ElMessage } from 'element-plus'

const props = defineProps<{
  projectId?: string | null
  loading?: boolean
}>()

const dashboard = ref<ModelCostDashboard | null>(null)
const alerts = ref<ModelCostAlertItem[]>([])
const loadingDashboard = ref(false)
const error = ref(false)
const refreshing = ref(false)
const scanning = ref(false)
const exportDialogVisible = ref(false)
const exportContent = ref('')
const exporting = ref(false)

function loadDashboard() {
  if (!props.projectId) return
  loadingDashboard.value = true
  error.value = false
  getModelCostDashboard(props.projectId)
    .then(res => { dashboard.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loadingDashboard.value = false })
}

function loadAlerts() {
  if (!props.projectId) return
  listModelCostAlerts(props.projectId, { page: 1, size: 20 })
    .then(res => { alerts.value = res.data.data })
    .catch(() => {})
}

async function handleRefresh() {
  if (!props.projectId) return
  refreshing.value = true
  try {
    await refreshModelCost(props.projectId)
    await scanModelCostAlerts(props.projectId)
    ElMessage.success('成本数据刷新并扫描完成')
    loadDashboard()
    loadAlerts()
  } catch {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

async function handleScan() {
  if (!props.projectId) return
  scanning.value = true
  try {
    const res = await scanModelCostAlerts(props.projectId)
    ElMessage.success(`扫描完成，发现 ${res.data.data.length} 条告警`)
    loadAlerts()
  } catch {
    ElMessage.error('扫描失败')
  } finally {
    scanning.value = false
  }
}

async function handleUpdateAlertStatus(alertId: string, status: string) {
  try {
    await updateModelCostAlertStatus(alertId, status)
    ElMessage.success('状态已更新')
    loadAlerts()
    loadDashboard()
  } catch {
    ElMessage.error('更新失败')
  }
}

async function handleExport() {
  if (!props.projectId) return
  exporting.value = true
  try {
    const res = await exportModelCostReport(props.projectId)
    exportContent.value = res.data.data.content
    exportDialogVisible.value = true
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

function alertTypeTag(type: string) {
  const map: Record<string, 'danger' | 'warning' | 'info'> = {
    DAILY_COST_SPIKE: 'danger',
    HIGH_FAILURE_COST: 'warning',
    HIGH_FALLBACK_RATE: 'warning',
    HIGH_RETRY_COST: 'info',
    LATENCY_COST_ANOMALY: 'info',
  }
  return map[type] || 'info'
}

function severityTag(severity: string) {
  const map: Record<string, 'danger' | 'warning' | 'info'> = {
    CRITICAL: 'danger',
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'info',
    INFO: 'info',
  }
  return map[severity] || 'info'
}

watch(() => props.projectId, (val) => {
  if (val) {
    loadDashboard()
    loadAlerts()
  }
}, { immediate: true })
</script>

<template>
  <TechPanel title="模型成本分析" glow v-loading="loadingDashboard" data-testid="model-cost-panel">
    <ErrorState v-if="error" title="加载失败" message="无法获取成本数据" retry-text="重试" @retry="loadDashboard" />
    <template v-else>
      <div style="display:flex;gap:6px;margin-bottom:12px;flex-wrap:wrap">
        <ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新与扫描</ElButton>
        <ElButton size="small" :loading="scanning" @click="handleScan">仅扫描告警</ElButton>
        <ElButton size="small" :loading="exporting" @click="handleExport">导出报告</ElButton>
      </div>

      <div class="card-grid" v-if="dashboard">
        <MetricTile :value="'$' + (dashboard.totalCostToday ?? 0).toFixed(4)" label="今日成本" accent="warning" />
        <MetricTile :value="'$' + (dashboard.totalCostThisWeek ?? 0).toFixed(4)" label="本周成本" accent="accent" />
        <MetricTile :value="'$' + (dashboard.totalCostThisMonth ?? 0).toFixed(4)" label="本月成本" />
        <MetricTile :value="dashboard.totalRequestsToday ?? 0" label="今日请求数" accent="success" />
        <MetricTile :value="'$' + (dashboard.averageCostPerRequest ?? 0).toFixed(6)" label="平均单次成本" />
        <MetricTile
          :value="(dashboard.costChangePercent != null ? (dashboard.costChangePercent > 0 ? '+' : '') + dashboard.costChangePercent.toFixed(1) + '%' : '-')"
          label="周环比"
          :accent="(dashboard.costChangePercent ?? 0) > 0 ? 'danger' : 'success'"
        />
      </div>

      <div v-if="dashboard && dashboard.topModelsByCost && dashboard.topModelsByCost.length > 0" style="margin-top:16px">
        <div style="font-size:12px;font-weight:700;color:var(--app-text-soft);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.05em">Top 模型成本</div>
        <div v-for="m in dashboard.topModelsByCost" :key="m.modelName" class="cost-model-row">
          <div class="cost-model-info">
            <span class="cost-model-name">{{ m.provider }}/{{ m.modelName }}</span>
            <span class="cost-model-count">{{ m.requestCount }} 请求</span>
          </div>
          <span class="cost-model-value">${{ (m.estimatedCost ?? 0).toFixed(4) }}</span>
        </div>
      </div>

      <div style="margin-top:16px">
        <div style="font-size:12px;font-weight:700;color:var(--app-text-soft);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.05em">模型成本告警</div>
        <div v-if="!alerts || alerts.length === 0" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">暂无告警</div>
        <div v-for="a in alerts" :key="a.id" class="cost-alert-item">
          <div class="cost-alert-header">
            <ElTag :type="severityTag(a.severity)" size="small" effect="dark">{{ a.severity }}</ElTag>
            <ElTag :type="alertTypeTag(a.alertType)" size="small">{{ a.alertType }}</ElTag>
            <ElTag size="small" effect="plain">{{ a.status }}</ElTag>
            <span class="cost-alert-summary">{{ a.summary }}</span>
            <span class="cost-alert-date">{{ a.statDate }}</span>
          </div>
          <div v-if="a.status === 'OPEN'" class="cost-alert-actions">
            <ElButton size="small" type="primary" link @click="handleUpdateAlertStatus(a.id, 'ACKNOWLEDGED')">确认</ElButton>
            <ElButton size="small" link @click="handleUpdateAlertStatus(a.id, 'IGNORED')">忽略</ElButton>
          </div>
          <div v-else-if="a.status === 'ACKNOWLEDGED'" class="cost-alert-actions">
            <ElButton size="small" type="success" link @click="handleUpdateAlertStatus(a.id, 'RESOLVED')">解决</ElButton>
          </div>
        </div>
      </div>
    </template>

    <ElDialog v-model="exportDialogVisible" title="模型成本报告" width="80%" data-testid="cost-export-dialog">
      <pre style="background:var(--app-surface);padding:16px;border-radius:6px;max-height:500px;overflow:auto;font-size:12px;white-space:pre-wrap">{{ exportContent }}</pre>
    </ElDialog>
  </TechPanel>
</template>

<style scoped>
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
}
.cost-model-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  margin-bottom: 4px;
}
.cost-model-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cost-model-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-bright);
}
.cost-model-count {
  font-size: 11px;
  color: var(--app-text-muted);
}
.cost-model-value {
  font-size: 13px;
  font-weight: 700;
  color: var(--app-warning);
}
.cost-alert-item {
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 6px;
}
.cost-alert-header {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.cost-alert-summary {
  font-size: 12px;
  color: var(--app-text-soft);
  flex: 1;
}
.cost-alert-date {
  font-size: 11px;
  color: var(--app-text-muted);
}
.cost-alert-actions {
  margin-top: 6px;
  display: flex;
  gap: 6px;
}
</style>
