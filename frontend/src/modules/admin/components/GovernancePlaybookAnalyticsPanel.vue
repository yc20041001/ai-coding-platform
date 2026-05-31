<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPlaybookAnalyticsDashboard, refreshPlaybookAnalytics, type GovernancePlaybookAnalyticsItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const dashboard = ref<any>(null); const loading = ref(false); const error = ref(false); const refreshing = ref(false)
function loadData() { loading.value = true; error.value = false; getPlaybookAnalyticsDashboard().then(r => { dashboard.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function handleRefresh() { refreshing.value = true; try { await refreshPlaybookAnalytics(); loadData(); ElMessage.success('已刷新') } catch { ElMessage.error('刷新失败') } finally { refreshing.value = false } }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Playbook 分析</span><ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="Playbook数" :value="dashboard.playbookCount" accent="primary" />
        <MetricTile label="总计划" :value="dashboard.totalPlanCount" accent="primary" />
        <MetricTile label="已完成" :value="dashboard.totalCompletedCount" accent="success" />
        <MetricTile label="阻塞" :value="dashboard.totalBlockedCount" accent="danger" />
      </div>
      <div v-for="r in dashboard.records" :key="r.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
        <span style="color:var(--app-text-bright)">{{ r.templateName }}</span>
        <ElTag size="small">完成率:{{ r.avgCompletionRate }}%</ElTag>
        <ElTag size="small">步骤:{{ r.avgStepCompletionRate }}%</ElTag>
        <span style="color:var(--app-text-muted)">计划:{{ r.planCount }} 阻塞:{{ r.blockedPlanCount }} 平均解决:{{ r.avgResolutionHours }}h</span>
      </div>
    </div>
    <EmptyState v-if="!loading && !dashboard && !error" description="暂无分析数据" />
  </TechPanel>
</template>
