<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getExecutionDashboard, listExecutionPlans, updateExecutionPlanStatus, updateExecutionStepStatus, type GovernanceExecutionDashboardItem, type GovernanceExecutionPlanItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const dashboard = ref<GovernanceExecutionDashboardItem | null>(null)
const plans = ref<GovernanceExecutionPlanItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; Promise.all([getExecutionDashboard(), listExecutionPlans()]).then(([d, p]) => { dashboard.value = d.data.data; plans.value = p.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function updateStatus(id: string, status: string) { try { await updateExecutionPlanStatus(id, status); ElMessage.success('已更新'); loadData() } catch { ElMessage.error('操作失败') } }
async function updateStep(planId: string, stepKey: string, status: string) { try { await updateExecutionStepStatus(planId, stepKey, status); loadData() } catch { ElMessage.error('操作失败') } }
function statusTag(s: string) { if (s === 'COMPLETED') return 'success' as const; if (s === 'BLOCKED') return 'danger' as const; if (s === 'IN_PROGRESS') return 'warning' as const; if (s === 'READY') return 'primary' as const; return 'info' as const }
function planStatusLabel(s: string) { return { DRAFT: '草稿', READY: '就绪', IN_PROGRESS: '进行中', BLOCKED: '阻塞', COMPLETED: '已完成', ARCHIVED: '已归档' }[s] || s }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">执行计划</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="总计划" :value="dashboard.totalPlanCount" accent="primary" />
        <MetricTile label="就绪" :value="dashboard.readyPlanCount" accent="primary" />
        <MetricTile label="进行中" :value="dashboard.inProgressPlanCount" accent="warning" />
        <MetricTile label="阻塞" :value="dashboard.blockedPlanCount" accent="danger" />
        <MetricTile label="已完成" :value="dashboard.completedPlanCount" accent="success" />
        <MetricTile label="完成率" :value="dashboard.averageCompletionRate + '%'" accent="success" />
        <MetricTile label="待交接" :value="dashboard.handoffOpenCount" accent="warning" />
      </div>
      <div v-for="p in plans.slice(0, 8)" :key="p.id" style="padding:8px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <div style="display:flex;align-items:center;gap:6px">
            <ElTag size="small" :type="statusTag(p.planStatus)">{{ planStatusLabel(p.planStatus) }}</ElTag>
            <span style="font-size:12px;color:var(--app-text-muted)">完成: {{ p.completionRate }}%</span>
          </div>
          <div style="display:flex;gap:4px">
            <ElButton v-if="p.planStatus === 'DRAFT'" size="small" link @click="updateStatus(p.id, 'READY')">就绪</ElButton>
            <ElButton v-if="p.planStatus === 'READY'" size="small" link @click="updateStatus(p.id, 'IN_PROGRESS')">开始</ElButton>
            <ElButton v-if="p.planStatus === 'IN_PROGRESS'" size="small" link @click="updateStatus(p.id, 'COMPLETED')">完成</ElButton>
            <ElButton v-if="p.planStatus === 'IN_PROGRESS'" size="small" link @click="updateStatus(p.id, 'BLOCKED')">阻塞</ElButton>
            <ElButton v-if="p.planStatus === 'BLOCKED'" size="small" link @click="updateStatus(p.id, 'IN_PROGRESS')">继续</ElButton>
          </div>
        </div>
        <div style="font-size:11px;color:var(--app-text-muted);margin-top:2px">{{ p.summaryText }}</div>
      </div>
      <div v-if="dashboard.topBlockedPlans.length > 0" style="margin-top:12px">
        <div style="font-size:11px;font-weight:600;color:var(--color-error);margin-bottom:4px">阻塞计划</div>
        <div v-for="bp in dashboard.topBlockedPlans" :key="'b-' + bp.id" style="font-size:11px;color:var(--app-text-muted)">{{ bp.summaryText }}</div>
      </div>
    </div>
    <EmptyState v-if="!loading && !dashboard && !error" description="暂无执行计划" />
  </TechPanel>
</template>
