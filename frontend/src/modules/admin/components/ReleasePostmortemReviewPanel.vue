<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  getPostmortemReview,
  createPostmortemReview,
  updatePostmortemReview,
  updatePostmortemReviewStatus,
  getPrefilledPostmortemReview,
  type ReleasePostmortemReviewItem,
  type CreateReleasePostmortemReviewRequest,
  type UpdateReleasePostmortemReviewRequest,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag, ElButton, ElDialog, ElMessage, ElInput, ElSelect, ElOption, ElForm, ElFormItem } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId?: string | null
  planId?: string | null
}>()

const emit = defineEmits<{
  (e: 'dashboard-refresh'): void
}>()

const review = ref<ReleasePostmortemReviewItem | null>(null)
const loading = ref(false)
const error = ref(false)
const editDialogVisible = ref(false)
const prefillDialogVisible = ref(false)
const prefillData = ref<ReleasePostmortemReviewItem | null>(null)

const form = ref<CreateReleasePostmortemReviewRequest>({})

function loadReview() {
  if (!props.planId) return
  loading.value = true
  error.value = false
  getPostmortemReview(props.planId)
    .then(res => { review.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleCreate() {
  if (!props.planId) return
  try {
    await createPostmortemReview(props.planId, { ...form.value })
    ElMessage.success('复盘创建成功')
    editDialogVisible.value = false
    loadReview()
    emit('dashboard-refresh')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  }
}

function openCreate() {
  form.value = {
    overallOutcome: 'SUCCESS_WITH_ISSUES',
  }
  editDialogVisible.value = true
}

function openEdit() {
  if (!review.value) return
  form.value = {
    overallOutcome: review.value.overallOutcome ?? undefined,
    summary: review.value.summary ?? undefined,
    whatWentWell: review.value.whatWentWell ?? undefined,
    whatWentWrong: review.value.whatWentWrong ?? undefined,
    customerImpact: review.value.customerImpact ?? undefined,
    followUpActions: review.value.followUpActions ?? undefined,
  }
  editDialogVisible.value = true
}

async function handleEdit() {
  if (!props.planId || !review.value) return
  try {
    await updatePostmortemReview(props.planId, review.value.id, form.value as UpdateReleasePostmortemReviewRequest)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadReview()
    emit('dashboard-refresh')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '更新失败')
  }
}

async function handleStatus(reviewStatus: string) {
  if (!props.planId || !review.value) return
  try {
    await updatePostmortemReviewStatus(props.planId, review.value.id, reviewStatus)
    ElMessage.success('状态更新成功')
    loadReview()
    emit('dashboard-refresh')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '状态更新失败')
  }
}

async function handlePrefill() {
  if (!props.planId) return
  try {
    const res = await getPrefilledPostmortemReview(props.planId)
    prefillData.value = res.data.data
    prefillDialogVisible.value = true
  } catch {
    ElMessage.error('预填失败')
  }
}

function statusTag(status: string) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    DRAFT: 'info',
    REVIEWED: 'success',
    PUBLISHED: 'success',
    ARCHIVED: 'warning',
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    REVIEWED: '已审查',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档',
  }
  return map[status] || status
}

function outcomeLabel(outcome: string): string {
  const map: Record<string, string> = {
    SUCCESS: '成功',
    SUCCESS_WITH_ISSUES: '有问题的成功',
    ROLLBACK_NEEDED: '需要回滚',
    FAILED_RELEASE: '发布失败',
  }
  return map[outcome] || outcome
}

function canTransition(status: string): string[] {
  switch (status) {
    case 'DRAFT': return ['REVIEWED', 'PUBLISHED']
    case 'REVIEWED': return ['PUBLISHED', 'DRAFT']
    case 'PUBLISHED': return ['ARCHIVED']
    default: return []
  }
}

function transitionLabel(status: string): string {
  const map: Record<string, string> = {
    REVIEWED: '审查通过',
    PUBLISHED: '发布',
    DRAFT: '返回草稿',
    ARCHIVED: '归档',
  }
  return map[status] || status
}

watch(() => props.planId, () => { if (props.planId) loadReview() }, { immediate: true })
</script>

