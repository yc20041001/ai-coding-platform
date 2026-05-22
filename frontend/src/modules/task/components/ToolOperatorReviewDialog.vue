<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElDialog, ElForm, ElFormItem, ElSelect, ElOption, ElInput, ElButton, ElMessage } from 'element-plus'
import { createOperatorReview, updateOperatorReview, listTargetOperatorReviews } from '@/modules/task/api'
import type { ToolOperatorReview } from '@/modules/task/api'

const props = defineProps<{
  modelValue: boolean
  targetType: string
  targetId: string
  projectId: string
  reviewId?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [review: ToolOperatorReview]
}>()

const loading = ref(false)
const existingReviews = ref<ToolOperatorReview[]>([])
const isEditMode = ref(false)

const form = ref({
  severity: 'MEDIUM',
  title: '',
  summary: '',
  assigneeId: '',
})

const severityOptions = [
  { value: 'INFO', label: '提示' },
  { value: 'LOW', label: '低' },
  { value: 'MEDIUM', label: '中' },
  { value: 'HIGH', label: '高' },
  { value: 'CRITICAL', label: '严重' },
]

watch(() => props.modelValue, async (val) => {
  if (val) {
    form.value.severity = 'MEDIUM'
    form.value.title = ''
    form.value.summary = ''
    form.value.assigneeId = ''
    isEditMode.value = !!(props.reviewId)
    if (!props.reviewId) {
      await loadExisting()
    }
  }
})

async function loadExisting() {
  try {
    const res = await listTargetOperatorReviews(props.targetType, props.targetId)
    existingReviews.value = res.data.data
  } catch {
    existingReviews.value = []
  }
}

async function handleSave() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入审查标题')
    return
  }

  loading.value = true
  try {
    if (isEditMode.value && props.reviewId) {
      const res = await updateOperatorReview(props.reviewId, {
        severity: form.value.severity,
        title: form.value.title.trim(),
        summary: form.value.summary.trim() || undefined,
        assigneeId: form.value.assigneeId || undefined,
      })
      emit('saved', res.data.data)
      ElMessage.success('审查已更新')
      closeDialog()
    } else {
      const res = await createOperatorReview({
        reviewTargetType: props.targetType,
        reviewTargetId: props.targetId,
        severity: form.value.severity,
        title: form.value.title.trim(),
        summary: form.value.summary.trim() || undefined,
        assigneeId: form.value.assigneeId || undefined,
      })
      emit('saved', res.data.data)
      ElMessage.success('Operator 审查已创建')
      closeDialog()
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

function closeDialog() {
  emit('update:modelValue', false)
}

function severityBadge(severity: string): string {
  const map: Record<string, string> = { INFO: 'info', LOW: '', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }
  return map[severity] || 'info'
}
</script>

<template>
  <ElDialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="isEditMode ? '编辑 Operator 审查' : '创建 Operator 审查'"
    width="560px"
    :close-on-click-modal="false"
    destroy-on-close
    data-testid="operator-review-dialog"
  >
    <!-- Existing reviews -->
    <div v-if="!isEditMode && existingReviews.length > 0" class="tor-existing">
      <div class="tor-existing-title">已有审查记录</div>
      <div v-for="r in existingReviews" :key="r.id" class="tor-existing-item" data-testid="tor-existing-review-item">
        <ElTag :type="severityBadge(r.severity)" size="small" effect="dark">{{ r.severity }}</ElTag>
        <span class="tor-existing-status" :class="`tor-existing-status--${r.status.toLowerCase()}`">{{ r.status }}</span>
        <span class="tor-existing-title-text">{{ r.title }}</span>
      </div>
    </div>

    <ElForm
      ref="formRef"
      :model="form"
      label-position="top"
      size="default"
      data-testid="tor-form"
    >
      <ElFormItem label="目标类型" data-testid="tor-target-type">
        <ElInput :model-value="targetType" disabled />
      </ElFormItem>
      <ElFormItem label="目标 ID" data-testid="tor-target-id">
        <ElInput :model-value="targetId" disabled />
      </ElFormItem>
      <ElFormItem label="严重级别" required data-testid="tor-severity">
        <ElSelect v-model="form.severity">
          <ElOption v-for="opt in severityOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </ElSelect>
      </ElFormItem>
      <ElFormItem label="审查标题" required data-testid="tor-title">
        <ElInput v-model="form.title" placeholder="简要描述需要审查的内容" maxlength="255" />
      </ElFormItem>
      <ElFormItem label="审查摘要" data-testid="tor-summary">
        <ElInput v-model="form.summary" type="textarea" :rows="3" placeholder="详细描述（可选）" />
      </ElFormItem>
      <ElFormItem label="处理人 ID" data-testid="tor-assignee">
        <ElInput v-model="form.assigneeId" placeholder="指定处理人 ID（可选）" />
      </ElFormItem>
    </ElForm>

    <template #footer>
      <ElButton @click="closeDialog" :disabled="loading" data-testid="tor-cancel-btn">取消</ElButton>
      <ElButton type="primary" @click="handleSave" :loading="loading" data-testid="tor-save-btn">
        {{ isEditMode ? '更新' : '创建' }}
      </ElButton>
    </template>
  </ElDialog>
</template>

<style scoped>
.tor-existing {
  background: rgba(251, 191, 36, 0.06);
  border: 1px solid rgba(251, 191, 36, 0.15);
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 16px;
}

.tor-existing-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.tor-existing-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 12px;
}

.tor-existing-status {
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
}

.tor-existing-status--open { color: var(--el-color-primary); }
.tor-existing-status--in_progress { color: var(--el-color-warning); }
.tor-existing-status--resolved { color: var(--el-color-success); }
.tor-existing-status--wont_fix { color: var(--app-text-muted); }
.tor-existing-status--false_positive { color: var(--app-text-muted); }

.tor-existing-title-text {
  color: var(--app-text-soft);
  flex: 1;
}
</style>
