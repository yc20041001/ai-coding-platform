<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listScorecards, type GovernanceMaturityScorecardItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<GovernanceMaturityScorecardItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listScorecards().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function maturityTag(l: string) { if (l === 'OPTIMIZING') return 'success' as const; if (l === 'DEFINED' || l === 'MANAGED') return 'primary' as const; if (l === 'DEVELOPING') return 'warning' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">成熟度记分卡</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="s in items" :key="s.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small" :type="maturityTag(s.maturityLevel)">{{ s.maturityLevel }}</ElTag>
        <span style="color:var(--app-text-bright)">{{ s.projectName }}</span>
        <span style="color:var(--app-text-muted)">总分:{{ s.totalScore }} 起草:{{ s.draftAdoptionScore }} 辅助:{{ s.assistiveQualityScore }} 提交包:{{ s.packageQualityScore }}</span>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无成熟度数据" />
    </div>
  </TechPanel>
</template>
