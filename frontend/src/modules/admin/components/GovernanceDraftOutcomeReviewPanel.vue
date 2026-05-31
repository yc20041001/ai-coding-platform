<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listDraftAdoptionReviews, recordDraftAdoptionReview, type GovernanceDraftAdoptionReviewItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'
const items = ref<GovernanceDraftAdoptionReviewItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listDraftAdoptionReviews().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function recordAdopted() { try { await recordDraftAdoptionReview('1', 'ADOPTED', 5, 'NONE', 'HIGH_QUALITY_DRAFT'); loadData(); ElMessage.success('已记录') } catch { ElMessage.error('记录失败') } }
function resultTag(r: string) { if (r === 'ADOPTED') return 'success' as const; if (r === 'REJECTED') return 'danger' as const; if (r === 'MODIFIED_AND_ADOPTED') return 'warning' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">草稿采用评估</span><ElButton size="small" @click="recordAdopted">记录采用</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="r in items.slice(0, 8)" :key="r.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small" :type="resultTag(r.adoptionResult)">{{ r.adoptionResult }}</ElTag>
        <span style="color:var(--app-text-bright)">评分: {{ r.usefulnessRating }}/5</span>
        <span style="color:var(--app-text-muted)">{{ r.modificationLevel }}</span>
        <span v-if="r.reasonCode" style="color:var(--app-text-muted)">{{ r.reasonCode }}</span>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无采用评估" />
    </div>
  </TechPanel>
</template>
