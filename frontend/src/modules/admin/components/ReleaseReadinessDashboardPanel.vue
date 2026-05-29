<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  getRolloutDashboard,
  getRolloutSummary,
  generateRolloutReport,
  listRolloutPlans,
  type ReleaseReadinessDashboard,
  type ReleaseRolloutSummary,
  type ReleaseReadinessReport,
  type ReleaseRolloutPlanItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag, ElButton, ElDialog, ElMessage, ElSelect, ElOption } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId?: string | null
}>()

const dashboard = ref<ReleaseReadinessDashboard | null>(null)
const plans = ref<ReleaseRolloutPlanItem[]>([])
const loading = ref(false)
const error = ref(false)
const selectedPlanId = ref<string | null>(null)
const summary = ref<ReleaseRolloutSummary | null>(null)
const loadingSummary = ref(false)
const reportData = ref<ReleaseReadinessReport | null>(null)
const reportDialogVisible = ref(false)

function loadDashboard() {
  if (!props.projectId) return
  loading.value = true
  error.value = false
  Promise.all([
    getRolloutDashboard(props.projectId),
    listRolloutPlans(props.projectId),
  ])
    .then(([dRes, pRes]) => {
      dashboard.value = dRes.data.data
      plans.value = pRes.data.data
      if (pRes.data.data.length > 0 && !selectedPlanId.value) {
        selectedPlanId.value = pRes.data.data[0].id
      }
    })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

function loadSummary() {
  if (!props.projectId || !selectedPlanId.value) return
  loadingSummary.value = true
  getRolloutSummary(props.projectId, selectedPlanId.value)
    .then(res => { summary.value = res.data.data })
    .catch(() => { summary.value = null })
    .finally(() => { loadingSummary.value = false })
}

async function handleViewReport() {
  if (!props.projectId || !selectedPlanId.value) return
  try {
    const res = await generateRolloutReport(props.projectId, selectedPlanId.value)
    reportData.value = res.data.data
    reportDialogVisible.value = true
  } catch {
    ElMessage.error('生成报告失败')
  }
}

function statusTag(status: string | null | undefined) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    READY: 'success',
    IN_PROGRESS: 'warning',
    OBSERVING: 'warning',
    COMPLETED: 'success',
    ROLLED_BACK: 'danger',
    CANCELLED: 'info',
    DRAFT: 'info',
  }
  return map[status || ''] || 'info'
}

function readinessTag(status: string | null | undefined) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    READY: 'success',
    BLOCKED: 'danger',
    WARN: 'warning',
    PENDING: 'info',
  }
  return map[status || ''] || 'info'
}

watch(() => props.projectId, () => { loadDashboard() }, { immediate: true })
watch(selectedPlanId, () => { loadSummary() })
</script>

<template>
  <div>
    <div style="margin-bottom:12px;display:flex;align-items:center;gap:8px">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">发布就绪仪表板</span>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取发布就绪数据" retry-text="重试" @retry="loadDashboard" />

    <div v-if="dashboard" class="card-grid" style="margin-bottom:16px">
      <MetricTile label="决策状态" :value="dashboard.decisionStatus || 'N/A'" accent="accent" />
      <MetricTile label="发布状态" :value="dashboard.rolloutStatus || 'N/A'" />
      <MetricTile label="整体就绪" :value="dashboard.overallReadinessStatus || 'N/A'" />
      <MetricTile :value="dashboard.blockingIssueCount" label="阻塞问题" accent="danger" />
      <MetricTile :value="dashboard.warningIssueCount" label="警告" accent="warning" />
      <MetricTile :value="dashboard.preReleasePassRate != null ? (dashboard.preReleasePassRate * 100).toFixed(0) + '%' : 'N/A'" label="预发通过率" accent="accent" />
      <MetricTile :value="dashboard.observationVerificationCount" label="观测验证数" />
      <MetricTile :value="dashboard.rollbackRecommended ? '是' : '否'" label="建议回滚" :accent="dashboard.rollbackRecommended ? 'danger' : 'success'" />
    </div>

    <div v-if="plans.length > 0" style="margin-bottom:12px;display:flex;align-items:center;gap:8px">
      <span style="font-size:12px;color:var(--app-text-muted)">选择 Plan:</span>
      <ElSelect v-model="selectedPlanId" size="small" style="width:200px">
        <ElOption v-for="p in plans" :key="p.id" :label="p.releaseLabel" :value="p.id" />
      </ElSelect>
      <ElButton size="small" @click="handleViewReport" :disabled="!selectedPlanId">查看报告</ElButton>
    </div>

    <div v-if="summary" v-loading="loadingSummary" class="card-grid" style="margin-bottom:12px">
      <MetricTile :value="summary.totalSteps" label="总步骤" />
      <MetricTile :value="summary.passedSteps" label="通过" accent="success" />
      <MetricTile :value="summary.failedSteps" label="失败" accent="danger" />
      <MetricTile :value="summary.blockedSteps" label="阻塞" accent="warning" />
      <MetricTile :value="summary.totalVerifications" label="验证数" />
      <MetricTile :value="summary.failedVerifications" label="失败验证" accent="danger" />
      <MetricTile :value="summary.overallResult || 'N/A'" label="总体结果" :accent="summary.overallResult === 'SUCCESS' ? 'success' : summary.overallResult === 'ROLLED_BACK' ? 'danger' : 'warning'" />
    </div>

    <EmptyState v-if="!loading && plans.length === 0 && !error" description="暂无发布数据" />

    <div v-if="reportData" style="font-size:11px;color:var(--app-text-muted)">上次报告生成: {{ formatDateTime(reportData.generatedAt) }}</div>

    <ElDialog v-model="reportDialogVisible" title="发布就绪报告" width="70%" top="5vh">
      <div v-if="reportData" style="white-space:pre-wrap;font-family:'SF Mono','Cascadia Code',monospace;font-size:12px;line-height:1.6;background:rgba(15,23,42,0.3);padding:16px;border-radius:8px;max-height:60vh;overflow-y:auto">
        {{ reportData.reportMarkdown }}
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
}
</style>
