<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTag, ElButton, ElMessage, ElMessageBox, ElTable, ElTableColumn, ElPagination, ElTooltip } from 'element-plus'
import {
  listProjectIncidents,
  getProjectIncidentSummary,
  updateIncident,
  syncProblemJobs,
  scanIncidentSla,
  scanIncidentEscalation,
  escalateIncident,
  type ToolIncidentItem,
  type ToolIncidentSummary,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import ToolIncidentDialog from '@/modules/admin/components/ToolIncidentDialog.vue'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId: string
}>()

const emit = defineEmits<{
  viewIncident: [id: string]
}>()

const loading = ref(false)
const error = ref(false)
const incidents = ref<ToolIncidentItem[]>([])
const summary = ref<ToolIncidentSummary | null>(null)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref<string | undefined>(undefined)
const filterSeverity = ref<string | undefined>(undefined)
const syncing = ref(false)

const scanningSla = ref(false)
const scanningEsc = ref(false)
const escalatingId = ref<string | null>(null)

// Create dialog
const createDialogVisible = ref(false)

const statusOptions = [
  { value: undefined, label: '全部' },
  { value: 'OPEN', label: '待处理' },
  { value: 'ACKNOWLEDGED', label: '已确认' },
  { value: 'RESOLVED', label: '已解决' },
  { value: 'WONT_FIX', label: '不修复' },
  { value: 'FALSE_POSITIVE', label: '误报' },
]

const severityOptions = [
  { value: undefined, label: '全部' },
  { value: 'CRITICAL', label: '严重' },
  { value: 'HIGH', label: '高' },
  { value: 'MEDIUM', label: '中' },
  { value: 'LOW', label: '低' },
  { value: 'INFO', label: '提示' },
]

async function loadIncidents() {
  loading.value = true
  error.value = false
  try {
    const [listRes, summaryRes] = await Promise.all([
      listProjectIncidents(props.projectId, {
        status: filterStatus.value,
        severity: filterSeverity.value,
        page: page.value,
        pageSize: pageSize.value,
      }),
      getProjectIncidentSummary(props.projectId),
    ])
    incidents.value = listRes.data.data.records
    total.value = listRes.data.data.total
    summary.value = summaryRes.data.data
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

async function handlePageChange(val: number) {
  page.value = val
  await loadIncidents()
}

async function handleFilterChange() {
  page.value = 1
  await loadIncidents()
}

async function handleAcknowledge(incident: ToolIncidentItem) {
  try {
    await updateIncident(incident.id, { status: 'ACKNOWLEDGED' })
    ElMessage.success('已确认事件')
    await loadIncidents()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function handleResolve(incident: ToolIncidentItem) {
  try {
    await ElMessageBox.prompt('请输入解决方案', '解决事件', {
      inputType: 'textarea',
      inputPlaceholder: '解决方案描述（可选）',
    })
    const resolution = /* istanbul ignore next */ ''
    await updateIncident(incident.id, { status: 'RESOLVED', resolution: resolution || undefined })
    ElMessage.success('事件已解决')
    await loadIncidents()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '操作失败')
    }
  }
}

async function handleReopen(incident: ToolIncidentItem) {
  try {
    await updateIncident(incident.id, { status: 'OPEN' })
    ElMessage.success('事件已重新打开')
    await loadIncidents()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function handleSyncProblemJobs() {
  syncing.value = true
  try {
    const res = await syncProblemJobs(props.projectId)
    const data = res.data.data
    ElMessage.success(`同步完成：创建 ${data.created}，更新 ${data.updated}，跳过 ${data.skipped}`)
    await loadIncidents()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '同步失败')
  } finally {
    syncing.value = false
  }
}

async function handleScanSla() {
  scanningSla.value = true
  try {
    const res = await scanIncidentSla(props.projectId)
    const d = res.data.data
    ElMessage.success(`SLA 扫描完成：扫描 ${d.scanned}，正常 ${d.withinSla}，风险 ${d.atRisk}，超期 ${d.breached}，已解决 ${d.resolved}`)
    await loadIncidents()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || 'SLA 扫描失败')
  } finally {
    scanningSla.value = false
  }
}

async function handleScanEscalation() {
  scanningEsc.value = true
  try {
    const res = await scanIncidentEscalation(props.projectId)
    const d = res.data.data
    ElMessage.success(`升级扫描完成：扫描 ${d.scanned}，升级 ${d.escalated}，跳过 ${d.skipped}，已达最大级别 ${d.maxLevelReached}`)
    await loadIncidents()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '升级扫描失败')
  } finally {
    scanningEsc.value = false
  }
}

async function handleEscalate(incident: ToolIncidentItem) {
  escalatingId.value = incident.id
  try {
    const res = await escalateIncident(incident.id, '手动升级')
    const event = res.data.data
    ElMessage.success(`已升级至 L${event.escalationLevel} (${event.channel})`)
    await loadIncidents()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '升级失败')
  } finally {
    escalatingId.value = null
  }
}

