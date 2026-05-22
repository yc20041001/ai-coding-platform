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

export interface ToolExecutionApprovalResponse {
  id: string
  projectId: string
  taskId: string | null
  runId: string | null
  stepId: string | null
  toolExecutionId: string
  toolId: string | null
  toolKey: string
  approvalKey: string
  title: string
  description: string | null
  riskLevel: string
  status: string
  requestedBy: string | null
  decidedBy: string | null
  decisionComment: string | null
  requestedAt: string | null
  decidedAt: string | null
  expiresAt: string | null
  createTime: string
}

export interface ToolSandboxExecutionResponse {
  id: string
  projectId: string
  taskId: string | null
  runId: string | null
  phaseId: string | null
  stepId: string | null
  agentId: string | null
  toolName: string
  toolType: string
  executionMode: string
  status: string
  inputPayload: string | null
  outputPayload: string | null
  summary: string | null
  errorMessage: string | null
  startedAt: string | null
  finishedAt: string | null
  durationMs: number
  createTime: string
  approval?: ToolExecutionApprovalResponse
  requiresApproval?: boolean
  artifactId?: string | null
  artifactName?: string | null
  artifactType?: string | null
  jobId?: string | null
  job?: ToolExecutionJob | null
}

// ========================
// Tool Execution Job
// ========================

export interface ToolExecutionJob {
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
  requestPayload: string | null
  resultPayload: string | null
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
  updateTime: string | null
}

export interface RetryToolExecutionJobRequest {
  reason?: string
}

export function getToolExecutionJob(jobId: string) {
  return client.get<ApiResponse<ToolExecutionJob>>(`/api/tool-execution-jobs/${jobId}`)
}

export function getToolExecutionJobsByExecution(executionId: string) {
  return client.get<ApiResponse<ToolExecutionJob[]>>(`/api/tool-sandbox-executions/${executionId}/jobs`)
}

export function getToolExecutionJobsByRun(runId: string) {
  return client.get<ApiResponse<ToolExecutionJob[]>>(`/api/multi-agent-runs/${runId}/tool-execution-jobs`)
}

export function retryToolExecutionJob(jobId: string, data?: RetryToolExecutionJobRequest) {
  return client.post<ApiResponse<ToolExecutionJob>>(`/api/tool-execution-jobs/${jobId}/retry`, data || {})
}

export function cancelToolExecutionJob(jobId: string) {
  return client.post<ApiResponse<ToolExecutionJob>>(`/api/tool-execution-jobs/${jobId}/cancel`)
}

// ========================
// 37C: DLQ / Retry Backoff
// ========================

export function listFailedToolExecutionJobs(projectId: string, status?: string) {
  return client.get<ApiResponse<ToolExecutionJob[]>>(`/api/projects/${projectId}/tool-execution-jobs/failed`, {
    params: status ? { status } : {},
  })
}

export function manualRetryToolExecutionJob(jobId: string, reason?: string) {
  return client.post<ApiResponse<ToolExecutionJob>>(`/api/tool-execution-jobs/${jobId}/manual-retry`, reason ? { reason } : {})
}

export function recoverTimedOutToolExecutionJobs() {
  return client.post<ApiResponse<number>>('/api/tool-execution-jobs/recover-timeouts')
}

export function dispatchRetryToolExecutionJobs() {
  return client.post<ApiResponse<number>>('/api/tool-execution-jobs/dispatch-retries')
}

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
  toolExecutions?: ToolSandboxExecutionResponse[]
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
  toolExecutions?: ToolSandboxExecutionResponse[]
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

// ========================
// Tool Sandbox Execution
// ========================

export function getMultiAgentRunToolExecutions(runId: string) {
  return client.get<ApiResponse<ToolSandboxExecutionResponse[]>>(`/api/multi-agent-runs/${runId}/tool-executions`)
}

export function getMultiAgentStepToolExecutions(stepId: string) {
  return client.get<ApiResponse<ToolSandboxExecutionResponse[]>>(`/api/multi-agent-steps/${stepId}/tool-executions`)
}

