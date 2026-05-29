<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  refreshGuardrails,
  getGuardrailDashboard,
  getRecommendations,
  type ReleaseGuardrailDashboardItem,
  type GovernanceRecommendationItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElTooltip } from 'element-plus'

const dashboard = ref<ReleaseGuardrailDashboardItem | null>(null)
const recommendations = ref<GovernanceRecommendationItem[]>([])
const loading = ref(false)
const error = ref(false)
const refreshing = ref(false)

function loadData() {
  loading.value = true
  error.value = false
  Promise.all([
    getGuardrailDashboard(),
    getRecommendations(),
  ])
    .then(([dashRes, recRes]) => {
      dashboard.value = dashRes.data.data
      recommendations.value = recRes.data.data
    })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await refreshGuardrails()
    loadData()
    ElMessage.success('Guardrail 已刷新')
  } catch {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

function statusTag(status: string) {
  const map: Record<string, string> = { PASS: 'success', WARN: 'warning', BLOCK: 'danger', SKIP: 'info' }
  return map[status] || 'info'
}

function severityTag(sev: string) {
  const map: Record<string, string> = { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: '', LOW: 'info', INFO: 'info' }
  return map[sev] || 'info'
}

const priorityLabel = (p: string) => ({ P0: 'P0-紧急', P1: 'P1-高', P2: 'P2-中', P3: 'P3-低' }[p] || p)

onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Release Guardrail 看板</span>
      <ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新评估</ElButton>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取 guardrail 数据" retry-text="重试" @retry="loadData" />

    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:10px;margin-bottom:16px;flex-wrap:wrap">
        <MetricTile label="项目数" :value="dashboard.projectCount" accent="primary" />
        <MetricTile label="通过" :value="dashboard.passCount" accent="success" />
        <MetricTile label="警告" :value="dashboard.warnCount" accent="warning" />
        <MetricTile label="阻塞" :value="dashboard.blockCount" accent="danger" />
        <MetricTile label="严重" :value="dashboard.criticalCount" accent="danger" />
        <MetricTile label="建议数" :value="dashboard.recommendationCount" accent="primary" />
      </div>

      <div v-if="dashboard.topBlockedProjects.length > 0" style="margin-bottom:16px">
        <div style="font-size:11px;font-weight:600;color:var(--app-text-soft);margin-bottom:6px">阻塞项目 Top</div>
        <div v-for="item in dashboard.topBlockedProjects" :key="item.id" style="padding:6px 8px;margin-bottom:4px;background:rgba(239,68,68,0.08);border-radius:4px;font-size:12px">
          <span style="color:var(--app-text-bright)">{{ item.projectName }}</span>
          <ElTag size="small" :type="severityTag(item.severity) as any" style="margin-left:6px">{{ item.severity }}</ElTag>
          <span style="margin-left:8px;color:var(--app-text-muted)">{{ item.summary }}</span>
        </div>
      </div>

      <div v-if="recommendations.length > 0">
        <div style="font-size:11px;font-weight:600;color:var(--app-text-soft);margin-bottom:6px">治理建议</div>
        <div v-for="rec in recommendations.slice(0, 8)" :key="rec.guardrailKey + rec.projectId" style="padding:6px 8px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <ElTag size="small" :type="rec.priority === 'P0' || rec.priority === 'P1' ? 'danger' : 'warning'">{{ priorityLabel(rec.priority) }}</ElTag>
          <span style="color:var(--app-text-bright)">{{ rec.projectName }}</span>
          <span style="color:var(--app-text-muted)">{{ rec.title }}</span>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !dashboard && !error" description="暂无 guardrail 数据，请先刷新评估" />
  </TechPanel>
</template>
