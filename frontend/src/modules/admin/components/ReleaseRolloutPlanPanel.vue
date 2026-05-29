<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  listRolloutPlans,
  createRolloutPlan,
  updateRolloutPlan,
  updateRolloutPlanStatus,
  type ReleaseRolloutPlanItem,
  type CreateReleaseRolloutPlanRequest,
  type UpdateReleaseRolloutPlanRequest,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag, ElButton, ElDialog, ElMessage, ElInput, ElSelect, ElOption, ElForm, ElFormItem, ElDatePicker, ElInputNumber } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId?: string | null
}>()

const emit = defineEmits<{
  (e: 'plan-selected', planId: string): void
  (e: 'dashboard-refresh'): void
}>()

const plans = ref<ReleaseRolloutPlanItem[]>([])
const loading = ref(false)
const error = ref(false)
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editingPlan = ref<ReleaseRolloutPlanItem | null>(null)

const form = ref<CreateReleaseRolloutPlanRequest>({
  releaseLabel: '',
  rolloutStrategy: 'MANUAL_FULL',
  targetEnvironment: 'production',
  observationWindowMinutes: 60,
})

function resetForm() {
  form.value = {
    releaseLabel: '',
    rolloutStrategy: 'MANUAL_FULL',
    targetEnvironment: 'production',
    observationWindowMinutes: 60,
  }
}

function loadPlans() {
  if (!props.projectId) return
  loading.value = true
  error.value = false
  listRolloutPlans(props.projectId)
    .then(res => { plans.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleCreate() {
  if (!props.projectId || !form.value.releaseLabel) {
    ElMessage.warning('请输入发布标签')
    return
  }
  try {
    await createRolloutPlan(props.projectId, { ...form.value })
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    resetForm()
    loadPlans()
    emit('dashboard-refresh')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  }
}

async function handleStatus(planId: string, status: string) {
  if (!props.projectId) return
  try {
    await updateRolloutPlanStatus(props.projectId, planId, status)
    ElMessage.success('状态更新成功')
    loadPlans()
    emit('dashboard-refresh')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '状态更新失败')
  }
}

function openEdit(plan: ReleaseRolloutPlanItem) {
  editingPlan.value = plan
  form.value = {
    releaseLabel: plan.releaseLabel,
    rolloutStrategy: plan.rolloutStrategy,
    targetEnvironment: plan.targetEnvironment,
    observationWindowMinutes: plan.observationWindowMinutes ?? undefined,
    rollbackTriggerSummary: plan.rollbackTriggerSummary ?? undefined,
    successCriteriaSummary: plan.successCriteriaSummary ?? undefined,
    readinessSummary: plan.readinessSummary ?? undefined,
  }
  editDialogVisible.value = true
}

async function handleEdit() {
  if (!props.projectId || !editingPlan.value) return
  try {
    await updateRolloutPlan(props.projectId, editingPlan.value.id, form.value as UpdateReleaseRolloutPlanRequest)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    editingPlan.value = null
    loadPlans()
    emit('dashboard-refresh')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '更新失败')
  }
}

function statusTag(status: string) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    DRAFT: 'info',
    READY: 'success',
    IN_PROGRESS: 'warning',
    OBSERVING: 'warning',
    COMPLETED: 'success',
    ROLLED_BACK: 'danger',
    CANCELLED: 'info',
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    READY: '就绪',
    IN_PROGRESS: '进行中',
    OBSERVING: '观测中',
    COMPLETED: '已完成',
    ROLLED_BACK: '已回滚',
    CANCELLED: '已取消',
  }
  return map[status] || status
}

function canTransition(status: string): string[] {
  switch (status) {
    case 'DRAFT': return ['READY', 'CANCELLED']
    case 'READY': return ['IN_PROGRESS', 'CANCELLED']
    case 'IN_PROGRESS': return ['OBSERVING', 'ROLLED_BACK', 'CANCELLED']
    case 'OBSERVING': return ['COMPLETED', 'ROLLED_BACK']
    default: return []
  }
}

function transitionLabel(status: string): string {
  const map: Record<string, string> = {
    READY: '标记就绪',
    IN_PROGRESS: '开始发布',
    OBSERVING: '开始观测',
    COMPLETED: '完成',
    ROLLED_BACK: '回滚',
    CANCELLED: '取消',
  }
  return map[status] || status
}

watch(() => props.projectId, () => { loadPlans() }, { immediate: true })
</script>

