<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  getPrReviewQualityDashboard,
  listPrReviewQualityRecords,
  createPrReviewQualityRecord,
  updatePrReviewQualityRecord,
  exportPrReviewQualityReport,
  type PrReviewQualityDashboard,
  type PrReviewQualityRecordItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { formatDateTime } from '@/shared/utils/format'
import { ElTag, ElButton, ElDialog, ElInput, ElMessage } from 'element-plus'

const props = defineProps<{
  projectId?: string | null
  loading?: boolean
}>()

const dashboard = ref<PrReviewQualityDashboard | null>(null)
const records = ref<PrReviewQualityRecordItem[]>([])
const loadingDashboard = ref(false)
const error = ref(false)
const exportDialogVisible = ref(false)
const exportContent = ref('')
const exporting = ref(false)
const recordDialogVisible = ref(false)
const editingRecordId = ref('')
const editForm = ref({
  usefulnessScore: '',
  falsePositiveScore: '',
  reviewComment: '',
})
const saving = ref(false)

function loadDashboard() {
  if (!props.projectId) return
  loadingDashboard.value = true
  error.value = false
  getPrReviewQualityDashboard(props.projectId)
    .then(res => { dashboard.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loadingDashboard.value = false })
}

function loadRecords() {
  if (!props.projectId) return
  listPrReviewQualityRecords(props.projectId, { page: 1, size: 20 })
    .then(res => { records.value = res.data.data })
    .catch(() => {})
}

