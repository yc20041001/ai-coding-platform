<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCapacityDashboard, refreshCapacityForecast, type GovernanceCapacityDashboardItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const dashboard = ref<GovernanceCapacityDashboardItem | null>(null)
const loading = ref(false); const error = ref(false); const refreshing = ref(false)
function loadData() { loading.value = true; error.value = false; getCapacityDashboard().then(r => { dashboard.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function handleRefresh() { refreshing.value = true; try { await refreshCapacityForecast(); loadData(); ElMessage.success('已刷新') } catch { ElMessage.error('刷新失败') } finally { refreshing.value = false } }
function riskTag(l: string) { if (l === 'CRITICAL') return 'danger' as const; if (l === 'HIGH') return 'warning' as const; if (l === 'WATCH') return 'info' as const; return 'success' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">容量预测</span><ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="Owner数" :value="dashboard.ownerCount" accent="primary" />
        <MetricTile label="低风险" :value="dashboard.lowRiskCount" accent="success" />
        <MetricTile label="关注" :value="dashboard.watchCount" accent="warning" />
        <MetricTile label="高风险" :value="dashboard.highCount" accent="danger" />
        <MetricTile label="严重" :value="dashboard.criticalCount" accent="danger" />
        <MetricTile label="预测积压" :value="dashboard.averageProjectedBacklog" accent="primary" />
        <MetricTile label="预测逾期" :value="dashboard.averageProjectedOverdue" accent="danger" />
      </div>
      <div v-if="dashboard.topRiskOwners.length > 0">
        <div style="font-size:11px;font-weight:600;color:var(--app-text-soft);margin-bottom:6px">高风险 Owner (7d)</div>
        <div v-for="o in dashboard.topRiskOwners" :key="o.ownerId" style="padding:6px 8px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <ElTag size="small" :type="riskTag(o.capacityRiskLevel)">{{ o.capacityRiskLevel }}</ElTag>
          <span style="color:var(--app-text-bright)">{{ o.ownerName }}</span>
          <span style="color:var(--app-text-muted)">积压: {{ o.projectedBacklogCount }} | 逾期: {{ o.projectedOverdueCount }} | 完成/日: {{ o.avgCompletedPerDay }}</span>
        </div>
      </div>
    </div>
    <EmptyState v-if="!loading && !dashboard && !error" description="暂无预测数据" />
  </TechPanel>
</template>
