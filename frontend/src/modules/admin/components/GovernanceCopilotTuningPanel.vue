<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCopilotTuningDashboard, type GovernanceCopilotTuningSnapshotItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const dashboard = ref<any>(null); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; getCopilotTuningDashboard().then(r => { dashboard.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Copilot 调优</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div v-if="dashboard.latestSnapshot" style="margin-bottom:12px">
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <MetricTile label="反馈数" :value="dashboard.latestSnapshot.totalFeedbackCount" accent="primary" />
          <MetricTile label="接受率" :value="dashboard.latestSnapshot.acceptanceRate + '%'" accent="success" />
          <MetricTile label="忽略率" :value="dashboard.latestSnapshot.dismissalRate + '%'" accent="danger" />
          <MetricTile label="评分" :value="dashboard.latestSnapshot.avgFeedbackRating + '/5'" accent="primary" />
          <MetricTile label="置信度" :value="dashboard.latestSnapshot.tuningConfidenceScore" accent="primary" />
        </div>
        <div style="margin-top:8px;font-size:12px;color:var(--app-text-muted)">
          最佳类型: {{ dashboard.latestSnapshot.topSuggestionType || '-' }} | 最弱类型: {{ dashboard.latestSnapshot.weakestSuggestionType || '-' }}
        </div>
      </div>
      <div style="font-size:11px;color:var(--app-text-muted)">快照数: {{ dashboard.snapshotCount }}</div>
    </div>
    <EmptyState v-if="!loading && !dashboard && !error" description="暂无调优数据" />
  </TechPanel>
</template>
