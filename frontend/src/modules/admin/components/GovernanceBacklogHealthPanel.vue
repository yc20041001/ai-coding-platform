<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getBacklogDashboard, refreshBacklog, type GovernanceBacklogDashboardItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const dashboard = ref<GovernanceBacklogDashboardItem | null>(null)
const loading = ref(false); const error = ref(false); const refreshing = ref(false)
function loadData() { loading.value = true; error.value = false; getBacklogDashboard().then(r => { dashboard.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function handleRefresh() { refreshing.value = true; try { await refreshBacklog(); loadData(); ElMessage.success('已刷新') } catch { ElMessage.error('刷新失败') } finally { refreshing.value = false } }
function healthTag(l: string) { if (l === 'CRITICAL') return 'danger' as const; if (l === 'RISK') return 'warning' as const; if (l === 'WATCH') return 'info' as const; return 'success' as const }
function healthLabel(l: string) { return { HEALTHY: '健康', WATCH: '关注', RISK: '风险', CRITICAL: '严重' }[l] || l }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">积压健康度</span><ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="项目数" :value="dashboard.projectCount" accent="primary" />
        <MetricTile label="健康" :value="dashboard.healthyCount" accent="success" />
        <MetricTile label="关注" :value="dashboard.watchCount" accent="warning" />
        <MetricTile label="风险" :value="dashboard.riskCount" accent="danger" />
        <MetricTile label="严重" :value="dashboard.criticalCount" accent="danger" />
      </div>
      <div v-if="dashboard.topGrowingBacklogs.length > 0" style="margin-bottom:12px">
        <div style="font-size:11px;font-weight:600;color:var(--color-error);margin-bottom:4px">增长最快积压</div>
        <div v-for="p in dashboard.topGrowingBacklogs" :key="p.projectId" style="padding:6px 8px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <ElTag size="small" :type="healthTag(p.backlogHealthLevel)">{{ healthLabel(p.backlogHealthLevel) }}</ElTag>
          <span style="color:var(--app-text-bright)">{{ p.projectName }}</span>
          <span style="color:var(--app-text-muted)">增长率: {{ p.backlogGrowthRate }} | 开放: {{ p.openCount }} | 阻塞: {{ p.blockedCount }}</span>
        </div>
      </div>
      <div v-if="dashboard.topOverdueProjects.length > 0">
        <div style="font-size:11px;font-weight:600;color:var(--color-error);margin-bottom:4px">逾期最多项目</div>
        <div v-for="p in dashboard.topOverdueProjects" :key="'ov-' + p.projectId" style="padding:6px 8px;margin-bottom:4px;background:rgba(239,68,68,0.08);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <span style="color:var(--color-error)">⚠</span>
          <span style="color:var(--app-text-bright)">{{ p.projectName }}</span>
          <span style="color:var(--app-text-muted)">逾期: {{ p.overdueCount }} | 开放: {{ p.openCount }} | 7d完成: {{ p.completed7dCount }}</span>
        </div>
      </div>
    </div>
    <EmptyState v-if="!loading && !dashboard && !error" description="暂无积压数据" />
  </TechPanel>
</template>
