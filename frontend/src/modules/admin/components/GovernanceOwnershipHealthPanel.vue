<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOwnershipDashboard, refreshOwnership, type GovernanceOwnershipDashboardItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const dashboard = ref<GovernanceOwnershipDashboardItem | null>(null)
const loading = ref(false); const error = ref(false); const refreshing = ref(false)

function loadData() {
  loading.value = true; error.value = false
  getOwnershipDashboard().then(res => { dashboard.value = res.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false })
}
async function handleRefresh() {
  refreshing.value = true
  try { await refreshOwnership(); loadData(); ElMessage.success('已刷新') } catch { ElMessage.error('刷新失败') } finally { refreshing.value = false }
}
function healthLabel(l: string) { return { HEALTHY: '健康', WATCH: '关注', RISK: '风险', CRITICAL: '严重' }[l] || l }
function healthTag(l: string): 'danger' | 'warning' | 'success' | 'info' { if (l === 'CRITICAL') return 'danger'; if (l === 'RISK') return 'warning'; if (l === 'WATCH') return 'info'; return 'success' }
onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Owner 健康度</span>
      <ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新</ElButton>
    </div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="Owner数" :value="dashboard.ownerCount" accent="primary" />
        <MetricTile label="健康" :value="dashboard.healthyCount" accent="success" />
        <MetricTile label="关注" :value="dashboard.watchCount" accent="warning" />
        <MetricTile label="风险" :value="dashboard.riskCount" accent="danger" />
        <MetricTile label="严重" :value="dashboard.criticalCount" accent="danger" />
        <MetricTile label="7d完成" :value="dashboard.overallThroughput7d" accent="primary" />
      </div>

      <div v-if="dashboard.topOverloadedOwners.length > 0" style="margin-bottom:12px">
        <div style="font-size:11px;font-weight:600;color:var(--color-error);margin-bottom:4px">超载 Owner Top</div>
        <div v-for="o in dashboard.topOverloadedOwners" :key="o.ownerId" style="padding:6px 8px;margin-bottom:4px;background:rgba(239,68,68,0.08);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <span style="color:var(--app-text-bright)">{{ o.ownerName }}</span>
          <ElTag size="small" :type="healthTag(o.ownerHealthLevel)">{{ healthLabel(o.ownerHealthLevel) }}</ElTag>
          <span style="color:var(--app-text-muted)">得分: {{ o.ownerHealthScore }} | 逾期: {{ o.overdueCount }} | 开放: {{ o.openCount }}</span>
        </div>
      </div>

      <div v-if="dashboard.topHealthyOwners.length > 0">
        <div style="font-size:11px;font-weight:600;color:var(--color-success);margin-bottom:4px">健康 Owner Top</div>
        <div v-for="o in dashboard.topHealthyOwners.slice(0, 3)" :key="'h-' + o.ownerId" style="padding:6px 8px;margin-bottom:4px;background:rgba(34,197,94,0.08);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <span style="color:var(--app-text-bright)">{{ o.ownerName }}</span>
          <ElTag size="small" type="success">{{ healthLabel(o.ownerHealthLevel) }}</ElTag>
          <span style="color:var(--app-text-muted)">得分: {{ o.ownerHealthScore }} | 7d完成: {{ o.completed7dCount }}</span>
        </div>
      </div>
    </div>
    <EmptyState v-if="!loading && !dashboard && !error" description="暂无 Owner 数据，请先刷新" />
  </TechPanel>
</template>
