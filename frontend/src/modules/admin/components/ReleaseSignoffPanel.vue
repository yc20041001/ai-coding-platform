<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import {
  listSignoffs,
  createSignoff,
  updateSignoff,
  updateSignoffStatus,
  type ReleaseSignoffRecordItem,
  type CreateReleaseSignoffRecordRequest,
  type UpdateReleaseSignoffRecordRequest,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag, ElButton, ElDialog, ElMessage, ElInput, ElSelect, ElOption, ElForm, ElFormItem, ElProgress } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId?: string | null
  planId?: string | null
}>()

const signoffs = ref<ReleaseSignoffRecordItem[]>([])
const loading = ref(false)
const error = ref(false)
const editDialogVisible = ref(false)
const editingSignoff = ref<ReleaseSignoffRecordItem | null>(null)

const form = ref<CreateReleaseSignoffRecordRequest | UpdateReleaseSignoffRecordRequest>({})

const completionRate = computed(() => {
  if (signoffs.value.length === 0) return 0
  const completed = signoffs.value.filter(s => s.signoffStatus !== 'PENDING').length
  return Math.round((completed / signoffs.value.length) * 100)
})

function loadSignoffs() {
  if (!props.planId) return
  loading.value = true
  error.value = false
  listSignoffs(props.planId)
    .then(res => { signoffs.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

function openCreate() {
  editingSignoff.value = null
  form.value = {
    projectId: props.projectId || undefined,
    signoffRole: 'TECH_OWNER',
    signoffStatus: 'PENDING',
  }
  editDialogVisible.value = true
}

function openEdit(signoff: ReleaseSignoffRecordItem) {
  editingSignoff.value = signoff
  form.value = {
    signoffStatus: signoff.signoffStatus,
    signerName: signoff.signerName ?? undefined,
    commentText: signoff.commentText ?? undefined,
  }
  editDialogVisible.value = true
}

async function handleSave() {
  if (!props.planId) return
  try {
    if (editingSignoff.value) {
      await updateSignoff(props.planId, editingSignoff.value.id, form.value as UpdateReleaseSignoffRecordRequest)
      ElMessage.success('签字更新成功')
    } else {
      await createSignoff(props.planId, form.value as CreateReleaseSignoffRecordRequest)
      ElMessage.success('签字创建成功')
    }
    editDialogVisible.value = false
    loadSignoffs()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function handleStatus(signoff: ReleaseSignoffRecordItem, status: string) {
  if (!props.planId) return
  try {
    await updateSignoffStatus(props.planId, signoff.id, status)
    ElMessage.success('状态更新成功')
    loadSignoffs()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '状态更新失败')
  }
}

function roleLabel(role: string): string {
  const map: Record<string, string> = {
    TECH_OWNER: '技术负责人',
    PRODUCT_OWNER: '产品负责人',
    OPS_OWNER: '运维负责人',
    SECURITY_REVIEWER: '安全审查人',
    QA_REVIEWER: 'QA 审查人',
  }
  return map[role] || role
}

function statusTag(status: string) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    PENDING: 'info',
    APPROVED: 'success',
    CONDITIONAL: 'warning',
    REJECTED: 'danger',
    SKIPPED: 'info',
  }
  return map[status] || 'info'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理',
    APPROVED: '已批准',
    CONDITIONAL: '有条件批准',
    REJECTED: '已拒绝',
    SKIPPED: '已跳过',
  }
  return map[status] || status
}

function canTransition(status: string): string[] {
  switch (status) {
    case 'PENDING': return ['APPROVED', 'CONDITIONAL', 'REJECTED', 'SKIPPED']
    case 'CONDITIONAL': return ['APPROVED', 'REJECTED']
    default: return []
  }
}

watch(() => props.planId, () => { if (props.planId) loadSignoffs() }, { immediate: true })
</script>

<template>
  <div>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">发布签字</span>
      <ElButton v-if="props.planId && signoffs.length === 0" size="small" type="primary" @click="openCreate">新建签字</ElButton>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取签字数据" retry-text="重试" @retry="loadSignoffs" />

    <div v-if="!props.planId" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">请先选择一个 Rollout Plan</div>

    <div v-if="signoffs.length > 0 && props.planId" v-loading="loading">
      <div style="margin-bottom:12px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px">
          <span style="font-size:11px;color:var(--app-text-muted)">签字完成率</span>
          <span style="font-size:11px;color:var(--app-text-soft)">{{ completionRate }}%</span>
        </div>
        <ElProgress :percentage="completionRate" :stroke-width="8" :show-text="false" />
      </div>

      <div v-for="signoff in signoffs" :key="signoff.id" style="display:flex;align-items:center;gap:8px;padding:6px 0;border-bottom:1px solid rgba(56,189,248,0.05)">
        <div style="min-width:120px;font-size:12px;color:var(--app-text-bright)">{{ roleLabel(signoff.signoffRole) }}</div>
        <ElTag :type="statusTag(signoff.signoffStatus)" size="small" effect="dark">{{ statusLabel(signoff.signoffStatus) }}</ElTag>
        <div v-if="signoff.signerName" style="font-size:11px;color:var(--app-text-muted)">{{ signoff.signerName }}</div>
        <div v-if="signoff.signedAt" style="font-size:11px;color:var(--app-text-muted)">{{ formatDateTime(signoff.signedAt) }}</div>
        <div style="flex:1" />
        <template v-for="t in canTransition(signoff.signoffStatus)" :key="t">
          <ElButton size="small" link @click="handleStatus(signoff, t)">{{ statusLabel(t) }}</ElButton>
        </template>
        <ElButton v-if="signoff.signoffStatus === 'PENDING' || signoff.signoffStatus === 'CONDITIONAL'" size="small" link @click="openEdit(signoff)">编辑</ElButton>
      </div>
    </div>

    <EmptyState v-if="!loading && signoffs.length === 0 && props.planId && !error" description="暂无签字记录" />

    <ElDialog v-model="editDialogVisible" :title="editingSignoff ? '编辑签字' : '新建签字'" width="500px">
      <ElForm label-position="top" size="small">
        <ElFormItem v-if="!editingSignoff" label="签字角色">
          <ElSelect v-model="(form as CreateReleaseSignoffRecordRequest).signoffRole">
            <ElOption label="技术负责人" value="TECH_OWNER" />
            <ElOption label="产品负责人" value="PRODUCT_OWNER" />
            <ElOption label="运维负责人" value="OPS_OWNER" />
            <ElOption label="安全审查人" value="SECURITY_REVIEWER" />
            <ElOption label="QA 审查人" value="QA_REVIEWER" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="签字状态">
          <ElSelect v-model="(form as UpdateReleaseSignoffRecordRequest).signoffStatus">
            <ElOption label="待处理" value="PENDING" />
            <ElOption label="已批准" value="APPROVED" />
            <ElOption label="有条件批准" value="CONDITIONAL" />
            <ElOption label="已拒绝" value="REJECTED" />
            <ElOption label="已跳过" value="SKIPPED" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="签字人">
          <ElInput v-model="(form as UpdateReleaseSignoffRecordRequest).signerName" placeholder="签字人姓名" />
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput v-model="(form as UpdateReleaseSignoffRecordRequest).commentText" type="textarea" :rows="2" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="editDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSave">{{ editingSignoff ? '保存' : '创建' }}</ElButton>
      </template>
    </ElDialog>
  </div>
</template>
