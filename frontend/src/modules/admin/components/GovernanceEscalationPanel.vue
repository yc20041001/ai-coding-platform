<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { scanEscalations, listEscalations, getEscalationDashboard, updateEscalationStatus, type GovernanceEscalationDashboardItem, type GovernanceEscalationEventItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'

const dashboard = ref<GovernanceEscalationDashboardItem | null>(null)
const events = ref<GovernanceEscalationEventItem[]>([]); const loading = ref(false); const error = ref(false); const scanning = ref(false)

function loadData() {
  loading.value = true; error.value = false
  Promise.all([getEscalationDashboard(), listEscalations()]).then(([d, e]) => { dashboard.value = d.data.data; events.value = e.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false })
}
async function handleScan() {
  scanning.value = true
  try { await scanEscalations(); loadData(); ElMessage.success('已扫描') } catch { ElMessage.error('扫描失败') } finally { scanning.value = false }
}
async function handleStatus(id: string, status: string) { try { await updateEscalationStatus(id, status); ElMessage.success('已更新'); loadData() } catch { ElMessage.error('操作失败') } }

function levelTag(l: string) { if (l === 'CRITICAL') return 'danger'; if (l === 'HIGH') return 'warning'; if (l === 'MEDIUM') return ''; return 'info' }
function typeLabel(t: string) { const m: Record<string, string> = { OVERDUE_RECOMMENDATION: '逾期', WAIVER_EXPIRING_SOON: 'Waiver将到期', WAIVER_EXPIRED: 'Waiver已过期', OWNER_OVERLOADED: 'Owner超载', OWNER_MISSING: 'Owner缺失' }; return m[t] || t }
onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">升级事件</span>
      <ElButton size="small" type="primary" :loading="scanning" @click="handleScan">扫描</ElButton>
    </div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="dashboard && !loading && !error">
      <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
        <MetricTile label="开放" :value="dashboard.openEscalationCount" accent="danger" />
        <MetricTile label="严重" :value="dashboard.criticalEscalationCount" accent="danger" />
        <MetricTile label="高" :value="dashboard.highEscalationCount" accent="warning" />
        <MetricTile label="Waiver将到期" :value="dashboard.waiverExpiringSoonCount" accent="warning" />
        <MetricTile label="Owner缺失" :value="dashboard.ownerMissingCount" accent="primary" />
      </div>
      <div v-for="e in events.slice(0, 15)" :key="e.id" style="padding:6px 8px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px">
        <div style="display:flex;align-items:center;justify-content:space-between;gap:6px">
          <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">
            <ElTag size="small" :type="levelTag(e.escalationLevel) as any">{{ typeLabel(e.escalationType) }}</ElTag>
            <ElTag size="small">{{ e.escalationLevel }}</ElTag>
            <span style="color:var(--app-text-bright)">{{ e.summary }}</span>
            <span style="color:var(--app-text-muted)">{{ e.ownerName || '' }}</span>
          </div>
          <div v-if="e.eventStatus === 'OPEN'" style="display:flex;gap:4px">
            <ElButton size="small" link @click="handleStatus(e.id, 'ACKNOWLEDGED')">确认</ElButton>
            <ElButton size="small" link @click="handleStatus(e.id, 'IGNORED')">忽略</ElButton>
          </div>
          <div v-if="e.eventStatus === 'ACKNOWLEDGED'">
            <ElButton size="small" link @click="handleStatus(e.id, 'RESOLVED')">解决</ElButton>
          </div>
        </div>
      </div>
      <EmptyState v-if="events.length === 0 && !loading" description="暂无升级事件" />
    </div>
  </TechPanel>
</template>
