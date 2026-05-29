<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getWorkflowDashboard,
  getWorkflowSummary,
  refreshWorkflowSnapshot,
  type GovernanceWorkflowDashboard,
  type GovernanceWorkflowSummary,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElTooltip } from 'element-plus'

const dashboard = ref<GovernanceWorkflowDashboard | null>(null)
const summary = ref<GovernanceWorkflowSummary | null>(null)
const loading = ref(false)
const error = ref(false)
const refreshing = ref(false)

async function copyReport(md: string) {
  try {
    await navigator.clipboard.writeText(md)
    ElMessage.success('已复制')
  } catch { /* ignore */ }
}

function loadData() {
  loading.value = true
  error.value = false
  Promise.all([
    getWorkflowDashboard(),
    getWorkflowSummary(),
  ])
    .then(([dashRes, sumRes]) => {
      dashboard.value = dashRes.data.data
      summary.value = sumRes.data.data
    })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await refreshWorkflowSnapshot()
    loadData()
    ElMessage.success('工作流快照已刷新')
  } catch {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

function priorityLabel(p: string): string {
  return { P0: 'P0-紧急', P1: 'P1-高', P2: 'P2-中', P3: 'P3-低' }[p] || p
}

function priorityTag(p: string): 'danger' | 'warning' | 'info' {
  if (p === 'P0') return 'danger'
  if (p === 'P1') return 'warning'
  return 'info'
}

onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">工作流概览</span>
      <div style="display:flex;gap:6px">
        <ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新快照</ElButton>
      </div>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取工作流数据" retry-text="重试" @retry="loadData" />

    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:10px;margin-bottom:16px;flex-wrap:wrap">
        <MetricTile label="总事项" :value="dashboard.totalRecommendationCount" accent="primary" />
        <MetricTile label="开放" :value="dashboard.openRecommendationCount" accent="primary" />
        <MetricTile label="处理中" :value="dashboard.inProgressCount" accent="warning" />
        <MetricTile label="已完成" :value="dashboard.completedCount" accent="success" />
        <MetricTile label="阻塞" :value="dashboard.blockedCount" accent="danger" />
        <MetricTile label="逾期" :value="dashboard.overdueCount" accent="danger" />
        <MetricTile label="活跃 Waiver" :value="dashboard.activeWaiverCount" accent="warning" />
        <MetricTile label="完成率" :value="dashboard.completionRate + '%'" accent="success" />
        <MetricTile label="逾期率" :value="dashboard.overdueRate + '%'" accent="danger" />
      </div>

      <div v-if="dashboard.topPriorityItems.length > 0" style="margin-bottom:12px">
        <div style="font-size:11px;font-weight:600;color:var(--app-text-soft);margin-bottom:6px">高优先级事项</div>
        <div v-for="item in dashboard.topPriorityItems" :key="item.id" style="padding:6px 8px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <ElTag size="small" :type="priorityTag(item.priority)">{{ priorityLabel(item.priority) }}</ElTag>
          <span style="color:var(--app-text-bright)">{{ item.projectName }}</span>
          <span style="color:var(--app-text-muted)">{{ item.title }}</span>
        </div>
      </div>

      <div v-if="dashboard.topOverdueItems.length > 0">
        <div style="font-size:11px;font-weight:600;color:var(--color-error);margin-bottom:6px">逾期事项</div>
        <div v-for="item in dashboard.topOverdueItems" :key="'ov-' + item.id" style="padding:6px 8px;margin-bottom:4px;background:rgba(239,68,68,0.08);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <span style="color:var(--color-error)">⚠</span>
          <span style="color:var(--app-text-bright)">{{ item.projectName }}</span>
          <span style="color:var(--app-text-muted)">{{ item.title }}</span>
          <span style="color:var(--app-text-muted);margin-left:auto">截止: {{ item.dueAt ? new Date(item.dueAt).toLocaleDateString() : '无' }}</span>
        </div>
      </div>

      <div v-if="summary && summary.summaryMarkdown" style="margin-top:12px">
        <ElButton size="small" @click="copyReport(summary.summaryMarkdown)">复制报告</ElButton>
      </div>
    </div>

    <EmptyState v-if="!loading && !dashboard && !error" description="暂无工作流数据，请先刷新快照" />
  </TechPanel>
</template>
