<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElDialog, ElForm, ElFormItem, ElSelect, ElOption, ElInput, ElButton, ElMessage } from 'element-plus'
import { createIncident, type ToolIncidentItem } from '@/modules/admin/api'

const props = defineProps<{
  modelValue: boolean
  projectId: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [incident: ToolIncidentItem]
}>()

const loading = ref(false)

const form = ref({
  sourceType: 'MANUAL',
  severity: 'MEDIUM',
  title: '',
  summary: '',
  assigneeId: '',
  toolExecutionId: '',
  toolJobId: '',
  operatorReviewId: '',
  sourceId: '',
})

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

watch(() => props.modelValue, (val) => {
  if (val) {
    form.value = {
      sourceType: 'MANUAL',
      severity: 'MEDIUM',
      title: '',
      summary: '',
      assigneeId: '',
      toolExecutionId: '',
      toolJobId: '',
      operatorReviewId: '',
      sourceId: '',
    }
  }
})

async function handleSave() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入事件标题')
    return
  }

  loading.value = true
  try {
    const res = await createIncident({
      projectId: props.projectId,
      sourceType: form.value.sourceType,
      sourceId: form.value.sourceId || undefined,
      severity: form.value.severity,
      title: form.value.title.trim(),
      summary: form.value.summary.trim() || undefined,
      assigneeId: form.value.assigneeId || undefined,
      toolExecutionId: form.value.toolExecutionId || undefined,
      toolJobId: form.value.toolJobId || undefined,
      operatorReviewId: form.value.operatorReviewId || undefined,
    })
    emit('saved', res.data.data)
    closeDialog()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally {
    loading.value = false
  }
}

function closeDialog() {
  emit('update:modelValue', false)
}
</script>

<template>
  <ElDialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="创建工具事件"
    width="560px"
    :close-on-click-modal="false"
    destroy-on-close
    data-testid="incident-dialog"
  >
    <ElForm
      :model="form"
      label-position="top"
      size="default"
      data-testid="tid-form"
    >
      <ElFormItem label="事件来源" required data-testid="tid-source-type">
        <ElSelect v-model="form.sourceType">
          <ElOption v-for="opt in sourceTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="严重级别" required data-testid="tid-severity">
        <ElSelect v-model="form.severity">
          <ElOption v-for="opt in severityOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="事件标题" required data-testid="tid-title">
        <ElInput v-model="form.title" placeholder="简要描述事件" maxlength="255" />
      </ElFormItem>
      <ElFormItem label="摘要" data-testid="tid-summary">
        <ElInput v-model="form.summary" type="textarea" :rows="3" placeholder="详细描述（可选）" />
      </ElFormItem>
      <ElFormItem label="来源 ID" data-testid="tid-source-id">
        <ElInput v-model="form.sourceId" placeholder="关联资源 ID（可选）" />
      </ElFormItem>
      <ElFormItem label="Tool 执行 ID" data-testid="tid-execution">
        <ElInput v-model="form.toolExecutionId" placeholder="关联 Tool Execution ID（可选）" />
      </ElFormItem>
      <ElFormItem label="Tool Job ID" data-testid="tid-job">
        <ElInput v-model="form.toolJobId" placeholder="关联 Tool Job ID（可选）" />
      </ElFormItem>
      <ElFormItem label="Operator 审查 ID" data-testid="tid-review">
        <ElInput v-model="form.operatorReviewId" placeholder="关联 Operator Review ID（可选）" />
      </ElFormItem>
      <ElFormItem label="处理人 ID" data-testid="tid-assignee">
        <ElInput v-model="form.assigneeId" placeholder="指定处理人 ID（可选）" />
      </ElFormItem>
    </ElForm>

    <template #footer>
      <ElButton @click="closeDialog" :disabled="loading" data-testid="tid-cancel-btn">取消</ElButton>
      <ElButton type="primary" @click="handleSave" :loading="loading" data-testid="tid-save-btn">创建</ElButton>
    </template>
  </ElDialog>
</template>
