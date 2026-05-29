<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import {
  listBetaReadinessChecks, createBetaReadinessCheck, getBetaDashboard,
  type BetaEnvironmentReadinessItem, type BetaTrialDashboard,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import { ElTag, ElButton, ElDialog, ElInput, ElSelect, ElOption, ElMessage } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = withDefaults(defineProps<{
  projectId?: string | null
  sessionId?: string | null
  loading?: boolean
}>(), {
  projectId: null,
  sessionId: null,
  loading: false,
})

const emit = defineEmits<{
  dashboardRefresh: []
}>()

const checks = ref<BetaEnvironmentReadinessItem[]>([])
const dashboard = ref<BetaTrialDashboard | null>(null)
const loadingChecks = ref(false)
const error = ref(false)
const createDialogVisible = ref(false)

const createForm = ref({
  sessionId: '', targetName: '', targetType: 'SERVICE', checkStatus: 'PASS', summary: '', detailJson: '',
})

async function loadData() {
  if (!props.projectId && !props.sessionId) return
  loadingChecks.value = true
  error.value = false
  try {
    const [checksRes, dashRes] = await Promise.all([
      listBetaReadinessChecks(props.projectId || undefined, props.sessionId || undefined),
      props.projectId ? getBetaDashboard(props.projectId) : Promise.resolve(null),
    ])
    checks.value = checksRes.data.data
    if (dashRes) {
      dashboard.value = dashRes.data.data
    }
  } catch {
    error.value = true
  } finally {
    loadingChecks.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.targetName || !props.projectId) return
  try {
    await createBetaReadinessCheck(props.projectId, {
      sessionId: createForm.value.sessionId || props.sessionId || undefined,
      targetName: createForm.value.targetName,
      targetType: createForm.value.targetType,
      checkStatus: createForm.value.checkStatus,
      summary: createForm.value.summary || undefined,
      detailJson: createForm.value.detailJson || undefined,
    })
    ElMessage.success('检查记录已创建')
    createDialogVisible.value = false
    createForm.value = { sessionId: '', targetName: '', targetType: 'SERVICE', checkStatus: 'PASS', summary: '', detailJson: '' }
    await loadData()
    emit('dashboardRefresh')
  } catch {
    ElMessage.error('创建失败')
  }
}

function statusTag(s: string): 'info' | 'warning' | 'success' | 'danger' {
  const map: Record<string, 'info' | 'warning' | 'success' | 'danger'> = { PASS: 'success', WARN: 'warning', FAIL: 'danger', SKIP: 'info' }
  return map[s] || 'info'
}

function statusLabel(s: string) {
  const map: Record<string, string> = { PASS: '通过', WARN: '警告', FAIL: '失败', SKIP: '跳过' }
  return map[s] || s
}

onMounted(() => { loadData() })
watch(() => [props.projectId, props.sessionId], () => { loadData() })
</script>

<template>
  <TechPanel
    v-loading="loadingChecks || loading"
    title="Environment Readiness"
    data-testid="beta-readiness-panel"
  >
    <ErrorState
      v-if="error"
      title="无法加载环境就绪数据"
      message="加载失败，但不影响其他面板"
    />
    <template v-else>
      <!-- Dashboard Summary -->
      <div v-if="dashboard" class="beta-dashboard-tiles">
        <MetricTile label="总会话" :value="dashboard.totalSessions" />
        <MetricTile label="已完成" :value="dashboard.completedSessions" :accent="'success'" />
        <MetricTile label="进行中" :value="dashboard.inProgressSessions" :accent="'warning'" />
        <MetricTile label="已阻塞" :value="dashboard.blockedSessions" :accent="'danger'" />
        <MetricTile label="满意度" :value="dashboard.averageSatisfactionScore.toFixed(1)" />
        <MetricTile label="继续意向" :value="dashboard.continueYesCount" />
        <MetricTile label="P0/P1" :value="dashboard.p0Count + dashboard.p1Count" :accent="'danger'" />
        <MetricTile label="Release Blocking" :value="dashboard.releaseBlockingCount" :accent="'danger'" />
      </div>

      <NeonDivider />

      <div class="beta-readiness-sub-header">
        <span class="beta-readiness-title">环境检查记录</span>
        <div class="beta-readiness-stats" v-if="dashboard">
          <ElTag size="small" type="success">通过 {{ dashboard.readinessPassCount }}</ElTag>
          <ElTag size="small" type="warning">警告 {{ dashboard.readinessWarnCount }}</ElTag>
          <ElTag size="small" type="danger">失败 {{ dashboard.readinessFailCount }}</ElTag>
        </div>
      </div>

      <div class="beta-toolbar">
        <ElButton size="small" type="primary" @click="createDialogVisible = true" :disabled="!projectId">
          + 添加检查
        </ElButton>
      </div>

      <div v-if="checks.length === 0" class="beta-empty">
        <EmptyState title="暂无检查记录" message="尚无环境就绪检查记录" />
      </div>

      <div v-for="c in checks" :key="c.id" class="beta-check-row">
        <div class="beta-check-info">
          <div class="beta-check-title-row">
            <ElTag :type="statusTag(c.checkStatus)" size="small">{{ statusLabel(c.checkStatus) }}</ElTag>
            <span class="beta-check-name">{{ c.targetName }}</span>
            <span class="beta-check-type">{{ c.targetType }}</span>
          </div>
          <div v-if="c.summary" class="beta-check-summary">{{ c.summary }}</div>
          <div class="beta-check-time">{{ c.checkedAt ? formatDateTime(c.checkedAt) : '-' }}</div>
        </div>
      </div>

      <!-- Create Check Dialog -->
      <ElDialog v-model="createDialogVisible" title="添加环境检查记录" width="500px" destroy-on-close>
        <div class="beta-create-form">
          <div class="beta-form-row">
            <label>目标名称 *</label>
            <ElInput v-model="createForm.targetName" placeholder="如：Docker Service" />
          </div>
          <div class="beta-form-row">
            <label>目标类型</label>
            <ElSelect v-model="createForm.targetType">
              <ElOption label="服务" value="SERVICE" />
              <ElOption label="容器" value="CONTAINER" />
              <ElOption label="端点" value="ENDPOINT" />
              <ElOption label="数据库" value="DATABASE" />
              <ElOption label="配置" value="CONFIG" />
              <ElOption label="网络" value="NETWORK" />
            </ElSelect>
          </div>
          <div class="beta-form-row">
            <label>检查状态</label>
            <ElSelect v-model="createForm.checkStatus">
              <ElOption label="通过" value="PASS" />
              <ElOption label="警告" value="WARN" />
              <ElOption label="失败" value="FAIL" />
              <ElOption label="跳过" value="SKIP" />
            </ElSelect>
          </div>
          <div class="beta-form-row">
            <label>摘要</label>
            <ElInput v-model="createForm.summary" placeholder="检查摘要" />
          </div>
          <div class="beta-form-row">
            <label>详情 JSON</label>
            <ElInput v-model="createForm.detailJson" type="textarea" :rows="3" placeholder='{"key": "value"}' />
          </div>
        </div>
        <template #footer>
          <ElButton @click="createDialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleCreate" :disabled="!createForm.targetName">创建</ElButton>
        </template>
      </ElDialog>
    </template>
  </TechPanel>
</template>

<style scoped>
.beta-dashboard-tiles {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
  margin-bottom: 8px;
}
.beta-readiness-sub-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.beta-readiness-title {
  font-size: 12px;
  color: var(--app-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.beta-readiness-stats {
  display: flex;
  gap: 6px;
}
.beta-toolbar {
  display: flex;
  margin-bottom: 8px;
}
.beta-empty {
  padding: 16px 0;
}
.beta-check-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 8px 0;
  border-bottom: 1px solid var(--app-border);
}
.beta-check-row:last-child { border-bottom: none; }
.beta-check-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.beta-check-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.beta-check-name {
  font-size: 13px;
  color: var(--app-text);
  font-weight: 500;
}
.beta-check-type {
  font-size: 11px;
  color: var(--app-text-secondary);
}
.beta-check-summary {
  font-size: 12px;
  color: var(--app-text-secondary);
}
.beta-check-time {
  font-size: 11px;
  color: var(--app-text-tertiary);
}
.beta-create-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.beta-form-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.beta-form-row label {
  font-size: 12px;
  color: var(--app-text-secondary);
}
</style>