<template>
  <div>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">发布计划 (Rollout Plans)</span>
      <ElButton size="small" type="primary" @click="createDialogVisible = true; resetForm()">新建 Plan</ElButton>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取发布计划" retry-text="重试" @retry="loadPlans" />

    <div v-loading="loading">
      <div v-for="plan in plans" :key="plan.id" style="background:rgba(15,23,42,0.3);border:1px solid rgba(56,189,248,0.1);border-radius:6px;padding:10px;margin-bottom:8px">
        <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:6px">
          <div style="display:flex;align-items:center;gap:8px">
            <span style="font-weight:600;font-size:13px;color:var(--app-text-bright)">{{ plan.releaseLabel }}</span>
            <ElTag :type="statusTag(plan.rolloutStatus)" size="small" effect="dark">{{ statusLabel(plan.rolloutStatus) }}</ElTag>
          </div>
          <div style="display:flex;align-items:center;gap:4px;flex-wrap:wrap">
            <ElButton size="small" link type="primary" @click="$emit('plan-selected', plan.id)">选择</ElButton>
            <ElButton size="small" link @click="openEdit(plan)">编辑</ElButton>
            <template v-for="t in canTransition(plan.rolloutStatus)" :key="t">
              <ElButton size="small" link @click="handleStatus(plan.id, t)">{{ transitionLabel(t) }}</ElButton>
            </template>
          </div>
        </div>
        <div style="margin-top:6px;display:flex;gap:12px;font-size:11px;color:var(--app-text-muted)">
          <span>步骤: {{ plan.stepCount }} (通过 {{ plan.passedStepCount }})</span>
          <span>验证: {{ plan.verificationCount }}</span>
          <span>策略: {{ plan.rolloutStrategy }}</span>
          <span>环境: {{ plan.targetEnvironment }}</span>
          <span>{{ formatDateTime(plan.createTime) }}</span>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && plans.length === 0 && !error" description="暂无发布计划，点击右上角新建" />

    <!-- Create Dialog -->
    <ElDialog v-model="createDialogVisible" title="新建 Rollout Plan" width="500px">
      <ElForm label-position="top" size="small">
        <ElFormItem label="发布标签" required>
          <ElInput v-model="form.releaseLabel" placeholder="例如: v2.3.1" />
        </ElFormItem>
        <ElFormItem label="发布策略">
          <ElSelect v-model="form.rolloutStrategy">
            <ElOption label="手动全量" value="MANUAL_FULL" />
            <ElOption label="手动分批" value="MANUAL_BATCHED" />
            <ElOption label="自动金丝雀" value="AUTO_CANARY" />
            <ElOption label="自动蓝绿" value="AUTO_BLUE_GREEN" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="目标环境">
          <ElInput v-model="form.targetEnvironment" placeholder="production" />
        </ElFormItem>
        <ElFormItem label="观测窗口 (分钟)">
          <ElInputNumber v-model="form.observationWindowMinutes" :min="0" style="width:100%" />
        </ElFormItem>
        <ElFormItem label="回滚触发条件">
          <ElInput v-model="form.rollbackTriggerSummary" type="textarea" :rows="2" />
        </ElFormItem>
        <ElFormItem label="成功标准">
          <ElInput v-model="form.successCriteriaSummary" type="textarea" :rows="2" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="createDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleCreate">创建</ElButton>
      </template>
    </ElDialog>

    <!-- Edit Dialog -->
    <ElDialog v-model="editDialogVisible" title="编辑 Rollout Plan" width="500px">
      <ElForm label-position="top" size="small">
        <ElFormItem label="发布策略">
          <ElSelect v-model="form.rolloutStrategy">
            <ElOption label="手动全量" value="MANUAL_FULL" />
            <ElOption label="手动分批" value="MANUAL_BATCHED" />
            <ElOption label="自动金丝雀" value="AUTO_CANARY" />
            <ElOption label="自动蓝绿" value="AUTO_BLUE_GREEN" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="目标环境">
          <ElInput v-model="form.targetEnvironment" />
        </ElFormItem>
        <ElFormItem label="观测窗口 (分钟)">
          <ElInputNumber v-model="form.observationWindowMinutes" :min="0" style="width:100%" />
        </ElFormItem>
        <ElFormItem label="回滚触发条件">
          <ElInput v-model="form.rollbackTriggerSummary" type="textarea" :rows="2" />
        </ElFormItem>
        <ElFormItem label="成功标准">
          <ElInput v-model="form.successCriteriaSummary" type="textarea" :rows="2" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="editDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleEdit">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>
