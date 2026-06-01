<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listImprovementCampaigns, createImprovementCampaign, updateCampaignStatus, type GovernanceImprovementCampaignItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'
const items = ref<GovernanceImprovementCampaignItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listImprovementCampaigns().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function handleCreate() { try { await createImprovementCampaign('cmp-' + Date.now(), 'Campaign'); loadData(); ElMessage.success('已创建') } catch { ElMessage.error('创建失败') } }
async function updateStatus(id: string, s: string) { try { await updateCampaignStatus(id, s); loadData() } catch { ElMessage.error('操作失败') } }
function statusTag(s: string) { if (s === 'ACTIVE') return 'success' as const; if (s === 'COMPLETED') return 'primary' as const; if (s === 'CANCELLED') return 'info' as const; return 'warning' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">改进活动</span><ElButton size="small" @click="handleCreate">新建</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="c in items.slice(0, 8)" :key="c.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small" :type="statusTag(c.campaignStatus)">{{ c.campaignStatus }}</ElTag>
        <span style="color:var(--app-text-bright)">{{ c.campaignName }}</span>
        <span style="color:var(--app-text-muted)">{{ c.campaignKey }} {{ c.improvementWindow }}</span>
        <div style="margin-left:auto;display:flex;gap:4px">
          <ElButton v-if="c.campaignStatus === 'DRAFT'" size="small" link @click="updateStatus(c.id, 'ACTIVE')">启动</ElButton>
          <ElButton v-if="c.campaignStatus === 'ACTIVE'" size="small" link @click="updateStatus(c.id, 'COMPLETED')">完成</ElButton>
          <ElButton v-if="c.campaignStatus === 'ACTIVE'" size="small" link @click="updateStatus(c.id, 'CANCELLED')">取消</ElButton>
        </div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无改进活动" />
    </div>
  </TechPanel>
</template>
