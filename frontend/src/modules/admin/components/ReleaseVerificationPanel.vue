<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  listVerifications,
  createVerification,
  updateVerification,
  listRolloutSteps,
  updateRolloutStepStatus,
  type ReleaseVerificationRecordItem,
  type ReleaseRolloutStepItem,
  type CreateReleaseVerificationRequest,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag, ElButton, ElDialog, ElMessage, ElInput, ElSelect, ElOption, ElForm, ElFormItem, ElTable, ElTableColumn } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId?: string | null
  planId?: string | null
}>()

const emit = defineEmits<{
  (e: 'dashboard-refresh'): void
}>()

const verifications = ref<ReleaseVerificationRecordItem[]>([])
const steps = ref<ReleaseRolloutStepItem[]>([])
const loading = ref(false)
const error = ref(false)
const selectedPhase = ref<string>('')
const createDialogVisible = ref(false)

const form = ref<CreateReleaseVerificationRequest>({
  verificationPhase: 'PRE_RELEASE',
  displayName: '',
  verificationStatus: 'PENDING',
  severity: 'MEDIUM',
})

function resetForm() {
  form.value = {
    verificationPhase: 'PRE_RELEASE',
    displayName: '',
    verificationStatus: 'PENDING',
    severity: 'MEDIUM',
  }
}

function loadData() {
  if (!props.projectId || !props.planId) return
  loading.value = true
  error.value = false
  Promise.all([
    listVerifications(props.projectId, props.planId, selectedPhase.value || undefined),
    listRolloutSteps(props.projectId, props.planId),
  ])
    .then(([vRes, sRes]) => {
      verifications.value = vRes.data.data
      steps.value = sRes.data.data
    })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleCreate() {
  if (!props.projectId || !props.planId || !form.value.displayName) {
    ElMessage.warning('请输入验证名称')
    return
  }
  try {
    await createVerification(props.projectId, props.planId, { ...form.value })
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    resetForm()
    loadData()
    emit('dashboard-refresh')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  }
}

async function handleStepStatus(stepId: string, stepStatus: string) {
  if (!props.projectId || !props.planId) return
  try {
    await updateRolloutStepStatus(props.projectId, props.planId, stepId, stepStatus, { operatorId: 'system' })
    ElMessage.success('步骤状态已更新')
    loadData()
    emit('dashboard-refresh')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '更新失败')
  }
}

function statusTag(status: string | null | undefined) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    PASSED: 'success',
    FAILED: 'danger',
    PENDING: 'info',
    IN_PROGRESS: 'warning',
    SKIPPED: 'info',
    BLOCKED: 'danger',
  }
  return map[status || ''] || 'info'
}

function severityTag(severity: string | null | undefined) {
  const map: Record<string, 'danger' | 'warning' | 'info'> = {
    CRITICAL: 'danger',
    BLOCKING: 'danger',
    HIGH: 'warning',
    MEDIUM: 'warning',
    LOW: 'info',
  }
  return map[severity || ''] || 'info'
}

watch(() => props.planId, () => { if (props.planId) loadData() })
watch(selectedPhase, () => { if (props.planId) loadData() })
</script>

<template>
  <div>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">验证与步骤</span>
      <div style="display:flex;gap:8px">
        <ElSelect v-model="selectedPhase" size="small" placeholder="所有阶段" clearable style="width:140px">
          <ElOption label="预发布" value="PRE_RELEASE" />
          <ElOption label="观测" value="OBSERVATION" />
          <ElOption label="回滚" value="ROLLBACK" />
        </ElSelect>
        <ElButton v-if="planId" size="small" type="primary" @click="createDialogVisible = true; resetForm()">新建验证</ElButton>
      </div>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取验证数据" retry-text="重试" @retry="loadData" />

    <div v-loading="loading">
      <!-- Steps Section -->
      <div v-if="steps.length > 0" style="margin-bottom:16px">
        <div style="font-size:12px;font-weight:600;color:var(--app-text-soft);margin-bottom:6px">步骤</div>
        <ElTable :data="steps" size="small" style="width:100%" max-height="300">
          <ElTableColumn prop="stepOrder" label="#" width="50" />
          <ElTableColumn prop="displayName" label="名称" width="140" />
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="statusTag(row.stepStatus)" size="small" effect="dark">{{ row.stepStatus }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="200">
            <template #default="{ row }">
              <template v-if="row.stepStatus === 'PENDING'">
                <ElButton size="small" link type="primary" @click="handleStepStatus(row.id, 'IN_PROGRESS')">开始</ElButton>
              </template>
              <template v-if="row.stepStatus === 'IN_PROGRESS'">
                <ElButton size="small" link type="success" @click="handleStepStatus(row.id, 'PASSED')">通过</ElButton>
                <ElButton size="small" link type="danger" @click="handleStepStatus(row.id, 'FAILED')">失败</ElButton>
              </template>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="actualResult" label="结果" min-width="120" />
        </ElTable>
      </div>

      <!-- Verifications Section -->
      <div v-if="verifications.length > 0">
        <div style="font-size:12px;font-weight:600;color:var(--app-text-soft);margin-bottom:6px">验证记录</div>
        <ElTable :data="verifications" size="small" style="width:100%" max-height="300">
          <ElTableColumn prop="displayName" label="名称" width="150" />
          <ElTableColumn prop="verificationPhase" label="阶段" width="100" />
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="statusTag(row.verificationStatus)" size="small" effect="dark">{{ row.verificationStatus }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="严重程度" width="100">
            <template #default="{ row }">
              <ElTag v-if="row.severity" :type="severityTag(row.severity)" size="small">{{ row.severity }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="summary" label="摘要" min-width="180" />
          <ElTableColumn label="时间" width="140">
            <template #default="{ row }">{{ formatDateTime(row.recordedAt) }}</template>
          </ElTableColumn>
        </ElTable>
      </div>
    </div>

    <EmptyState v-if="!loading && verifications.length === 0 && steps.length === 0 && !error" description="暂无验证数据，请选择一个 Plan" />

    <!-- Create Dialog -->
    <ElDialog v-model="createDialogVisible" title="新建验证记录" width="500px">
      <ElForm label-position="top" size="small">
        <ElFormItem label="验证名称" required>
          <ElInput v-model="form.displayName" placeholder="例如: 预发冒烟测试" />
        </ElFormItem>
        <ElFormItem label="验证阶段">
          <ElSelect v-model="form.verificationPhase">
            <ElOption label="预发布" value="PRE_RELEASE" />
            <ElOption label="观测" value="OBSERVATION" />
            <ElOption label="回滚" value="ROLLBACK" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="验证 Key">
          <ElInput v-model="form.verificationKey" placeholder="可选唯一标识" />
        </ElFormItem>
        <ElFormItem label="验证状态">
          <ElSelect v-model="form.verificationStatus">
            <ElOption label="待处理" value="PENDING" />
            <ElOption label="通过" value="PASSED" />
            <ElOption label="失败" value="FAILED" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="严重程度">
          <ElSelect v-model="form.severity">
            <ElOption label="Critical" value="CRITICAL" />
            <ElOption label="High" value="HIGH" />
            <ElOption label="Medium" value="MEDIUM" />
            <ElOption label="Low" value="LOW" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="摘要">
          <ElInput v-model="form.summary" type="textarea" :rows="2" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="createDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleCreate">创建</ElButton>
      </template>
    </ElDialog>
  </div>
</template>
