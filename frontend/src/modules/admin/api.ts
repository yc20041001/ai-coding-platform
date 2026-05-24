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

// ========================
// Tool Incidents (37H)
// ========================

export interface ToolIncidentItem {
  id: string
  projectId: string
  taskId: string | null
  runId: string | null
  toolExecutionId: string | null
  toolJobId: string | null
  operatorReviewId: string | null
  sourceType: string
  sourceId: string | null
  severity: string
  status: string
  title: string
  summary: string | null
  resolution: string | null
  assigneeId: string | null
  createdBy: string | null
  acknowledgedBy: string | null
  resolvedBy: string | null
  firstSeenAt: string
  lastSeenAt: string
  acknowledgedAt: string | null
  resolvedAt: string | null
  slaMinutes: number | null
  dueAt: string | null
  breachedAt: string | null
  slaStatus: string | null
  escalationLevel: number | null
  createTime: string
  updateTime: string
}

export interface ToolIncidentSummary {
  openCount: number
  acknowledgedCount: number
  resolvedCount: number
  criticalCount: number
  highCount: number
  deadLetteredCount: number
  retryPendingCount: number
}

export interface CreateToolIncidentRequest {
  projectId: string
  sourceType: string
  sourceId?: string
  severity: string
  title: string
  summary?: string
  assigneeId?: string
  toolExecutionId?: string
  toolJobId?: string
  operatorReviewId?: string
}

export interface UpdateToolIncidentRequest {
  status?: string
  severity?: string
  title?: string
  summary?: string
  resolution?: string
  assigneeId?: string
}

export function createIncident(data: CreateToolIncidentRequest) {
  return client.post<ApiResponse<ToolIncidentItem>>('/api/orchestration/incidents', data)
}

export function updateIncident(id: string, data: UpdateToolIncidentRequest) {
  return client.put<ApiResponse<ToolIncidentItem>>(`/api/orchestration/incidents/${id}`, data)
}

export function getIncident(id: string) {
  return client.get<ApiResponse<ToolIncidentItem>>(`/api/orchestration/incidents/${id}`)
}

export function listProjectIncidents(projectId: string, params?: { status?: string; severity?: string; page?: number; pageSize?: number }) {
  return client.get<ApiResponse<PageResult<ToolIncidentItem>>>(`/api/projects/${projectId}/incidents`, { params })
}

export function getProjectIncidentSummary(projectId: string) {
  return client.get<ApiResponse<ToolIncidentSummary>>(`/api/projects/${projectId}/incidents/summary`)
}

export function syncProblemJobs(projectId: string) {
  return client.post<ApiResponse<Record<string, number>>>(`/api/projects/${projectId}/incidents/sync-problem-jobs`)
}

// ========================
// Tool Alert Rules (37H)
// ========================

export interface ToolAlertRuleItem {
  id: string
  projectId: string
  name: string
  enabled: boolean
  sourceType: string
  minSeverity: string
  channel: string
  routeTarget: string | null
  configJson: string | null
  createTime: string
  updateTime: string
}

export interface CreateToolAlertRuleRequest {
  projectId: string
  name: string
  sourceType: string
  minSeverity: string
  channel: string
  routeTarget?: string
  configJson?: string
}

export interface UpdateToolAlertRuleRequest {
  name?: string
  enabled?: boolean
  sourceType?: string
  minSeverity?: string
  channel?: string
  routeTarget?: string
  configJson?: string
}

export function createAlertRule(data: CreateToolAlertRuleRequest) {
  return client.post<ApiResponse<ToolAlertRuleItem>>('/api/orchestration/alert-rules', data)
}

export function updateAlertRule(id: string, data: UpdateToolAlertRuleRequest) {
  return client.put<ApiResponse<ToolAlertRuleItem>>(`/api/orchestration/alert-rules/${id}`, data)
}

export function listProjectAlertRules(projectId: string) {
  return client.get<ApiResponse<ToolAlertRuleItem[]>>(`/api/projects/${projectId}/alert-rules`)
}

