<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listBenchmarkEvolution, type GovernanceEvolutionItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<GovernanceEvolutionItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listBenchmarkEvolution().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function signalTag(l: string) { if (l === 'IMPROVING') return 'success' as const; if (l === 'DECLINING') return 'danger' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">基准演化</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="e in items" :key="e.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small" :type="signalTag(e.signalLevel)">{{ e.signalLevel }}</ElTag>
        <span style="color:var(--app-text-bright)">{{ e.metricKey }}</span>
        <span style="color:var(--app-text-muted)">{{ e.previousValue }} → {{ e.currentValue }} ({{ e.delta > 0 ? '+' : '' }}{{ e.delta }})</span>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无演化数据" />
    </div>
  </TechPanel>
</template>
