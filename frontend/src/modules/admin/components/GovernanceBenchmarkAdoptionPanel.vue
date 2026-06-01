<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listAdoptionRecords, updateAdoptionRecordStatus, createAdoptionRecord, type GovernanceAdoptionRecordItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'
const items = ref<GovernanceAdoptionRecordItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listAdoptionRecords().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function createSample() { try { await createAdoptionRecord('1', 'Project-X', 'draft_adoption_rate'); loadData(); ElMessage.success('已创建') } catch { ElMessage.error('创建失败') } }
async function updateStatus(id: string, s: string) { try { await updateAdoptionRecordStatus(id, s); loadData() } catch { ElMessage.error('操作失败') } }
function statusTag(s: string) { if (s === 'ADOPTED') return 'success' as const; if (s === 'BLOCKED') return 'danger' as const; if (s === 'IN_PROGRESS') return 'warning' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">基准采用</span><ElButton size="small" @click="createSample">新建</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="r in items.slice(0, 8)" :key="r.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small" :type="statusTag(r.adoptionStatus)">{{ r.adoptionStatus }}</ElTag>
        <span style="color:var(--app-text-bright)">{{ r.projectName }}</span>
        <span style="color:var(--app-text-muted)">{{ r.metricKey }} {{ r.currentScore }}/{{ r.targetScore }}</span>
        <div style="margin-left:auto;display:flex;gap:4px">
          <ElButton v-if="r.adoptionStatus === 'IDENTIFIED'" size="small" link @click="updateStatus(r.id, 'IN_PROGRESS')">开始</ElButton>
          <ElButton v-if="r.adoptionStatus === 'IN_PROGRESS'" size="small" link @click="updateStatus(r.id, 'ADOPTED')">完成</ElButton>
          <ElButton v-if="r.adoptionStatus === 'IN_PROGRESS'" size="small" link @click="updateStatus(r.id, 'BLOCKED')">阻塞</ElButton>
        </div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无采用记录" />
    </div>
  </TechPanel>
</template>