// ========================
// Tool Alert Deliveries (37H)
// ========================

export interface ToolAlertDeliveryItem {
  id: string
  incidentId: string
  projectId: string
  ruleId: string
  channel: string
  routeTarget: string | null
  status: string
  payload: string | null
  errorMessage: string | null
  deliveredAt: string | null
  createTime: string
  updateTime: string
}

export function listProjectAlertDeliveries(projectId: string) {
  return client.get<ApiResponse<ToolAlertDeliveryItem[]>>(`/api/projects/${projectId}/alert-deliveries`)
}

export function listIncidentAlertDeliveries(incidentId: string) {
  return client.get<ApiResponse<ToolAlertDeliveryItem[]>>(`/api/orchestration/incidents/${incidentId}/alert-deliveries`)
}

export function retryAlertDelivery(id: string) {
  return client.post<ApiResponse<ToolAlertDeliveryItem>>(`/api/orchestration/alert-deliveries/${id}/retry`)
}

// ========================
// Tool SLA & Escalation (37I)
// ========================

export interface ToolIncidentSlaScanResult {
  scanned: number
  withinSla: number
  atRisk: number
  breached: number
  resolved: number
}

export interface ToolIncidentEscalationScanResult {
  scanned: number
  escalated: number
  skipped: number
  maxLevelReached: number
}

export interface ToolEscalationEventItem {
  id: string
  incidentId: string
  projectId: string
  policyId: string
  escalationLevel: number
  severity: string
  channel: string
  routeTarget: string | null
  status: string
  reason: string | null
  createTime: string
  updateTime: string
}

export interface ToolEscalationPolicyItem {
  id: string
  projectId: string
  name: string
  enabled: boolean
  severity: string
  slaMinutes: number | null
  escalationAfterMinutes: number | null
  maxEscalationLevel: number
  channel: string
  routeTarget: string | null
  createTime: string
  updateTime: string
}

export interface CreateToolEscalationPolicyRequest {
  projectId: string
  name: string
  severity: string
  slaMinutes?: number
  escalationAfterMinutes?: number
  maxEscalationLevel?: number
  channel?: string
  routeTarget?: string
}

export interface UpdateToolEscalationPolicyRequest {
  name?: string
  enabled?: boolean
  severity?: string
  slaMinutes?: number
  escalationAfterMinutes?: number
  maxEscalationLevel?: number
  channel?: string
  routeTarget?: string
}

export interface ToolIncidentTimelineEvent {
  eventType: string
  title: string
  description: string
  status: string
  eventTime: string
}

export interface ToolIncidentTimeline {
  incidentId: string
  events: ToolIncidentTimelineEvent[]
}

export function scanIncidentSla(projectId: string) {
  return client.post<ApiResponse<ToolIncidentSlaScanResult>>(`/api/projects/${projectId}/incident-sla/scan`)
}

export function scanIncidentEscalation(projectId: string) {
  return client.post<ApiResponse<ToolIncidentEscalationScanResult>>(`/api/projects/${projectId}/incident-escalation/scan`)
}

export function escalateIncident(incidentId: string, reason?: string) {
  return client.post<ApiResponse<ToolEscalationEventItem>>(`/api/orchestration/incidents/${incidentId}/escalate`, reason ? { reason } : {})
}

export function listIncidentEscalationEvents(incidentId: string) {
  return client.get<ApiResponse<ToolEscalationEventItem[]>>(`/api/orchestration/incidents/${incidentId}/escalation-events`)
}

export function getIncidentTimeline(incidentId: string) {
  return client.get<ApiResponse<ToolIncidentTimeline>>(`/api/orchestration/incidents/${incidentId}/timeline`)
}

export function listProjectEscalationPolicies(projectId: string) {
  return client.get<ApiResponse<ToolEscalationPolicyItem[]>>(`/api/projects/${projectId}/escalation-policies`)
}

export function getEscalationPolicy(policyId: string) {
  return client.get<ApiResponse<ToolEscalationPolicyItem>>(`/api/orchestration/escalation-policies/${policyId}`)
}