<template>
  <div>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">发布复盘</span>
      <div style="display:flex;gap:4px">
        <ElButton v-if="props.planId && !review" size="small" type="primary" @click="openCreate()">新建复盘</ElButton>
        <ElButton v-if="props.planId && !review" size="small" @click="handlePrefill">预填信号</ElButton>
        <ElButton v-if="props.planId && review" size="small" @click="openEdit()">编辑</ElButton>
      </div>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取发布复盘" retry-text="重试" @retry="loadReview" />

    <div v-if="!props.planId" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">请先选择一个 Rollout Plan</div>

    <div v-if="review && props.planId" v-loading="loading">
      <div style="display:flex;gap:8px;margin-bottom:10px;flex-wrap:wrap">
        <ElTag :type="statusTag(review.reviewStatus)" size="small" effect="dark">{{ statusLabel(review.reviewStatus) }}</ElTag>
        <ElTag size="small" effect="plain">{{ outcomeLabel(review.overallOutcome) }}</ElTag>
        <template v-for="t in canTransition(review.reviewStatus)" :key="t">
          <ElButton size="small" link @click="handleStatus(t)">{{ transitionLabel(t) }}</ElButton>
        </template>
      </div>

      <div style="font-size:12px;line-height:1.8;color:var(--app-text-bright)">
        <div v-if="review.summary" style="margin-bottom:8px">
          <span style="color:var(--app-text-muted)">总结:</span><br>
          {{ review.summary }}
        </div>
        <div v-if="review.whatWentWell" style="margin-bottom:8px">
          <span style="color:var(--app-text-muted)">做得好:</span><br>
          {{ review.whatWentWell }}
        </div>
        <div v-if="review.whatWentWrong" style="margin-bottom:8px">
          <span style="color:var(--app-text-muted)">做得差:</span><br>
          {{ review.whatWentWrong }}
        </div>
        <div v-if="review.customerImpact" style="margin-bottom:8px">
          <span style="color:var(--app-text-muted)">客户影响:</span><br>
          {{ review.customerImpact }}
        </div>
        <div v-if="review.followUpActions" style="margin-bottom:8px">
          <span style="color:var(--app-text-muted)">改进项:</span><br>
          {{ review.followUpActions }}
        </div>
      </div>

      <div v-if="review.reviewedAt" style="margin-top:8px;font-size:11px;color:var(--app-text-muted)">
        审查日期: {{ formatDateTime(review.reviewedAt) }}
      </div>
    </div>

    <EmptyState v-if="!loading && !review && props.planId && !error" description="暂无发布复盘" />

    <!-- Create/Edit Dialog -->
    <ElDialog v-model="editDialogVisible" :title="review ? '编辑发布复盘' : '新建发布复盘'" width="600px">
      <ElForm label-position="top" size="small">
        <ElFormItem label="总体结果">
          <ElSelect v-model="form.overallOutcome">
            <ElOption label="成功" value="SUCCESS" />
            <ElOption label="有问题的成功" value="SUCCESS_WITH_ISSUES" />
            <ElOption label="需要回滚" value="ROLLBACK_NEEDED" />
            <ElOption label="发布失败" value="FAILED_RELEASE" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="总结">
          <ElInput v-model="form.summary" type="textarea" :rows="3" />
        </ElFormItem>
        <ElFormItem label="做得好">
          <ElInput v-model="form.whatWentWell" type="textarea" :rows="3" />
        </ElFormItem>
        <ElFormItem label="做得差">
          <ElInput v-model="form.whatWentWrong" type="textarea" :rows="3" />
        </ElFormItem>
        <ElFormItem label="客户影响">
          <ElInput v-model="form.customerImpact" type="textarea" :rows="2" />
        </ElFormItem>
        <ElFormItem label="改进项">
          <ElInput v-model="form.followUpActions" type="textarea" :rows="3" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="editDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="review ? handleEdit() : handleCreate()">{{ review ? '保存' : '创建' }}</ElButton>
      </template>
    </ElDialog>

    <!-- Prefill Dialog -->
    <ElDialog v-model="prefillDialogVisible" title="预填发布复盘" width="600px">
      <div v-if="prefillData" style="font-size:12px;line-height:1.8;color:var(--app-text-bright)">
        <div style="margin-bottom:8px"><span style="color:var(--app-text-muted)">总体结果:</span> {{ outcomeLabel(prefillData.overallOutcome) }}</div>
        <div style="margin-bottom:8px"><span style="color:var(--app-text-muted)">总结:</span><br>{{ prefillData.summary }}</div>
        <div style="margin-bottom:8px"><span style="color:var(--app-text-muted)">做得好:</span><br>{{ prefillData.whatWentWell }}</div>
        <div style="margin-bottom:8px"><span style="color:var(--app-text-muted)">做得差:</span><br>{{ prefillData.whatWentWrong }}</div>
        <div style="margin-bottom:8px"><span style="color:var(--app-text-muted)">客户影响:</span><br>{{ prefillData.customerImpact }}</div>
        <div style="margin-bottom:8px"><span style="color:var(--app-text-muted)">改进项:</span><br>{{ prefillData.followUpActions }}</div>
      </div>
      <template #footer>
        <ElButton @click="prefillDialogVisible = false">关闭</ElButton>
      </template>
    </ElDialog>
  </div>
</template>
