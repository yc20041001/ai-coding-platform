<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getRiskDashboard, refreshRiskSignals, type PredictiveRiskDashboardItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const dashboard = ref<PredictiveRiskDashboardItem | null>(null)
const loading = ref(false); const error = ref(false); const refreshing = ref(false)
function loadData() { loading.value = true; error.value = false; getRiskDashboard().then(r => { dashboard.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function handleRefresh() { refreshing.value = true; try { await refreshRiskSignals(); loadData(); ElMessage.success('已刷新') } catch { ElMessage.error('刷新失败') } finally { refreshing.value = false } }
function riskTag(l: string) { if (l === 'CRITICAL') return 'danger' as const; if (l === 'HIGH') return 'warning' as const; return 'info' as const }
function typeLabel(t: string) { const m: Record<string, string> = { OWNER_OVERLOAD_FORECAST: 'Owner超载', OVERDUE_TREND_FORECAST: '逾期趋势', WAIVER_EXPIRY_CLUSTER: 'Waiver到期', PROJECT_BACKLOG_GROWTH: '积压增长', THROUGHPUT_DEFICIT: '吞吐不足' }; return m[t] || t }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">风险预测信号</span><ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="信号数" :value="dashboard.signalCount" accent="primary" />
        <MetricTile label="严重" :value="dashboard.criticalSignalCount" accent="danger" />
        <MetricTile label="高" :value="dashboard.highSignalCount" accent="warning" />
        <MetricTile label="Owner风险" :value="dashboard.ownerRiskSignals" accent="danger" />
        <MetricTile label="项目风险" :value="dashboard.projectRiskSignals" accent="warning" />
        <MetricTile label="组合风险" :value="dashboard.portfolioRiskSignals" accent="primary" />
      </div>
      <div v-if="dashboard.topSignals.length > 0">
        <div v-for="s in dashboard.topSignals" :key="s.id" style="padding:6px 8px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <ElTag size="small" :type="riskTag(s.riskLevel)">{{ s.riskLevel }}</ElTag>
          <span style="color:var(--app-text-bright)">{{ typeLabel(s.signalType) }}</span>
          <span style="color:var(--app-text-muted)">{{ s.targetName }}: {{ s.summary }}</span>
          <span style="color:var(--app-text-muted);margin-left:auto">风险: {{ s.riskScore }} | 概率: {{ s.probabilityScore }}%</span>
        </div>
      </div>
    </div>
    <EmptyState v-if="!loading && !dashboard && !error" description="暂无风险信号" />
  </TechPanel>
</template>
