import client from '@/shared/api/client'
import type { ApiResponse } from '@/shared/api/types'
import type { WorkflowStrategyResponse } from '@/modules/task/api'

export interface WorkflowTemplateResponse {
  id: string
  templateKey: string
  name: string
  description?: string
  category: string
  status: 'ENABLED' | 'DISABLED'
  builtIn: boolean
  templateJson: string
  strategy?: WorkflowStrategyResponse
  phaseCount?: number
  stepCount?: number
  createTime?: string
  updateTime?: string
}

export function listWorkflowTemplates(status?: string) {
  return client.get<ApiResponse<WorkflowTemplateResponse[]>>('/api/workflow-templates', {
    params: status ? { status } : {},
  })
}

export function getWorkflowTemplate(templateId: string) {
  return client.get<ApiResponse<WorkflowTemplateResponse>>(`/api/workflow-templates/${templateId}`)
}

export function updateWorkflowTemplateStatus(templateId: string, status: string) {
  return client.put<ApiResponse<WorkflowTemplateResponse>>(`/api/workflow-templates/${templateId}/status`, { status })
}
