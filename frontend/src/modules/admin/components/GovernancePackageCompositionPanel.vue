<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listPackageComposition, type GovernancePackageCompositionItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<GovernancePackageCompositionItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listPackageComposition().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function tuneTag(l: string) { if (l === 'ADD_SECTION') return 'success' as const; if (l === 'REMOVE_SECTION') return 'danger' as const; return 'warning' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">提交包组成</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="c in items" :key="c.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small" :type="tuneTag(c.tuningLevel)">{{ c.tuningLevel }}</ElTag>
        <span style="color:var(--app-text-bright)">{{ c.scoreRange }}</span>
        <span style="color:var(--app-text-muted)">完整性:{{ c.avgCompleteness }} 准确性:{{ c.avgAccuracy }} 综合:{{ c.avgOverall }}</span>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无组成调优" />
    </div>
  </TechPanel>
</template>
