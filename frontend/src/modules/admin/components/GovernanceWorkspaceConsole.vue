<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { createWorkspaceSession, getWorkspaceDashboard, refreshWorkspace, listWorkspaceSessions, updateWorkspaceSessionStatus, type GovernanceWorkspaceSessionItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElSelect, ElOption } from 'element-plus'

const dashboard = ref<any>(null); const sessions = ref<GovernanceWorkspaceSessionItem[]>([]); const loading = ref(false); const error = ref(false); const refreshing = ref(false)
const focusMode = ref('PRIORITY_FIRST')
function loadData() { loading.value = true; error.value = false; Promise.all([getWorkspaceDashboard(), listWorkspaceSessions()]).then(([d, s]) => { dashboard.value = d.data.data; sessions.value = s.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function handleRefresh() { const d = dashboard.value; if (!d?.activeSession?.id) return; refreshing.value = true; try { await refreshWorkspace(d.activeSession.id); loadData(); ElMessage.success('已刷新') } catch { ElMessage.error('刷新失败') } finally { refreshing.value = false } }
async function handleCreate() { try { await createWorkspaceSession(focusMode.value); loadData(); ElMessage.success('已创建') } catch { ElMessage.error('创建失败') } }
async function handleStatus(id: string, status: string) { try { await updateWorkspaceSessionStatus(id, status); loadData() } catch { ElMessage.error('操作失败') } }
function focusLabel(f: string) { return { PRIORITY_FIRST: '优先级优先', OWNER_CENTRIC: 'Owner中心', PROJECT_CENTRIC: '项目中心', WAIVER_REDUCTION: 'Waiver缩减', BACKLOG_REDUCTION: '积压缩减' }[f] || f }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Copilot 工作台</span><div style="display:flex;gap:6px"><ElSelect v-model="focusMode" size="small"><ElOption label="优先级优先" value="PRIORITY_FIRST" /><ElOption label="Owner中心" value="OWNER_CENTRIC" /><ElOption label="项目中心" value="PROJECT_CENTRIC" /><ElOption label="Waiver缩减" value="WAIVER_REDUCTION" /></ElSelect><ElButton size="small" type="primary" @click="handleCreate">新建会话</ElButton><ElButton size="small" :loading="refreshing" @click="handleRefresh">刷新</ElButton></div></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="开放任务" :value="dashboard.openTaskCount" accent="primary" />
        <MetricTile label="进行中" :value="dashboard.inProgressTaskCount" accent="warning" />
        <MetricTile label="阻塞" :value="dashboard.blockedTaskCount" accent="danger" />
      </div>
      <div v-if="dashboard.activeSession" style="padding:8px;background:rgba(255,255,255,0.03);border-radius:6px;margin-bottom:8px;font-size:12px">
        <span style="color:var(--app-text-bright)">活跃会话: {{ dashboard.activeSession.id?.substring(0, 8) }}...</span>
        <ElTag size="small" style="margin-left:6px">{{ focusLabel(dashboard.activeSession.focusMode) }}</ElTag>
        <ElTag size="small" style="margin-left:4px">{{ dashboard.activeSession.sessionStatus }}</ElTag>
        <div style="margin-top:4px;display:flex;gap:4px">
          <ElButton v-if="dashboard.activeSession.sessionStatus === 'ACTIVE'" size="small" link @click="handleStatus(dashboard.activeSession.id, 'PAUSED')">暂停</ElButton>
          <ElButton v-if="dashboard.activeSession.sessionStatus === 'PAUSED'" size="small" link @click="handleStatus(dashboard.activeSession.id, 'ACTIVE')">恢复</ElButton>
        </div>
      </div>
      <div v-if="dashboard.topNextStepRecommendations && dashboard.topNextStepRecommendations.length > 0">
        <div style="font-size:11px;font-weight:600;color:var(--app-accent);margin-bottom:4px">下一步建议</div>
        <div v-for="ns in dashboard.topNextStepRecommendations" :key="ns.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;border-left:3px solid var(--app-accent)">
          <div style="font-weight:500;color:var(--app-text-bright)">{{ ns.title }}</div>
          <div style="color:var(--app-text-muted)">{{ ns.rationaleText }}</div>
        </div>
      </div>
    </div>
    <EmptyState v-if="!loading && !dashboard && !error" description="暂无工作台数据" />
  </TechPanel>
</template>
