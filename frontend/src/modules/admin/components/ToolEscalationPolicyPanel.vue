<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTag, ElButton, ElMessage, ElMessageBox, ElTable, ElTableColumn, ElTooltip, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElSwitch } from 'element-plus'
import {
  listProjectEscalationPolicies,
  createEscalationPolicy,
  updateEscalationPolicy,
  deleteEscalationPolicy,
  type ToolEscalationPolicyItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId: string
}>()

const loading = ref(false)
const error = ref(false)
const policies = ref<ToolEscalationPolicyItem[]>([])
const dialogVisible = ref(false)
const editingItem = ref<ToolEscalationPolicyItem | null>(null)
const saving = ref(false)

// Form
const form = ref({
  name: '',
  severity: 'MEDIUM',
  slaMinutes: 30,
  escalationAfterMinutes: 5,
  maxEscalationLevel: 3,
  channel: 'IN_APP',
  routeTarget: '',
  enabled: true,
})

const severityOptions = [
  { value: 'CRITICAL', label: '严重' },
  { value: 'HIGH', label: '高' },
  { value: 'MEDIUM', label: '中' },
  { value: 'LOW', label: '低' },
  { value: 'INFO', label: '提示' },
]

const channelOptions = [
  { value: 'IN_APP', label: '站内信' },
  { value: 'EMAIL', label: '邮件' },
  { value: 'SLACK', label: 'Slack' },
  { value: 'PAGERDUTY', label: 'PagerDuty' },
  { value: 'WEBHOOK', label: 'Webhook' },
]

