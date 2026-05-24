<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTag, ElButton, ElInput, ElMessage, ElDialog, ElForm, ElFormItem, ElInputNumber, ElTable, ElTableColumn } from 'element-plus'
import { getProjectKnowledgeQualitySummary, createKnowledgeQualityReview, type KnowledgeQualityStatusSummary, type KnowledgeQualityReviewItem } from '@/modules/admin/api'

const props = defineProps<{
  projectId: string
}>()

const emit = defineEmits<{
  saved: []
}>()

const loading = ref(false)
const summary = ref<KnowledgeQualityStatusSummary | null>(null)
const dialogVisible = ref(false)
const saving = ref(false)

// New review form
const newReview = ref({
  incidentId: '',
  completenessScore: 3,
  accuracyScore: 3,
  actionabilityScore: 3,
  relevanceScore: 3,
  reviewComment: '',
})

function overallStatusTag(status: string) {
  const map: Record<string, string> = { APPROVED: 'success', NEEDS_WORK: 'warning', REJECTED: 'danger' }
  return map[status] || 'info'
}

function overallStatusText(status: string) {
  const map: Record<string, string> = { APPROVED: '已通过', NEEDS_WORK: '需改进', REJECTED: '已拒绝' }
  return map[status] || status
}

async function loadSummary() {
  loading.value = true
  try {
    const res = await getProjectKnowledgeQualitySummary(props.projectId)
    summary.value = res.data.data
  } catch {
    summary.value = null
  } finally {
    loading.value = false
  }
}

async function handleCreateReview() {
  if (!newReview.value.incidentId) {
    ElMessage.warning('请输入事件 ID')
    return
  }
  saving.value = true
  try {
    await createKnowledgeQualityReview(newReview.value.incidentId, {
      completenessScore: newReview.value.completenessScore,
      accuracyScore: newReview.value.accuracyScore,
      actionabilityScore: newReview.value.actionabilityScore,
      relevanceScore: newReview.value.relevanceScore,
      reviewComment: newReview.value.reviewComment || undefined,
    })
    ElMessage.success('审查创建成功')
    dialogVisible.value = false
    newReview.value = { incidentId: '', completenessScore: 3, accuracyScore: 3, actionabilityScore: 3, relevanceScore: 3, reviewComment: '' }
    loadSummary()
    emit('saved')
  } catch {
    ElMessage.error('创建审查失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadSummary()
})
</script>

<template>
  <div class="quality-panel" data-testid="knowledge-quality-panel">
    <div class="quality-header">
      <span class="quality-title">知识质量审查概览</span>
      <ElButton size="small" type="primary" @click="dialogVisible = true" data-testid="create-quality-review-btn">
        新建审查
      </ElButton>
    </div>

    <div v-if="loading" class="quality-loading">加载中...</div>

    <div v-else-if="summary" class="quality-summary">
      <div class="quality-stats">
        <div class="quality-stat">
          <span class="quality-stat-value">{{ summary.totalReviews }}</span>
          <span class="quality-stat-label">总审查数</span>
        </div>
        <div class="quality-stat">
          <span class="quality-stat-value success">{{ summary.approvedCount }}</span>
          <span class="quality-stat-label">已通过</span>
        </div>
        <div class="quality-stat">
          <span class="quality-stat-value warning">{{ summary.needsWorkCount }}</span>
          <span class="quality-stat-label">需改进</span>
        </div>
        <div class="quality-stat">
          <span class="quality-stat-value danger">{{ summary.rejectedCount }}</span>
          <span class="quality-stat-label">已拒绝</span>
        </div>
      </div>

      <div class="quality-scores">
        <div class="quality-score-item">
          <span class="quality-score-label">完整性</span>
          <span class="quality-score-value">{{ summary.averageCompletenessScore.toFixed(1) }}</span>
        </div>
        <div class="quality-score-item">
          <span class="quality-score-label">准确性</span>
          <span class="quality-score-value">{{ summary.averageAccuracyScore.toFixed(1) }}</span>
        </div>
        <div class="quality-score-item">
          <span class="quality-score-label">可操作性</span>
          <span class="quality-score-value">{{ summary.averageActionabilityScore.toFixed(1) }}</span>
        </div>
        <div class="quality-score-item">
          <span class="quality-score-label">相关性</span>
          <span class="quality-score-value">{{ summary.averageRelevanceScore.toFixed(1) }}</span>
        </div>
        <div class="quality-score-item overall">
          <span class="quality-score-label">综合</span>
          <span class="quality-score-value">{{ summary.overallAverageScore.toFixed(1) }}</span>
        </div>
      </div>

      <div class="quality-status-breakdown">
        <span>待审查: {{ summary.pendingCount }}</span>
        <span>审查中: {{ summary.inReviewCount }}</span>
      </div>
    </div>

    <div v-else class="quality-empty">
      暂无质量审查数据
    </div>

    <ElDialog v-model="dialogVisible" title="新建知识质量审查" width="500px">
      <ElForm label-position="top">
        <ElFormItem label="事件 ID" required>
          <ElInput v-model="newReview.incidentId" placeholder="输入事件 ID" />
        </ElFormItem>
        <div class="quality-score-grid">
          <ElFormItem label="完整性 (0-5)">
            <ElInputNumber v-model="newReview.completenessScore" :min="0" :max="5" />
          </ElFormItem>
          <ElFormItem label="准确性 (0-5)">
            <ElInputNumber v-model="newReview.accuracyScore" :min="0" :max="5" />
          </ElFormItem>
          <ElFormItem label="可操作性 (0-5)">
            <ElInputNumber v-model="newReview.actionabilityScore" :min="0" :max="5" />
          </ElFormItem>
          <ElFormItem label="相关性 (0-5)">
            <ElInputNumber v-model="newReview.relevanceScore" :min="0" :max="5" />
          </ElFormItem>
        </div>
        <ElFormItem label="审查意见">
          <ElInput v-model="newReview.reviewComment" type="textarea" :rows="3" placeholder="可选" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="handleCreateReview">创建</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.quality-panel {
  padding: 4px 0;
}

.quality-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.quality-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--app-text-soft);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.quality-loading, .quality-empty {
  padding: 20px;
  text-align: center;
  color: var(--app-text-muted);
  font-size: 13px;
}

.quality-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.quality-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  padding: 10px;
}

.quality-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--app-text-bright);
}

.quality-stat-value.success { color: var(--el-color-success); }
.quality-stat-value.warning { color: var(--el-color-warning); }
.quality-stat-value.danger { color: var(--el-color-danger); }

.quality-stat-label {
  font-size: 11px;
  color: var(--app-text-muted);
  margin-top: 2px;
}

.quality-scores {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.quality-score-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px;
  background: rgba(15, 23, 42, 0.2);
  border-radius: 4px;
}

.quality-score-item.overall {
  background: rgba(56, 189, 248, 0.1);
  border: 1px solid rgba(56, 189, 248, 0.2);
}

.quality-score-label {
  font-size: 11px;
  color: var(--app-text-muted);
}

.quality-score-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text-bright);
}

.quality-status-breakdown {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--app-text-muted);
}

.quality-score-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>
