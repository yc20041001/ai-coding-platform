<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  listRollbackDrills,
  createRollbackDrill,
  updateRollbackDrill,
  updateRollbackDrillStatus,
  checkRollbackReadiness,
  type ReleaseRollbackDrillItem,
  type CreateReleaseRollbackDrillRequest,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import { ElTag, ElButton, ElDialog, ElMessage, ElInput, ElSelect, ElOption, ElForm, ElFormItem, ElInputNumber } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId?: string | null
  planId?: string | null
}>()

const emit = defineEmits<{
  (e: 'dashboard-refresh'): void
}>()

const drills = ref<ReleaseRollbackDrillItem[]>([])
const loading = ref(false)
const error = ref(false)
const rollbackReady = ref(false)
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editingDrill = ref<ReleaseRollbackDrillItem | null>(null)

const form = ref<CreateReleaseRollbackDrillRequest>({
  drillScope: 'CONFIG_ONLY',
  environmentName: 'production',
})

function resetForm() {
  form.value = {
    drillScope: 'CONFIG_ONLY',
    environmentName: 'production',
  }
}

function loadDrills() {
  if (!props.planId) return
  loading.value = true
  error.value = false
  Promise.all([
    listRollbackDrills(props.planId),
    checkRollbackReadiness(props.planId),
  ])
    .then(([dRes, rRes]) => {
      drills.value = dRes.data.data
      rollbackReady.value = rRes.data.data
    })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleCreate() {
  if (!props.planId) return
  try {
    await createRollbackDrill(props.planId, { ...form.value })
    ElMessage.success('演练创建成功')
    createDialogVisible.value = false
    resetForm()
    loadDrills()
    emit('dashboard-refresh')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  }
}

async function handleStatus(drillId: string, drillStatus: string) {
  if (!props.planId) return
  try {
    await updateRollbackDrillStatus(props.planId, drillId, drillStatus)
    ElMessage.success('状态更新成功')
    loadDrills()
    emit('dashboard-refresh')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '状态更新失败')
  }
}

function openEdit(drill: ReleaseRollbackDrillItem) {
  editingDrill.value = drill
  form.value = {
    drillScope: drill.drillScope,
    environmentName: drill.environmentName,
    successCriteria: drill.successCriteria ?? undefined,
    rollbackStepsSummary: drill.rollbackStepsSummary ?? undefined,
    blockersSummary: drill.blockersSummary ?? undefined,
    resultSummary: drill.resultSummary ?? undefined,
  }
  editDialogVisible.value = true
}

async function handleEdit() {
  if (!props.planId || !editingDrill.value) return
  try {
    await updateRollbackDrill(props.planId, editingDrill.value.id, form.value)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    editingDrill.value = null
    loadDrills()
    emit('dashboard-refresh')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '更新失败')
  }
}

function statusTag(status: string) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    PLANNED: 'info',
    RUNNING: 'warning',
    PASSED: 'success',
    FAILED: 'danger',
    BLOCKED: 'danger',
    CANCELLED: 'info',
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PLANNED: '已计划',
    RUNNING: '执行中',
    PASSED: '通过',
    FAILED: '失败',
    BLOCKED: '阻塞',
    CANCELLED: '已取消',
  }
  return map[status] || status
}

function canTransition(status: string): string[] {
  switch (status) {
    case 'PLANNED': return ['RUNNING', 'CANCELLED']
    case 'RUNNING': return ['PASSED', 'FAILED', 'BLOCKED']
    default: return []
  }
}

function transitionLabel(status: string): string {
  const map: Record<string, string> = {
    RUNNING: '开始执行',
    PASSED: '通过',
    FAILED: '失败',
    BLOCKED: '阻塞',
    CANCELLED: '取消',
  }
  return map[status] || status
}

function scopeLabel(scope: string): string {
  const map: Record<string, string> = {
    CONFIG_ONLY: '仅配置',
    APP_VERSION: '应用版本',
    DB_AND_APP: '数据库+应用',
    FULL_ENVIRONMENT: '全环境',
  }
  return map[scope] || scope
}

watch(() => props.planId, () => { if (props.planId) loadDrills() }, { immediate: true })
</script>

