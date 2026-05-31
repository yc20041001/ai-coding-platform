<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { createWorkspaceSession, getWorkspaceTasks, updateWorkspaceTaskStatus, type GovernanceGuidedTaskItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const tasks = ref<GovernanceGuidedTaskItem[]>([]); const loading = ref(false); const error = ref(false); const sessionId = ref('')
function loadData() { loading.value = true; error.value = false; createWorkspaceSession().then(s => { sessionId.value = s.data.data.id; return getWorkspaceTasks(s.data.data.id) }).then(r => { tasks.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function updateStatus(id: string, status: string) { try { await updateWorkspaceTaskStatus(id, status); loadData() } catch { ElMessage.error('操作失败') } }
function typeLabel(t: string) { return { TRIAGE_RECOMMENDATION:'分类', RUN_PLAYBOOK:'执行Playbook', APPLY_RECIPE_GUIDANCE:'应用Recipe', PREPARE_HANDOFF:'准备交接', REVIEW_WAIVER:'审查Waiver', REDUCE_BACKLOG:'缩减积压' }[t]||t }
function statusTag(s: string) { if (s === 'DONE') return 'success' as const; if (s === 'BLOCKED') return 'danger' as const; if (s === 'IN_PROGRESS') return 'warning' as const; return 'info' as const }
function statusLabel(s: string) { return { OPEN:'开放', IN_PROGRESS:'进行中', DONE:'已完成', SKIPPED:'已跳过', BLOCKED:'阻塞' }[s]||s }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">引导任务</span><ElButton size="small" @click="loadData">刷新</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="t in tasks.slice(0, 10)" :key="t.id" style="padding:8px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;justify-content:space-between"><div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap"><ElTag size="small" :type="statusTag(t.taskStatus)">{{ statusLabel(t.taskStatus) }}</ElTag><ElTag size="small">{{ typeLabel(t.taskType) }}</ElTag><ElTag size="small">{{ t.priority }}</ElTag><span style="font-size:13px;color:var(--app-text-bright)">{{ t.title }}</span></div><div style="display:flex;gap:4px">
          <ElButton v-if="t.taskStatus === 'OPEN'" size="small" link @click="updateStatus(t.id, 'IN_PROGRESS')">开始</ElButton>
          <ElButton v-if="t.taskStatus === 'IN_PROGRESS'" size="small" link @click="updateStatus(t.id, 'DONE')">完成</ElButton>
          <ElButton v-if="t.taskStatus === 'IN_PROGRESS'" size="small" link @click="updateStatus(t.id, 'BLOCKED')">阻塞</ElButton>
          <ElButton v-if="t.taskStatus === 'BLOCKED'" size="small" link @click="updateStatus(t.id, 'IN_PROGRESS')">继续</ElButton>
        </div></div>
        <div v-if="t.linkedPlaybookKey || t.linkedRecipeKey" style="font-size:11px;color:var(--app-text-muted);margin-top:2px">
          Playbook: {{ t.linkedPlaybookKey || '-' }} | Recipe: {{ t.linkedRecipeKey || '-' }}
        </div>
      </div>
      <EmptyState v-if="tasks.length === 0 && !loading && !error" description="暂无引导任务" />
    </div>
  </TechPanel>
</template>
