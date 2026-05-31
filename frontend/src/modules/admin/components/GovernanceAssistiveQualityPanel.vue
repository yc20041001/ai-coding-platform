<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listAssistiveQualityReviews, type GovernanceAssistiveQualityReviewItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<GovernanceAssistiveQualityReviewItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listAssistiveQualityReviews().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function outcomeTag(o: string) { if (o === 'USEFUL') return 'success' as const; if (o === 'NOT_USEFUL') return 'danger' as const; if (o === 'PARTIALLY_USEFUL') return 'warning' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">辅助动作质量</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="r in items.slice(0, 8)" :key="r.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small" :type="outcomeTag(r.outcomeResult)">{{ r.outcomeResult }}</ElTag>
        <span style="color:var(--app-text-bright)">评分: {{ r.usefulnessRating }}/5</span>
        <span v-if="r.reasonCode" style="color:var(--app-text-muted)">{{ r.reasonCode }}</span>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无质量评估" />
    </div>
  </TechPanel>
</template>
