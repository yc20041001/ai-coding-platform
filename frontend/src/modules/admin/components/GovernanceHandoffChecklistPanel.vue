<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listHandoffChecklists, updateHandoffChecklistStatus, type GovernanceHandoffChecklistItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const items = ref<GovernanceHandoffChecklistItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listHandoffChecklists().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function updateStatus(id: string, status: string) { try { await updateHandoffChecklistStatus(id, status); ElMessage.success('已更新'); loadData() } catch { ElMessage.error('操作失败') } }
function statusTag(s: string) { if (s === 'COMPLETED') return 'success' as const; if (s === 'IN_PROGRESS') return 'warning' as const; if (s === 'CANCELLED') return 'info' as const; return 'info' as const }
function statusLabel(s: string) { return { OPEN: '待交接', IN_PROGRESS: '交接中', COMPLETED: '已完成', CANCELLED: '已取消' }[s] || s }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Handoff 清单</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="h in items" :key="h.id" style="padding:8px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <div style="display:flex;align-items:center;gap:6px">
            <ElTag size="small" :type="statusTag(h.checklistStatus)">{{ statusLabel(h.checklistStatus) }}</ElTag>
            <span style="font-size:12px;color:var(--app-text-bright)">{{ h.fromOwnerName || '?' }} → {{ h.toOwnerName || '?' }}</span>
          </div>
          <div style="display:flex;gap:4px">
            <ElButton v-if="h.checklistStatus === 'OPEN'" size="small" link @click="updateStatus(h.id, 'IN_PROGRESS')">开始</ElButton>
            <ElButton v-if="h.checklistStatus === 'IN_PROGRESS'" size="small" link @click="updateStatus(h.id, 'COMPLETED')">完成</ElButton>
          </div>
        </div>
        <div v-if="h.handoffNote" style="font-size:11px;color:var(--app-text-muted);margin-top:2px">{{ h.handoffNote }}</div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无交接记录" />
    </div>
  </TechPanel>
</template>
