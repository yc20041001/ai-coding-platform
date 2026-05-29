<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  listOrganizationPolicies,
  createOrganizationPolicy,
  updateOrganizationPolicy,
  updateOrganizationPolicyStatus,
  type OrganizationTrialPolicyItem,
  type CreateOrganizationTrialPolicyRequest,
  type UpdateOrganizationTrialPolicyRequest,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElTooltip } from 'element-plus'

const policies = ref<OrganizationTrialPolicyItem[]>([])
const loading = ref(false)
const error = ref(false)

const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const form = ref<{
  policyKey: string
  displayName: string
  policyScope: string
  thresholdJson: string
  signoffPolicyJson: string
  rollbackPolicyJson: string
  verificationPolicyJson: string
  recommendationPolicyJson: string
  notes: string
}>({
  policyKey: '',
  displayName: '',
  policyScope: 'GLOBAL',
  thresholdJson: '',
  signoffPolicyJson: '',
  rollbackPolicyJson: '',
  verificationPolicyJson: '',
  recommendationPolicyJson: '',
  notes: '',
})

function loadData() {
  loading.value = true
  error.value = false
  listOrganizationPolicies()
    .then(res => { policies.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

function openCreate() {
  editingId.value = null
  form.value = { policyKey: '', displayName: '', policyScope: 'GLOBAL', thresholdJson: '', signoffPolicyJson: '', rollbackPolicyJson: '', verificationPolicyJson: '', recommendationPolicyJson: '', notes: '' }
  dialogVisible.value = true
}

function openEdit(policy: OrganizationTrialPolicyItem) {
  editingId.value = policy.id
  form.value = {
    policyKey: policy.policyKey,
    displayName: policy.displayName,
    policyScope: policy.policyScope,
    thresholdJson: policy.thresholdJson || '',
    signoffPolicyJson: policy.signoffPolicyJson || '',
    rollbackPolicyJson: policy.rollbackPolicyJson || '',
    verificationPolicyJson: policy.verificationPolicyJson || '',
    recommendationPolicyJson: policy.recommendationPolicyJson || '',
    notes: policy.notes || '',
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (editingId.value) {
    const data: UpdateOrganizationTrialPolicyRequest = {
      displayName: form.value.displayName,
      policyScope: form.value.policyScope,
      thresholdJson: form.value.thresholdJson || null,
      signoffPolicyJson: form.value.signoffPolicyJson || null,
      rollbackPolicyJson: form.value.rollbackPolicyJson || null,
      verificationPolicyJson: form.value.verificationPolicyJson || null,
      recommendationPolicyJson: form.value.recommendationPolicyJson || null,
      notes: form.value.notes || null,
    }
    try {
      await updateOrganizationPolicy(editingId.value, data)
      ElMessage.success('策略已更新')
      dialogVisible.value = false
      loadData()
    } catch { ElMessage.error('更新失败') }
  } else {
    const data: CreateOrganizationTrialPolicyRequest = {
      policyKey: form.value.policyKey,
      displayName: form.value.displayName,
      policyScope: form.value.policyScope,
    }
    if (form.value.thresholdJson) data.thresholdJson = form.value.thresholdJson
    if (form.value.signoffPolicyJson) data.signoffPolicyJson = form.value.signoffPolicyJson
    if (form.value.rollbackPolicyJson) data.rollbackPolicyJson = form.value.rollbackPolicyJson
    if (form.value.verificationPolicyJson) data.verificationPolicyJson = form.value.verificationPolicyJson
    if (form.value.recommendationPolicyJson) data.recommendationPolicyJson = form.value.recommendationPolicyJson
    if (form.value.notes) data.notes = form.value.notes
    try {
      await createOrganizationPolicy(data)
      ElMessage.success('策略已创建')
      dialogVisible.value = false
      loadData()
    } catch { ElMessage.error('创建失败') }
  }
}

async function toggleStatus(policy: OrganizationTrialPolicyItem) {
  try {
    await updateOrganizationPolicyStatus(policy.id, !policy.enabled)
    ElMessage.success(policy.enabled ? '策略已禁用' : '策略已启用')
    loadData()
  } catch { ElMessage.error('操作失败') }
}

const scopeLabel = (s: string) => ({ GLOBAL: '全局', PROJECT_GROUP: '项目组', PROJECT_OVERRIDE: '项目覆盖' }[s] || s)

onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">组织 Trial 策略</span>
      <ElButton size="small" type="primary" @click="openCreate">新建策略</ElButton>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取策略列表" retry-text="重试" @retry="loadData" />

    <div v-if="!loading && !error">
      <div v-for="p in policies" :key="p.id" style="padding:10px;margin-bottom:8px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:6px">
          <div style="display:flex;align-items:center;gap:8px">
            <span style="font-weight:600;font-size:13px;color:var(--app-text-bright)">{{ p.displayName }}</span>
            <ElTag size="small" :type="p.enabled ? 'success' : 'info'">{{ p.enabled ? '启用' : '禁用' }}</ElTag>
            <ElTag size="small">{{ scopeLabel(p.policyScope) }}</ElTag>
          </div>
          <div style="display:flex;gap:4px">
            <ElButton size="small" link @click="openEdit(p)">编辑</ElButton>
            <ElButton size="small" link @click="toggleStatus(p)">{{ p.enabled ? '禁用' : '启用' }}</ElButton>
          </div>
        </div>
        <div style="font-size:11px;color:var(--app-text-muted)">
          <span>Key: {{ p.policyKey }}</span>
          <span v-if="p.notes" style="margin-left:12px">{{ p.notes }}</span>
        </div>
      </div>
      <EmptyState v-if="!loading && policies.length === 0 && !error" description="暂无策略，请新建" />
    </div>

    <ElDialog v-model="dialogVisible" :title="editingId ? '编辑策略' : '新建策略'" width="500px" destroy-on-close>
      <ElForm label-width="120px" label-position="top" size="small">
        <ElFormItem label="策略 Key" v-if="!editingId">
          <ElInput v-model="form.policyKey" placeholder="唯一标识" />
        </ElFormItem>
        <ElFormItem label="名称">
          <ElInput v-model="form.displayName" placeholder="策略名称" />
        </ElFormItem>
        <ElFormItem label="范围">
          <ElSelect v-model="form.policyScope">
            <ElOption label="全局" value="GLOBAL" />
            <ElOption label="项目组" value="PROJECT_GROUP" />
            <ElOption label="项目覆盖" value="PROJECT_OVERRIDE" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="阈值策略(JSON)">
          <ElInput v-model="form.thresholdJson" :rows="2" type="textarea" placeholder='{"minConfidence": 60}' />
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput v-model="form.notes" :rows="2" type="textarea" placeholder="策略说明" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSave">保存</ElButton>
      </template>
    </ElDialog>
  </TechPanel>
</template>