export function createEscalationPolicy(data: CreateToolEscalationPolicyRequest) {
  return client.post<ApiResponse<ToolEscalationPolicyItem>>('/api/orchestration/escalation-policies', data)
}

export function updateEscalationPolicy(id: string, data: UpdateToolEscalationPolicyRequest) {
  return client.put<ApiResponse<ToolEscalationPolicyItem>>(`/api/orchestration/escalation-policies/${id}`, data)
}

export function deleteEscalationPolicy(id: string) {
  return client.delete<ApiResponse<void>>(`/api/orchestration/escalation-policies/${id}`)
}

// ========================
// Incident Root Cause Notes (37J)
// ========================

export interface IncidentRootCauseNoteItem {
  id: string
  projectId: string
  incidentId: string
  rootCause: string | null
  impact: string | null
  resolution: string | null
  prevention: string | null
  followUpActions: string | null
  tags: string | null
  confidence: string
  status: string
  authorId: string | null
  lastEditorId: string | null
  publishedAt: string | null
  createTime: string
  updateTime: string
}

export interface CreateIncidentRootCauseNoteRequest {
  rootCause?: string
  impact?: string
  resolution?: string
  prevention?: string
  followUpActions?: string
  tags?: string
  confidence?: string
}

export interface UpdateIncidentRootCauseNoteRequest {
  rootCause?: string
  impact?: string
  resolution?: string
  prevention?: string
  followUpActions?: string
  tags?: string
  confidence?: string
  status?: string
}

export function createRootCauseNote(incidentId: string, data: CreateIncidentRootCauseNoteRequest) {
  return client.post<ApiResponse<IncidentRootCauseNoteItem>>(`/api/orchestration/incidents/${incidentId}/root-cause-note`, data)
}

export function updateRootCauseNote(noteId: string, data: UpdateIncidentRootCauseNoteRequest) {
  return client.put<ApiResponse<IncidentRootCauseNoteItem>>(`/api/orchestration/incident-root-cause-notes/${noteId}`, data)
}

export function getIncidentRootCauseNote(incidentId: string) {
  return client.get<ApiResponse<IncidentRootCauseNoteItem>>(`/api/orchestration/incidents/${incidentId}/root-cause-note`)
}

export function listProjectRootCauseNotes(projectId: string, params?: { status?: string; page?: number; pageSize?: number }) {
  return client.get<ApiResponse<PageResult<IncidentRootCauseNoteItem>>>(`/api/projects/${projectId}/incident-root-cause-notes`, { params })
}

export function exportRootCauseNoteMarkdown(noteId: string) {
  return client.get<ApiResponse<string>>(`/api/orchestration/incident-root-cause-notes/${noteId}/markdown`)
}

// ========================
// Known Issue Templates (37J)
// ========================

export interface KnownIssueTemplateItem {
  id: string
  projectId: string
  title: string
  category: string | null
  severity: string | null
  rootCauseTemplate: string | null
  impactTemplate: string | null
  resolutionTemplate: string | null
  preventionTemplate: string | null
  tags: string | null
  enabled: boolean
  createTime: string
  updateTime: string
}

export interface CreateKnownIssueTemplateRequest {
  title: string
  category?: string
  severity?: string
  rootCauseTemplate?: string
  impactTemplate?: string
  resolutionTemplate?: string
  preventionTemplate?: string
  tags?: string
}

export interface UpdateKnownIssueTemplateRequest {
  title?: string
  category?: string
  severity?: string
  rootCauseTemplate?: string
  impactTemplate?: string
  resolutionTemplate?: string
  preventionTemplate?: string
  tags?: string
  enabled?: boolean
}

export function createKnownIssueTemplate(projectId: string, data: CreateKnownIssueTemplateRequest) {
  return client.post<ApiResponse<KnownIssueTemplateItem>>(`/api/projects/${projectId}/known-issue-templates`, data)
}

