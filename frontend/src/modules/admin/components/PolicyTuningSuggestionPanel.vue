<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listTuningSuggestions, refreshTuningSuggestions, type PolicyTuningSuggestionItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const items = ref<PolicyTuningSuggestionItem[]>([]); const loading = ref(false); const error = ref(false); const refreshing = ref(false)
function loadData() { loading.value = true; error.value = false; listTuningSuggestions().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function handleRefresh() { refreshing.value = true; try { await refreshTuningSuggestions(); loadData(); ElMessage.success('已刷新') } catch { ElMessage.error('刷新失败') } finally { refreshing.value = false } }
function typeLabel(t: string) { const m: Record<string, string> = { ADJUST_SLA: 'SLA调整', REBALANCE_OWNER_LOAD: 'Owner重平衡', REDUCE_WAIVER_CLUSTER: 'Waiver缩减', ADJUST_GUARDRAIL_THRESHOLD: 'Guardrail阈值' }; return m[t] || t }
function priorityTag(p: string) { if (p === 'P0') return 'danger' as const; if (p === 'P1') return 'warning' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">策略调优建议</span><ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="s in items" :key="s.id" style="padding:8px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;gap:6px;margin-bottom:4px;flex-wrap:wrap">
          <ElTag size="small" :type="priorityTag(s.priority)" effect="dark">{{ s.priority }}</ElTag>
          <ElTag size="small">{{ typeLabel(s.suggestionType) }}</ElTag>
          <span style="font-weight:500;font-size:13px;color:var(--app-text-bright)">{{ s.expectedImpactText }}</span>
        </div>
        <div style="font-size:11px;color:var(--app-text-muted)">
          {{ s.currentValue }} → {{ s.suggestedValue }}
          <span v-if="s.rationaleText" style="margin-left:8px">{{ s.rationaleText }}</span>
        </div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无调优建议" />
    </div>
  </TechPanel>
</template>
