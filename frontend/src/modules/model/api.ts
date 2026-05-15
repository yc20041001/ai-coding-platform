import client from '@/shared/api/client'
import type { ApiResponse } from '@/shared/api/types'

export interface ModelProviderOption {
  provider: string
  displayName: string
  supportsStream: boolean
  supportsNonStream: boolean
  requiresApiKey: boolean
  requiresBaseUrl: boolean
  defaultBaseUrl: string | null
  knownModels: string[]
}

export interface ModelConfigItem {
  id: number
  provider: string
  modelName: string
  modelType: string
  apiBase: string | null
  maskedApiKey: string | null
  status: string
  timeoutMs: number | null
  maxRetries: number | null
  fallbackEnabled: boolean | null
  streamEnabled: boolean | null
  lastTestTime: string | null
  lastTestSuccess: boolean | null
  lastTestError: string | null
  createTime: string
  updateTime: string
}

export interface ModelConfigRequest {
  provider: string
  modelName: string
  modelType?: string
  apiBase?: string
  apiKey?: string
  status?: string
  timeoutMs?: number
  maxRetries?: number
  fallbackEnabled?: boolean
  streamEnabled?: boolean
}

export interface ConnectionTestRequest {
  provider: string
  baseUrl?: string
  modelName?: string
  apiKey?: string
}

export interface ConnectionTestResponse {
  success: boolean
  latencyMs: number
  message: string
  maskedApiKey: string
  modelName: string
  errorCode: string | null
}

export interface ProviderBreakdown {
  provider: string
  requestCount: number
  successCount: number
  tokenCount: number
  cost: number
}

export interface ModelBreakdown {
  modelName: string
  requestCount: number
  tokenCount: number
  cost: number
}

export interface ModelUsageCost {
  totalRequests: number
  successCount: number
  failureCount: number
  fallbackCount: number
  successRate: number
  totalTokens: number
  promptTokens: number
  completionTokens: number
  estimatedCost: number
  providerBreakdowns: ProviderBreakdown[]
  modelBreakdowns: ModelBreakdown[]
}

export function getProviderOptions() {
  return client.get<ApiResponse<ModelProviderOption[]>>('/api/model-gateway/providers')
}

export function getModelConfigs() {
  return client.get<ApiResponse<ModelConfigItem[]>>('/api/model-gateway/configs')
}

export function createModelConfig(data: ModelConfigRequest) {
  return client.post<ApiResponse<ModelConfigItem>>('/api/model-gateway/configs', data)
}

export function updateModelConfig(id: number, data: ModelConfigRequest) {
  return client.put<ApiResponse<ModelConfigItem>>(`/api/model-gateway/configs/${id}`, data)
}

export function deleteModelConfig(id: number) {
  return client.delete<ApiResponse<null>>(`/api/model-gateway/configs/${id}`)
}

export function testConnection(data: ConnectionTestRequest) {
  return client.post<ApiResponse<ConnectionTestResponse>>('/api/model-gateway/test-connection', data)
}

export function getGlobalCostSummary() {
  return client.get<ApiResponse<ModelUsageCost>>('/api/observability/model-usage/cost-summary')
}

export function getProjectCostSummary(projectId: string) {
  return client.get<ApiResponse<ModelUsageCost>>(`/api/projects/${projectId}/observability/model-usage/cost-summary`)
}
