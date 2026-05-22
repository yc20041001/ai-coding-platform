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

// ========================
// Tool Execution Metrics (37D)
// ========================

export interface ToolExecutionSummary {
  totalJobs: number
  pendingJobs: number
  runningJobs: number
  completedJobs: number
  failedJobs: number
  retryPendingJobs: number
  canceledJobs: number
  deadLetteredJobs: number
  successRate: number
  failureRate: number
  retryRate: number
  avgDurationMs: number
  maxDurationMs: number
  totalRetries: number
}

export interface ToolExecutionToolMetric {
  toolKey: string
  totalJobs: number
  completedJobs: number
  failedJobs: number
  deadLetteredJobs: number
  successRate: number
  avgDurationMs: number
  totalRetries: number
  topErrorCode: string | null
  topFailureStage: string | null
}

export interface ToolExecutionDailyMetric {
  date: string
  totalJobs: number
  completedJobs: number
  failedJobs: number
  deadLetteredJobs: number
  retryPendingJobs: number
  avgDurationMs: number
}

export interface ToolExecutionFailureMetric {
  errorCode: string
  count: number
  latestTime: string | null
}

export interface ToolExecutionMetrics {
  summary: ToolExecutionSummary
  tools: ToolExecutionToolMetric[]
  daily: ToolExecutionDailyMetric[]
  errorCodes: ToolExecutionFailureMetric[]
  failureStages: ToolExecutionFailureMetric[]
}

export function getGlobalToolExecutionMetrics() {
  return client.get<ApiResponse<ToolExecutionMetrics>>('/api/observability/tool-executions/metrics')
}

export function getProjectToolExecutionMetrics(projectId: string) {
  return client.get<ApiResponse<ToolExecutionMetrics>>(`/api/projects/${projectId}/observability/tool-executions/metrics`)
}

export interface ToolExecutionProblemJob {
  id: string
  projectId: string | null
  taskId: string | null
  runId: string | null
  stepId: string | null
  toolExecutionId: string | null
  toolKey: string | null
  status: string
  priority: string
  retryCount: number
  maxRetryCount: number
  lastError: string | null
  errorCode: string | null
  failureStage: string | null
  nextRetryAt: string | null
  deadLetteredAt: string | null
  deadLetterReason: string | null
  sourceJobId: string | null
  startedAt: string | null
  finishedAt: string | null
  durationMs: number
  createTime: string | null
}

export function getProjectProblemToolJobs(projectId: string, status?: string, limit?: number) {
  const params: Record<string, unknown> = {}
  if (status) params.status = status
  if (limit) params.limit = limit
  return client.get<ApiResponse<ToolExecutionProblemJob[]>>(`/api/projects/${projectId}/observability/tool-executions/problem-jobs`, { params })
}
