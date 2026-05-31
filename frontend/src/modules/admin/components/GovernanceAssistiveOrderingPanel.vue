<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listAssistiveOrdering, type GovernanceAssistiveOrderingItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<GovernanceAssistiveOrderingItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listAssistiveOrdering().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function optTag(l: string) { if (l === 'PROMOTE') return 'success' as const; if (l === 'DEMOTE') return 'danger' as const; if (l === 'REMOVE') return 'info' as const; return 'warning' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">辅助动作排序</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="a in items" :key="a.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small" :type="optTag(a.optimizationLevel)">{{ a.optimizationLevel }}</ElTag>
        <span style="color:var(--app-text-bright)">{{ a.actionType }}</span>
        <span style="color:var(--app-text-muted)">评分:{{ a.avgUsefulnessRating }} 新排序:{{ a.suggestedNewOrder }}</span>
        <span style="color:var(--app-text-muted)">{{ a.rationaleText }}</span>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无排序优化" />
    </div>
  </TechPanel>
</template>
