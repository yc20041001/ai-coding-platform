<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getWorkspaceDashboard, type GovernanceWorkspaceSessionItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'

const insight = ref<any>(null); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; getWorkspaceDashboard().then(r => { insight.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">会话洞察</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="insight && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="总会话" :value="insight.totalSessions || 0" accent="primary" />
        <MetricTile label="总动作" :value="insight.totalActions || 0" accent="primary" />
        <MetricTile label="接受率" :value="insight.acceptanceRate || '0%'" accent="success" />
        <MetricTile label="完成率" :value="insight.guidedTaskCompletionRate || '0%'" accent="success" />
      </div>
      <div v-if="insight.topActionPatterns && insight.topActionPatterns.length > 0">
        <div style="font-size:11px;font-weight:600;color:var(--app-text-soft);margin-bottom:4px">常用模式</div>
        <div v-for="p in insight.topActionPatterns" :key="p" style="padding:4px 6px;margin-bottom:3px;font-size:11px;color:var(--app-text-muted)">{{ p }}</div>
      </div>
    </div>
    <EmptyState v-if="!loading && !insight && !error" description="暂无洞察数据" />
  </TechPanel>
</template>