export function getToolSandboxExecution(executionId: string) {
  return client.get<ApiResponse<ToolSandboxExecutionResponse>>(`/api/tool-sandbox-executions/${executionId}`)
}

// ========================
// Tool Execution Approval
// ========================

export function getToolExecutionApproval(executionId: string) {
  return client.get<ApiResponse<ToolExecutionApprovalResponse>>(`/api/tool-sandbox-executions/${executionId}/approval`)
}

export function approveToolExecution(executionId: string, comment?: string) {
  return client.post<ApiResponse<ToolSandboxExecutionResponse>>(`/api/tool-sandbox-executions/${executionId}/approve`, { comment })
}

export function rejectToolExecution(executionId: string, comment?: string) {
  return client.post<ApiResponse<ToolSandboxExecutionResponse>>(`/api/tool-sandbox-executions/${executionId}/reject`, { comment })
}

export function listProjectToolApprovals(projectId: string, status?: string) {
  return client.get<ApiResponse<ToolExecutionApprovalResponse[]>>(`/api/projects/${projectId}/tool-approvals`, { params: { status } })
}

// ========================
// Patch Proposal Review (36H)
// ========================

export interface PatchProposalReview {
  id: string
  projectId: string
  taskId: string
  artifactId: string
  toolExecutionId: string | null
  status: string
  decision: string | null
  reviewerId: string | null
  reviewComment: string | null
  reviewedAt: string | null
  safetyConfirmed: boolean
  checklistJson: string | null
  createTime: string
  updateTime: string
}

export interface PatchProposalReviewDecisionRequest {
  decision: string
  comment?: string
  safetyConfirmed: boolean
  checklist?: Record<string, unknown>
}

export function getPatchProposalReview(artifactId: string) {
  return client.get<ApiResponse<PatchProposalReview>>(`/api/task-artifacts/${artifactId}/patch-review`)
}

export function submitPatchProposalReviewDecision(artifactId: string, data: PatchProposalReviewDecisionRequest) {
  return client.post<ApiResponse<PatchProposalReview>>(`/api/task-artifacts/${artifactId}/patch-review/decision`, data)
}

export function listTaskPatchReviews(taskId: string) {
  return client.get<ApiResponse<PatchProposalReview[]>>(`/api/tasks/${taskId}/patch-reviews`)
}

// ========================
// Tool Execution Trace (37F)
// ========================

export interface ToolExecutionTraceEvent {
  eventType: string
  title: string
  description?: string
  status?: string
  eventTime?: string
  metadata?: Record<string, unknown>
}

export interface ToolExecutionFileEvidence {
  path: string
  reason?: string
  sizeBytes?: number
  lineStart?: number
  lineEnd?: number
  redacted?: boolean
  truncated?: boolean
}

export interface ToolExecutionArtifactEvidence {
  artifactId: string
  artifactType: string
  title?: string
  patchReviewStatus?: string
  patchReviewDecision?: string
  createTime?: string
}

export interface ToolExecutionApprovalEvidence {
  approvalId: string
  status: string
  approverId?: string
  approverName?: string
  comment?: string
  createTime?: string
  decidedAt?: string
}

export interface ToolExecutionJobEvidence {
  jobId: string
  status: string
  priority?: string
  attemptCount?: number
  errorCode?: string
  failureStage?: string
  nextRetryAt?: string
  deadLetteredAt?: string
  deadLetterReason?: string
  sourceJobId?: string
  createTime?: string
  startedAt?: string
  finishedAt?: string
}

export interface ToolExecutionEvidence {
  filesReadCount: number
  skippedFilesCount: number
  redacted: boolean
  truncated: boolean
  binarySkipped?: boolean
  pathSandboxApplied?: boolean
  sensitiveDenylistApplied?: boolean
  filesRead?: ToolExecutionFileEvidence[]
  skippedFiles?: ToolExecutionFileEvidence[]
  artifacts?: ToolExecutionArtifactEvidence[]
}

