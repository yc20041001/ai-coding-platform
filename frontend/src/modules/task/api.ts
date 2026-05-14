import client from '@/shared/api/client'
import type { ApiResponse, PageResult } from '@/shared/api/types'

export interface TaskItem {
  id: string
  projectId: string
  title: string
  description: string
  taskType: string
  priority: string
  status: string
  agentId: string
  createTime: string
}

export interface TaskLog {
  id: string
  level: string
  stage: string
  message: string
  createTime: string
}

export interface TaskArtifact {
  id: string
  name: string
  artifactType: string
  content: string
  createTime: string
}

export interface CreateTaskRequest {
  title: string
  description: string
  taskType: string
  priority: string
  agentId: string
}

export interface ExecuteTaskRequest {
  instruction: string
  agentId: string
  useRag: boolean
  ragLimit: number
}

export async function listTasks(projectId: string, page: number, pageSize: number) {
  return client.get<ApiResponse<PageResult<TaskItem>>>(`/api/projects/${projectId}/tasks`, {
    params: { page, pageSize },
  })
}

export async function createTask(projectId: string, data: CreateTaskRequest) {
  return client.post<ApiResponse<TaskItem>>(`/api/projects/${projectId}/tasks`, data)
}

export async function executeTask(taskId: string, data: ExecuteTaskRequest) {
  return client.post<ApiResponse<{ id: string; status: string }>>(`/api/tasks/${taskId}/execute`, data)
}

export async function getTaskLogs(taskId: string) {
  return client.get<ApiResponse<TaskLog[]>>(`/api/tasks/${taskId}/logs`)
}

export async function getTaskArtifacts(taskId: string) {
  return client.get<ApiResponse<TaskArtifact[]>>(`/api/tasks/${taskId}/artifacts`)
}

export async function getTaskDetail(taskId: string) {
  return client.get<ApiResponse<TaskDetail>>(`/api/tasks/${taskId}`)
}

export interface TaskDetail {
  id: string
  projectId: string
  title: string
  description: string
  taskType: string
  agentId: string
  agentName: string
  creatorId: string
  creatorName: string
  assigneeId: string | null
  status: string
  priority: string
  sourceType: string | null
  sourceId: string | null
  branch: string | null
  retryCount: number
  maxRetryCount: number
  errorMessage: string | null
  createTime: string
  startTime: string | null
  endTime: string | null
}

export interface AgentExecution {
  id: string
  projectId: string
  taskId: string
  chatSessionId: string | null
  chatMessageId: string | null
  agentId: string
  agentName: string
  executionType: string
  status: string
  inputPrompt: string
  outputContent: string
  errorMessage: string | null
  tokenUsage: number
  startedAt: string
  finishedAt: string | null
  createTime: string
  ragUsed: boolean
}

export interface ModelRequestLog {
  id: string
  projectId: string
  executionId: string
  provider: string
  modelName: string
  requestType: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  latencyMs: number
  success: boolean
  errorMessage: string | null
  createTime: string
}

export async function startTask(taskId: string) {
  return client.post<ApiResponse<TaskItem>>(`/api/tasks/${taskId}/start`)
}

export async function cancelTask(taskId: string, reason: string) {
  return client.post<ApiResponse<TaskItem>>(`/api/tasks/${taskId}/cancel`, { reason })
}

export async function retryTask(taskId: string) {
  return client.post<ApiResponse<TaskItem>>(`/api/tasks/${taskId}/retry`)
}

export async function getTaskExecutions(taskId: string, page = 1, pageSize = 10) {
  return client.get<ApiResponse<PageResult<AgentExecution>>>(`/api/tasks/${taskId}/executions`, {
    params: { page, pageSize },
  })
}

export async function getAgentExecution(executionId: string) {
  return client.get<ApiResponse<AgentExecution>>(`/api/agent-executions/${executionId}`)
}

export async function getExecutionModelLogs(executionId: string) {
  return client.get<ApiResponse<ModelRequestLog[]>>(`/api/agent-executions/${executionId}/model-logs`)
}
