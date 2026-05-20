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
  agentVersionId?: string
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
  agentVersionId: string | null
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

// ========================
// Multi-Agent Orchestration
// ========================

export interface MultiAgentStepResponse {
  id: string
  runId: string
  phaseId: string | null
  phaseOrder: number | null
  laneKey: string | null
  stepOrder: number
  stepType: string
  status: string
  agentId: string | null
  agentName: string | null
  agentExecutionId: string | null
  inputContext: string | null
  outputContent: string | null
  errorMessage: string | null
  startedAt: string | null
  finishedAt: string | null
  createTime: string
}

export interface MultiAgentPhaseResponse {
  id: string
  runId: string
  phaseOrder: number
  phaseKey: string
  title: string
  status: string
  inputSummary: string | null
  outputSummary: string | null
  startedAt: string | null
  finishedAt: string | null
  steps: MultiAgentStepResponse[]
}

export interface MultiAgentMessageResponse {
  id: string
  runId: string
  fromStepId: string | null
  toStepId: string | null
  fromAgentId: string | null
  toAgentId: string | null
  messageType: string
  content: string
  summary: string | null
  createTime: string
}

export interface MultiAgentApprovalGateResponse {
  id: string
  runId: string
  phaseId: string | null
  gateKey: string
  title: string
  description: string | null
  status: string
  requestedBy: string | null
  decidedBy: string | null
  decisionComment: string | null
  requestedAt: string | null
  decidedAt: string | null
}

export interface MultiAgentApprovalDecisionRequest {
  comment?: string
}

export interface MultiAgentRunResponse {
  id: string
  projectId: string
  taskId: string
  status: string
  strategy: string
  strategyKey: string
  strategyName: string
  strategyDescription: string
  title: string | null
  inputSummary: string | null
  finalSummary: string | null
  errorMessage: string | null
  startedAt: string | null
  finishedAt: string | null
  createTime: string
  updateTime: string
  phases: MultiAgentPhaseResponse[]
  steps: MultiAgentStepResponse[]
  messages: MultiAgentMessageResponse[]
  approvalGates?: MultiAgentApprovalGateResponse[]
  pendingApprovalGate?: MultiAgentApprovalGateResponse | null
}

// ========================
// Workflow Strategy Catalog
// ========================

export interface WorkflowStepTemplateResponse {
  stepOrder: number
  stepType: string
  agentCode: string
  laneKey: string
  title: string
}

export interface WorkflowPhaseTemplateResponse {
  phaseOrder: number
  phaseKey: string
  title: string
  steps: WorkflowStepTemplateResponse[]
}

export interface WorkflowStrategyResponse {
  strategyKey: string
  name: string
  description: string
  phaseCount: number
  stepCount: number
  phases: WorkflowPhaseTemplateResponse[]
}

export function getMultiAgentStrategies() {
  return client.get<ApiResponse<WorkflowStrategyResponse[]>>('/api/multi-agent-strategies')
}

export interface StartMultiAgentRunRequest {
  strategy?: string
  instruction?: string
  useRag?: boolean
  knowledgeBaseId?: string
}

export function startMultiAgentRun(taskId: string, data?: StartMultiAgentRunRequest) {
  return client.post<ApiResponse<MultiAgentRunResponse>>(`/api/tasks/${taskId}/multi-agent-runs`, data || {})
}

export function listMultiAgentRuns(taskId: string) {
  return client.get<ApiResponse<MultiAgentRunResponse[]>>(`/api/tasks/${taskId}/multi-agent-runs`)
}

export function getMultiAgentRun(runId: string) {
  return client.get<ApiResponse<MultiAgentRunResponse>>(`/api/multi-agent-runs/${runId}`)
}

export function getMultiAgentRunMessages(runId: string) {
  return client.get<ApiResponse<MultiAgentMessageResponse[]>>(`/api/multi-agent-runs/${runId}/messages`)
}

export function getMultiAgentRunPhases(runId: string) {
  return client.get<ApiResponse<MultiAgentPhaseResponse[]>>(`/api/multi-agent-runs/${runId}/phases`)
}

// ========================
// Multi-Agent Approval Gates
// ========================

export function getMultiAgentApprovalGates(runId: string) {
  return client.get<ApiResponse<MultiAgentApprovalGateResponse[]>>(`/api/multi-agent-runs/${runId}/approval-gates`)
}

export function approveMultiAgentGate(runId: string, gateId: string, payload?: MultiAgentApprovalDecisionRequest) {
  return client.post<ApiResponse<MultiAgentRunResponse>>(`/api/multi-agent-runs/${runId}/approval-gates/${gateId}/approve`, payload || {})
}

export function rejectMultiAgentGate(runId: string, gateId: string, payload?: MultiAgentApprovalDecisionRequest) {
  return client.post<ApiResponse<MultiAgentRunResponse>>(`/api/multi-agent-runs/${runId}/approval-gates/${gateId}/reject`, payload || {})
}
