<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTag, ElButton, ElMessage, ElMessageBox, ElTable, ElTableColumn, ElSwitch } from 'element-plus'
import {
  listProjectAlertRules,
  createAlertRule,
  updateAlertRule,
  type ToolAlertRuleItem,
  type CreateToolAlertRuleRequest,
  type UpdateToolAlertRuleRequest,
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
const rules = ref<ToolAlertRuleItem[]>([])
const showForm = ref(false)
const saving = ref(false)

const form = ref<CreateToolAlertRuleRequest>({
  projectId: props.projectId,
  name: '',
  sourceType: 'TOOL_EXECUTION_FAILED',
  minSeverity: 'MEDIUM',
  channel: 'IN_APP',
  routeTarget: '',
  configJson: '',
})

const channelOptions = [
  { value: 'IN_APP', label: '站内通知' },
  { value: 'MOCK_WEBHOOK', label: 'Webhook（模拟）' },
  { value: 'MOCK_SLACK', label: 'Slack（模拟）' },
  { value: 'MOCK_EMAIL', label: '邮件（模拟）' },
]

const sourceTypeOptions = [
  { value: 'TOOL_EXECUTION_FAILED', label: '执行失败' },
  { value: 'TOOL_JOB_FAILED', label: 'Job 失败' },
  { value: 'TOOL_JOB_RETRY_PENDING', label: '待重试' },
  { value: 'TOOL_JOB_DEAD_LETTERED', label: '死信' },
  { value: 'READ_ONLY_CONTRACT_WARNING', label: '只读合约警告' },
  { value: 'TRACE_OUTPUT_PARSE_WARNING', label: '输出解析警告' },
  { value: 'HIGH_RISK_REVIEW', label: '高风险审查' },
  { value: 'OPERATOR_REVIEW', label: 'Operator 审查' },
  { value: 'MANUAL', label: '手动创建' },
]

const severityOptions = [
  { value: 'INFO', label: '提示' },
  { value: 'LOW', label: '低' },
  { value: 'MEDIUM', label: '中' },
  { value: 'HIGH', label: '高' },
  { value: 'CRITICAL', label: '严重' },
]

async function loadRules() {
  loading.value = true
  error.value = false
  try {
    const res = await listProjectAlertRules(props.projectId)
    rules.value = res.data.data
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

async function handleToggleEnabled(rule: ToolAlertRuleItem) {
  try {
    await updateAlertRule(rule.id, { enabled: !rule.enabled })
    rule.enabled = !rule.enabled
    ElMessage.success(rule.enabled ? '规则已启用' : '规则已禁用')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function handleDeleteRule(rule: ToolAlertRuleItem) {
  try {
    await ElMessageBox.confirm(`确定删除规则「${rule.name}」？`, '确认删除', { type: 'warning' })
    // Soft delete via setting enabled=false
    await updateAlertRule(rule.id, { enabled: false })
    ElMessage.success('规则已禁用')
    await loadRules()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '操作失败')
    }
  }
}

async function handleCreateRule() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入规则名称')
    return
  }

  saving.value = true
  try {
    await createAlertRule({
      ...form.value,
      projectId: props.projectId,
      routeTarget: form.value.routeTarget || undefined,
      configJson: form.value.configJson || undefined,
    })
    ElMessage.success('规则已创建')
    showForm.value = false
    form.value.routeTarget = ''
    form.value.configJson = ''
    await loadRules()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally {
    saving.value = false
  }
}

function channelTag(channel: string) {
  const map: Record<string, 'primary' | 'warning' | 'success' | 'info'> = { IN_APP: 'primary', MOCK_WEBHOOK: 'warning', MOCK_SLACK: 'success', MOCK_EMAIL: 'info' }
  return map[channel] || 'info'
}

onMounted(() => {
  loadRules()
})
</script>

<template>
  <TechPanel
    v-loading="loading"
    title="告警规则 (Alert Rules)"
    data-testid="alert-rule-panel"
  >
    <ErrorState
      v-if="error"
      title="加载失败"
      message="无法加载告警规则"
      retry-text="重试"
      @retry="loadRules"
    />

    <!-- Create form -->
    <div v-if="showForm" class="tar-form" data-testid="tar-form">
      <div class="tar-form-grid">
        <el-input v-model="form.name" placeholder="规则名称" size="small" data-testid="tar-name" />
        <el-select v-model="form.sourceType" size="small" style="width:140px">
          <el-option v-for="opt in sourceTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-select v-model="form.minSeverity" size="small" style="width:100px">
          <el-option v-for="opt in severityOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-select v-model="form.channel" size="small" style="width:120px">
          <el-option v-for="opt in channelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-input v-model="form.routeTarget" placeholder="路由目标" size="small" style="flex:1" data-testid="tar-target" />
      </div>
      <div class="tar-form-actions">
        <ElButton size="small" type="primary" @click="handleCreateRule" :loading="saving" data-testid="tar-save-btn">创建</ElButton>
        <ElButton size="small" @click="showForm = false">取消</ElButton>
      </div>
    </div>

    <div v-else class="tar-toolbar">
      <ElButton size="small" type="primary" @click="showForm = true" data-testid="tar-add-btn">添加规则</ElButton>
    </div>

    <!-- Rules table -->
    <ElTable v-if="rules.length > 0" :data="rules" size="small" style="width:100%" data-testid="tar-table">
      <ElTableColumn label="启用" width="60">
        <template #default="{ row }">
          <ElSwitch :model-value="row.enabled" size="small" @click.stop @change="handleToggleEnabled(row)" data-testid="tar-toggle" />
        </template>
      </ElTableColumn>
      <ElTableColumn label="名称" min-width="120" prop="name" />
      <ElTableColumn label="来源" width="90">
        <template #default="{ row }">
          <ElTag size="small">{{ row.sourceType }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="最低级别" width="80">
        <template #default="{ row }">{{ row.minSeverity }}</template>
      </ElTableColumn>
      <ElTableColumn label="通道" width="90">
        <template #default="{ row }">
          <ElTag :type="channelTag(row.channel)" size="small" effect="dark">{{ row.channel }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="目标" min-width="120" prop="routeTarget" />
      <ElTableColumn label="创建时间" width="150">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" type="danger" link @click="handleDeleteRule(row)" data-testid="tar-delete-btn">禁用</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <EmptyState v-if="!error && !loading && rules.length === 0" description="暂无告警规则" />
  </TechPanel>
</template>

<style scoped>
.tar-form {
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(56, 189, 248, 0.15);
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}

.tar-form-grid {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.tar-form-actions {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(56, 189, 248, 0.1);
}

.tar-toolbar {
  margin-bottom: 12px;
}
</style>
