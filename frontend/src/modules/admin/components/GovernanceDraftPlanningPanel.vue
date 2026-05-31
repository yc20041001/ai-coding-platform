<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listDraftPlans, createDraftPlan, updateDraftPlanStatus, refreshDraftPlan, type GovernanceDraftPlanItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'
const items = ref<GovernanceDraftPlanItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listDraftPlans().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function handleCreate() { try { await createDraftPlan('Draft Plan ' + Date.now()); loadData(); ElMessage.success('已创建') } catch { ElMessage.error('创建失败') } }
async function handleStatus(id: string, s: string) { try { await updateDraftPlanStatus(id, s); loadData() } catch { ElMessage.error('操作失败') } }
async function handleRefresh(id: string) { try { await refreshDraftPlan(id); loadData(); ElMessage.success('已刷新') } catch { ElMessage.error('刷新失败') } }
function statusTag(s: string) { if (s === 'DRAFT') return 'info' as const; if (s === 'READY_FOR_REVIEW') return 'warning' as const; if (s === 'REVIEWED') return 'success' as const; return 'info' as const }
function riskTag(r: string) { if (r === 'HIGH' || r === 'CRITICAL') return 'danger' as const; if (r === 'MEDIUM') return 'warning' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">草稿计划</span><ElButton size="small" type="primary" @click="handleCreate">新建草稿</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="p in items.slice(0, 8)" :key="p.id" style="padding:8px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;justify-content:space-between"><div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap"><ElTag size="small" :type="statusTag(p.planStatus)">{{ p.planStatus }}</ElTag><ElTag size="small" :type="riskTag(p.riskLevel)">{{ p.riskLevel }}</ElTag><span style="font-weight:500;font-size:13px;color:var(--app-text-bright)">{{ p.planTitle }}</span></div><div style="display:flex;gap:4px">
          <ElButton v-if="p.planStatus === 'DRAFT'" size="small" link @click="handleStatus(p.id, 'READY_FOR_REVIEW')">提交审阅</ElButton>
          <ElButton v-if="p.planStatus === 'READY_FOR_REVIEW'" size="small" link @click="handleStatus(p.id, 'REVIEWED')">已审阅</ElButton>
          <ElButton v-if="p.planStatus === 'DRAFT'" size="small" link @click="handleRefresh(p.id)">刷新</ElButton>
        </div></div>
        <div style="font-size:11px;color:var(--app-text-muted);margin-top:2px">{{ p.scopeType }} | 需确认: {{ p.humanConfirmationRequired ? '是' : '否' }}</div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无草稿计划" />
    </div>
  </TechPanel>
</template>
