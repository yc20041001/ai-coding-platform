import client from '@/shared/api/client'
import type { ApiResponse, PageResult } from '@/shared/api/types'

export interface AgentItem {
  id: string
  name: string
  code: string
  type: string
  status: string
  description: string
  createTime: string
}

export interface AgentVersionInfo {
  id: string
  versionNo: string
  modelConfigId: string | null
  status: string
}

export interface AgentDetail {
  id: string
  name: string
  code: string
  type: string
  status: string
  description: string | null
  avatar: string | null
  latestVersion: AgentVersionInfo | null
  modelConfigId: string | null
  systemPrompt: string | null
  toolPolicy: string | null
  executionPolicy: string | null
}

export async function listAgents(params?: { type?: string; status?: string }) {
  return client.get<ApiResponse<AgentItem[]>>('/api/agents', { params })
}

export async function getAgentDetail(agentId: string) {
  return client.get<ApiResponse<AgentDetail>>(`/api/agents/${agentId}`)
}

export interface ProjectAgentRuntimeConfig {
  temperature: number
  maxTokens: number
  timeoutSeconds: number
  useRag: boolean
  knowledgeBaseId: string | null
  customInstruction: string
}

export function defaultRuntimeConfig(): ProjectAgentRuntimeConfig {
  return {
    temperature: 0.2,
    maxTokens: 4096,
    timeoutSeconds: 60,
    useRag: false,
    knowledgeBaseId: null,
    customInstruction: '',
  }
}

export function runtimeConfigSummary(cfg: ProjectAgentRuntimeConfig): string {
  const parts: string[] = []
  parts.push(`Temp ${cfg.temperature}`)
  parts.push(`${cfg.maxTokens} tokens`)
  if (cfg.useRag) parts.push('RAG 开')
  return parts.join(' / ')
}

export interface ProjectAgentConfig {
  projectId: string
  agentId: string
  agentName: string
  agentCode: string
  agentType: string
  agentStatus: string
  agentDescription: string | null
  enabled: boolean
  projectAgentConfigId: string | null
  agentVersionId: string | null
  agentVersionNo: string | null
  modelConfigId: string | null
  modelProvider: string | null
  modelName: string | null
  configJson: string | null
  config: ProjectAgentRuntimeConfig | null
  updateTime: string | null
}

export interface EnableProjectAgentPayload {
  agentVersionId?: string
  modelConfigId?: string
  config?: ProjectAgentRuntimeConfig
}

export function listProjectAgents(projectId: string) {
  return client.get<ApiResponse<ProjectAgentConfig[]>>(`/api/projects/${projectId}/agents`)
}

export function enableProjectAgent(projectId: string, agentId: string, payload: EnableProjectAgentPayload) {
  return client.post<ApiResponse<boolean>>(`/api/projects/${projectId}/agents/${agentId}/enable`, payload)
}

export function disableProjectAgent(projectId: string, agentId: string) {
  return client.post<ApiResponse<boolean>>(`/api/projects/${projectId}/agents/${agentId}/disable`)
}

export interface AgentVersion {
  id: string
  agentId: string
  versionNo: string
  status: string
  systemPrompt: string
  toolPolicy: string
  executionPolicy: string
  publishTime: string | null
  createTime: string
}

export function getAgentVersions(agentId: string) {
  return client.get<ApiResponse<AgentVersion[]>>(`/api/agents/${agentId}/versions`)
}

export function getAgentVersion(agentId: string, versionId: string) {
  return client.get<ApiResponse<AgentVersion>>(`/api/agents/${agentId}/versions/${versionId}`)
}
