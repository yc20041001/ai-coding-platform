<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  syncRecommendations,
  listRecommendations,
  listWaivers,
  createWaiver,
  updateWaiverStatus,
  type GovernanceRecommendationWorkflowItem,
  type GovernanceWaiverRequest,
  type CreateGovernanceWaiverRequest,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption } from 'element-plus'

const items = ref<GovernanceRecommendationWorkflowItem[]>([])
const waivers = ref<GovernanceWaiverRequest[]>([])
const loading = ref(false)
const error = ref(false)
const selectedItemId = ref<string | null>(null)
const dialogVisible = ref(false)
const waiverForm = ref<{ waiverScope: string; reasonText: string; expiresAt: string }>({ waiverScope: 'POLICY_EXCEPTION', reasonText: '', expiresAt: '' })

function loadData() {
  loading.value = true
  error.value = false
  listRecommendations()
    .then(res => { items.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

function loadWaivers(itemId: string) {
  selectedItemId.value = itemId
  listWaivers(itemId).then(res => { waivers.value = res.data.data })
}

function openCreateWaiver(itemId: string) {
  selectedItemId.value = itemId
  waiverForm.value = { waiverScope: 'POLICY_EXCEPTION', reasonText: '', expiresAt: '' }
  dialogVisible.value = true
}

async function handleCreateWaiver() {
  if (!selectedItemId.value) return
  const data: CreateGovernanceWaiverRequest = {
    recommendationId: selectedItemId.value,
    waiverScope: waiverForm.value.waiverScope,
    reasonText: waiverForm.value.reasonText,
    expiresAt: waiverForm.value.expiresAt || null,
  }
  try {
    await createWaiver(selectedItemId.value, data)
    ElMessage.success('Waiver 已申请')
    dialogVisible.value = false
    loadWaivers(selectedItemId.value)
  } catch { ElMessage.error('申请失败') }
}

async function handleApprove(waiverId: string) {
  try {
    await updateWaiverStatus(waiverId, 'APPROVED', 'Approved by admin')
    ElMessage.success('已批准')
    if (selectedItemId.value) loadWaivers(selectedItemId.value)
  } catch { ElMessage.error('操作失败') }
}

async function handleReject(waiverId: string) {
  try {
    await updateWaiverStatus(waiverId, 'REJECTED', 'Rejected by admin')
    ElMessage.success('已拒绝')
    if (selectedItemId.value) loadWaivers(selectedItemId.value)
  } catch { ElMessage.error('操作失败') }
}

async function handleRevoke(waiverId: string) {
  try {
    await updateWaiverStatus(waiverId, 'REVOKED', 'Revoked by admin')
    ElMessage.success('已撤销')
    if (selectedItemId.value) loadWaivers(selectedItemId.value)
  } catch { ElMessage.error('操作失败') }
}

function statusLabel(s: string): string {
  const map: Record<string, string> = { REQUESTED: '待审批', APPROVED: '已批准', REJECTED: '已拒绝', EXPIRED: '已过期', REVOKED: '已撤销' }
  return map[s] || s
}

function scopeLabel(s: string): string {
  const map: Record<string, string> = { PROJECT_RELEASE: '发布例外', POLICY_EXCEPTION: '策略例外', TEMPORARY_SIGNOFF_GAP: '签字缺口', ROLLBACK_READINESS_EXCEPTION: '回滚例外' }
  return map[s] || s
}

onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Waiver 管理</span>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取 waiver 数据" retry-text="重试" @retry="loadData" />

    <div v-if="!loading && !error">
      <div v-for="item in items" :key="item.id" style="padding:8px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <div>
            <span style="font-size:13px;font-weight:500;color:var(--app-text-bright)">{{ item.projectName }}: {{ item.title }}</span>
            <ElTag v-if="item.waiverStatus" size="small" style="margin-left:6px">{{ statusLabel(item.waiverStatus) }}</ElTag>
          </div>
          <ElButton size="small" link @click="loadWaivers(item.id); selectedItemId = item.id">查看 Waiver</ElButton>
        </div>

        <div v-if="selectedItemId === item.id && waivers.length > 0" style="margin-top:8px;padding-left:12px;border-left:2px solid rgba(255,255,255,0.1)">
          <div v-for="w in waivers" :key="w.id" style="padding:6px 0;font-size:12px;border-bottom:1px solid rgba(255,255,255,0.05)">
            <div style="display:flex;align-items:center;gap:6px">
              <ElTag size="small">{{ scopeLabel(w.waiverScope) }}</ElTag>
              <ElTag size="small" :type="w.waiverStatus === 'APPROVED' ? 'success' : w.waiverStatus === 'REJECTED' ? 'danger' : 'info'">{{ statusLabel(w.waiverStatus) }}</ElTag>
              <span style="color:var(--app-text-muted)">{{ w.reasonText }}</span>
              <div style="margin-left:auto;display:flex;gap:4px">
                <ElButton v-if="w.waiverStatus === 'REQUESTED'" size="small" link @click="handleApprove(w.id)">批准</ElButton>
                <ElButton v-if="w.waiverStatus === 'REQUESTED'" size="small" link @click="handleReject(w.id)">拒绝</ElButton>
                <ElButton v-if="w.waiverStatus === 'APPROVED'" size="small" link @click="handleRevoke(w.id)">撤销</ElButton>
              </div>
            </div>
            <div v-if="w.expiresAt" style="color:var(--app-text-muted);margin-top:2px">到期: {{ new Date(w.expiresAt).toLocaleDateString() }}</div>
          </div>
        </div>

        <div v-if="selectedItemId === item.id" style="margin-top:6px">
          <ElButton size="small" @click="openCreateWaiver(item.id)">申请 Waiver</ElButton>
        </div>
      </div>

      <EmptyState v-if="!loading && items.length === 0 && !error" description="暂无推荐事项，请先同步" />
    </div>

    <ElDialog v-model="dialogVisible" title="申请 Waiver" width="450px" destroy-on-close>
      <ElForm label-position="top" size="small">
        <ElFormItem label="Waiver 范围">
          <ElSelect v-model="waiverForm.waiverScope">
            <ElOption label="发布例外" value="PROJECT_RELEASE" />
            <ElOption label="策略例外" value="POLICY_EXCEPTION" />
            <ElOption label="签字缺口" value="TEMPORARY_SIGNOFF_GAP" />
            <ElOption label="回滚例外" value="ROLLBACK_READINESS_EXCEPTION" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="申请原因">
          <ElInput v-model="waiverForm.reasonText" :rows="3" type="textarea" placeholder="说明需要 waiver 的原因" />
        </ElFormItem>
        <ElFormItem label="到期时间(可选)">
          <ElInput v-model="waiverForm.expiresAt" placeholder="2026-06-30T23:59:59" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleCreateWaiver">提交申请</ElButton>
      </template>
    </ElDialog>
  </TechPanel>
</template>
