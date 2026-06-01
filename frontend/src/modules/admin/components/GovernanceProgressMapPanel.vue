<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listProgressMap, type GovernanceProgressMapItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<GovernanceProgressMapItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listProgressMap().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function signalTag(l: string) { if (l === 'ON_TRACK') return 'success' as const; if (l === 'AT_RISK') return 'warning' as const; if (l === 'BEHIND') return 'danger' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">进展地图</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="p in items" :key="p.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small" :type="signalTag(p.signalLevel)">{{ p.signalLevel }}</ElTag>
        <span style="color:var(--app-text-bright)">{{ p.projectName }}</span>
        <span style="color:var(--app-text-muted)">{{ p.baselineScore }} → {{ p.currentScore }}/{{ p.targetScore }} ({{ p.progressPercentage }}%)</span>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无进展数据" />
    </div>
  </TechPanel>
</template>
