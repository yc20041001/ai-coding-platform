import client from '@/shared/api/client'
import type { ApiResponse, PageResult } from '@/shared/api/types'

export interface AuditLogItem {
  id: string
  projectId: string | null
  userId: string | null
  username: string | null
  actionType: string
  resourceType: string | null
  resourceId: string | null
  description: string | null
  traceId: string | null
  success: boolean
  createTime: string
}

export interface ModelUsageSummary {
  requestCount: number
  successCount: number
  failureCount: number
  successRate: number
  promptTokens: number
  completionTokens: number
  totalTokens: number
  avgLatencyMs: number
  mockCount: number
  realProviderCount: number
}

export interface SystemOverview {
  projectCount: number
  userCount: number
  taskCount: number
  runningTaskCount: number
  completedTaskCount: number
  agentCount: number
  knowledgeBaseCount: number
  documentCount: number
  chatMessageCount: number
  modelRequestCount: number
  todayModelRequestCount: number
  todayTokenUsage: number
}

export async function getOverview() {
  return client.get<ApiResponse<SystemOverview>>('/api/observability/overview')
}

export async function getModelUsageSummary() {
  return client.get<ApiResponse<ModelUsageSummary>>('/api/observability/model-usage/summary')
}

export async function getAuditLogs(page: number, pageSize: number) {
  return client.get<ApiResponse<PageResult<AuditLogItem>>>('/api/audit/logs', {
    params: { page, pageSize },
  })
}
