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

export interface AgentDetail {
  id: string
  name: string
  code: string
  type: string
  status: string
  description: string
  version: string
  createTime: string
}

export async function listAgents(params?: { type?: string; status?: string }) {
  return client.get<ApiResponse<AgentItem[]>>('/api/agents', { params })
}

export async function getAgentDetail(agentId: string) {
  return client.get<ApiResponse<AgentDetail>>(`/api/agents/${agentId}`)
}
