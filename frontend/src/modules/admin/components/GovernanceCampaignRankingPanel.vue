<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listCampaignRanking, type GovernanceCampaignRankItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<GovernanceCampaignRankItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listCampaignRanking().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function effTag(l: string) { if (l === 'HIGH') return 'success' as const; if (l === 'MEDIUM') return 'primary' as const; if (l === 'LOW') return 'warning' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">活动排名</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="r in items" :key="r.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small">#{{ r.rankPosition }}</ElTag>
        <ElTag size="small" :type="effTag(r.effectivenessLevel)">{{ r.effectivenessLevel }}</ElTag>
        <span style="color:var(--app-text-bright)">{{ r.campaignName }}</span>
        <span style="color:var(--app-text-muted)">提升:{{ r.avgUplift }} 项目:{{ r.projectCount }}</span>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无活动排名" />
    </div>
  </TechPanel>
</template>
