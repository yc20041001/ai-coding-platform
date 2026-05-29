<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  refreshDrift,
  getDriftDashboard,
  type PortfolioDriftDashboardItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElTooltip } from 'element-plus'

const dashboard = ref<PortfolioDriftDashboardItem | null>(null)
const loading = ref(false)
const error = ref(false)
const refreshing = ref(false)

function loadData() {
  loading.value = true
  error.value = false
  getDriftDashboard()
    .then(res => { dashboard.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await refreshDrift()
    loadData()
    ElMessage.success('Drift 快照已刷新')
  } catch {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

function driftColor(level: string): string {
  const map: Record<string, string> = { STABLE: 'var(--color-success)', WATCH: 'var(--color-caution)', HIGH: 'var(--color-warning)', CRITICAL: 'var(--color-error)' }
  return map[level] || 'var(--app-text-muted)'
}

function driftLabel(level: string): string {
  const map: Record<string, string> = { STABLE: '稳定', WATCH: '关注', HIGH: '高', CRITICAL: '严重' }
  return map[level] || level
}

onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Portfolio Drift 检测</span>
      <ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新 Drift</ElButton>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取 drift 数据" retry-text="重试" @retry="loadData" />

    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:10px;margin-bottom:16px;flex-wrap:wrap">
        <MetricTile label="稳定" :value="dashboard.stableCount" accent="success" />
        <MetricTile label="关注" :value="dashboard.watchCount" accent="warning" />
        <MetricTile label="高" :value="dashboard.highCount" accent="danger" />
        <MetricTile label="严重" :value="dashboard.criticalCount" accent="danger" />
      </div>

      <div style="font-size:11px;color:var(--app-text-muted);margin-bottom:8px">
        {{ dashboard.driftTrendSummary }}
      </div>

      <div v-if="dashboard.topDriftProjects.length > 0">
        <div style="font-size:11px;font-weight:600;color:var(--app-text-soft);margin-bottom:6px">Top Drift 项目</div>
        <div v-for="item in dashboard.topDriftProjects" :key="item.id" style="padding:6px 8px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:8px">
          <span style="color:var(--app-text-bright)">{{ item.projectName }}</span>
          <ElTag size="small" :color="driftColor(item.driftLevel)" style="color:#fff;border:none">{{ driftLabel(item.driftLevel) }}</ElTag>
          <span style="color:var(--app-text-muted);margin-left:auto">得分: {{ item.driftScore }}</span>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !dashboard && !error" description="暂无 drift 数据，请先刷新快照" />
  </TechPanel>
</template>
