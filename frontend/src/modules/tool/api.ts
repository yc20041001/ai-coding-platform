import client from '@/shared/api/client'
import type { ApiResponse } from '@/shared/api/types'

export interface ToolParameterGroup {
  key: string
  title: string
  description?: string
  fields: string[]
}

export interface ToolParameterDependsOn {
  field: string
  equals: string | number | boolean
}

export interface ToolParameterPathRules {
  deny?: string[]
  allowPrefixes?: string[]
}

export interface ToolParameterField {
  key: string
  label: string
  type: 'text' | 'textarea' | 'boolean' | 'number' | 'select' | 'array'
  itemType?: 'text'
  required?: boolean
  defaultValue?: string | number | boolean | string[]
  options?: string[]
  min?: number
  max?: number
  maxLength?: number
  maxItems?: number
  itemMaxLength?: number
  dependsOn?: ToolParameterDependsOn
  pathRules?: ToolParameterPathRules
}

export interface ToolParameterSchema {
  schemaVersion?: number
  groups?: ToolParameterGroup[]
  fields: ToolParameterField[]
}

export interface ToolCatalog {
  id: string
  toolKey: string
  name: string
  description: string | null
  toolType: string
  riskLevel: string
  executionMode: string
  enabled: boolean
  builtIn: boolean
  policyJson: string | null
  parameterSchemaJson: string | null
  createTime: string
  updateTime: string
}

export interface ProjectToolConfig {
  id: string | null
  projectId: string
  toolId: string
  toolKey: string
  name: string
  description: string | null
  toolType: string
  riskLevel: string
  executionMode: string
  globalEnabled: boolean
  projectEnabled: boolean
  configJson: string | null
  parameterSchemaJson: string | null
  parametersJson: string | null
  createTime: string | null
  updateTime: string | null
}

export function listToolCatalog(params?: {
  toolType?: string
  riskLevel?: string
  enabled?: boolean
}) {
  return client.get<ApiResponse<ToolCatalog[]>>('/api/tool-catalog', { params })
}

export function listProjectTools(projectId: string) {
  return client.get<ApiResponse<ProjectToolConfig[]>>(`/api/projects/${projectId}/tools`)
}

export function enableProjectTool(projectId: string, toolId: string, data?: { parameters?: Record<string, unknown> }) {
  return client.post<ApiResponse<ProjectToolConfig>>(`/api/projects/${projectId}/tools/${toolId}/enable`, data || {})
}

export function disableProjectTool(projectId: string, toolId: string) {
  return client.post<ApiResponse<ProjectToolConfig>>(`/api/projects/${projectId}/tools/${toolId}/disable`)
}