async function loadPolicies() {
  loading.value = true
  error.value = false
  try {
    const res = await listProjectEscalationPolicies(props.projectId)
    policies.value = res.data.data
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  editingItem.value = null
  form.value = { name: '', severity: 'MEDIUM', slaMinutes: 30, escalationAfterMinutes: 5, maxEscalationLevel: 3, channel: 'IN_APP', routeTarget: '', enabled: true }
  dialogVisible.value = true
}

function handleEdit(item: ToolEscalationPolicyItem) {
  editingItem.value = item
  form.value = {
    name: item.name,
    severity: item.severity,
    slaMinutes: item.slaMinutes ?? 30,
    escalationAfterMinutes: item.escalationAfterMinutes ?? 5,
    maxEscalationLevel: item.maxEscalationLevel,
    channel: item.channel,
    routeTarget: item.routeTarget ?? '',
    enabled: item.enabled,
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入策略名称')
    return
  }
  saving.value = true
  try {
    if (editingItem.value) {
      await updateEscalationPolicy(editingItem.value.id, form.value)
      ElMessage.success('策略已更新')
    } else {
      await createEscalationPolicy({ ...form.value, projectId: props.projectId })
      ElMessage.success('策略已创建')
    }
    dialogVisible.value = false
    await loadPolicies()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(policy: ToolEscalationPolicyItem) {
  try {
    await ElMessageBox.confirm(`确定删除升级策略「${policy.name}」？`, '确认删除', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await deleteEscalationPolicy(policy.id)
    ElMessage.success('策略已删除')
    await loadPolicies()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '删除失败')
    }
  }
}

function channelTag(channel: string) {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    IN_APP: 'primary', EMAIL: 'info', SLACK: 'success', PAGERDUTY: 'danger', WEBHOOK: 'warning',
  }
  return map[channel] || 'info'
}

function severityText(sev: string) {
  const map: Record<string, string> = { CRITICAL: '严重', HIGH: '高', MEDIUM: '中', LOW: '低', INFO: '提示' }
  return map[sev] || sev
}

function channelText(ch: string) {
  const map: Record<string, string> = { IN_APP: '站内信', EMAIL: '邮件', SLACK: 'Slack', PAGERDUTY: 'PagerDuty', WEBHOOK: 'Webhook' }
  return map[ch] || ch
}

onMounted(() => {
  loadPolicies()
})
</script>

<template>
  <TechPanel
    v-loading="loading"
    title="事件升级策略 (Escalation Policies)"
    data-testid="tool-escalation-policy-panel"
  >
    <ErrorState
      v-if="error"
      title="加载失败"
      message="无法加载升级策略数据"
      retry-text="重试"
      @retry="loadPolicies"
    />

    <div class="tep-toolbar">
      <ElButton size="small" type="primary" @click="handleCreate" data-testid="tep-create-btn">创建策略</ElButton>
    </div>

    <ElTable
      v-if="policies.length > 0"
      :data="policies"
      size="small"
      style="width:100%"
      data-testid="tep-table"
    >
      <ElTableColumn label="名称" min-width="150">
        <template #default="{ row }">
          <span class="tep-name">{{ row.name }}</span>
          <ElTag v-if="!row.enabled" size="small" type="info" effect="dark" style="margin-left:4px">已禁用</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="严重级别" width="80">
        <template #default="{ row }">{{ severityText(row.severity) }}</template>
      </ElTableColumn>
      <ElTableColumn label="通道" width="100">
        <template #default="{ row }">
          <ElTag :type="channelTag(row.channel)" size="small" effect="dark">{{ channelText(row.channel) }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="目标" width="120">
        <template #default="{ row }">
          <ElTooltip :content="row.routeTarget || '-'" placement="top">
            <span>{{ row.routeTarget || '-' }}</span>
          </ElTooltip>
        </template>
      </ElTableColumn>
      <ElTableColumn label="最大级别" width="80" align="center">
        <template #default="{ row }">L{{ row.maxEscalationLevel }}</template>
      </ElTableColumn>
      <ElTableColumn label="升级间隔" width="90" align="center">
        <template #default="{ row }">{{ row.escalationAfterMinutes ?? '-' }}分钟</template>
      </ElTableColumn>
      <ElTableColumn label="创建时间" width="150">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" @click.stop="handleEdit(row)" data-testid="tep-edit-btn">编辑</ElButton>
          <ElButton size="small" type="danger" @click.stop="handleDelete(row)" data-testid="tep-delete-btn">删除</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <EmptyState v-if="!error && !loading && policies.length === 0" description="暂无升级策略" />

    <!-- Create/Edit dialog -->
    <ElDialog
      v-model="dialogVisible"
      :title="editingItem ? '编辑升级策略' : '创建升级策略'"
      width="540px"
      data-testid="tep-dialog"
    >
      <ElForm label-width="120px" label-position="top" size="small">
        <ElFormItem label="策略名称" required>
          <ElInput v-model="form.name" placeholder="例如：严重事件升级" maxlength="128" data-testid="tep-form-name" />
        </ElFormItem>
        <ElFormItem label="严重级别" required>
          <ElSelect v-model="form.severity" style="width:100%" data-testid="tep-form-severity">
            <ElOption v-for="opt in severityOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </ElSelect>
        </ElFormItem>
        <div style="display:flex;gap:12px">
          <ElFormItem label="SLA (分钟)" style="flex:1">
            <ElInput v-model.number="form.slaMinutes" type="number" :min="1" placeholder="30" />
          </ElFormItem>
          <ElFormItem label="升级间隔 (分钟)" style="flex:1">
            <ElInput v-model.number="form.escalationAfterMinutes" type="number" :min="1" placeholder="5" />
          </ElFormItem>
          <ElFormItem label="最大级别" style="flex:1">
            <ElInput v-model.number="form.maxEscalationLevel" type="number" :min="1" :max="10" placeholder="3" />
          </ElFormItem>
        </div>
        <ElFormItem label="通知通道" required>
          <ElSelect v-model="form.channel" style="width:100%" data-testid="tep-form-channel">
            <ElOption v-for="opt in channelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="路由目标">
          <ElInput v-model="form.routeTarget" placeholder="例如：oncall、#alerts、admin@example.com" maxlength="255" />
        </ElFormItem>
        <ElFormItem label="启用">
          <ElSwitch v-model="form.enabled" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="handleSave" data-testid="tep-save-btn">保存</ElButton>
      </template>
    </ElDialog>
  </TechPanel>
</template>

<style scoped>
.tep-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.tep-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-bright);
}
</style>