export interface ToolExecutionTrace {
  executionId: string
  projectId: string
  taskId?: string
  runId?: string
  stepId?: string
  toolKey: string
  toolName?: string
  riskLevel?: string
  status: string
  mode?: string
  readOnly?: boolean
  policyAllowed?: boolean
  policyReason?: string
  inputPayload?: string
  outputPayload?: string
  approval?: ToolExecutionApprovalEvidence
  job?: ToolExecutionJobEvidence
  evidence?: ToolExecutionEvidence
  events: ToolExecutionTraceEvent[]
  createTime?: string
  updateTime?: string
}

export function getToolExecutionTrace(executionId: string) {
  return client.get<ApiResponse<ToolExecutionTrace>>(`/api/tool-sandbox-executions/${executionId}/trace`)
}

export function getMultiAgentRunToolExecutionTraces(runId: string) {
  return client.get<ApiResponse<ToolExecutionTrace[]>>(`/api/multi-agent-runs/${runId}/tool-execution-traces`)
}

export function getTaskToolExecutionTraces(taskId: string) {
  return client.get<ApiResponse<ToolExecutionTrace[]>>(`/api/tasks/${taskId}/tool-execution-traces`)
}

// ========================
// Tool Execution Audit Export (37G)
// ========================

export interface ToolAuditExport {
  targetType: string
  targetId: string
  fileName: string
  contentType: string
  markdown: string
  traceCount: number
  redacted: boolean
  truncated: boolean
  generatedAt: string
}

export function exportExecutionAudit(executionId: string) {
  return client.get<ApiResponse<ToolAuditExport>>(`/api/orchestration/executions/${executionId}/audit-export`)
}

export function exportRunEvidence(runId: string) {
  return client.get<ApiResponse<ToolAuditExport>>(`/api/orchestration/runs/${runId}/evidence-export`)
}

export function exportTaskToolAudit(taskId: string) {
  return client.get<ApiResponse<ToolAuditExport>>(`/api/orchestration/tasks/${taskId}/tool-audit-export`)
}

// ========================
// Tool Operator Review (37G)
// ========================

export interface ToolOperatorReview {
  id: string
  projectId: string
  taskId: string | null
  runId: string | null
  toolExecutionId: string | null
  toolJobId: string | null
  reviewTargetType: string
  reviewTargetId: string
  status: string
  severity: string
  title: string
  summary: string | null
  resolution: string | null
  assigneeId: string | null
  createdBy: string | null
  resolvedBy: string | null
  createTime: string
  updateTime: string
  resolvedAt: string | null
}

export interface CreateToolOperatorReviewRequest {
  reviewTargetType: string
  reviewTargetId: string
  severity: string
  title: string
  summary?: string
  assigneeId?: string
}

export interface UpdateToolOperatorReviewRequest {
  status?: string
  severity?: string
  title?: string
  summary?: string
  resolution?: string
  assigneeId?: string
}

export function createOperatorReview(data: CreateToolOperatorReviewRequest) {
  return client.post<ApiResponse<ToolOperatorReview>>('/api/orchestration/operator-reviews', data)
}

export function updateOperatorReview(id: string, data: UpdateToolOperatorReviewRequest) {
  return client.put<ApiResponse<ToolOperatorReview>>(`/api/orchestration/operator-reviews/${id}`, data)
}

export function getOperatorReview(id: string) {
  return client.get<ApiResponse<ToolOperatorReview>>(`/api/orchestration/operator-reviews/${id}`)
}

export function listProjectOperatorReviews(projectId: string, params?: { status?: string; severity?: string; page?: number; pageSize?: number; sort?: string }) {
  return client.get<ApiResponse<PageResult<ToolOperatorReview>>>(`/api/projects/${projectId}/operator-reviews`, { params })
}

export function listTargetOperatorReviews(targetType: string, targetId: string) {
  return client.get<ApiResponse<ToolOperatorReview[]>>('/api/orchestration/operator-reviews/by-target', {
    params: { targetType, targetId },
  })
}