export function updateKnownIssueTemplate(templateId: string, data: UpdateKnownIssueTemplateRequest) {
  return client.put<ApiResponse<KnownIssueTemplateItem>>(`/api/orchestration/known-issue-templates/${templateId}`, data)
}

export function listProjectKnownIssueTemplates(projectId: string, params?: { category?: string; enabled?: boolean }) {
  return client.get<ApiResponse<KnownIssueTemplateItem[]>>(`/api/projects/${projectId}/known-issue-templates`, { params })
}

export function applyKnownIssueTemplate(incidentId: string, templateId: string) {
  return client.post<ApiResponse<IncidentRootCauseNoteItem>>(`/api/orchestration/incidents/${incidentId}/apply-known-issue-template/${templateId}`)
}

// ========================
// Incident Knowledge Links (37J)
// ========================

export interface IncidentKnowledgeLinkItem {
  id: string
  projectId: string
  incidentId: string
  rootCauseNoteId: string | null
  knowledgeBaseId: string | null
  knowledgeDocumentId: string | null
  linkType: string
  title: string | null
  createTime: string
}

export interface GenerateKnowledgeDocumentRequest {
  knowledgeBaseId: string
  title?: string
  includeTimeline?: boolean
  includeTraceSummary?: boolean
  includeOperatorReview?: boolean
  includeEscalation?: boolean
}

export interface IncidentKnowledgeDocumentDraft {
  documentId: string
  title: string
  status: string
  knowledgeBaseId: string
  knowledgeBaseName: string | null
  createTime: string
}

export function generateKnowledgeDocument(incidentId: string, data: GenerateKnowledgeDocumentRequest) {
  return client.post<ApiResponse<IncidentKnowledgeDocumentDraft>>(`/api/orchestration/incidents/${incidentId}/knowledge-document`, data)
}

export function listIncidentKnowledgeLinks(incidentId: string) {
  return client.get<ApiResponse<IncidentKnowledgeLinkItem[]>>(`/api/orchestration/incidents/${incidentId}/knowledge-links`)
}

export function deleteKnowledgeLink(linkId: string) {
  return client.delete<ApiResponse<void>>(`/api/orchestration/incident-knowledge-links/${linkId}`)
}

// ========================
// Similar Incident Search (37J)
// ========================

export interface SimilarIncidentItem {
  incidentId: string
  title: string
  status: string
  severity: string
  score: number
  matchedField: string
  snippet: string | null
  createTime: string | null
}

export function searchSimilarIncidents(incidentId: string, params?: { query?: string; limit?: number }) {
  return client.get<ApiResponse<SimilarIncidentItem[]>>(`/api/orchestration/incidents/${incidentId}/similar`, { params })
}

// ========================
// Incident Retrospectives (37K)
// ========================

export interface IncidentRetrospectiveItem {
  id: string
  projectId: string
  incidentId: string
  rootCauseNoteId: string | null
  title: string
  summary: string | null
  whatHappened: string | null
  impactSummary: string | null
  responseSummary: string | null
  lessonsLearned: string | null
  preventionPlan: string | null
  actionItems: string | null
  ownerId: string | null
  dueAt: string | null
  regressionRisk: string
  repeatedIncident: boolean
  status: string
  publishedAt: string | null
  createTime: string
  updateTime: string
}

export interface IncidentRetrospectiveSummaryItem {
  id: string
  projectId: string
  incidentId: string
  title: string
  summary: string | null
  ownerId: string | null
  dueAt: string | null
  regressionRisk: string
  repeatedIncident: boolean
  status: string
  publishedAt: string | null
  createTime: string
  updateTime: string
}

export interface UpdateIncidentRetrospectiveRequest {
  title?: string
  summary?: string
  whatHappened?: string
  impactSummary?: string
  responseSummary?: string
  lessonsLearned?: string
  preventionPlan?: string
  actionItems?: string
  ownerId?: string
  dueAt?: string
  regressionRisk?: string
  repeatedIncident?: boolean
  status?: string
}

