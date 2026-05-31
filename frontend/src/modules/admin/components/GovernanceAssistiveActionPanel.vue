<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listDraftPlans, listAssistiveActions, generateAssistiveActions, updateAssistiveActionStatus, type GovernanceAssistiveActionItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'
const items = ref<GovernanceAssistiveActionItem[]>([]); const loading = ref(false); const error = ref(false); const planId = ref('')
function loadData() { loading.value = true; error.value = false; listDraftPlans().then(plans => { if (plans.data.data.length > 0) { planId.value = plans.data.data[0].id; return listAssistiveActions(plans.data.data[0].id) } return Promise.resolve({ data: { data: [] } as any }) }).then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function handleGenerate() { if (!planId.value) return; try { await generateAssistiveActions(planId.value); loadData(); ElMessage.success('已生成') } catch { ElMessage.error('生成失败') } }
async function updateStatus(id: string, s: string) { try { await updateAssistiveActionStatus(id, s); loadData() } catch { ElMessage.error('操作失败') } }
function safetyTag(l: string) { if (l === 'REVIEW_REQUIRED') return 'danger' as const; if (l === 'CAUTION') return 'warning' as const; if (l === 'SAFE') return 'success' as const; return 'info' as const }
function statusTag(s: string) { if (s === 'READY') return 'success' as const; if (s === 'REVIEWED') return 'primary' as const; if (s === 'SKIPPED') return 'info' as const; return 'warning' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">安全辅助动作</span><ElButton size="small" @click="handleGenerate">生成动作</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="a in items" :key="a.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px">
        <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">
          <ElTag size="small" :type="statusTag(a.actionStatus)">{{ a.actionStatus }}</ElTag>
          <ElTag size="small" :type="safetyTag(a.safetyLevel)">{{ a.safetyLevel }}</ElTag>
          <span style="color:var(--app-text-bright)">{{ a.actionTitle }}</span>
          <div style="margin-left:auto;display:flex;gap:4px">
            <ElButton v-if="a.actionStatus === 'PENDING'" size="small" link @click="updateStatus(a.id, 'REVIEWED')">审阅</ElButton>
            <ElButton v-if="a.actionStatus === 'PENDING'" size="small" link @click="updateStatus(a.id, 'SKIPPED')">跳过</ElButton>
            <ElButton v-if="a.actionStatus === 'REVIEWED'" size="small" link @click="updateStatus(a.id, 'READY')">就绪</ElButton>
          </div>
        </div>
        <div style="color:var(--app-text-muted);font-size:11px;margin-top:2px">{{ a.actionSummary }}</div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无辅助动作" />
    </div>
  </TechPanel>
</template>
