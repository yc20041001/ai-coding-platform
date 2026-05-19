<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listAgents, type AgentItem } from '@/modules/agent/api'
import AgentDetailDrawer from '@/modules/agent/components/AgentDetailDrawer.vue'
import AgentVersionDrawer from '@/modules/agent/components/AgentVersionDrawer.vue'
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { formatDateTime } from '@/shared/utils/format'

const agents = ref<AgentItem[]>([])
const loading = ref(false)
const selectedAgentId = ref<string | null>(null)
const detailVisible = ref(false)
const versionDrawerVisible = ref(false)
const versionDrawerAgentId = ref<string | null>(null)
const versionDrawerAgentName = ref('')

onMounted(async () => {
  loading.value = true
  try {
    const res = await listAgents()
    agents.value = res.data.data
  } catch { /* handled */ } finally {
    loading.value = false
  }
})

function handleRowClick(row: AgentItem) {
  openDetail(row.id)
}

function openDetail(agentId: string) {
  selectedAgentId.value = agentId
  detailVisible.value = true
}

function onDrawerClose() {
  detailVisible.value = false
  selectedAgentId.value = null
}

function openVersions(agent: AgentItem) {
  versionDrawerAgentId.value = agent.id
  versionDrawerAgentName.value = agent.name
  versionDrawerVisible.value = true
}

function onVersionDrawerClose() {
  versionDrawerVisible.value = false
  versionDrawerAgentId.value = null
  versionDrawerAgentName.value = ''
}

function typeIcon(type: string) {
  switch (type) {
    case 'CODING': return '◇'
    case 'REVIEW': return '◎'
    case 'DOCS': return '◈'
    case 'GENERAL': return '◆'
    default: return '◇'
  }
}

function statusTone(status: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'primary' | 'muted'> = {
    ACTIVE: 'success', ENABLED: 'success', DISABLED: 'muted', ERROR: 'danger',
  }
  return map[status] || 'muted'
}
</script>

<template>
  <div class="page-container">
    <DynamicWorkspace
      title="智能体"
      subtitle="AI 智能体管理"
      eyebrow="智能体工作台"
      :status="`${agents.length} agents`"
    >
      <template #actions>
        <StatusPulse
          :status="agents.length > 0 ? 'Online' : 'Idle'"
          :tone="agents.length > 0 ? 'success' : 'muted'"
        />
      </template>

      <NeonDivider tone="primary" style="margin-bottom:20px" />

      <div v-loading="loading">
        <el-table
          v-if="agents.length > 0"
          :data="agents"
          style="width:100%"
          @row-click="handleRowClick"
        >
          <el-table-column label="智能体" min-width="200">
            <template #default="{ row }">
              <div class="agent-row">
                <span class="agent-icon">{{ typeIcon(row.type) }}</span>
                <div>
                  <div class="agent-name">{{ row.name }}</div>
                  <div class="agent-code">{{ row.code }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <StatusPulse :status="row.status" :tone="statusTone(row.status)" />
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="240">
            <template #default="{ row }">
              <span v-if="row.description" style="color:var(--app-text-muted);font-size:13px">{{ row.description }}</span>
              <span v-else style="color:var(--app-text-muted)">-</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button
                data-testid="btn-agent-versions"
                type="primary"
                size="small"
                text
                @click.stop="openVersions(row)"
              >
                版本
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-if="!loading && agents.length === 0" description="暂无智能体" />
      </div>
    </DynamicWorkspace>

    <AgentDetailDrawer
      :agent-id="selectedAgentId"
      :visible="detailVisible"
      @close="onDrawerClose"
    />

    <AgentVersionDrawer
      :agent-id="versionDrawerAgentId"
      :agent-name="versionDrawerAgentName"
      :visible="versionDrawerVisible"
      @close="onVersionDrawerClose"
    />
  </div>
</template>

<style scoped>
.agent-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.agent-icon {
  font-size: 22px;
  color: var(--app-primary);
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-primary-soft);
  border-radius: 8px;
  border: 1px solid rgba(56, 189, 248, 0.15);
}
.agent-name { font-size: 14px; font-weight: 600; color: var(--app-text); }
.agent-code { font-size: 11px; color: var(--app-text-muted); font-family: monospace; margin-top: 2px; }

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
