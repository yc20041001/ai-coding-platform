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
      title="可观测性"
      subtitle="系统遥测与审计日志"
      eyebrow="监控"
    >
      <template #actions>
        <StatusPulse
          :status="overview ? 'Live' : 'Loading...'"
          :tone="overview ? 'success' : 'warning'"
        />
      </template>

      <NeonDivider tone="primary" style="margin-bottom:20px" />

      <ErrorState v-if="overviewError" title="加载失败" message="无法获取系统概览数据" retry-text="重试" @retry="loadOverview" style="margin-bottom:20px" />

      <TechPanel v-if="!overviewError" title="系统总览" glow style="margin-bottom:20px" v-loading="loadingOverview">
        <div class="card-grid" v-if="overview">
          <MetricTile :value="overview.projectCount" label="项目" />
          <MetricTile :value="overview.userCount" label="用户" accent="accent" />
          <MetricTile :value="overview.taskCount" label="任务" accent="success" />
          <MetricTile :value="overview.runningTaskCount" label="运行中" accent="warning" />
          <MetricTile :value="overview.completedTaskCount" label="已完成" accent="success" />
          <MetricTile :value="overview.agentCount" label="智能体" accent="accent" />
          <MetricTile :value="overview.knowledgeBaseCount" label="知识库" accent="warning" />
          <MetricTile :value="overview.documentCount" label="文档" />
          <MetricTile :value="overview.modelRequestCount" label="模型调用" accent="accent" />
          <MetricTile :value="overview.todayModelRequestCount" label="今日调用" accent="success" />
          <MetricTile :value="formatNumber(overview.todayTokenUsage)" label="今日 Token" accent="accent" />
          <MetricTile :value="overview.chatMessageCount" label="对话消息" accent="warning" />
        </div>
      </TechPanel>

      <TechPanel v-if="!overviewError" title="模型用量" glow style="margin-bottom:20px" v-loading="loadingUsage">
        <div class="card-grid" v-if="modelUsage">
          <MetricTile :value="modelUsage.requestCount" label="请求数" />
          <MetricTile :value="modelUsage.successCount" label="成功" accent="success" />
          <MetricTile :value="modelUsage.failureCount" label="失败" accent="danger" />
          <MetricTile :value="(modelUsage.successRate * 100).toFixed(1) + '%'" label="成功率" accent="accent" />
          <MetricTile :value="formatNumber(modelUsage.totalTokens)" label="Token 总量" accent="accent" />
          <MetricTile :value="modelUsage.avgLatencyMs.toFixed(0) + 'ms'" label="平均延迟" accent="warning" />
          <MetricTile :value="modelUsage.mockCount" label="模拟调用" />
          <MetricTile :value="modelUsage.realProviderCount" label="真实调用" accent="success" />
        </div>
      </TechPanel>

      <TechPanel v-loading="loadingAudit" title="审计日志">
        <AuditLogFilters @search="handleAuditSearch" @reset="handleAuditReset" />
        <el-table :data="auditLogs" size="small" style="width:100%">
          <el-table-column prop="actionType" label="操作" width="140" />
          <el-table-column label="资源" width="140">
            <template #default="{ row }">{{ row.resourceType }} #{{ row.resourceId }}</template>
          </el-table-column>
          <el-table-column prop="username" label="用户" width="100" />
          <el-table-column label="成功" width="80">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column label="时间" width="160">
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
        <EmptyState v-if="!loadingAudit && auditLogs.length === 0" description="暂无审计日志" />
      </TechPanel>
    </DynamicWorkspace>
  </div>
</template>