<template>
  <div>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <div style="display:flex;align-items:center;gap:8px">
        <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">回滚演练</span>
        <StatusPulse v-if="props.planId" :status="rollbackReady ? 'ready' : 'warning'" :tone="rollbackReady ? 'success' : 'warning'" />
        <span v-if="props.planId" style="font-size:11px;color:var(--app-text-muted)">{{ rollbackReady ? '回滚就绪' : '回滚未就绪' }}</span>
      </div>
      <ElButton v-if="props.planId" size="small" type="primary" @click="createDialogVisible = true; resetForm()">新建演练</ElButton>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取回滚演练数据" retry-text="重试" @retry="loadDrills" />

    <div v-if="!props.planId" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">请先选择一个 Rollout Plan</div>

    <div v-loading="loading" v-else>
      <div v-for="drill in drills" :key="drill.id" style="background:rgba(15,23,42,0.3);border:1px solid rgba(56,189,248,0.1);border-radius:6px;padding:10px;margin-bottom:8px">
        <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:6px">
          <div style="display:flex;align-items:center;gap:8px">
            <span style="font-weight:600;font-size:13px;color:var(--app-text-bright)">{{ drill.releaseLabel }}</span>
            <ElTag :type="statusTag(drill.drillStatus)" size="small" effect="dark">{{ statusLabel(drill.drillStatus) }}</ElTag>
            <ElTag size="small" effect="plain">{{ scopeLabel(drill.drillScope) }}</ElTag>
          </div>
          <div style="display:flex;align-items:center;gap:4px;flex-wrap:wrap">
            <ElButton size="small" link @click="openEdit(drill)">编辑</ElButton>
            <template v-for="t in canTransition(drill.drillStatus)" :key="t">
              <ElButton size="small" link @click="handleStatus(drill.id, t)">{{ transitionLabel(t) }}</ElButton>
            </template>
          </div>
        </div>
        <div style="margin-top:6px;display:flex;gap:12px;font-size:11px;color:var(--app-text-muted)">
          <span>环境: {{ drill.environmentName }}</span>
          <span v-if="drill.durationSeconds">耗时: {{ drill.durationSeconds }}s</span>
          <span v-if="drill.blockersSummary">阻塞: {{ drill.blockersSummary }}</span>
          <span>{{ formatDateTime(drill.createTime) }}</span>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && drills.length === 0 && props.planId" description="暂无回滚演练" />

    <!-- Create Dialog -->
    <ElDialog v-model="createDialogVisible" title="新建 Rollback Drill" width="500px">
      <ElForm label-position="top" size="small">
        <ElFormItem label="演练范围">
          <ElSelect v-model="form.drillScope">
            <ElOption label="仅配置" value="CONFIG_ONLY" />
            <ElOption label="应用版本" value="APP_VERSION" />
            <ElOption label="数据库+应用" value="DB_AND_APP" />
            <ElOption label="全环境" value="FULL_ENVIRONMENT" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="环境名称">
          <ElInput v-model="form.environmentName" placeholder="production" />
        </ElFormItem>
        <ElFormItem label="成功标准">
          <ElInput v-model="form.successCriteria" type="textarea" :rows="2" />
        </ElFormItem>
        <ElFormItem label="回滚步骤">
          <ElInput v-model="form.rollbackStepsSummary" type="textarea" :rows="3" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="createDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleCreate">创建</ElButton>
      </template>
    </ElDialog>

    <!-- Edit Dialog -->
    <ElDialog v-model="editDialogVisible" title="编辑 Rollback Drill" width="500px">
      <ElForm label-position="top" size="small">
        <ElFormItem label="演练范围">
          <ElSelect v-model="form.drillScope">
            <ElOption label="仅配置" value="CONFIG_ONLY" />
            <ElOption label="应用版本" value="APP_VERSION" />
            <ElOption label="数据库+应用" value="DB_AND_APP" />
            <ElOption label="全环境" value="FULL_ENVIRONMENT" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="环境名称">
          <ElInput v-model="form.environmentName" />
        </ElFormItem>
        <ElFormItem label="回滚步骤">
          <ElInput v-model="form.rollbackStepsSummary" type="textarea" :rows="3" />
        </ElFormItem>
        <ElFormItem label="阻塞项">
          <ElInput v-model="form.blockersSummary" type="textarea" :rows="2" />
        </ElFormItem>
        <ElFormItem label="结果">
          <ElInput v-model="form.resultSummary" type="textarea" :rows="2" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="editDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleEdit">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>
