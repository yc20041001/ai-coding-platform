<script setup lang="ts">
import { ref, onMounted } from 'vue'
import client from '@/shared/api/client'
import type { ApiResponse, PageResult } from '@/shared/api/types'
import { getOverview, getModelUsageSummary, type SystemOverview, type ModelUsageSummary, type AuditLogItem } from '@/modules/admin/api'
import AuditLogFilters, { type AuditLogFilterValues } from '@/modules/admin/components/AuditLogFilters.vue'
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import { formatDateTime, formatNumber } from '@/shared/utils/format'

const overview = ref<SystemOverview | null>(null)
const modelUsage = ref<ModelUsageSummary | null>(null)
const overviewError = ref(false)

const auditLogs = ref<AuditLogItem[]>([])
const loadingOverview = ref(false)
const loadingUsage = ref(false)
const loadingAudit = ref(false)
const auditPage = ref(1)
const auditTotal = ref(0)
const auditFilters = ref<AuditLogFilterValues>({})

async function loadOverview() {
  loadingOverview.value = true
  overviewError.value = false
  try {
    const [oRes, mRes] = await Promise.all([getOverview(), getModelUsageSummary()])
    overview.value = oRes.data.data
    modelUsage.value = mRes.data.data
  } catch {
    overviewError.value = true
  } finally {
    loadingOverview.value = false
    loadingUsage.value = false
  }
}

async function loadAudit(page: number, filters: AuditLogFilterValues = {}) {
  auditPage.value = page
  loadingAudit.value = true
  try {
    const params: Record<string, unknown> = { page, pageSize: 10 }
    if (filters.userId) params.userId = filters.userId
    if (filters.actionType) params.actionType = filters.actionType
    if (filters.resourceType) params.resourceType = filters.resourceType
    if (filters.resourceId) params.resourceId = filters.resourceId
    if (filters.startTime) params.startTime = filters.startTime
    if (filters.endTime) params.endTime = filters.endTime

    const res = await client.get<ApiResponse<PageResult<AuditLogItem>>>('/api/audit/logs', { params })
    auditLogs.value = res.data.data.records
    auditTotal.value = res.data.data.total
  } catch { /* handled */ } finally {
    loadingAudit.value = false
  }
}

function handleAuditSearch(filters: AuditLogFilterValues) {
  auditFilters.value = filters
  loadAudit(1, filters)
}

function handleAuditReset() {
  auditFilters.value = {}
  loadAudit(1)
}

onMounted(() => {
  loadOverview()
  loadAudit(1)
})
</script>

<template>
  <div class="page-container">
    <DynamicWorkspace
      title="Observability"
      subtitle="System Telemetry & Audit Logs"
      eyebrow="Monitoring"
    >
      <template #actions>
        <StatusPulse
          :status="overview ? 'Live' : 'Loading...'"
          :tone="overview ? 'success' : 'warning'"
        />
      </template>

      <NeonDivider tone="primary" style="margin-bottom:20px" />

      <ErrorState v-if="overviewError" title="Load Failed" message="Cannot fetch system overview data" retry-text="Retry" @retry="loadOverview" style="margin-bottom:20px" />

      <TechPanel v-if="!overviewError" title="System Overview" glow style="margin-bottom:20px" v-loading="loadingOverview">
        <div class="card-grid" v-if="overview">
          <MetricTile :value="overview.projectCount" label="Projects" />
          <MetricTile :value="overview.userCount" label="Users" accent="accent" />
          <MetricTile :value="overview.taskCount" label="Tasks" accent="success" />
          <MetricTile :value="overview.runningTaskCount" label="Running" accent="warning" />
          <MetricTile :value="overview.completedTaskCount" label="Completed" accent="success" />
          <MetricTile :value="overview.agentCount" label="Agents" accent="accent" />
          <MetricTile :value="overview.knowledgeBaseCount" label="Knowledge Bases" accent="warning" />
          <MetricTile :value="overview.documentCount" label="Documents" />
          <MetricTile :value="overview.modelRequestCount" label="Model Calls" accent="accent" />
          <MetricTile :value="overview.todayModelRequestCount" label="Today Calls" accent="success" />
          <MetricTile :value="formatNumber(overview.todayTokenUsage)" label="Today Tokens" accent="accent" />
          <MetricTile :value="overview.chatMessageCount" label="Chat Messages" accent="warning" />
        </div>
      </TechPanel>

      <TechPanel v-if="!overviewError" title="Model Usage" glow style="margin-bottom:20px" v-loading="loadingUsage">
        <div class="card-grid" v-if="modelUsage">
          <MetricTile :value="modelUsage.requestCount" label="Requests" />
          <MetricTile :value="modelUsage.successCount" label="Success" accent="success" />
          <MetricTile :value="modelUsage.failureCount" label="Failure" accent="danger" />
          <MetricTile :value="(modelUsage.successRate * 100).toFixed(1) + '%'" label="Success Rate" accent="accent" />
          <MetricTile :value="formatNumber(modelUsage.totalTokens)" label="Total Tokens" accent="accent" />
          <MetricTile :value="modelUsage.avgLatencyMs.toFixed(0) + 'ms'" label="Avg Latency" accent="warning" />
          <MetricTile :value="modelUsage.mockCount" label="Mock Calls" />
          <MetricTile :value="modelUsage.realProviderCount" label="Real Calls" accent="success" />
        </div>
      </TechPanel>

      <TechPanel v-loading="loadingAudit" title="Audit Logs">
        <AuditLogFilters @search="handleAuditSearch" @reset="handleAuditReset" />
        <el-table :data="auditLogs" size="small" style="width:100%">
          <el-table-column prop="actionType" label="Action" width="140" />
          <el-table-column label="Resource" width="140">
            <template #default="{ row }">{{ row.resourceType }} #{{ row.resourceId }}</template>
          </el-table-column>
          <el-table-column prop="username" label="User" width="100" />
          <el-table-column label="Success" width="80">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? 'Yes' : 'No' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="Description" min-width="200" />
          <el-table-column label="Time" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="auditTotal > 0" v-model:current-page="auditPage"
          :page-size="10" :total="auditTotal"
          layout="total, prev, pager, next" size="small"
          style="margin-top:16px;justify-content:flex-end"
          @current-change="loadAudit"
        />
        <EmptyState v-if="!loadingAudit && auditLogs.length === 0" description="No audit logs" />
      </TechPanel>
    </DynamicWorkspace>
  </div>
</template>
