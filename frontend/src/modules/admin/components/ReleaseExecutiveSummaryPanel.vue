<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import {
  getExecutiveSummary,
  getConfidenceSnapshot,
  getComparison,
  getConfidenceTrend,
  takeConfidenceSnapshot,
  type ReleaseExecutiveSummaryItem,
  type ReleaseConfidenceSnapshotItem,
  type ReleaseComparisonItem,
  type ReleaseConfidenceTrendItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import { ElTag, ElButton, ElMessage } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId?: string | null
  planId?: string | null
}>()

const summary = ref<ReleaseExecutiveSummaryItem | null>(null)
const snapshot = ref<ReleaseConfidenceSnapshotItem | null>(null)
const comparison = ref<ReleaseComparisonItem | null>(null)
const trends = ref<ReleaseConfidenceTrendItem[]>([])
const loading = ref(false)
const error = ref(false)
const snapshotting = ref(false)

function loadData() {
  if (!props.planId) return
  loading.value = true
  error.value = false
  Promise.all([
    getExecutiveSummary(props.planId),
    getConfidenceSnapshot(props.planId),
    getComparison(props.planId),
  ])
    .then(([summaryRes, snapshotRes, comparisonRes]) => {
      summary.value = summaryRes.data.data
      snapshot.value = snapshotRes.data.data
      comparison.value = comparisonRes.data.data
    })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

function loadTrends() {
  getConfidenceTrend()
    .then(res => { trends.value = res.data.data })
    .catch(() => { /* silent */ })
}

async function handleTakeSnapshot() {
  if (!props.planId) return
  snapshotting.value = true
  try {
    const res = await takeConfidenceSnapshot(props.planId)
    snapshot.value = res.data.data
    ElMessage.success('信心快照已保存')
    loadTrends()
  } catch {
    ElMessage.error('快照保存失败')
  } finally {
    snapshotting.value = false
  }
}

function confidenceLevelTag(level: string) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    HIGH: 'success',
    MEDIUM: 'warning',
    LOW: 'warning',
    CRITICAL: 'danger',
  }
  return map[level] || 'info'
}

function confidenceLevelLabel(level: string): string {
  const map: Record<string, string> = {
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低',
    CRITICAL: '严重',
  }
  return map[level] || level
}

function deltaLabel(delta: number | null | undefined): string {
  if (delta == null) return '-'
  return delta > 0 ? `+${delta}` : `${delta}`
}

function statusPulseTone(rollbackReady: boolean | null | undefined): 'success' | 'warning' | 'danger' {
  if (rollbackReady) return 'success'
  return 'danger'
}

watch(() => props.planId, () => {
  if (props.planId) {
    loadData()
    loadTrends()
  }
}, { immediate: true })

onMounted(() => {
  loadTrends()
})
</script>

<template>
  <div>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">执行摘要与信心评估</span>
      <div v-if="props.planId" style="display:flex;gap:4px">
        <ElButton size="small" type="primary" :loading="snapshotting" @click="handleTakeSnapshot">保存快照</ElButton>
      </div>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取执行摘要" retry-text="重试" @retry="loadData" />

    <div v-if="!props.planId" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">请先选择一个 Rollout Plan</div>

    <div v-if="summary && props.planId" v-loading="loading">
      <!-- Confidence Score -->
      <div style="display:flex;gap:12px;margin-bottom:12px;flex-wrap:wrap;align-items:center">
        <div style="display:flex;align-items:center;gap:6px">
          <span style="font-size:24px;font-weight:700;color:var(--app-text-bright)">{{ summary.confidenceScore }}</span>
          <span style="font-size:11px;color:var(--app-text-muted)">/ 100</span>
        </div>
        <ElTag :type="confidenceLevelTag(summary.confidenceLevel)" size="small" effect="dark">{{ confidenceLevelLabel(summary.confidenceLevel) }}</ElTag>
        <StatusPulse :status="summary.rollbackReady ? '就绪' : '未就绪'" :tone="statusPulseTone(summary.rollbackReady)" />
        <span style="font-size:11px;color:var(--app-text-muted)">签字完成率: {{ summary.signoffCompletionRate }}%</span>
      </div>

      <!-- Metrics -->
      <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(100px,1fr));gap:8px;margin-bottom:12px">
        <MetricTile :value="summary.blockingIssueCount" label="阻塞问题" accent="danger" />
        <MetricTile :value="summary.warningIssueCount" label="警告" accent="warning" />
        <MetricTile :value="summary.openIncidentCount" label="事件" accent="danger" />
        <MetricTile :value="summary.activeAlertCount" label="告警" accent="warning" />
        <MetricTile :value="summary.failedVerificationCount" label="验证失败" accent="danger" />
      </div>

      <NeonDivider tone="muted" style="margin:12px 0" />

      <!-- Summary Text -->
      <div v-if="summary.summaryText" style="font-size:12px;color:var(--app-text-bright);line-height:1.6;margin-bottom:12px">
        {{ summary.summaryText }}
      </div>

      <!-- Rollout status -->
      <div style="font-size:11px;color:var(--app-text-muted);margin-bottom:8px">
        发布状态: <strong>{{ summary.rolloutStatus }}</strong>
        <span v-if="summary.overallOutcome"> | 复盘结果: <strong>{{ summary.overallOutcome }}</strong></span>
        <span v-if="summary.lastUpdatedAt"> | 更新于 {{ formatDateTime(summary.lastUpdatedAt) }}</span>
      </div>

      <!-- Comparison -->
      <NeonDivider tone="muted" style="margin:12px 0" />
      <div v-if="comparison">
        <div style="font-size:11px;color:var(--app-text-muted);margin-bottom:6px">对比分析</div>
        <div v-if="comparison.baselineReleaseLabel" style="font-size:12px;color:var(--app-text-bright);margin-bottom:4px">
          当前: {{ comparison.currentReleaseLabel }} vs 基准: {{ comparison.baselineReleaseLabel }}
        </div>
        <div style="display:flex;gap:12px;flex-wrap:wrap;font-size:11px;color:var(--app-text-soft)">
          <span>信心分: {{ deltaLabel(comparison.confidenceScoreDelta) }}</span>
          <span>阻塞问题: {{ deltaLabel(comparison.blockingIssueDelta) }}</span>
          <span>验证失败: {{ deltaLabel(comparison.failedVerificationDelta) }}</span>
          <span v-if="comparison.trendSummary" style="color:var(--app-text-muted)">{{ comparison.trendSummary }}</span>
        </div>
      </div>

      <!-- Confidence Trend -->
      <NeonDivider tone="muted" style="margin:12px 0" />
      <div v-if="trends.length > 0">
        <div style="font-size:11px;color:var(--app-text-muted);margin-bottom:6px">信心趋势 (最近 {{ trends.length }} 条)</div>
        <div v-for="(t, idx) in trends" :key="t.planId ?? 'trend-' + idx" style="display:flex;gap:8px;padding:3px 0;font-size:11px;border-bottom:1px solid rgba(56,189,248,0.03)">
          <span style="min-width:100px;color:var(--app-text-muted)">{{ t.releaseLabel }}</span>
          <span style="font-weight:600;color:var(--app-text-bright)">{{ t.confidenceScore }}</span>
          <ElTag :type="confidenceLevelTag(t.confidenceLevel)" size="small" effect="dark">{{ confidenceLevelLabel(t.confidenceLevel) }}</ElTag>
          <span v-if="t.snapshotTime" style="color:var(--app-text-muted)">{{ formatDateTime(t.snapshotTime) }}</span>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !summary && props.planId && !error" description="暂无执行摘要数据" />
  </div>
</template>
