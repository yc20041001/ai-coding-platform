<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getRecipeEffectivenessDashboard, refreshRecipeEffectiveness, type GovernanceRecipeEffectivenessSnapshotItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const dashboard = ref<any>(null); const loading = ref(false); const error = ref(false); const refreshing = ref(false)
function loadData() { loading.value = true; error.value = false; getRecipeEffectivenessDashboard().then(r => { dashboard.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function handleRefresh() { refreshing.value = true; try { await refreshRecipeEffectiveness(); loadData(); ElMessage.success('已刷新') } catch { ElMessage.error('刷新失败') } finally { refreshing.value = false } }
function levelTag(l: string) { if (l === 'TOP') return 'success' as const; if (l === 'HIGH') return 'primary' as const; if (l === 'LOW') return 'danger' as const; return 'warning' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Recipe 效果</span><ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="Recipe数" :value="dashboard.recipeCount" accent="primary" />
        <MetricTile label="TOP" :value="dashboard.topRecipeCount" accent="success" />
        <MetricTile label="高" :value="dashboard.highRecipeCount" accent="primary" />
        <MetricTile label="低" :value="dashboard.lowRecipeCount" accent="danger" />
        <MetricTile label="平均分" :value="dashboard.averageEffectivenessScore" accent="primary" />
      </div>
      <div v-if="dashboard.topRecipes && dashboard.topRecipes.length > 0" style="margin-bottom:12px">
        <div style="font-size:11px;font-weight:600;color:var(--color-success);margin-bottom:4px">TOP Recipe</div>
        <div v-for="r in dashboard.topRecipes" :key="r.id" style="padding:6px;margin-bottom:4px;background:rgba(34,197,94,0.08);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <ElTag size="small" :type="levelTag(r.effectivenessLevel)">{{ r.effectivenessLevel }}</ElTag><span style="color:var(--app-text-bright)">{{ r.recipeName }}</span><span style="color:var(--app-text-muted)">分:{{ r.effectivenessScore }} 使用:{{ r.usageCount }} 完成率:{{ r.successRate }}%</span>
        </div>
      </div>
      <div v-if="dashboard.lowValueRecipes && dashboard.lowValueRecipes.length > 0">
        <div style="font-size:11px;font-weight:600;color:var(--color-error);margin-bottom:4px">低价值 Recipe</div>
        <div v-for="r in dashboard.lowValueRecipes" :key="'l-'+r.id" style="padding:6px;margin-bottom:4px;background:rgba(239,68,68,0.08);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
          <ElTag size="small" type="danger">LOW</ElTag><span style="color:var(--app-text-bright)">{{ r.recipeName }}</span><span style="color:var(--app-text-muted)">分:{{ r.effectivenessScore }} 失败率:{{ r.failureRate }}%</span>
        </div>
      </div>
    </div>
    <EmptyState v-if="!loading && !dashboard && !error" description="暂无效果数据" />
  </TechPanel>
</template>
