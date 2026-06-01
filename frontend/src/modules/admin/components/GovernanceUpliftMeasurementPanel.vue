<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listUpliftMeasurements, type GovernanceUpliftMeasurementItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<GovernanceUpliftMeasurementItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listUpliftMeasurements().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function levelTag(l: string) { if (l === 'SIGNIFICANT') return 'success' as const; if (l === 'MODERATE') return 'primary' as const; if (l === 'MINIMAL') return 'warning' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">提升测量</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="u in items" :key="u.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small" :type="levelTag(u.upliftLevel)">{{ u.upliftLevel }}</ElTag>
        <span style="color:var(--app-text-bright)">{{ u.projectName }}</span>
        <span style="color:var(--app-text-muted)">{{ u.metricKey }} {{ u.beforeScore }} → {{ u.afterScore }} (提升: {{ u.uplift }})</span>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无提升数据" />
    </div>
  </TechPanel>
</template>