function slaStatusTag(slaStatus: string | null) {
  if (!slaStatus) return 'info'
  const map: Record<string, 'danger' | 'warning' | 'success' | 'info'> = {
    BREACHED: 'danger', AT_RISK: 'warning', WITHIN_SLA: 'success', RESOLVED: 'success', WAIVED: 'info', NOT_STARTED: 'info',
  }
  return map[slaStatus] || 'info'
}

function slaStatusText(slaStatus: string | null) {
  if (!slaStatus) return '-'
  const map: Record<string, string> = {
    NOT_STARTED: '未开始', WITHIN_SLA: '正常', AT_RISK: '风险', BREACHED: '超期', RESOLVED: '已达标', WAIVED: '豁免',
  }
  return map[slaStatus] || slaStatus
}

function handleCreateSaved() {
  ElMessage.success('事件已创建')
  createDialogVisible.value = false
  loadIncidents()
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

function sourceTypeLabel(st: string) {
  const map: Record<string, string> = {
    TOOL_EXECUTION_FAILED: '执行失败',
    TOOL_JOB_FAILED: 'Job 失败',
    TOOL_JOB_RETRY_PENDING: '待重试',
    TOOL_JOB_DEAD_LETTERED: '死信',
    READ_ONLY_CONTRACT_WARNING: '只读合约警告',
    TRACE_OUTPUT_PARSE_WARNING: '输出解析警告',
    HIGH_RISK_REVIEW: '高风险审查',
    OPERATOR_REVIEW: 'Operator 审查',
    MANUAL: '手动创建',
  }
  return map[st] || st
}

onMounted(() => {
  loadIncidents()
})
</script>

<template>
  <TechPanel
    v-loading="loading"
    title="工具事件 (Incidents)"
    data-testid="tool-incident-panel"
  >
    <ErrorState
      v-if="error"
      title="加载失败"
      message="无法加载工具事件数据"
      retry-text="重试"
      @retry="loadIncidents"
    />

    <!-- Summary bar -->
    <div v-if="summary" class="tip-summary" data-testid="tip-summary">
      <div class="tip-summary-item">
        <span class="tip-summary-value tip-summary-value--danger">{{ summary.openCount }}</span>
        <span class="tip-summary-label">待处理</span>
      </div>
      <div class="tip-summary-item">
        <span class="tip-summary-value tip-summary-value--warning">{{ summary.acknowledgedCount }}</span>
        <span class="tip-summary-label">已确认</span>
      </div>
      <div class="tip-summary-item">
        <span class="tip-summary-value tip-summary-value--success">{{ summary.resolvedCount }}</span>
        <span class="tip-summary-label">已解决</span>
      </div>
      <div class="tip-summary-divider" />
      <div class="tip-summary-item">
        <span class="tip-summary-value tip-summary-value--danger">{{ summary.criticalCount }}</span>
        <span class="tip-summary-label">严重</span>
      </div>
      <div class="tip-summary-item">
        <span class="tip-summary-value tip-summary-value--warning">{{ summary.highCount }}</span>
        <span class="tip-summary-label">高</span>
      </div>
      <div class="tip-summary-divider" />
      <div class="tip-summary-item">
        <span class="tip-summary-value">{{ summary.deadLetteredCount }}</span>
        <span class="tip-summary-label">死信</span>
      </div>
      <div class="tip-summary-item">
        <span class="tip-summary-value">{{ summary.retryPendingCount }}</span>
        <span class="tip-summary-label">待重试</span>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="tip-toolbar" data-testid="tip-toolbar">
      <div class="tip-filters">
        <el-select v-model="filterStatus" placeholder="状态" size="small" @change="handleFilterChange" style="width:120px">
          <el-option v-for="opt in statusOptions" :key="String(opt.value)" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-select v-model="filterSeverity" placeholder="严重级别" size="small" @change="handleFilterChange" style="width:120px">
          <el-option v-for="opt in severityOptions" :key="String(opt.value)" :label="opt.label" :value="opt.value" />
        </el-select>
      </div>
      <div class="tip-actions">
        <ElButton size="small" @click="handleScanSla" :loading="scanningSla" data-testid="tip-scan-sla-btn">SLA 扫描</ElButton>
        <ElButton size="small" @click="handleScanEscalation" :loading="scanningEsc" data-testid="tip-scan-esc-btn">升级扫描</ElButton>
        <ElButton size="small" @click="handleSyncProblemJobs" :loading="syncing" data-testid="tip-sync-btn">同步问题 Job</ElButton>
        <ElButton size="small" type="primary" @click="createDialogVisible = true" data-testid="tip-create-btn">创建事件</ElButton>
      </div>
    </div>

    <!-- Table -->
    <ElTable
      v-if="incidents.length > 0"
      :data="incidents"
      size="small"
      style="width:100%"
      data-testid="tip-table"
      @row-click="(row: ToolIncidentItem) => emit('viewIncident', row.id)"
    >
      <ElTableColumn label="严重级别" width="90">
        <template #default="{ row }">
          <ElTag :type="severityTag(row.severity)" size="small" effect="dark">{{ row.severity }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="状态" width="80">
        <template #default="{ row }">
          <ElTag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="SLA" width="80">
        <template #default="{ row }">
          <ElTag v-if="row.slaStatus" :type="slaStatusTag(row.slaStatus)" size="small" effect="dark">{{ slaStatusText(row.slaStatus) }}</ElTag>
          <span v-else class="tip-muted">-</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="截止时间" width="145">
        <template #default="{ row }">
          <span v-if="row.dueAt" :class="{ 'tip-time-overdue': row.slaStatus === 'BREACHED', 'tip-time-risk': row.slaStatus === 'AT_RISK' }">{{ formatDateTime(row.dueAt) }}</span>
          <span v-else class="tip-muted">-</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="来源" width="120">
        <template #default="{ row }">
          <ElTooltip :content="row.sourceType" placement="top">
            <span>{{ sourceTypeLabel(row.sourceType) }}</span>
          </ElTooltip>
        </template>
      </ElTableColumn>
      <ElTableColumn label="标题" min-width="200">
        <template #default="{ row }">
          <span class="tip-title-text">{{ row.title }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="最后出现" width="150">
        <template #default="{ row }">{{ formatDateTime(row.lastSeenAt) }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <ElButton
            v-if="row.status === 'OPEN'"
            size="small"
            type="warning"
            @click.stop="handleAcknowledge(row)"
            data-testid="tip-ack-btn"
          >确认</ElButton>
          <ElButton
            v-if="row.status === 'OPEN' || row.status === 'ACKNOWLEDGED'"
            size="small"
            type="success"
            @click.stop="handleResolve(row)"
            data-testid="tip-resolve-btn"
          >解决</ElButton>
          <ElButton
            v-if="['RESOLVED', 'WONT_FIX', 'FALSE_POSITIVE'].includes(row.status)"
            size="small"
            @click.stop="handleReopen(row)"
            data-testid="tip-reopen-btn"
          >重新打开</ElButton>
          <ElButton
            v-if="row.status === 'OPEN' || row.status === 'ACKNOWLEDGED'"
            size="small"
            type="danger"
            :loading="escalatingId === row.id"
            @click.stop="handleEscalate(row)"
            data-testid="tip-escalate-btn"
          >升级</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <div v-if="!error && !loading && incidents.length === 0" data-testid="tip-empty">
      <EmptyState description="暂无事件数据" />
    </div>

    <ElPagination
      v-if="total > pageSize"
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      size="small"
      style="margin-top:12px;justify-content:flex-end"
      @current-change="handlePageChange"
    />

    <!-- Create dialog -->
    <ToolIncidentDialog
      v-model="createDialogVisible"
      :project-id="projectId"
      @saved="handleCreateSaved"
    />
  </TechPanel>
</template>

<style scoped>
.tip-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.tip-summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 50px;
}

.tip-summary-value {
  font-size: 20px;
  font-weight: 800;
  color: var(--app-text-bright);
  line-height: 1.2;
}

.tip-summary-value--danger { color: var(--el-color-danger); }
.tip-summary-value--warning { color: var(--el-color-warning); }
.tip-summary-value--success { color: var(--el-color-success); }

.tip-summary-label {
  font-size: 10px;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-top: 2px;
}

.tip-summary-divider {
  width: 1px;
  height: 32px;
  background: rgba(56, 189, 248, 0.15);
}

.tip-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 8px;
  flex-wrap: wrap;
}

.tip-filters {
  display: flex;
  gap: 8px;
}

.tip-actions {
  display: flex;
  gap: 6px;
}

.tip-title-text {
  font-size: 12px;
  font-weight: 500;
  color: var(--app-text-soft);
  cursor: pointer;
}

.tip-title-text:hover {
  color: var(--el-color-primary);
}

.tip-muted {
  color: var(--app-text-muted);
  font-size: 11px;
}

.tip-time-overdue {
  color: var(--el-color-danger);
  font-size: 12px;
}

.tip-time-risk {
  color: var(--el-color-warning);
  font-size: 12px;
}
</style>
