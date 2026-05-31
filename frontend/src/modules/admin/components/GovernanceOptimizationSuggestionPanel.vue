<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOptimizationDashboard, refreshOptimizations, type GovernanceOptimizationSuggestionItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const dashboard = ref<any>(null); const loading = ref(false); const error = ref(false); const refreshing = ref(false)
function loadData() { loading.value = true; error.value = false; getOptimizationDashboard().then(r => { dashboard.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function handleRefresh() { refreshing.value = true; try { await refreshOptimizations(); loadData(); ElMessage.success('已刷新') } catch { ElMessage.error('刷新失败') } finally { refreshing.value = false } }
function typeLabel(t: string) { const m: Record<string,string> = { PROMOTE_RECIPE:'提升', PRUNE_RECIPE:'裁剪', REFINE_PLAYBOOK:'优化', SPLIT_PATTERN:'拆分', MERGE_DUPLICATE_RECIPES:'合并' }; return m[t]||t }
function priorityTag(p: string) { if (p === 'P0') return 'danger' as const; if (p === 'P1') return 'warning' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">优化建议</span><ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="建议数" :value="dashboard.suggestionCount" accent="primary" />
        <MetricTile label="高优先级" :value="dashboard.highPrioritySuggestionCount" accent="danger" />
        <MetricTile label="提升" :value="dashboard.promoteSuggestionCount" accent="success" />
        <MetricTile label="裁剪" :value="dashboard.pruneSuggestionCount" accent="danger" />
        <MetricTile label="优化" :value="dashboard.refinePlaybookCount" accent="warning" />
      </div>
      <div v-for="s in dashboard.topSuggestions" :key="s.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px">
        <div style="display:flex;align-items:center;gap:6px">
          <ElTag size="small" :type="priorityTag(s.priority)">{{ s.priority }}</ElTag>
          <ElTag size="small">{{ typeLabel(s.suggestionType) }}</ElTag>
          <span style="color:var(--app-text-bright)">{{ s.expectedImpactText }}</span>
        </div>
        <div style="color:var(--app-text-muted);margin-top:2px">{{ s.currentMetricValue }} → {{ s.suggestedAction }}</div>
      </div>
    </div>
    <EmptyState v-if="!loading && !dashboard && !error" description="暂无优化建议" />
  </TechPanel>
</template>