async function handleExport() {
  if (!props.projectId) return
  exporting.value = true
  try {
    const res = await exportPrReviewQualityReport(props.projectId)
    exportContent.value = res.data.data.content
    exportDialogVisible.value = true
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

function openEditRecord(record: PrReviewQualityRecordItem) {
  editingRecordId.value = record.id
  editForm.value = {
    usefulnessScore: record.usefulnessScore != null ? String(record.usefulnessScore) : '',
    falsePositiveScore: record.falsePositiveScore != null ? String(record.falsePositiveScore) : '',
    reviewComment: record.reviewComment || '',
  }
  recordDialogVisible.value = true
}

async function handleSaveRecord() {
  saving.value = true
  try {
    await updatePrReviewQualityRecord(editingRecordId.value, {
      usefulnessScore: editForm.value.usefulnessScore ? parseInt(editForm.value.usefulnessScore) : undefined,
      falsePositiveScore: editForm.value.falsePositiveScore ? parseInt(editForm.value.falsePositiveScore) : undefined,
      reviewComment: editForm.value.reviewComment || undefined,
    })
    ElMessage.success('已更新')
    recordDialogVisible.value = false
    loadRecords()
    loadDashboard()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function reviewStatusTag(status: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    HIGH_VALUE: 'success',
    ACTIONABLE: 'warning',
    LOW_SIGNAL: 'info',
    FAILED: 'danger',
    COMPLETED: 'success',
  }
  return map[status] || 'info'
}

function humanFeedbackTag(status: string) {
  const map: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
    PENDING: 'info',
    REVIEWED: 'warning',
    CONFIRMED: 'success',
    DISMISSED: 'danger',
  }
  return map[status] || 'info'
}

function adoptionTag(status: string) {
  const map: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
    UNKNOWN: 'info',
    PARTIAL: 'warning',
    ADOPTED: 'success',
    NOT_ADOPTED: 'danger',
  }
  return map[status] || 'info'
}

watch(() => props.projectId, (val) => {
  if (val) {
    loadDashboard()
    loadRecords()
  }
}, { immediate: true })
</script>

<template>
  <TechPanel title="PR 评审质量" glow v-loading="loadingDashboard" data-testid="pr-quality-panel">
    <ErrorState v-if="error" title="加载失败" message="无法获取评审质量数据" retry-text="重试" @retry="loadDashboard" />
    <template v-else>
      <div style="display:flex;gap:6px;margin-bottom:12px;flex-wrap:wrap">
        <ElButton size="small" :loading="exporting" @click="handleExport">导出报告</ElButton>
      </div>

      <div class="card-grid" v-if="dashboard">
        <MetricTile :value="dashboard.totalReviews ?? 0" label="总评审数" />
        <MetricTile :value="dashboard.highValueReviews ?? 0" label="高价值" accent="success" />
        <MetricTile :value="dashboard.actionableReviews ?? 0" label="可操作" accent="warning" />
        <MetricTile :value="dashboard.lowSignalReviews ?? 0" label="低信号" />
        <MetricTile :value="dashboard.failedReviews ?? 0" label="失败" accent="danger" />
        <MetricTile :value="dashboard.pendingFeedbackReviews ?? 0" label="待反馈" accent="warning" />
        <MetricTile :value="dashboard.adoptedReviews ?? 0" label="已采纳" accent="success" />
        <MetricTile :value="dashboard.averageUsefulnessScore != null ? dashboard.averageUsefulnessScore.toFixed(2) + '/5' : '-'" label="平均有用性" accent="accent" />
      </div>

      <div style="margin-top:16px">
        <div style="font-size:12px;font-weight:700;color:var(--app-text-soft);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.05em">评审记录</div>
        <div v-if="!records || records.length === 0" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">暂无记录</div>
        <div v-for="r in records" :key="r.id" class="quality-record-item">
          <div class="quality-record-header">
            <ElTag :type="reviewStatusTag(r.reviewStatus)" size="small" effect="dark">{{ r.reviewStatus }}</ElTag>
            <ElTag :type="humanFeedbackTag(r.humanFeedbackStatus)" size="small">{{ r.humanFeedbackStatus }}</ElTag>
            <ElTag :type="adoptionTag(r.adoptionStatus)" size="small">{{ r.adoptionStatus }}</ElTag>
            <span class="quality-record-repo">{{ r.repositoryFullName }} #{{ r.pullRequestNumber }}</span>
            <span class="quality-record-date">{{ formatDateTime(r.createTime) }}</span>
          </div>
          <div class="quality-record-meta">
            <span>发现: {{ r.findingsTotal }} (高:{{ r.highRiskFindings }} 中:{{ r.mediumRiskFindings }} 低:{{ r.lowRiskFindings }})</span>
            <span v-if="r.usefulnessScore != null">有用性: {{ r.usefulnessScore }}/5</span>
            <ElButton size="small" type="primary" link @click="openEditRecord(r)">编辑</ElButton>
          </div>
        </div>
      </div>
    </template>

    <ElDialog v-model="recordDialogVisible" title="编辑评审记录" width="500px" data-testid="quality-edit-dialog">
      <div style="display:flex;flex-direction:column;gap:12px">
        <div>
          <label style="font-size:12px;color:var(--app-text-soft)">有用性评分 (0-5)</label>
          <ElInput v-model="editForm.usefulnessScore" placeholder="留空不修改" size="small" />
        </div>
        <div>
          <label style="font-size:12px;color:var(--app-text-soft)">假阳性评分 (0-5)</label>
          <ElInput v-model="editForm.falsePositiveScore" placeholder="留空不修改" size="small" />
        </div>
        <div>
          <label style="font-size:12px;color:var(--app-text-soft)">评审意见</label>
          <ElInput v-model="editForm.reviewComment" type="textarea" :rows="3" size="small" />
        </div>
      </div>
      <template #footer>
        <ElButton @click="recordDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="handleSaveRecord">保存</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="exportDialogVisible" title="PR 评审质量报告" width="80%" data-testid="quality-export-dialog">
      <pre style="background:var(--app-surface);padding:16px;border-radius:6px;max-height:500px;overflow:auto;font-size:12px;white-space:pre-wrap">{{ exportContent }}</pre>
    </ElDialog>
  </TechPanel>
</template>

<style scoped>
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
}
.quality-record-item {
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 6px;
}
.quality-record-header {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.quality-record-repo {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-bright);
}
.quality-record-date {
  font-size: 11px;
  color: var(--app-text-muted);
  margin-left: auto;
}
.quality-record-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
  font-size: 11px;
  color: var(--app-text-muted);
}
</style>
