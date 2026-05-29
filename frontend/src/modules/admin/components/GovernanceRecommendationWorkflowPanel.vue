<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  listRecommendations,
  updateRecommendation,
  updateRecommendationStatus,
  type GovernanceRecommendationWorkflowItem,
  type UpdateGovernanceRecommendationItemRequest,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElDialog, ElForm, ElFormItem, ElInput } from 'element-plus'

const items = ref<GovernanceRecommendationWorkflowItem[]>([])
const loading = ref(false)
const error = ref(false)

const dialogVisible = ref(false)
const editingItem = ref<GovernanceRecommendationWorkflowItem | null>(null)
const formNote = ref('')

function loadData() {
  loading.value = true
  error.value = false
  listRecommendations()
    .then(res => { items.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

function priorityLabel(p: string): string {
  return { P0: 'P0-紧急', P1: 'P1-高', P2: 'P2-中', P3: 'P3-低' }[p] || p
}

function priorityTag(p: string): 'danger' | 'warning' | 'info' {
  if (p === 'P0') return 'danger'
  if (p === 'P1') return 'warning'
  return 'info'
}

function statusLabel(s: string): string {
  const map: Record<string, string> = { OPEN: '待确认', ACKNOWLEDGED: '已确认', IN_PROGRESS: '处理中', COMPLETED: '已完成', BLOCKED: '阻塞', REJECTED: '已拒绝' }
  return map[s] || s
}

function statusTag(s: string): 'info' | 'primary' | 'warning' | 'success' | 'danger' {
  const map: Record<string, 'info' | 'primary' | 'warning' | 'success' | 'danger'> = { OPEN: 'info', ACKNOWLEDGED: 'primary', IN_PROGRESS: 'warning', COMPLETED: 'success', BLOCKED: 'danger', REJECTED: 'info' }
  return map[s] || 'info'
}

function isOverdue(item: GovernanceRecommendationWorkflowItem): boolean {
  if (!item.dueAt) return false
  if (item.workflowStatus === 'COMPLETED' || item.workflowStatus === 'REJECTED') return false
  return new Date(item.dueAt) < new Date()
}

async function handleStatusTransition(item: GovernanceRecommendationWorkflowItem, newStatus: string) {
  try {
    await updateRecommendationStatus(item.id, newStatus)
    ElMessage.success(`状态已变更为 ${statusLabel(newStatus)}`)
    loadData()
  } catch { ElMessage.error('状态变更失败') }
}

function openResolveDialog(item: GovernanceRecommendationWorkflowItem) {
  editingItem.value = item
  formNote.value = item.resolutionNote || ''
  dialogVisible.value = true
}

async function handleResolve() {
  if (!editingItem.value) return
  try {
    await updateRecommendation(editingItem.value.id, { resolutionNote: formNote.value || null })
    await updateRecommendationStatus(editingItem.value.id, 'COMPLETED')
    ElMessage.success('已解决')
    dialogVisible.value = false
    loadData()
  } catch { ElMessage.error('操作失败') }
}

onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">推荐事项工作流</span>
      <ElButton size="small" type="primary" @click="loadData">刷新</ElButton>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取推荐事项" retry-text="重试" @retry="loadData" />

    <div v-if="!loading && !error">
      <div v-for="item in items" :key="item.id" style="padding:10px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)"
        :style="isOverdue(item) ? { borderLeft: '3px solid var(--color-error)' } : {}">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:4px">
          <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">
            <ElTag size="small" :type="priorityTag(item.priority)" effect="dark">{{ priorityLabel(item.priority) }}</ElTag>
            <ElTag size="small" :type="statusTag(item.workflowStatus)">{{ statusLabel(item.workflowStatus) }}</ElTag>
            <span v-if="item.waiverStatus" style="font-size:11px">
              <ElTag size="small" type="warning">Waiver: {{ item.waiverStatus }}</ElTag>
            </span>
            <span v-if="isOverdue(item)" style="font-size:11px;color:var(--color-error)">⚠ 逾期</span>
          </div>
          <div style="display:flex;gap:4px">
            <ElButton v-if="item.workflowStatus === 'OPEN'" size="small" link @click="handleStatusTransition(item, 'ACKNOWLEDGED')">确认</ElButton>
            <ElButton v-if="item.workflowStatus === 'ACKNOWLEDGED'" size="small" link @click="handleStatusTransition(item, 'IN_PROGRESS')">开始处理</ElButton>
            <ElButton v-if="item.workflowStatus === 'IN_PROGRESS' || item.workflowStatus === 'BLOCKED'" size="small" link @click="openResolveDialog(item)">完成</ElButton>
            <ElButton v-if="item.workflowStatus === 'BLOCKED'" size="small" link @click="handleStatusTransition(item, 'IN_PROGRESS')">继续</ElButton>
            <ElButton v-if="item.workflowStatus === 'OPEN' || item.workflowStatus === 'ACKNOWLEDGED'" size="small" link @click="handleStatusTransition(item, 'REJECTED')">拒绝</ElButton>
          </div>
        </div>
        <div style="font-size:13px;font-weight:500;color:var(--app-text-bright)">{{ item.projectName }}: {{ item.title }}</div>
        <div style="font-size:11px;color:var(--app-text-muted);margin-top:2px">
          责任人: {{ item.ownerName || '未分配' }} | 截止: {{ item.dueAt ? new Date(item.dueAt).toLocaleDateString() : '无' }}
          <span v-if="item.itemSummary" style="margin-left:12px">{{ item.itemSummary }}</span>
        </div>
      </div>
      <EmptyState v-if="!loading && items.length === 0 && !error" description="暂无推荐事项，请先同步" />
    </div>

    <ElDialog v-model="dialogVisible" title="完成事项" width="400px" destroy-on-close>
      <ElForm label-position="top" size="small">
        <ElFormItem label="解决备注">
          <ElInput v-model="formNote" :rows="3" type="textarea" placeholder="描述解决方式" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleResolve">确认完成</ElButton>
      </template>
    </ElDialog>
  </TechPanel>
</template>
