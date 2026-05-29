<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSimulationDashboard, getSimulationReport, type GovernanceSimulationDashboardItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const dashboard = ref<GovernanceSimulationDashboardItem | null>(null)
const loading = ref(false); const error = ref(false)

function loadData() { loading.value = true; error.value = false; getSimulationDashboard().then(r => { dashboard.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function statusTag(s: string) { if (s === 'SUCCESS') return 'success' as const; if (s === 'WARNING') return 'warning' as const; return 'danger' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">模拟对比</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="场景数" :value="dashboard.scenarioCount" accent="primary" />
        <MetricTile label="成功" :value="dashboard.successfulScenarioCount" accent="success" />
        <MetricTile label="警告" :value="dashboard.warningScenarioCount" accent="warning" />
        <MetricTile label="无改善" :value="dashboard.noImprovementCount" accent="danger" />
      </div>
      <div v-if="dashboard.topScenarios.length > 0">
        <div style="font-size:11px;font-weight:600;color:var(--app-text-soft);margin-bottom:6px">最近场景</div>
        <div v-for="s in dashboard.topScenarios" :key="s.id" style="padding:6px 8px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <span style="color:var(--app-text-bright)">{{ s.scenarioName }}</span>
          <ElTag size="small">{{ s.scenarioType }}</ElTag>
          <ElTag size="small" :type="s.scenarioStatus === 'SIMULATED' ? 'success' : 'info'">{{ s.scenarioStatus }}</ElTag>
        </div>
      </div>
      <div v-if="dashboard.topSuggestions.length > 0" style="margin-top:12px">
        <div style="font-size:11px;font-weight:600;color:var(--app-text-soft);margin-bottom:6px">Top 建议</div>
        <div v-for="sg in dashboard.topSuggestions" :key="sg.id" style="padding:6px 8px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px">
          <span style="color:var(--app-text-bright)">{{ sg.expectedImpactText }}</span>
        </div>
      </div>
    </div>
    <EmptyState v-if="!loading && !dashboard && !error" description="暂无模拟数据" />
  </TechPanel>
</template>
