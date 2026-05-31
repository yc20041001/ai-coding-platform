<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listDraftOptimizationSignals, type GovernanceDraftOptimizationSignalItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<GovernanceDraftOptimizationSignalItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listDraftOptimizationSignals().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function levelTag(l: string) { if (l === 'HIGH_CONFIDENCE') return 'success' as const; return 'warning' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">起草优化信号</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="s in items" :key="s.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px">
        <div style="display:flex;align-items:center;gap:6px"><ElTag size="small" :type="levelTag(s.signalLevel)">{{ s.signalLevel }}</ElTag><ElTag size="small">{{ s.signalType }}</ElTag><span style="color:var(--app-text-bright)">{{ s.scopeKey }}</span><span style="color:var(--app-text-muted)">采用:{{ s.adoptionRate }}% 拒绝:{{ s.rejectionRate }}% 评分:{{ s.avgUsefulnessRating }}</span></div>
        <div style="color:var(--app-text-muted);font-size:11px;margin-top:2px">{{ s.suggestionText }}</div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无优化信号" />
    </div>
  </TechPanel>
</template>