export interface SimilarIncidentRegressionCheck {
  repeatedIncident: boolean
  regressionRisk: string
  highestScore: number
  similarCount: number
  similarIncidents: SimilarIncidentItem[]
}

export function createRetrospectiveDraft(incidentId: string) {
  return client.post<ApiResponse<IncidentRetrospectiveItem>>(`/api/orchestration/incidents/${incidentId}/retrospective-draft`, {})
}

export function updateRetrospective(retrospectiveId: string, data: UpdateIncidentRetrospectiveRequest) {
  return client.put<ApiResponse<IncidentRetrospectiveItem>>(`/api/orchestration/incident-retrospectives/${retrospectiveId}`, data)
}

export function getRetrospective(retrospectiveId: string) {
  return client.get<ApiResponse<IncidentRetrospectiveItem>>(`/api/orchestration/incident-retrospectives/${retrospectiveId}`)
}

export function getIncidentRetrospective(incidentId: string) {
  return client.get<ApiResponse<IncidentRetrospectiveItem>>(`/api/orchestration/incidents/${incidentId}/retrospective`)
}

export function listProjectRetrospectives(projectId: string, params?: { status?: string; page?: number; pageSize?: number }) {
  return client.get<ApiResponse<PageResult<IncidentRetrospectiveSummaryItem>>>(`/api/projects/${projectId}/incident-retrospectives`, { params })
}

export function checkRegression(incidentId: string) {
  return client.get<ApiResponse<SimilarIncidentRegressionCheck>>(`/api/orchestration/incidents/${incidentId}/regression-check`)
}

// ========================
// Knowledge Quality Reviews (37K)
// ========================

export interface KnowledgeQualityReviewItem {
  id: string
  projectId: string
  incidentId: string
  knowledgeDocumentId: string | null
  retrospectiveId: string | null
  completenessScore: number
  accuracyScore: number
  actionabilityScore: number
  relevanceScore: number
  averageScore: number | null
  reviewStatus: string
  overallStatus: string
  checklistJson: string | null
  reviewComment: string | null
  reviewerId: string | null
  reviewedAt: string | null
  createTime: string
  updateTime: string
}

export interface CreateKnowledgeQualityReviewRequest {
  knowledgeDocumentId?: string
  retrospectiveId?: string
  completenessScore: number
  accuracyScore: number
  actionabilityScore: number
  relevanceScore: number
  checklistJson?: string
  reviewComment?: string
}

export interface UpdateKnowledgeQualityReviewRequest {
  completenessScore?: number
  accuracyScore?: number
  actionabilityScore?: number
  relevanceScore?: number
  reviewStatus?: string
  checklistJson?: string
  reviewComment?: string
}

export interface KnowledgeQualityStatusSummary {
  totalReviews: number
  approvedCount: number
  needsWorkCount: number
  rejectedCount: number
  pendingCount: number
  inReviewCount: number
  averageCompletenessScore: number
  averageAccuracyScore: number
  averageActionabilityScore: number
  averageRelevanceScore: number
  overallAverageScore: number
}

export function createKnowledgeQualityReview(incidentId: string, data: CreateKnowledgeQualityReviewRequest) {
  return client.post<ApiResponse<KnowledgeQualityReviewItem>>(`/api/orchestration/incidents/${incidentId}/knowledge-quality-reviews`, data)
}

export function updateKnowledgeQualityReview(reviewId: string, data: UpdateKnowledgeQualityReviewRequest) {
  return client.put<ApiResponse<KnowledgeQualityReviewItem>>(`/api/orchestration/knowledge-quality-reviews/${reviewId}`, data)
}

export function getKnowledgeQualityReview(reviewId: string) {
  return client.get<ApiResponse<KnowledgeQualityReviewItem>>(`/api/orchestration/knowledge-quality-reviews/${reviewId}`)
}

export function getProjectKnowledgeQualitySummary(projectId: string) {
  return client.get<ApiResponse<KnowledgeQualityStatusSummary>>(`/api/projects/${projectId}/knowledge-quality-reviews`)
}
