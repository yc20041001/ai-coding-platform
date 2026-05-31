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

// ========================
// Beta Trial Sessions (38A)
// ========================

export interface BetaTrialSessionItem {
  id: string
  projectId: string
  title: string
  participantRole: string | null
  environmentType: string | null
  providerMode: string | null
  githubOauthStatus: string | null
  sessionStatus: string
  startedAt: string | null
  endedAt: string | null
  completedPathJson: string | null
  blockedAtStep: string | null
  blockerSummary: string | null
  satisfactionScore: number | null
  continueIntent: string | null
  summary: string | null
  createTime: string
  updateTime: string
}

export interface BetaTrialSessionSummary {
  id: string
  projectId: string
  title: string
  participantRole: string | null
  environmentType: string | null
  providerMode: string | null
  sessionStatus: string
  continueIntent: string | null
  satisfactionScore: number | null
  startedAt: string | null
  endedAt: string | null
  createTime: string
}

export interface CreateBetaTrialSessionRequest {
  projectId: string
  title: string
  participantRole?: string
  environmentType?: string
  providerMode?: string
  githubOauthStatus?: string
}

export interface UpdateBetaTrialSessionRequest {
  sessionStatus?: string
  blockedAtStep?: string
  blockerSummary?: string
  completedPathJson?: string
  satisfactionScore?: number
  continueIntent?: string
  summary?: string
  startedAt?: string
  endedAt?: string
}

export function createBetaSession(data: CreateBetaTrialSessionRequest) {
  return client.post<ApiResponse<BetaTrialSessionItem>>('/api/beta-sessions', data)
}

export function updateBetaSession(id: string, data: UpdateBetaTrialSessionRequest) {
  return client.put<ApiResponse<BetaTrialSessionItem>>(`/api/beta-sessions/${id}`, data)
}

export function getBetaSession(id: string) {
  return client.get<ApiResponse<BetaTrialSessionItem>>(`/api/beta-sessions/${id}`)
}

export function listBetaSessions(projectId: string) {
  return client.get<ApiResponse<BetaTrialSessionSummary[]>>(`/api/projects/${projectId}/beta-sessions`)
}

export function exportBetaSessionMarkdown(id: string) {
  return client.get<ApiResponse<string>>(`/api/beta-sessions/${id}/export-markdown`)
}

// ========================
// Beta Trial Feedback (38A)
// ========================

export interface BetaTrialFeedbackItem {
  id: string
  sessionId: string
  projectId: string
  category: string | null
  subcategory: string | null
  severity: string
  sourceType: string
  title: string
  detail: string | null
  expectedBehavior: string | null
  actualBehavior: string | null
  suggestedAction: string | null
  triageStatus: string
  mappedIncidentId: string | null
  mappedKnownIssueId: string | null
  releaseBlocking: boolean
  createTime: string
  updateTime: string
}

export interface BetaTrialFeedbackSummary {
  id: string
  sessionId: string
  category: string | null
  severity: string
  title: string
  triageStatus: string
  releaseBlocking: boolean
  createTime: string
}

export interface CreateBetaTrialFeedbackRequest {
  category?: string
  subcategory?: string
  severity: string
  sourceType?: string
  title: string
  detail?: string
  expectedBehavior?: string
  actualBehavior?: string
  suggestedAction?: string
  releaseBlocking?: boolean
}

export interface UpdateBetaTrialFeedbackRequest {
  category?: string
  subcategory?: string
  severity?: string
  title?: string
  detail?: string
  expectedBehavior?: string
  actualBehavior?: string
  suggestedAction?: string
  triageStatus?: string
  mappedIncidentId?: string
  mappedKnownIssueId?: string
  releaseBlocking?: boolean
}

export interface BetaPassBlockSummary {
  totalFeedback: number
  releaseBlockingCount: number
  p0Count: number
  p1Count: number
  newCount: number
  triagedCount: number
  scheduledCount: number
  doneCount: number
  wontFixCount: number
}

export function createBetaFeedback(sessionId: string, data: CreateBetaTrialFeedbackRequest) {
  return client.post<ApiResponse<BetaTrialFeedbackItem>>(`/api/beta-sessions/${sessionId}/feedback`, data)
}

export function updateBetaFeedback(id: string, data: UpdateBetaTrialFeedbackRequest) {
  return client.put<ApiResponse<BetaTrialFeedbackItem>>(`/api/beta-feedback/${id}`, data)
}

export function getBetaFeedback(id: string) {
  return client.get<ApiResponse<BetaTrialFeedbackItem>>(`/api/beta-feedback/${id}`)
}

export function listBetaFeedback(sessionId: string, severity?: string, triageStatus?: string) {
  const params: Record<string, string> = {}
  if (severity) params.severity = severity
  if (triageStatus) params.triageStatus = triageStatus
  return client.get<ApiResponse<BetaTrialFeedbackSummary[]>>(`/api/beta-sessions/${sessionId}/feedback`, { params })
}

export function getBetaPassBlockSummary(sessionId: string) {
  return client.get<ApiResponse<BetaPassBlockSummary>>(`/api/beta-sessions/${sessionId}/feedback/pass-block-summary`)
}

export function deleteBetaFeedback(id: string) {
  return client.delete<ApiResponse<void>>(`/api/beta-feedback/${id}`)
}

// ========================
// Beta Environment Readiness (38A)
// ========================

export interface BetaEnvironmentReadinessItem {
  id: string
  projectId: string
  sessionId: string | null
  targetName: string
  targetType: string
  checkStatus: string
  summary: string | null
  detailJson: string | null
  checkedAt: string | null
  createTime: string
}

export interface CreateBetaEnvironmentReadinessRequest {
  sessionId?: string
  targetName: string
  targetType: string
  checkStatus: string
  summary?: string
  detailJson?: string
}

export interface BetaTrialDashboard {
  totalSessions: number
  completedSessions: number
  blockedSessions: number
  inProgressSessions: number
  averageSatisfactionScore: number
  continueYesCount: number
  p0Count: number
  p1Count: number
  releaseBlockingCount: number
  readinessPassCount: number
  readinessWarnCount: number
  readinessFailCount: number
}

export function createBetaReadinessCheck(projectId: string, data: CreateBetaEnvironmentReadinessRequest) {
  return client.post<ApiResponse<BetaEnvironmentReadinessItem>>(`/api/projects/${projectId}/environment-readiness`, data)
}

export function getBetaReadinessCheck(id: string) {
  return client.get<ApiResponse<BetaEnvironmentReadinessItem>>(`/api/environment-readiness/${id}`)
}

export function listBetaReadinessChecks(projectId?: string, sessionId?: string) {
  const params: Record<string, string> = {}
  if (projectId) params.projectId = projectId
  if (sessionId) params.sessionId = sessionId
  return client.get<ApiResponse<BetaEnvironmentReadinessItem[]>>('/api/environment-readiness', { params })
}

export function getBetaDashboard(projectId: string) {
  return client.get<ApiResponse<BetaTrialDashboard>>(`/api/projects/${projectId}/beta-dashboard`)
}

// ========================
// Model Cost Analytics (38B)
// ========================

export interface ModelCostSummaryItem {
  id: string
  projectId: string
  provider: string
  modelName: string
  requestType: string
  statDate: string
  requestCount: number
  successCount: number
  failureCount: number
  fallbackCount: number
  promptTokens: number
  completionTokens: number
  totalTokens: number
  estimatedCost: number
  avgLatencyMs: number
  createTime: string
  updateTime: string
}

export interface ModelCostTrendItem {
  statDate: string
  provider: string
  modelName: string
  totalCost: number
  totalTokens: number
  requestCount: number
}

export interface ModelCostDashboard {
  totalCostToday: number
  totalCostThisWeek: number
  totalCostThisMonth: number
  totalRequestsToday: number
  averageCostPerRequest: number
  costChangePercent: number
  topModelsByCost: ModelCostSummaryItem[]
  recentAlerts: ModelCostAlertItem[]
}

export interface ModelCostAlertItem {
  id: string
  projectId: string
  provider: string
  modelName: string
  alertType: string
  severity: string
  status: string
  summary: string
  detail: string | null
  statDate: string
  thresholdValue: number
  actualValue: number
  createTime: string
  updateTime: string
}

export interface ExportModelCostReport {
  content: string
  fileName: string
}

export function refreshModelCost(projectId: string) {
  return client.post<ApiResponse<void>>(`/api/projects/${projectId}/model-cost/refresh`, {})
}

export function listModelCostSummaries(projectId: string, params?: {
  provider?: string
  modelName?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}) {
  return client.get<ApiResponse<ModelCostSummaryItem[]>>(`/api/projects/${projectId}/model-cost/summaries`, { params })
}

export function getModelCostTrend(projectId: string, params?: { startDate?: string; endDate?: string }) {
  return client.get<ApiResponse<ModelCostTrendItem[]>>(`/api/projects/${projectId}/model-cost/trend`, { params })
}

export function getModelCostDashboard(projectId: string) {
  return client.get<ApiResponse<ModelCostDashboard>>(`/api/projects/${projectId}/model-cost/dashboard`)
}

export function scanModelCostAlerts(projectId: string) {
  return client.post<ApiResponse<ModelCostAlertItem[]>>(`/api/projects/${projectId}/model-cost/alerts/scan`, {})
}

export function listModelCostAlerts(projectId: string, params?: {
  status?: string
  severity?: string
  page?: number
  size?: number
}) {
  return client.get<ApiResponse<ModelCostAlertItem[]>>(`/api/projects/${projectId}/model-cost/alerts`, { params })
}

export function updateModelCostAlertStatus(id: string, status: string) {
  return client.put<ApiResponse<ModelCostAlertItem>>(`/api/model-cost/alerts/${id}/status?status=${status}`, {})
}

export function exportModelCostReport(projectId: string, params?: { startDate?: string; endDate?: string }) {
  return client.get<ApiResponse<ExportModelCostReport>>(`/api/projects/${projectId}/export/model-cost-report`, { params })
}

// ========================
// PR Review Quality (38B)
// ========================

export interface PrReviewQualityRecordItem {
  id: string
  projectId: string
  reviewJobId: string
  githubBindingId: string | null
  repositoryFullName: string
  pullRequestNumber: number
  strategyKey: string | null
  modelProvider: string | null
  modelName: string | null
  findingsTotal: number
  highRiskFindings: number
  mediumRiskFindings: number
  lowRiskFindings: number
  reviewStatus: string
  humanFeedbackStatus: string
  adoptionStatus: string
  usefulnessScore: number | null
  falsePositiveScore: number | null
  reviewComment: string | null
  reviewedBy: string | null
  reviewedAt: string | null
  createTime: string
  updateTime: string
}

export interface PrReviewQualityDashboard {
  totalReviews: number
  highValueReviews: number
  actionableReviews: number
  lowSignalReviews: number
  failedReviews: number
  pendingFeedbackReviews: number
  adoptedReviews: number
  averageUsefulnessScore: number
  recentReviews: PrReviewQualityRecordItem[]
}

export interface CreatePrReviewQualityRecordRequest {
  reviewJobId: string
  usefulnessScore?: number
  falsePositiveScore?: number
  reviewComment?: string
}

export interface UpdatePrReviewQualityRecordRequest {
  humanFeedbackStatus?: string
  adoptionStatus?: string
  usefulnessScore?: number
  falsePositiveScore?: number
  reviewComment?: string
}

export interface ExportPrReviewQualityReport {
  content: string
  fileName: string
}

export function createPrReviewQualityRecord(projectId: string, data: CreatePrReviewQualityRecordRequest) {
  return client.post<ApiResponse<PrReviewQualityRecordItem>>(`/api/projects/${projectId}/pr-review-quality/records`, data)
}

export function updatePrReviewQualityRecord(id: string, data: UpdatePrReviewQualityRecordRequest) {
  return client.put<ApiResponse<PrReviewQualityRecordItem>>(`/api/pr-review-quality/records/${id}`, data)
}

export function listPrReviewQualityRecords(projectId: string, params?: { status?: string; page?: number; size?: number }) {
  return client.get<ApiResponse<PrReviewQualityRecordItem[]>>(`/api/projects/${projectId}/pr-review-quality/records`, { params })
}

export function getPrReviewQualityDashboard(projectId: string) {
  return client.get<ApiResponse<PrReviewQualityDashboard>>(`/api/projects/${projectId}/pr-review-quality/dashboard`)
}

export function exportPrReviewQualityReport(projectId: string) {
  return client.get<ApiResponse<ExportPrReviewQualityReport>>(`/api/projects/${projectId}/export/pr-review-quality-report`)
}

// ========================
// Beta Release Gate (38C)
// ========================

export interface BetaReleaseGateRuleItem {
  id: string
  projectId: string | null
  ruleKey: string
  category: string
  displayName: string
  thresholdOperator: string
  thresholdValue: number | null
  enabled: number
  blocking: number
  sortOrder: number
  description: string | null
  createTime: string
  updateTime: string
}

export interface BetaReleaseGateEvaluationItem {
  id: string
  projectId: string
  evaluationTarget: string
  evaluationType: string
  ruleKey: string
  category: string
  gateStatus: string
  actualValue: number | null
  thresholdValue: number | null
  blocking: number
  summary: string
  detail: string | null
  evidenceJson: string | null
  evaluatedAt: string
  createTime: string
  updateTime: string
}

export interface BetaReleaseDecisionItem {
  id: string
  projectId: string
  releaseLabel: string
  decisionStatus: string
  decisionReason: string | null
  blockingIssueCount: number
  warningIssueCount: number
  approverId: string | null
  approvedAt: string | null
  reportMarkdown: string | null
  createTime: string
  updateTime: string
}

export interface BetaReleaseGateDashboard {
  summary: {
    totalRules: number
    blockingFailures: number
    warningCount: number
    passCount: number
    overallStatus: string
  }
  evaluations: BetaReleaseGateEvaluationItem[]
  recentDecisions: BetaReleaseDecisionItem[]
}

export interface BetaReleaseReadinessReport {
  releaseLabel: string
  overallStatus: string
  reportMarkdown: string
  evaluations: BetaReleaseGateEvaluationItem[]
  decision: BetaReleaseDecisionItem | null
}

export interface CreateBetaReleaseDecisionRequest {
  releaseLabel: string
  decisionStatus: string
  decisionReason?: string
  approverId?: string
}

export interface UpdateBetaReleaseDecisionRequest {
  decisionStatus?: string
  decisionReason?: string
  approverId?: string
}

export function listGateRules(projectId: string) {
  return client.get<ApiResponse<BetaReleaseGateRuleItem[]>>(`/api/projects/${projectId}/beta/release-gate/rules`)
}

export function updateGateRule(projectId: string, ruleId: string, params?: { enabled?: string; blocking?: string; thresholdValue?: number }) {
  return client.put<ApiResponse<BetaReleaseGateRuleItem>>(`/api/projects/${projectId}/beta/release-gate/rules/${ruleId}`, {}, { params })
}

export function evaluateReleaseGate(projectId: string, params?: { evaluationType?: string; evaluationTarget?: string }) {
  return client.post<ApiResponse<BetaReleaseGateEvaluationItem[]>>(`/api/projects/${projectId}/beta/release-gate/evaluate`, {}, { params })
}

export function listGateEvaluations(projectId: string, params?: { evaluationTarget?: string; page?: number; size?: number }) {
  return client.get<ApiResponse<BetaReleaseGateEvaluationItem[]>>(`/api/projects/${projectId}/beta/release-gate/evaluations`, { params })
}

export function getGateDashboard(projectId: string) {
  return client.get<ApiResponse<BetaReleaseGateDashboard>>(`/api/projects/${projectId}/beta/release-gate/dashboard`)
}

export function createReleaseDecision(projectId: string, data: CreateBetaReleaseDecisionRequest) {
  return client.post<ApiResponse<BetaReleaseDecisionItem>>(`/api/projects/${projectId}/beta/release-gate/decisions`, data)
}

export function updateReleaseDecision(projectId: string, decisionId: string, data: UpdateBetaReleaseDecisionRequest) {
  return client.put<ApiResponse<BetaReleaseDecisionItem>>(`/api/projects/${projectId}/beta/release-gate/decisions/${decisionId}`, data)
}

export function listReleaseDecisions(projectId: string, params?: { page?: number; size?: number }) {
  return client.get<ApiResponse<BetaReleaseDecisionItem[]>>(`/api/projects/${projectId}/beta/release-gate/decisions`, { params })
}

export function getReleaseDecision(projectId: string, decisionId: string) {
  return client.get<ApiResponse<BetaReleaseDecisionItem>>(`/api/projects/${projectId}/beta/release-gate/decisions/${decisionId}`)
}

export function getReadinessReport(projectId: string, params?: { releaseLabel?: string }) {
  return client.get<ApiResponse<BetaReleaseReadinessReport>>(`/api/projects/${projectId}/beta/release-gate/readiness-report`, { params })
}

// ========================
// Release Rollout (39A)
// ========================

export interface ReleaseRolloutPlanItem {
  id: string
  projectId: string | null
  releaseLabel: string
  sourceDecisionId: string | null
  rolloutStatus: string
  rolloutStrategy: string
  targetEnvironment: string
  ownerId: string | null
  approverId: string | null
  plannedStartAt: string | null
  plannedEndAt: string | null
  observationWindowMinutes: number | null
  rollbackTriggerSummary: string | null
  successCriteriaSummary: string | null
  readinessSummary: string | null
  stepCount: number
  passedStepCount: number
  failedStepCount: number
  verificationCount: number
  blockingVerificationCount: number
  createTime: string
  updateTime: string
}

export interface CreateReleaseRolloutPlanRequest {
  releaseLabel: string
  sourceDecisionId?: string
  rolloutStrategy?: string
  targetEnvironment?: string
  ownerId?: string
  approverId?: string
  plannedStartAt?: string
  plannedEndAt?: string
  observationWindowMinutes?: number
  rollbackTriggerSummary?: string
  successCriteriaSummary?: string
  readinessSummary?: string
}

export interface UpdateReleaseRolloutPlanRequest {
  rolloutStrategy?: string
  targetEnvironment?: string
  plannedStartAt?: string
  plannedEndAt?: string
  observationWindowMinutes?: number
  rollbackTriggerSummary?: string
  successCriteriaSummary?: string
  readinessSummary?: string
}

export interface ReleaseRolloutStepItem {
  id: string
  planId: string | null
  projectId: string | null
  stepOrder: number | null
  stepKey: string | null
  displayName: string | null
  stepStatus: string | null
  verificationScope: string | null
  required: number | null
  blocking: number | null
  instructions: string | null
  expectedResult: string | null
  actualResult: string | null
  evidenceJson: string | null
  operatorId: string | null
  startedAt: string | null
  finishedAt: string | null
  createTime: string
  updateTime: string
}

export interface CreateReleaseRolloutStepRequest {
  stepOrder?: number
  stepKey?: string
  displayName?: string
  verificationScope?: string
  required?: number
  blocking?: number
  instructions?: string
  expectedResult?: string
}

export interface UpdateReleaseRolloutStepRequest {
  stepStatus?: string
  actualResult?: string
  evidenceJson?: string
  operatorId?: string
  startedAt?: string
  finishedAt?: string
}

export interface ReleaseVerificationRecordItem {
  id: string
  planId: string | null
  projectId: string | null
  verificationPhase: string | null
  verificationKey: string | null
  displayName: string | null
  verificationStatus: string | null
  severity: string | null
  summary: string | null
  detail: string | null
  evidenceJson: string | null
  relatedIncidentId: string | null
  relatedAlertId: string | null
  recordedBy: string | null
  recordedAt: string | null
  createTime: string
  updateTime: string
}

export interface CreateReleaseVerificationRequest {
  verificationPhase?: string
  verificationKey?: string
  displayName?: string
  verificationStatus?: string
  severity?: string
  summary?: string
  detail?: string
  evidenceJson?: string
  relatedIncidentId?: string
  relatedAlertId?: string
  recordedBy?: string
}

export interface UpdateReleaseVerificationRequest {
  verificationStatus?: string
  severity?: string
  summary?: string
  detail?: string
  evidenceJson?: string
}

export interface ReleaseReadinessDashboard {
  projectId: string | null
  releaseLabel: string | null
  decisionStatus: string | null
  rolloutStatus: string | null
  overallReadinessStatus: string | null
  blockingIssueCount: number
  warningIssueCount: number
  openIncidentCount: number
  activeAlertCount: number
  highRiskFeedbackCount: number
  costAlertCount: number
  prQualityWarnCount: number
  preReleasePassRate: number
  observationVerificationCount: number
  rollbackRecommended: boolean
  lastEvaluatedAt: string | null
}

export interface ReleaseRolloutSummary {
  planId: string | null
  releaseLabel: string | null
  rolloutStatus: string | null
  rolloutStrategy: string | null
  targetEnvironment: string | null
  totalSteps: number
  passedSteps: number
  failedSteps: number
  skippedSteps: number
  blockedSteps: number
  totalVerifications: number
  failedVerifications: number
  blockingVerifications: number
  rollbackRecommended: boolean
  overallResult: string | null
  startedAt: string | null
  completedAt: string | null
  createTime: string
}

export interface ReleaseReadinessReport {
  releaseLabel: string | null
  decisionStatus: string | null
  rolloutStatus: string | null
  overallReadinessStatus: string | null
  rolloutStrategy: string | null
  targetEnvironment: string | null
  verifications: ReleaseVerificationRecordItem[]
  steps: ReleaseRolloutStepItem[]
  reportMarkdown: string | null
  generatedAt: string | null
}

// Rollout Plan APIs
export function createRolloutPlan(projectId: string, data: CreateReleaseRolloutPlanRequest) {
  return client.post<ApiResponse<ReleaseRolloutPlanItem>>(`/api/projects/${projectId}/rollout/plans`, data)
}

export function listRolloutPlans(projectId: string) {
  return client.get<ApiResponse<ReleaseRolloutPlanItem[]>>(`/api/projects/${projectId}/rollout/plans`)
}

export function getRolloutPlan(projectId: string, planId: string) {
  return client.get<ApiResponse<ReleaseRolloutPlanItem>>(`/api/projects/${projectId}/rollout/plans/${planId}`)
}

export function updateRolloutPlan(projectId: string, planId: string, data: UpdateReleaseRolloutPlanRequest) {
  return client.put<ApiResponse<ReleaseRolloutPlanItem>>(`/api/projects/${projectId}/rollout/plans/${planId}`, data)
}

export function updateRolloutPlanStatus(projectId: string, planId: string, status: string) {
  return client.put<ApiResponse<ReleaseRolloutPlanItem>>(`/api/projects/${projectId}/rollout/plans/${planId}/status`, {}, { params: { status } })
}

// Rollout Step APIs
export function listRolloutSteps(projectId: string, planId: string) {
  return client.get<ApiResponse<ReleaseRolloutStepItem[]>>(`/api/projects/${projectId}/rollout/plans/${planId}/steps`)
}

export function createRolloutStep(projectId: string, planId: string, data: CreateReleaseRolloutStepRequest) {
  return client.post<ApiResponse<ReleaseRolloutStepItem>>(`/api/projects/${projectId}/rollout/plans/${planId}/steps`, data)
}

export function updateRolloutStep(projectId: string, planId: string, stepId: string, data: UpdateReleaseRolloutStepRequest) {
  return client.put<ApiResponse<ReleaseRolloutStepItem>>(`/api/projects/${projectId}/rollout/plans/${planId}/steps/${stepId}`, data)
}

export function updateRolloutStepStatus(projectId: string, planId: string, stepId: string, stepStatus: string, params?: { actualResult?: string; evidenceJson?: string; operatorId?: string }) {
  return client.put<ApiResponse<ReleaseRolloutStepItem>>(`/api/projects/${projectId}/rollout/plans/${planId}/steps/${stepId}/status`, {}, { params: { stepStatus, ...params } })
}

// Verification APIs
export function listVerifications(projectId: string, planId: string, phase?: string) {
  const params: Record<string, string> = {}
  if (phase) params.phase = phase
  return client.get<ApiResponse<ReleaseVerificationRecordItem[]>>(`/api/projects/${projectId}/rollout/plans/${planId}/verifications`, { params })
}

export function createVerification(projectId: string, planId: string, data: CreateReleaseVerificationRequest) {
  return client.post<ApiResponse<ReleaseVerificationRecordItem>>(`/api/projects/${projectId}/rollout/plans/${planId}/verifications`, data)
}

export function updateVerification(projectId: string, planId: string, recordId: string, data: UpdateReleaseVerificationRequest) {
  return client.put<ApiResponse<ReleaseVerificationRecordItem>>(`/api/projects/${projectId}/rollout/plans/${planId}/verifications/${recordId}`, data)
}

// Dashboard, Summary, Report APIs
export function getRolloutDashboard(projectId: string, releaseLabel?: string) {
  const params: Record<string, string> = {}
  if (releaseLabel) params.releaseLabel = releaseLabel
  return client.get<ApiResponse<ReleaseReadinessDashboard>>(`/api/projects/${projectId}/rollout/readiness-dashboard`, { params })
}

export function getRolloutSummary(projectId: string, planId: string) {
  return client.get<ApiResponse<ReleaseRolloutSummary>>(`/api/projects/${projectId}/rollout/plans/${planId}/summary`)
}

export function generateRolloutReport(projectId: string, planId: string) {
  return client.get<ApiResponse<ReleaseReadinessReport>>(`/api/projects/${projectId}/rollout/plans/${planId}/report`)
}

// ========================
// Release Rollback & Audit (39B)
// ========================

export interface ReleaseRollbackDrillItem {
  id: string
  planId: string | null
  projectId: string | null
  releaseLabel: string
  drillStatus: string
  drillScope: string
  environmentName: string
  ownerId: string | null
  executorId: string | null
  plannedAt: string | null
  startedAt: string | null
  finishedAt: string | null
  durationSeconds: number | null
  successCriteria: string | null
  rollbackStepsSummary: string | null
  blockersSummary: string | null
  resultSummary: string | null
  evidenceJson: string | null
  createTime: string
  updateTime: string
}

export interface CreateReleaseRollbackDrillRequest {
  projectId?: string
  releaseLabel?: string
  drillScope?: string
  environmentName?: string
  ownerId?: string
  executorId?: string
  plannedAt?: string
  successCriteria?: string
  rollbackStepsSummary?: string
  blockersSummary?: string
  resultSummary?: string
  evidenceJson?: string
}

export interface UpdateReleaseRollbackDrillRequest {
  drillScope?: string
  environmentName?: string
  ownerId?: string
  executorId?: string
  plannedAt?: string
  startedAt?: string
  finishedAt?: string
  durationSeconds?: number
  successCriteria?: string
  rollbackStepsSummary?: string
  blockersSummary?: string
  resultSummary?: string
  evidenceJson?: string
}

export interface ReleaseAuditEventItem {
  id: string
  projectId: string | null
  planId: string | null
  releaseLabel: string
  eventType: string
  actorId: string | null
  actorName: string | null
  summary: string
  detail: string | null
  relatedStepId: string | null
  relatedVerificationId: string | null
  relatedIncidentId: string | null
  relatedAlertId: string | null
  evidenceJson: string | null
  eventTime: string
  createTime: string
}

export interface ReleaseAuditTimeline {
  planId: string
  releaseLabel: string
  totalEvents: number
  latestEventTime: string | null
  eventCountsByType: Record<string, number>
  events: ReleaseAuditEventItem[]
}

export interface ReleaseAuditReport {
  planId: string | null
  releaseLabel: string | null
  reportMarkdown: string | null
  generatedAt: string | null
}

export interface ReleasePostmortemReviewItem {
  id: string
  planId: string | null
  projectId: string | null
  releaseLabel: string
  reviewStatus: string
  overallOutcome: string
  summary: string | null
  whatWentWell: string | null
  whatWentWrong: string | null
  customerImpact: string | null
  followUpActions: string | null
  reviewerId: string | null
  reviewedAt: string | null
  createTime: string
  updateTime: string
}

export interface CreateReleasePostmortemReviewRequest {
  projectId?: string
  releaseLabel?: string
  overallOutcome?: string
  summary?: string
  whatWentWell?: string
  whatWentWrong?: string
  customerImpact?: string
  followUpActions?: string
  reviewerId?: string
}

export interface UpdateReleasePostmortemReviewRequest {
  overallOutcome?: string
  summary?: string
  whatWentWell?: string
  whatWentWrong?: string
  customerImpact?: string
  followUpActions?: string
  reviewerId?: string
}

// Rollback Drill APIs
export function createRollbackDrill(planId: string, data: CreateReleaseRollbackDrillRequest) {
  return client.post<ApiResponse<ReleaseRollbackDrillItem>>(`/api/release-rollouts/${planId}/rollback-drills`, data)
}

export function listRollbackDrills(planId: string) {
  return client.get<ApiResponse<ReleaseRollbackDrillItem[]>>(`/api/release-rollouts/${planId}/rollback-drills`)
}

export function getRollbackDrill(planId: string, drillId: string) {
  return client.get<ApiResponse<ReleaseRollbackDrillItem>>(`/api/release-rollouts/${planId}/rollback-drills/${drillId}`)
}

export function updateRollbackDrill(planId: string, drillId: string, data: UpdateReleaseRollbackDrillRequest) {
  return client.put<ApiResponse<ReleaseRollbackDrillItem>>(`/api/release-rollouts/${planId}/rollback-drills/${drillId}`, data)
}

export function updateRollbackDrillStatus(planId: string, drillId: string, drillStatus: string) {
  return client.post<ApiResponse<ReleaseRollbackDrillItem>>(`/api/release-rollouts/${planId}/rollback-drills/${drillId}/status`, {}, { params: { drillStatus } })
}

export function checkRollbackReadiness(planId: string) {
  return client.get<ApiResponse<boolean>>(`/api/release-rollouts/${planId}/rollback-drills/readiness`)
}

// Audit Event APIs
export function listAuditEvents(planId: string) {
  return client.get<ApiResponse<ReleaseAuditEventItem[]>>(`/api/release-rollouts/${planId}/audit-events`)
}

export function getAuditTimeline(planId: string) {
  return client.get<ApiResponse<ReleaseAuditTimeline>>(`/api/release-rollouts/${planId}/audit-timeline`)
}

export function generateAuditReport(planId: string) {
  return client.get<ApiResponse<ReleaseAuditReport>>(`/api/release-rollouts/${planId}/audit-report`)
}

// Postmortem Review APIs
export function createPostmortemReview(planId: string, data: CreateReleasePostmortemReviewRequest) {
  return client.post<ApiResponse<ReleasePostmortemReviewItem>>(`/api/release-rollouts/${planId}/postmortem-review`, data)
}

export function getPostmortemReview(planId: string) {
  return client.get<ApiResponse<ReleasePostmortemReviewItem>>(`/api/release-rollouts/${planId}/postmortem-review`)
}

export function updatePostmortemReview(planId: string, reviewId: string, data: UpdateReleasePostmortemReviewRequest) {
  return client.put<ApiResponse<ReleasePostmortemReviewItem>>(`/api/release-rollouts/${planId}/postmortem-review/${reviewId}`, data)
}

export function updatePostmortemReviewStatus(planId: string, reviewId: string, reviewStatus: string) {
  return client.post<ApiResponse<ReleasePostmortemReviewItem>>(`/api/release-rollouts/${planId}/postmortem-review/${reviewId}/status`, {}, { params: { reviewStatus } })
}

export function getPrefilledPostmortemReview(planId: string) {
  return client.get<ApiResponse<ReleasePostmortemReviewItem>>(`/api/release-rollouts/${planId}/postmortem-review/prefill`)
}

// ========================
// Release Evidence & Summary (39C)
// ========================

export interface ReleaseEvidenceBundleItem {
  id: string
  projectId: string | null
  planId: string | null
  releaseLabel: string
  bundleStatus: string
  summaryMarkdown: string | null
  evidenceJson: string | null
  generatedBy: string | null
  generatedAt: string | null
  createTime: string
  updateTime: string
}

export interface GenerateReleaseEvidenceBundleRequest {
  projectId: string
  generatedBy?: string
}

export interface ReleaseSignoffRecordItem {
  id: string
  projectId: string | null
  planId: string | null
  releaseLabel: string
  signoffRole: string
  signoffStatus: string
  signerId: string | null
  signerName: string | null
  commentText: string | null
  signedAt: string | null
  createTime: string
  updateTime: string
}

export interface CreateReleaseSignoffRecordRequest {
  projectId?: string
  signoffRole?: string
  signoffStatus?: string
  signerId?: string
  signerName?: string
  commentText?: string
}

export interface UpdateReleaseSignoffRecordRequest {
  signoffStatus?: string
  signerId?: string
  signerName?: string
  commentText?: string
}

export interface ReleaseExecutiveSummaryItem {
  projectId: string | null
  planId: string | null
  releaseLabel: string
  decisionStatus: string | null
  rolloutStatus: string | null
  overallOutcome: string | null
  confidenceScore: number
  confidenceLevel: string
  blockingIssueCount: number
  warningIssueCount: number
  rollbackReady: boolean
  signoffCompletionRate: number
  openIncidentCount: number
  activeAlertCount: number
  failedVerificationCount: number
  latestPostmortemOutcome: string | null
  summaryText: string | null
  lastUpdatedAt: string | null
}

export interface ReleaseConfidenceSnapshotItem {
  id: string | null
  projectId: string | null
  planId: string | null
  releaseLabel: string | null
  confidenceScore: number
  confidenceLevel: string
  blockingIssueCount: number
  warningIssueCount: number
  openIncidentCount: number
  activeAlertCount: number
  failedVerificationCount: number
  rollbackReady: boolean
  signoffCompletionRate: number
  snapshotSummary: string | null
  snapshotTime: string | null
  createTime: string | null
}

export interface ReleaseComparisonItem {
  projectId: string | null
  currentReleaseLabel: string | null
  baselineReleaseLabel: string | null
  confidenceScoreDelta: number | null
  blockingIssueDelta: number | null
  warningIssueDelta: number | null
  failedVerificationDelta: number | null
  rollbackReadyChanged: boolean | null
  signoffCompletionDelta: number | null
  trendSummary: string | null
}

export interface ReleaseConfidenceTrendItem {
  planId: string | null
  releaseLabel: string | null
  confidenceScore: number
  confidenceLevel: string
  snapshotTime: string | null
}

export interface ReleaseExecutiveReportItem {
  planId: string | null
  releaseLabel: string | null
  reportMarkdown: string | null
  generatedAt: string | null
}

// Evidence Bundle APIs
export function generateEvidenceBundle(planId: string, data: GenerateReleaseEvidenceBundleRequest) {
  return client.post<ApiResponse<ReleaseEvidenceBundleItem>>(`/api/release-rollouts/${planId}/evidence-bundle/generate`, data)
}

export function getEvidenceBundle(planId: string) {
  return client.get<ApiResponse<ReleaseEvidenceBundleItem>>(`/api/release-rollouts/${planId}/evidence-bundle`)
}

export function updateEvidenceBundleStatus(planId: string, bundleStatus: string) {
  return client.post<ApiResponse<ReleaseEvidenceBundleItem>>(`/api/release-rollouts/${planId}/evidence-bundle/status`, {}, { params: { bundleStatus } })
}

// Signoff APIs
export function listSignoffs(planId: string) {
  return client.get<ApiResponse<ReleaseSignoffRecordItem[]>>(`/api/release-rollouts/${planId}/signoffs`)
}

export function createSignoff(planId: string, data: CreateReleaseSignoffRecordRequest) {
  return client.post<ApiResponse<ReleaseSignoffRecordItem>>(`/api/release-rollouts/${planId}/signoffs`, data)
}

export function updateSignoff(planId: string, signoffId: string, data: UpdateReleaseSignoffRecordRequest) {
  return client.put<ApiResponse<ReleaseSignoffRecordItem>>(`/api/release-rollouts/${planId}/signoffs/${signoffId}`, data)
}

export function updateSignoffStatus(planId: string, signoffId: string, signoffStatus: string) {
  return client.post<ApiResponse<ReleaseSignoffRecordItem>>(`/api/release-rollouts/${planId}/signoffs/${signoffId}/status`, {}, { params: { signoffStatus } })
}

// Executive Summary APIs
export function getExecutiveSummary(planId: string) {
  return client.get<ApiResponse<ReleaseExecutiveSummaryItem>>(`/api/release-rollouts/${planId}/executive-summary`)
}

export function getConfidenceSnapshot(planId: string) {
  return client.get<ApiResponse<ReleaseConfidenceSnapshotItem>>(`/api/release-rollouts/${planId}/confidence-snapshot`)
}

export function takeConfidenceSnapshot(planId: string) {
  return client.post<ApiResponse<ReleaseConfidenceSnapshotItem>>(`/api/release-rollouts/${planId}/confidence-snapshot`, {})
}

export function getComparison(planId: string) {
  return client.get<ApiResponse<ReleaseComparisonItem>>(`/api/release-rollouts/${planId}/comparison`)
}

export function getConfidenceTrend() {
  return client.get<ApiResponse<ReleaseConfidenceTrendItem[]>>(`/api/release-confidence/trend`)
}

export function generateExecutiveReport(planId: string) {
  return client.get<ApiResponse<ReleaseExecutiveReportItem>>(`/api/release-rollouts/${planId}/executive-report`)
}

// ========== Multi-Project Governance (40A) ==========

export interface ReleasePortfolioSnapshotItem {
  id: string | null
  snapshotDate: string | null
  projectId: string | null
  projectName: string | null
  latestReleaseLabel: string | null
  confidenceScore: number
  confidenceLevel: string
  rolloutStatus: string | null
  decisionStatus: string | null
  blockingIssueCount: number
  warningIssueCount: number
  openIncidentCount: number
  activeAlertCount: number
  failedVerificationCount: number
  rollbackReady: boolean
  signoffCompletionRate: number
  portfolioRank: number | null
  expansionRecommendation: string
  summaryText: string | null
}

export interface ReleasePortfolioDashboardItem {
  snapshotDate: string | null
  projectCount: number
  highConfidenceCount: number
  mediumConfidenceCount: number
  lowConfidenceCount: number
  criticalConfidenceCount: number
  expandNowCount: number
  expandWithGuardrailsCount: number
  holdCount: number
  blockCount: number
  averageConfidenceScore: number
  topProjects: ReleasePortfolioRankingItem[]
  bottomProjects: ReleasePortfolioRankingItem[]
}

export interface ReleasePortfolioRankingItem {
  projectId: string | null
  projectName: string | null
  latestReleaseLabel: string | null
  confidenceScore: number
  confidenceLevel: string
  portfolioRank: number | null
  expansionRecommendation: string
  blockingIssueCount: number
  warningIssueCount: number
  rollbackReady: boolean
  signoffCompletionRate: number
  summaryText: string | null
}

export interface GovernanceBaselineTemplateItem {
  id: string | null
  templateKey: string
  displayName: string
  templateScope: string
  enabled: boolean
  defaultSignoffRolesJson: string | null
  defaultVerificationRulesJson: string | null
  defaultRollbackRequirementsJson: string | null
  defaultConfidencePolicyJson: string | null
  notes: string | null
  createTime: string | null
  updateTime: string | null
}

export interface CreateGovernanceBaselineTemplateRequest {
  templateKey: string
  displayName: string
  templateScope: string
  defaultSignoffRolesJson?: string | null
  defaultVerificationRulesJson?: string | null
  defaultRollbackRequirementsJson?: string | null
  defaultConfidencePolicyJson?: string | null
  notes?: string | null
}

export interface UpdateGovernanceBaselineTemplateRequest {
  displayName?: string | null
  templateScope?: string | null
  defaultSignoffRolesJson?: string | null
  defaultVerificationRulesJson?: string | null
  defaultRollbackRequirementsJson?: string | null
  defaultConfidencePolicyJson?: string | null
  notes?: string | null
}

export interface ReleaseRiskHeatmapCellItem {
  projectId: string | null
  projectName: string | null
  riskCategory: string
  riskScore: number
  riskLevel: string
  sourceCount: number
}

export interface ReleaseRiskHeatmapItem {
  snapshotDate: string | null
  categories: string[]
  cells: ReleaseRiskHeatmapCellItem[]
}

export interface MultiProjectGovernanceSummaryItem {
  snapshotDate: string | null
  totalProjectCount: number
  expandNowCount: number
  expandWithGuardrailsCount: number
  holdCount: number
  blockCount: number
  averageConfidenceScore: number
  riskiestProjects: string[]
  improvingProject: string | null
  decliningProject: string | null
  summaryMarkdown: string | null
}

// Portfolio APIs
export function refreshPortfolio() {
  return client.post<ApiResponse<string>>('/api/release-governance/portfolio/refresh', {})
}

export function getPortfolioDashboard() {
  return client.get<ApiResponse<ReleasePortfolioDashboardItem>>('/api/release-governance/portfolio/dashboard')
}

export function getPortfolioRanking() {
  return client.get<ApiResponse<ReleasePortfolioRankingItem[]>>('/api/release-governance/portfolio/ranking')
}

export function getGovernanceSummary() {
  return client.get<ApiResponse<MultiProjectGovernanceSummaryItem>>('/api/release-governance/summary')
}

// Baseline Template APIs
export function createBaselineTemplate(data: CreateGovernanceBaselineTemplateRequest) {
  return client.post<ApiResponse<GovernanceBaselineTemplateItem>>('/api/release-governance/baseline-templates', data)
}

export function listBaselineTemplates(scope?: string) {
  const params = scope ? { params: { scope } } : {}
  return client.get<ApiResponse<GovernanceBaselineTemplateItem[]>>('/api/release-governance/baseline-templates', params)
}

export function getBaselineTemplate(templateId: string) {
  return client.get<ApiResponse<GovernanceBaselineTemplateItem>>(`/api/release-governance/baseline-templates/${templateId}`)
}

export function updateBaselineTemplate(templateId: string, data: UpdateGovernanceBaselineTemplateRequest) {
  return client.put<ApiResponse<GovernanceBaselineTemplateItem>>(`/api/release-governance/baseline-templates/${templateId}`, data)
}

export function updateBaselineTemplateStatus(templateId: string, enabled: boolean) {
  return client.post<ApiResponse<GovernanceBaselineTemplateItem>>(`/api/release-governance/baseline-templates/${templateId}/status`, {}, { params: { enabled } })
}

// Heatmap APIs
export function refreshHeatmap() {
  return client.post<ApiResponse<string>>('/api/release-governance/heatmap/refresh', {})
}

export function getHeatmap() {
  return client.get<ApiResponse<ReleaseRiskHeatmapItem>>('/api/release-governance/heatmap')
}

// ========== Organization Governance (40B) ==========

export interface OrganizationTrialPolicyItem {
  id: string
  policyKey: string
  displayName: string
  policyScope: string
  enabled: boolean
  thresholdJson: string | null
  signoffPolicyJson: string | null
  rollbackPolicyJson: string | null
  verificationPolicyJson: string | null
  recommendationPolicyJson: string | null
  notes: string | null
  createTime: string
  updateTime: string
}

export interface CreateOrganizationTrialPolicyRequest {
  policyKey: string
  displayName: string
  policyScope: string
  thresholdJson?: string | null
  signoffPolicyJson?: string | null
  rollbackPolicyJson?: string | null
  verificationPolicyJson?: string | null
  recommendationPolicyJson?: string | null
  notes?: string | null
}

export interface UpdateOrganizationTrialPolicyRequest {
  displayName?: string
  policyScope?: string
  thresholdJson?: string | null
  signoffPolicyJson?: string | null
  rollbackPolicyJson?: string | null
  verificationPolicyJson?: string | null
  recommendationPolicyJson?: string | null
  notes?: string | null
}

export interface ReleaseGuardrailEvaluationItem {
  id: string
  snapshotDate: string
  projectId: string
  projectName: string
  policyKey: string
  guardrailKey: string
  guardrailCategory: string
  evaluationStatus: string
  severity: string
  actualValue: number | null
  thresholdValue: number | null
  summary: string
  detail: string | null
  recommendationText: string | null
  evidenceJson: string | null
}

export interface ReleaseGuardrailDashboardItem {
  snapshotDate: string
  projectCount: number
  passCount: number
  warnCount: number
  blockCount: number
  criticalCount: number
  topBlockedProjects: ReleaseGuardrailEvaluationItem[]
  topWarningProjects: ReleaseGuardrailEvaluationItem[]
  recommendationCount: number
}

export interface PortfolioDriftSnapshotItem {
  id: string
  snapshotDate: string
  projectId: string
  projectName: string
  driftScore: number
  driftLevel: string
  baselineTemplateKey: string | null
  confidenceDelta: number
  signoffDelta: number
  verificationDelta: number
  rollbackReadinessChanged: number
  summaryText: string
  detailJson: string | null
}

export interface PortfolioDriftDashboardItem {
  snapshotDate: string
  stableCount: number
  watchCount: number
  highCount: number
  criticalCount: number
  topDriftProjects: PortfolioDriftSnapshotItem[]
  driftTrendSummary: string
}

export interface GovernanceRecommendationItem {
  projectId: string
  projectName: string
  priority: string
  category: string
  title: string
  summary: string
  sourceType: string
  policyKey: string
  guardrailKey: string
  snapshotDate: string
}

export interface OrganizationGovernanceSummaryItem {
  snapshotDate: string
  totalProjectCount: number
  blockCount: number
  warnCount: number
  topRiskProjects: string[]
  topDriftProjects: string[]
  topRecommendations: GovernanceRecommendationItem[]
  summaryMarkdown: string
}

// Organization Policy APIs
export function createOrganizationPolicy(data: CreateOrganizationTrialPolicyRequest) {
  return client.post<ApiResponse<OrganizationTrialPolicyItem>>('/api/organization-governance/policies', data)
}

export function listOrganizationPolicies(scope?: string) {
  const params = scope ? { params: { scope } } : {}
  return client.get<ApiResponse<OrganizationTrialPolicyItem[]>>('/api/organization-governance/policies', params)
}

export function getOrganizationPolicy(policyId: string) {
  return client.get<ApiResponse<OrganizationTrialPolicyItem>>(`/api/organization-governance/policies/${policyId}`)
}

export function updateOrganizationPolicy(policyId: string, data: UpdateOrganizationTrialPolicyRequest) {
  return client.put<ApiResponse<OrganizationTrialPolicyItem>>(`/api/organization-governance/policies/${policyId}`, data)
}

export function updateOrganizationPolicyStatus(policyId: string, enabled: boolean) {
  return client.post<ApiResponse<OrganizationTrialPolicyItem>>(`/api/organization-governance/policies/${policyId}/status`, {}, { params: { enabled } })
}

// Guardrail APIs
export function refreshGuardrails() {
  return client.post<ApiResponse<string>>('/api/organization-governance/guardrails/refresh', {})
}

export function getGuardrails() {
  return client.get<ApiResponse<ReleaseGuardrailEvaluationItem[]>>('/api/organization-governance/guardrails')
}

export function getGuardrailDashboard() {
  return client.get<ApiResponse<ReleaseGuardrailDashboardItem>>('/api/organization-governance/guardrails/dashboard')
}

export function getRecommendations() {
  return client.get<ApiResponse<GovernanceRecommendationItem[]>>('/api/organization-governance/recommendations')
}

// Drift APIs
export function refreshDrift() {
  return client.post<ApiResponse<string>>('/api/organization-governance/drift/refresh', {})
}

export function getDriftList() {
  return client.get<ApiResponse<PortfolioDriftSnapshotItem[]>>('/api/organization-governance/drift')
}

export function getDriftDashboard() {
  return client.get<ApiResponse<PortfolioDriftDashboardItem>>('/api/organization-governance/drift/dashboard')
}

// Summary and Report APIs
export function getOrganizationGovernanceSummary() {
  return client.get<ApiResponse<OrganizationGovernanceSummaryItem>>('/api/organization-governance/summary')
}

export function getOrganizationGovernanceReport() {
  return client.get<ApiResponse<OrganizationGovernanceSummaryItem>>('/api/organization-governance/report')
}

// ========== Governance Workflow & Waiver (40C) ==========

export interface GovernanceRecommendationWorkflowItem {
  id: string
  projectId: string
  projectName: string
  sourceSnapshotDate: string
  policyKey: string
  guardrailKey: string
  category: string
  priority: string
  workflowStatus: string
  title: string
  itemSummary: string | null
  ownerId: string | null
  ownerName: string | null
  dueAt: string | null
  resolvedAt: string | null
  resolutionNote: string | null
  waiverStatus: string | null
  createTime: string
  updateTime: string
}

export interface GovernanceWaiverRequest {
  id: string
  recommendationId: string
  projectId: string
  waiverStatus: string
  waiverScope: string
  requestedBy: string | null
  requestedByName: string | null
  approvedBy: string | null
  approvedByName: string | null
  reasonText: string
  approvalNote: string | null
  expiresAt: string | null
  revokedAt: string | null
  createTime: string
  updateTime: string
}

export interface GovernanceWorkflowDashboard {
  snapshotDate: string
  totalRecommendationCount: number
  openRecommendationCount: number
  inProgressCount: number
  completedCount: number
  blockedCount: number
  overdueCount: number
  activeWaiverCount: number
  expiredWaiverCount: number
  completionRate: number
  overdueRate: number
  topPriorityItems: GovernanceRecommendationWorkflowItem[]
  topOverdueItems: GovernanceRecommendationWorkflowItem[]
}

export interface GovernanceWorkflowSummary {
  snapshotDate: string
  totalRecommendationCount: number
  openCount: number
  inProgressCount: number
  completedCount: number
  blockedCount: number
  overdueCount: number
  activeWaiverCount: number
  completionRate: number
  overdueRate: number
  topPriorityItems: GovernanceRecommendationWorkflowItem[]
  topOverdueItems: GovernanceRecommendationWorkflowItem[]
  summaryMarkdown: string
}

export interface CreateGovernanceWaiverRequest {
  recommendationId: string
  waiverScope: string
  reasonText: string
  expiresAt?: string | null
}

export interface UpdateGovernanceRecommendationItemRequest {
  title?: string
  summary?: string | null
  priority?: string
  ownerId?: string | null
  ownerName?: string | null
  dueAt?: string | null
  resolutionNote?: string | null
}

// Recommendation Workflow APIs
export function syncRecommendations() {
  return client.post<ApiResponse<string>>('/api/governance-workflow/recommendations/sync', {})
}

export function listRecommendations(status?: string, priority?: string) {
  const params: Record<string, string> = {}
  if (status) params.status = status
  if (priority) params.priority = priority
  return client.get<ApiResponse<GovernanceRecommendationWorkflowItem[]>>('/api/governance-workflow/recommendations', { params })
}

export function getRecommendation(itemId: string) {
  return client.get<ApiResponse<GovernanceRecommendationWorkflowItem>>(`/api/governance-workflow/recommendations/${itemId}`)
}

export function updateRecommendation(itemId: string, data: UpdateGovernanceRecommendationItemRequest) {
  return client.put<ApiResponse<GovernanceRecommendationWorkflowItem>>(`/api/governance-workflow/recommendations/${itemId}`, data)
}

export function updateRecommendationStatus(itemId: string, status: string) {
  return client.post<ApiResponse<GovernanceRecommendationWorkflowItem>>(`/api/governance-workflow/recommendations/${itemId}/status`, {}, { params: { status } })
}

// Waiver APIs
export function createWaiver(itemId: string, data: CreateGovernanceWaiverRequest) {
  return client.post<ApiResponse<GovernanceWaiverRequest>>(`/api/governance-workflow/recommendations/${itemId}/waivers`, data)
}

export function listWaivers(itemId: string) {
  return client.get<ApiResponse<GovernanceWaiverRequest[]>>(`/api/governance-workflow/recommendations/${itemId}/waivers`)
}

export function updateWaiver(waiverId: string, data: { reasonText?: string; approvalNote?: string; expiresAt?: string | null }) {
  return client.put<ApiResponse<GovernanceWaiverRequest>>(`/api/governance-workflow/waivers/${waiverId}`, data)
}

export function updateWaiverStatus(waiverId: string, status: string, approvalNote?: string) {
  const params: Record<string, string> = { status }
  if (approvalNote) params.approvalNote = approvalNote
  return client.post<ApiResponse<GovernanceWaiverRequest>>(`/api/governance-workflow/waivers/${waiverId}/status`, {}, { params })
}

export function scanExpiredWaivers() {
  return client.post<ApiResponse<string>>('/api/governance-workflow/waivers/scan-expiry', {})
}

// Workflow Snapshot APIs
export function refreshWorkflowSnapshot() {
  return client.post<ApiResponse<string>>('/api/governance-workflow/snapshots/refresh', {})
}

export function getWorkflowDashboard() {
  return client.get<ApiResponse<GovernanceWorkflowDashboard>>('/api/governance-workflow/dashboard')
}

export function getWorkflowSummary() {
  return client.get<ApiResponse<GovernanceWorkflowSummary>>('/api/governance-workflow/summary')
}

export function getWorkflowReport() {
  return client.get<ApiResponse<GovernanceWorkflowSummary>>('/api/governance-workflow/report')
}

// ========== Governance SLA, Escalation & Ownership (41A) ==========

export interface GovernanceSlaPolicyItem {
  id: string; policyKey: string; displayName: string; priority: string
  category: string | null; slaHours: number; warningHours: number; enabled: boolean
  notes: string | null; createTime: string; updateTime: string
}

export interface CreateGovernanceSlaPolicyRequest {
  policyKey: string; displayName: string; priority: string; category?: string | null
  slaHours?: number; warningHours?: number; notes?: string | null
}

export interface GovernanceEscalationEventItem {
  id: string; recommendationId: string; projectId: string; escalationType: string
  escalationLevel: string; eventStatus: string; summary: string; detail: string | null
  ownerId: string | null; ownerName: string | null; triggeredAt: string; acknowledgedAt: string | null
  resolvedAt: string | null; createTime: string
}

export interface GovernanceEscalationDashboardItem {
  snapshotDate: string; openEscalationCount: number; highEscalationCount: number
  criticalEscalationCount: number; waiverExpiringSoonCount: number; waiverExpiredCount: number
  ownerMissingCount: number; topEscalations: GovernanceEscalationEventItem[]
}

export interface GovernanceOwnershipSnapshotItem {
  id: string; snapshotDate: string; ownerId: string; ownerName: string; totalAssignedCount: number
  openCount: number; inProgressCount: number; overdueCount: number; completed7dCount: number
  activeWaiverCount: number; ownerHealthScore: number; ownerHealthLevel: string; summaryText: string
}

export interface GovernanceOwnershipDashboardItem {
  snapshotDate: string; ownerCount: number; healthyCount: number; watchCount: number
  riskCount: number; criticalCount: number; topOverloadedOwners: GovernanceOwnershipSnapshotItem[]
  topHealthyOwners: GovernanceOwnershipSnapshotItem[]; overallThroughput7d: number
}

export interface GovernanceOperationsSummaryItem {
  snapshotDate: string; slaPolicyCount: number; openEscalationCount: number
  highEscalationCount: number; criticalEscalationCount: number; healthyOwnerCount: number
  watchOwnerCount: number; riskOwnerCount: number; criticalOwnerCount: number
  overdueRecommendationCount: number; waiverExpiringSoonCount: number
  overallThroughput7d: number; summaryMarkdown: string
}

// SLA Policy APIs
export function createSlaPolicy(data: CreateGovernanceSlaPolicyRequest) {
  return client.post<ApiResponse<GovernanceSlaPolicyItem>>('/api/governance-operations/sla-policies', data)
}
export function listSlaPolicies() {
  return client.get<ApiResponse<GovernanceSlaPolicyItem[]>>('/api/governance-operations/sla-policies')
}
export function getSlaPolicy(policyId: string) {
  return client.get<ApiResponse<GovernanceSlaPolicyItem>>(`/api/governance-operations/sla-policies/${policyId}`)
}
export function updateSlaPolicy(policyId: string, data: { displayName?: string; priority?: string; slaHours?: number; warningHours?: number; notes?: string | null }) {
  return client.put<ApiResponse<GovernanceSlaPolicyItem>>(`/api/governance-operations/sla-policies/${policyId}`, data)
}
export function updateSlaPolicyStatus(policyId: string, enabled: boolean) {
  return client.post<ApiResponse<GovernanceSlaPolicyItem>>(`/api/governance-operations/sla-policies/${policyId}/status`, {}, { params: { enabled } })
}

// Escalation APIs
export function scanEscalations() {
  return client.post<ApiResponse<string>>('/api/governance-operations/escalations/scan', {})
}
export function listEscalations() {
  return client.get<ApiResponse<GovernanceEscalationEventItem[]>>('/api/governance-operations/escalations')
}
export function getEscalationDashboard() {
  return client.get<ApiResponse<GovernanceEscalationDashboardItem>>('/api/governance-operations/escalations/dashboard')
}
export function updateEscalationStatus(eventId: string, status: string) {
  return client.post<ApiResponse<GovernanceEscalationEventItem>>(`/api/governance-operations/escalations/${eventId}/status`, {}, { params: { status } })
}

// Ownership APIs
export function refreshOwnership() {
  return client.post<ApiResponse<string>>('/api/governance-operations/ownership/refresh', {})
}
export function getOwnershipList() {
  return client.get<ApiResponse<GovernanceOwnershipSnapshotItem[]>>('/api/governance-operations/ownership')
}
export function getOwnershipDashboard() {
  return client.get<ApiResponse<GovernanceOwnershipDashboardItem>>('/api/governance-operations/ownership/dashboard')
}

// Summary APIs
export function getGovernanceOperationsSummary() {
  return client.get<ApiResponse<GovernanceOperationsSummaryItem>>('/api/governance-operations/summary')
}
export function getGovernanceOperationsReport() {
  return client.get<ApiResponse<GovernanceOperationsSummaryItem>>('/api/governance-operations/report')
}

// ========== Governance Forecast & Risk (41B) ==========

export interface GovernanceCapacityForecastItem {
  id: string; snapshotDate: string; forecastHorizonDays: number; ownerId: string; ownerName: string
  currentOpenCount: number; currentOverdueCount: number; avgCompletedPerDay: number
  projectedNewItems: number; projectedCompletedItems: number; projectedBacklogCount: number
  projectedOverdueCount: number; capacityRiskLevel: string; summaryText: string
}

export interface GovernanceCapacityDashboardItem {
  snapshotDate: string; ownerCount: number; lowRiskCount: number; watchCount: number
  highCount: number; criticalCount: number; topRiskOwners: GovernanceCapacityForecastItem[]
  averageProjectedBacklog: number; averageProjectedOverdue: number
}

export interface PredictiveRiskSignalItem {
  id: string; snapshotDate: string; targetType: string; targetId: string | null; targetName: string
  signalType: string; riskLevel: string; riskScore: number; probabilityScore: number
  timeHorizonDays: number; summary: string; detail: string | null
}

export interface PredictiveRiskDashboardItem {
  snapshotDate: string; signalCount: number; highSignalCount: number; criticalSignalCount: number
  ownerRiskSignals: number; projectRiskSignals: number; portfolioRiskSignals: number
  topSignals: PredictiveRiskSignalItem[]
}

export interface GovernanceBacklogSnapshotItem {
  id: string; snapshotDate: string; projectId: string; projectName: string
  openCount: number; inProgressCount: number; blockedCount: number; overdueCount: number
  waiverActiveCount: number; incoming7dCount: number; completed7dCount: number
  backlogGrowthRate: number; backlogHealthLevel: string; summaryText: string
}

export interface GovernanceBacklogDashboardItem {
  snapshotDate: string; projectCount: number; healthyCount: number; watchCount: number
  riskCount: number; criticalCount: number; topGrowingBacklogs: GovernanceBacklogSnapshotItem[]
  topOverdueProjects: GovernanceBacklogSnapshotItem[]
}

export interface GovernanceForecastSummaryItem {
  snapshotDate: string; ownerForecastCount: number; criticalOwnerCount: number; highOwnerCount: number
  signalCount: number; criticalSignalCount: number; projectCount: number; criticalBacklogCount: number
  riskBacklogCount: number; totalProjectedBacklog: number; totalProjectedOverdue: number; summaryMarkdown: string
}

// Capacity Forecast APIs
export function refreshCapacityForecast(horizonDays = 7) {
  return client.post<ApiResponse<string>>(`/api/governance-forecast/capacity/refresh?horizonDays=${horizonDays}`, {})
}
export function getCapacityForecasts(horizonDays?: number) {
  const params = horizonDays ? { params: { horizonDays } } : {}
  return client.get<ApiResponse<GovernanceCapacityForecastItem[]>>('/api/governance-forecast/capacity', params)
}
export function getCapacityDashboard(horizonDays?: number) {
  const params = horizonDays ? { params: { horizonDays } } : {}
  return client.get<ApiResponse<GovernanceCapacityDashboardItem>>('/api/governance-forecast/capacity/dashboard', params)
}

// Risk Signal APIs
export function refreshRiskSignals() {
  return client.post<ApiResponse<string>>('/api/governance-forecast/risk-signals/refresh', {})
}
export function getRiskSignals() {
  return client.get<ApiResponse<PredictiveRiskSignalItem[]>>('/api/governance-forecast/risk-signals')
}
export function getRiskDashboard() {
  return client.get<ApiResponse<PredictiveRiskDashboardItem>>('/api/governance-forecast/risk-signals/dashboard')
}

// Backlog APIs
export function refreshBacklog() {
  return client.post<ApiResponse<string>>('/api/governance-forecast/backlog/refresh', {})
}
export function getBacklogList() {
  return client.get<ApiResponse<GovernanceBacklogSnapshotItem[]>>('/api/governance-forecast/backlog')
}
export function getBacklogDashboard() {
  return client.get<ApiResponse<GovernanceBacklogDashboardItem>>('/api/governance-forecast/backlog/dashboard')
}

// Summary APIs
export function getGovernanceForecastSummary() {
  return client.get<ApiResponse<GovernanceForecastSummaryItem>>('/api/governance-forecast/summary')
}
export function getGovernanceForecastReport() {
  return client.get<ApiResponse<GovernanceForecastSummaryItem>>('/api/governance-forecast/report')
}

// ========== Governance Simulation & Tuning (41C) ==========

export interface GovernanceSimulationScenarioItem {
  id: string; scenarioName: string; scenarioType: string
  baselineSnapshotDate: string | null; scenarioStatus: string; inputJson: string
  notes: string | null; createdBy: string | null; createdByName: string | null
  createTime: string; updateTime: string
}

export interface GovernanceSimulationResultItem {
  id: string; scenarioId: string; resultStatus: string; impactedOwnerCount: number
  impactedProjectCount: number; projectedBacklogDelta: number; projectedOverdueDelta: number
  projectedRiskDelta: number; projectedCapacityDelta: number; summaryText: string
  detailJson: string | null; reportMarkdown: string | null; calculatedAt: string
}

export interface GovernanceSimulationComparisonItem {
  scenarioId: string; scenarioName: string; scenarioType: string
  baselineProjectedBacklog: number; simulatedProjectedBacklog: number
  baselineProjectedOverdue: number; simulatedProjectedOverdue: number
  baselineRiskScore: number; simulatedRiskScore: number; deltaSummary: string
}

export interface PolicyTuningSuggestionItem {
  id: string; snapshotDate: string; suggestionType: string; priority: string
  targetScope: string; targetKey: string | null; currentValue: string | null
  suggestedValue: string | null; expectedImpactText: string; rationaleText: string | null
}

export interface GovernanceSimulationDashboardItem {
  snapshotDate: string; scenarioCount: number; successfulScenarioCount: number
  warningScenarioCount: number; noImprovementCount: number
  topScenarios: GovernanceSimulationScenarioItem[]
  topSuggestions: PolicyTuningSuggestionItem[]
}

export interface CreateSimulationScenarioRequest {
  scenarioName: string; scenarioType: string; inputJson?: string; notes?: string
}

// Scenario APIs
export function createSimulationScenario(data: CreateSimulationScenarioRequest) {
  return client.post<ApiResponse<GovernanceSimulationScenarioItem>>('/api/governance-simulation/scenarios', data)
}
export function listSimulationScenarios() {
  return client.get<ApiResponse<GovernanceSimulationScenarioItem[]>>('/api/governance-simulation/scenarios')
}
export function getSimulationScenario(scenarioId: string) {
  return client.get<ApiResponse<GovernanceSimulationScenarioItem>>(`/api/governance-simulation/scenarios/${scenarioId}`)
}
export function updateSimulationScenario(scenarioId: string, data: { scenarioName?: string; inputJson?: string; notes?: string }) {
  return client.put<ApiResponse<GovernanceSimulationScenarioItem>>(`/api/governance-simulation/scenarios/${scenarioId}`, data)
}
export function updateSimulationScenarioStatus(scenarioId: string, status: string) {
  return client.post<ApiResponse<GovernanceSimulationScenarioItem>>(`/api/governance-simulation/scenarios/${scenarioId}/status`, {}, { params: { status } })
}
export function runSimulationScenario(scenarioId: string) {
  return client.post<ApiResponse<GovernanceSimulationResultItem>>(`/api/governance-simulation/scenarios/${scenarioId}/run`, {})
}

// Result / Comparison APIs
export function getSimulationResult(scenarioId: string) {
  return client.get<ApiResponse<GovernanceSimulationResultItem>>(`/api/governance-simulation/scenarios/${scenarioId}/result`)
}
export function getSimulationComparison(scenarioId: string) {
  return client.get<ApiResponse<GovernanceSimulationComparisonItem>>(`/api/governance-simulation/scenarios/${scenarioId}/comparison`)
}
export function getSimulationReport() {
  return client.get<ApiResponse<string>>('/api/governance-simulation/report')
}

// Suggestion APIs
export function refreshTuningSuggestions() {
  return client.post<ApiResponse<string>>('/api/governance-simulation/suggestions/refresh', {})
}
export function listTuningSuggestions() {
  return client.get<ApiResponse<PolicyTuningSuggestionItem[]>>('/api/governance-simulation/suggestions')
}

// Dashboard API
export function getSimulationDashboard() {
  return client.get<ApiResponse<GovernanceSimulationDashboardItem>>('/api/governance-simulation/dashboard')
}

// ========== Governance Execution & Playbook (42A) ==========

export interface GovernancePlaybookTemplateItem {
  id: string; templateKey: string; displayName: string; recommendationCategory: string | null
  guardrailKey: string | null; priority: string | null; enabled: boolean; templateStepsJson: string
  successCriteriaJson: string | null; handoffNotes: string | null; createTime: string; updateTime: string
}

export interface GovernanceExecutionPlanItem {
  id: string; recommendationId: string; projectId: string; planStatus: string; templateKey: string | null
  ownerId: string | null; ownerName: string | null; dueAt: string | null; stepsJson: string
  completionRate: number; summaryText: string; createTime: string; updateTime: string
}

export interface GovernanceHandoffChecklistItem {
  id: string; recommendationId: string; executionPlanId: string | null; fromOwnerId: string | null
  fromOwnerName: string | null; toOwnerId: string | null; toOwnerName: string | null
  checklistStatus: string; checklistItemsJson: string; handoffNote: string | null
  handedOffAt: string | null; createTime: string; updateTime: string
}

export interface GovernanceExecutionDashboardItem {
  totalPlanCount: number; readyPlanCount: number; inProgressPlanCount: number
  blockedPlanCount: number; completedPlanCount: number; averageCompletionRate: number
  handoffOpenCount: number; topBlockedPlans: GovernanceExecutionPlanItem[]
  topNearDuePlans: GovernanceExecutionPlanItem[]
}

export interface CreatePlaybookTemplateRequest {
  templateKey: string; displayName: string; recommendationCategory?: string; guardrailKey?: string
  priority?: string; templateStepsJson?: string; successCriteriaJson?: string; handoffNotes?: string
}

// Playbook Template APIs
export function createPlaybookTemplate(data: CreatePlaybookTemplateRequest) {
  return client.post<ApiResponse<GovernancePlaybookTemplateItem>>('/api/governance-execution/playbook-templates', data)
}
export function listPlaybookTemplates() {
  return client.get<ApiResponse<GovernancePlaybookTemplateItem[]>>('/api/governance-execution/playbook-templates')
}
export function getPlaybookTemplate(templateId: string) {
  return client.get<ApiResponse<GovernancePlaybookTemplateItem>>(`/api/governance-execution/playbook-templates/${templateId}`)
}
export function updatePlaybookTemplate(templateId: string, data: { displayName?: string; templateStepsJson?: string }) {
  return client.put<ApiResponse<GovernancePlaybookTemplateItem>>(`/api/governance-execution/playbook-templates/${templateId}`, data)
}
export function updatePlaybookTemplateStatus(templateId: string, enabled: boolean) {
  return client.post<ApiResponse<GovernancePlaybookTemplateItem>>(`/api/governance-execution/playbook-templates/${templateId}/status`, {}, { params: { enabled } })
}
export function getPlaybookMatchPreview(recommendationId: string) {
  return client.get<ApiResponse<any>>(`/api/governance-execution/playbook-match-preview/${recommendationId}`)
}

// Execution Plan APIs
export function createExecutionPlan(recommendationId: string, templateKey?: string) {
  const params: Record<string, string> = { recommendationId }
  if (templateKey) params.templateKey = templateKey
  return client.post<ApiResponse<GovernanceExecutionPlanItem>>('/api/governance-execution/plans', {}, { params })
}
export function listExecutionPlans() {
  return client.get<ApiResponse<GovernanceExecutionPlanItem[]>>('/api/governance-execution/plans')
}
export function getExecutionPlan(planId: string) {
  return client.get<ApiResponse<GovernanceExecutionPlanItem>>(`/api/governance-execution/plans/${planId}`)
}
export function updateExecutionPlan(planId: string, data: { ownerName?: string; summaryText?: string }) {
  return client.put<ApiResponse<GovernanceExecutionPlanItem>>(`/api/governance-execution/plans/${planId}`, {}, { params: data })
}
export function updateExecutionPlanStatus(planId: string, status: string) {
  return client.post<ApiResponse<GovernanceExecutionPlanItem>>(`/api/governance-execution/plans/${planId}/status`, {}, { params: { status } })
}
export function updateExecutionStepStatus(planId: string, stepKey: string, status: string) {
  return client.post<ApiResponse<GovernanceExecutionPlanItem>>(`/api/governance-execution/plans/${planId}/steps/${stepKey}/status`, {}, { params: { status } })
}
export function getExecutionDashboard() {
  return client.get<ApiResponse<GovernanceExecutionDashboardItem>>('/api/governance-execution/dashboard')
}
export function getExecutionReport() {
  return client.get<ApiResponse<string>>('/api/governance-execution/report')
}

// Handoff APIs
export function createHandoffChecklist(recommendationId: string, fromOwnerName?: string, toOwnerName?: string) {
  const params: Record<string, string> = { recommendationId }
  if (fromOwnerName) params.fromOwnerName = fromOwnerName
  if (toOwnerName) params.toOwnerName = toOwnerName
  return client.post<ApiResponse<GovernanceHandoffChecklistItem>>('/api/governance-execution/handoffs', {}, { params })
}
export function listHandoffChecklists() {
  return client.get<ApiResponse<GovernanceHandoffChecklistItem[]>>('/api/governance-execution/handoffs')
}
export function getHandoffChecklist(checklistId: string) {
  return client.get<ApiResponse<GovernanceHandoffChecklistItem>>(`/api/governance-execution/handoffs/${checklistId}`)
}
export function updateHandoffChecklist(checklistId: string, handoffNote?: string) {
  const params = handoffNote ? { params: { handoffNote } } : {}
  return client.put<ApiResponse<GovernanceHandoffChecklistItem>>(`/api/governance-execution/handoffs/${checklistId}`, {}, params)
}
export function updateHandoffChecklistStatus(checklistId: string, status: string) {
  return client.post<ApiResponse<GovernanceHandoffChecklistItem>>(`/api/governance-execution/handoffs/${checklistId}/status`, {}, { params: { status } })
}

// ========== Governance Knowledge & Recipe (42B) ==========

export interface GovernanceKnowledgeEntryItem {
  id: string; projectId: string | null; sourceType: string; sourceId: string | null; title: string
  category: string; tagsJson: string | null; summaryText: string | null; detailMarkdown: string | null
  effectivenessScore: number; reuseCount: number; createTime: string; updateTime: string
}

export interface GovernanceKnowledgeDashboardItem {
  entryCount: number; patternCount: number; recipeCount: number
  topKnowledgeEntries: GovernanceKnowledgeEntryItem[]
  topRecipes: GovernanceRemediationRecipeItem[]
  topPatterns: GovernancePatternLibraryItem[]
  averageEffectivenessScore: number; highReuseCount: number
}

export interface GovernancePatternLibraryItem {
  id: string; patternKey: string; displayName: string; recommendationCategory: string | null
  guardrailKey: string | null; priority: string | null; patternJson: string; notes: string | null
  enabled: boolean; createTime: string; updateTime: string
}

export interface GovernanceRemediationRecipeItem {
  id: string; recipeKey: string; displayName: string; recipeType: string
  recommendationCategory: string | null; guardrailKey: string | null; stepsJson: string
  prerequisitesJson: string | null; successCriteriaJson: string | null
  effectivenessScore: number; usageCount: number; enabled: boolean; createTime: string; updateTime: string
}

// Knowledge Entry APIs
export function createKnowledgeEntry(title: string, category: string, sourceType?: string, summaryText?: string, detailMarkdown?: string, tagsJson?: string) {
  const params: Record<string, string> = { title, category }
  if (sourceType) params.sourceType = sourceType; if (summaryText) params.summaryText = summaryText
  if (detailMarkdown) params.detailMarkdown = detailMarkdown; if (tagsJson) params.tagsJson = tagsJson
  return client.post<ApiResponse<GovernanceKnowledgeEntryItem>>('/api/governance-knowledge/entries', {}, { params })
}
export function listKnowledgeEntries() { return client.get<ApiResponse<GovernanceKnowledgeEntryItem[]>>('/api/governance-knowledge/entries') }
export function getKnowledgeEntry(entryId: string) { return client.get<ApiResponse<GovernanceKnowledgeEntryItem>>(`/api/governance-knowledge/entries/${entryId}`) }
export function searchKnowledgeEntries(keyword?: string, category?: string) {
  const params: Record<string, string> = {}
  if (keyword) params.keyword = keyword; if (category) params.category = category
  return client.get<ApiResponse<GovernanceKnowledgeEntryItem[]>>('/api/governance-knowledge/search', { params })
}
export function getKnowledgeDashboard() { return client.get<ApiResponse<GovernanceKnowledgeDashboardItem>>('/api/governance-knowledge/dashboard') }
export function getKnowledgeReport() { return client.get<ApiResponse<string>>('/api/governance-knowledge/report') }

// Pattern Library APIs
export function createPattern(patternKey: string, displayName: string, recommendationCategory?: string, guardrailKey?: string, priority?: string, patternJson?: string) {
  const params: Record<string, string> = { patternKey, displayName }
  if (recommendationCategory) params.recommendationCategory = recommendationCategory
  if (guardrailKey) params.guardrailKey = guardrailKey; if (priority) params.priority = priority
  if (patternJson) params.patternJson = patternJson
  return client.post<ApiResponse<GovernancePatternLibraryItem>>('/api/governance-knowledge/patterns', {}, { params })
}
export function listPatterns() { return client.get<ApiResponse<GovernancePatternLibraryItem[]>>('/api/governance-knowledge/patterns') }
export function getPattern(patternId: string) { return client.get<ApiResponse<GovernancePatternLibraryItem>>(`/api/governance-knowledge/patterns/${patternId}`) }
export function updatePatternStatus(patternId: string, enabled: boolean) { return client.post<ApiResponse<GovernancePatternLibraryItem>>(`/api/governance-knowledge/patterns/${patternId}/status`, {}, { params: { enabled } }) }

// Recipe APIs
export function createRecipe(recipeKey: string, displayName: string, recipeType?: string, recommendationCategory?: string, guardrailKey?: string, stepsJson?: string) {
  const params: Record<string, string> = { recipeKey, displayName }
  if (recipeType) params.recipeType = recipeType; if (recommendationCategory) params.recommendationCategory = recommendationCategory
  if (guardrailKey) params.guardrailKey = guardrailKey; if (stepsJson) params.stepsJson = stepsJson
  return client.post<ApiResponse<GovernanceRemediationRecipeItem>>('/api/governance-knowledge/recipes', {}, { params })
}
export function listRecipes() { return client.get<ApiResponse<GovernanceRemediationRecipeItem[]>>('/api/governance-knowledge/recipes') }
export function getRecipe(recipeId: string) { return client.get<ApiResponse<GovernanceRemediationRecipeItem>>(`/api/governance-knowledge/recipes/${recipeId}`) }
export function updateRecipeStatus(recipeId: string, enabled: boolean) { return client.post<ApiResponse<GovernanceRemediationRecipeItem>>(`/api/governance-knowledge/recipes/${recipeId}/status`, {}, { params: { enabled } }) }
export function getRecipeRecommendations(recommendationId: string) { return client.get<ApiResponse<GovernanceRemediationRecipeItem[]>>(`/api/governance-knowledge/recipe-recommendations/${recommendationId}`) }

// ========== Governance Effectiveness Analytics (42C) ==========

export interface GovernanceRecipeEffectivenessSnapshotItem {
  id: string; snapshotDate: string; recipeId: string; recipeKey: string; recipeName: string
  usageCount: number; completionCount: number; successRate: number; avgCompletionHours: number
  failureRate: number; effectivenessScore: number; effectivenessLevel: string; summaryText: string
}

export interface GovernancePlaybookAnalyticsItem {
  id: string; snapshotDate: string; templateKey: string; templateName: string; planCount: number
  completedPlanCount: number; blockedPlanCount: number; avgCompletionRate: number
  avgStepCompletionRate: number; avgResolutionHours: number; relatedRecipeCount: number
}

export interface GovernanceOptimizationSuggestionItem {
  id: string; snapshotDate: string; suggestionType: string; priority: string; targetType: string
  targetKey: string; currentMetricValue: string | null; suggestedAction: string
  expectedImpactText: string; rationaleText: string | null
}

// Recipe Effectiveness APIs
export function refreshRecipeEffectiveness() { return client.post<ApiResponse<string>>('/api/governance-effectiveness/recipes/refresh', {}) }
export function getRecipeEffectiveness(level?: string) { const params = level ? { params: { level } } : {}; return client.get<ApiResponse<GovernanceRecipeEffectivenessSnapshotItem[]>>('/api/governance-effectiveness/recipes', params) }
export function getRecipeEffectivenessDashboard() { return client.get<ApiResponse<any>>('/api/governance-effectiveness/recipes/dashboard') }
export function getRecipeTrend(window?: string) { const params = window ? { params: { window } } : {}; return client.get<ApiResponse<GovernanceRecipeEffectivenessSnapshotItem[]>>('/api/governance-effectiveness/recipes/trend', params) }

// Playbook Analytics APIs
export function refreshPlaybookAnalytics() { return client.post<ApiResponse<string>>('/api/governance-effectiveness/playbooks/refresh', {}) }
export function getPlaybookAnalytics() { return client.get<ApiResponse<GovernancePlaybookAnalyticsItem[]>>('/api/governance-effectiveness/playbooks') }
export function getPlaybookAnalyticsDashboard() { return client.get<ApiResponse<any>>('/api/governance-effectiveness/playbooks/dashboard') }

// Optimization APIs
export function refreshOptimizations() { return client.post<ApiResponse<string>>('/api/governance-effectiveness/optimizations/refresh', {}) }
export function getOptimizations() { return client.get<ApiResponse<GovernanceOptimizationSuggestionItem[]>>('/api/governance-effectiveness/optimizations') }
export function getOptimizationDashboard() { return client.get<ApiResponse<any>>('/api/governance-effectiveness/optimizations/dashboard') }
export function getEffectivenessReport() { return client.get<ApiResponse<string>>('/api/governance-effectiveness/report') }

// ========== Governance Copilot Workspace (43A) ==========

export interface GovernanceWorkspaceSessionItem {
  id: string; operatorId: string | null; operatorName: string | null; sessionStatus: string
  focusMode: string; selectedProjectId: string | null; selectedRecommendationId: string | null
  selectedOwnerId: string | null; contextSummary: string | null; startedAt: string; endedAt: string | null
  createTime: string; updateTime: string
}

export interface GovernanceGuidedTaskItem {
  id: string; sessionId: string; recommendationId: string | null; taskType: string; priority: string
  taskStatus: string; title: string; summary: string | null; sourceType: string; sourceId: string | null
  linkedPlaybookKey: string | null; linkedRecipeKey: string | null; linkedKnowledgeEntryId: string | null
  dueAt: string | null; createTime: string; updateTime: string
}

export interface GovernanceNextStepItem {
  id: string; sessionId: string; guidedTaskId: string | null; recommendationId: string | null
  suggestionRank: number; suggestionType: string; title: string; summaryText: string | null
  rationaleText: string | null; expectedOutcomeText: string | null; actionPayloadJson: string | null
}

// Workspace APIs
export function createWorkspaceSession(focusMode?: string) { const params = focusMode ? { params: { focusMode } } : {}; return client.post<ApiResponse<GovernanceWorkspaceSessionItem>>('/api/governance-workspace/sessions', {}, params) }
export function listWorkspaceSessions() { return client.get<ApiResponse<GovernanceWorkspaceSessionItem[]>>('/api/governance-workspace/sessions') }
export function getWorkspaceSession(sessionId: string) { return client.get<ApiResponse<GovernanceWorkspaceSessionItem>>(`/api/governance-workspace/sessions/${sessionId}`) }
export function updateWorkspaceSessionStatus(sessionId: string, status: string) { return client.post<ApiResponse<GovernanceWorkspaceSessionItem>>(`/api/governance-workspace/sessions/${sessionId}/status`, {}, { params: { status } }) }
export function refreshWorkspace(sessionId: string) { return client.post<ApiResponse<any>>(`/api/governance-workspace/sessions/${sessionId}/refresh`, {}) }
export function getWorkspaceNextSteps(sessionId: string) { return client.get<ApiResponse<GovernanceNextStepItem[]>>(`/api/governance-workspace/sessions/${sessionId}/next-steps`) }
export function getWorkspaceDashboard() { return client.get<ApiResponse<any>>('/api/governance-workspace/dashboard') }
export function getWorkspaceReport() { return client.get<ApiResponse<string>>('/api/governance-workspace/report') }

// Guided Task APIs
export function getWorkspaceTasks(sessionId: string) { return client.get<ApiResponse<GovernanceGuidedTaskItem[]>>(`/api/governance-workspace/sessions/${sessionId}/tasks`) }
export function updateWorkspaceTaskStatus(taskId: string, status: string) { return client.post<ApiResponse<GovernanceGuidedTaskItem>>(`/api/governance-workspace/tasks/${taskId}/status`, {}, { params: { status } }) }

// ========== Governance Operator Learning (43B) ==========

export interface GovernanceOperatorActionItem {
  id: string; sessionId: string; guidedTaskId: string | null; recommendationId: string | null
  operatorId: string | null; operatorName: string | null; actionType: string; actionTargetType: string
  actionTargetId: string | null; acceptedFlag: boolean; successFlag: boolean; durationSeconds: number | null
  noteText: string | null; occurredAt: string; createTime: string
}

export interface GovernanceSessionInsightItem {
  id: string; sessionId: string; operatorId: string | null; operatorName: string | null
  insightWindow: string; totalActions: number; acceptedRecommendationCount: number
  dismissedRecommendationCount: number; completedGuidedTaskCount: number
  blockedGuidedTaskCount: number; avgActionDurationSeconds: number | null
  productivityScore: number; dominantActionPattern: string | null; summaryMarkdown: string | null
}

export interface GovernanceRemediationReuseBundleItem {
  id: string; bundleKey: string; title: string; category: string; guardrailKey: string | null
  priority: string | null; effectivenessLevel: string; reuseCount: number; successRate: number
  actionSequenceJson: string; sourceSessionId: string | null; sourceOperatorId: string | null
  sourceOperatorName: string | null; enabled: boolean; summaryText: string | null
}

// Operator Action APIs
export function recordOperatorAction(sessionId: string, actionType: string, actionTargetType: string, operatorName?: string, acceptedFlag?: boolean, successFlag?: boolean, durationSeconds?: number) {
  const params: Record<string, any> = { sessionId, actionType, actionTargetType }
  if (operatorName) params.operatorName = operatorName; if (acceptedFlag !== undefined) params.acceptedFlag = acceptedFlag
  if (successFlag !== undefined) params.successFlag = successFlag; if (durationSeconds !== undefined) params.durationSeconds = durationSeconds
  return client.post<ApiResponse<GovernanceOperatorActionItem>>('/api/governance-operator-memory/actions', {}, { params })
}
export function listOperatorActions(sessionId?: string) { const params = sessionId ? { params: { sessionId } } : {}; return client.get<ApiResponse<GovernanceOperatorActionItem[]>>('/api/governance-operator-memory/actions', params) }

// Insight APIs
export function refreshSessionInsight(sessionId: string) { return client.post<ApiResponse<string>>('/api/governance-operator-memory/insights/refresh', {}, { params: { sessionId } }) }
export function listSessionInsights() { return client.get<ApiResponse<GovernanceSessionInsightItem[]>>('/api/governance-operator-memory/insights') }
export function getOperatorLearningDashboard() { return client.get<ApiResponse<any>>('/api/governance-operator-memory/dashboard') }
export function getOperatorLearningReport() { return client.get<ApiResponse<string>>('/api/governance-operator-memory/report') }

// Reuse Bundle APIs
export function createReuseBundle(bundleKey: string, title: string, category: string, guardrailKey?: string, priority?: string, actionSequenceJson?: string) {
  const params: Record<string, string> = { bundleKey, title, category }
  if (guardrailKey) params.guardrailKey = guardrailKey; if (priority) params.priority = priority; if (actionSequenceJson) params.actionSequenceJson = actionSequenceJson
  return client.post<ApiResponse<GovernanceRemediationReuseBundleItem>>('/api/governance-operator-memory/reuse-bundles', {}, { params })
}
export function listReuseBundles() { return client.get<ApiResponse<GovernanceRemediationReuseBundleItem[]>>('/api/governance-operator-memory/reuse-bundles') }
export function updateReuseBundleStatus(bundleId: string, enabled: boolean) { return client.post<ApiResponse<GovernanceRemediationReuseBundleItem>>(`/api/governance-operator-memory/reuse-bundles/${bundleId}/status`, {}, { params: { enabled } }) }
export function refreshReuseBundles() { return client.post<ApiResponse<string>>('/api/governance-operator-memory/reuse-bundles/refresh', {}) }

// ========== Governance Copilot Tuning (43C) ==========

export interface GovernanceOperatorFeedbackItem {
  id: string; sessionId: string; operatorId: string | null; operatorName: string | null
  suggestionType: string | null; suggestionId: string | null; guidedTaskId: string | null
  reuseBundleId: string | null; feedbackTargetType: string; feedbackRating: number
  helpfulFlag: boolean; acceptedFlag: boolean; reasonCode: string | null; noteText: string | null
  createTime: string
}

export interface GovernanceAdaptiveGuidanceSignalItem {
  id: string; signalType: string; focusMode: string | null; category: string | null
  suggestionType: string | null; recommendationPriority: string | null
  acceptanceRate: number; completionRate: number; avgFeedbackRating: number
  weightScore: number; signalLevel: string; rationaleText: string | null
}

export interface GovernanceCopilotTuningSnapshotItem {
  id: string; snapshotWindow: string; totalFeedbackCount: number; acceptanceRate: number
  dismissalRate: number; avgFeedbackRating: number; topSuggestionType: string | null
  weakestSuggestionType: string | null; topFocusMode: string | null; weakestFocusMode: string | null
  tuningConfidenceScore: number; summaryMarkdown: string | null
}

// Feedback APIs
export function recordFeedback(sessionId: string, feedbackTargetType: string, feedbackRating: number, helpfulFlag?: boolean, acceptedFlag?: boolean, reasonCode?: string) {
  const params: Record<string, any> = { sessionId, feedbackTargetType, feedbackRating }
  if (helpfulFlag !== undefined) params.helpfulFlag = helpfulFlag; if (acceptedFlag !== undefined) params.acceptedFlag = acceptedFlag
  if (reasonCode) params.reasonCode = reasonCode
  return client.post<ApiResponse<GovernanceOperatorFeedbackItem>>('/api/governance-copilot/feedback', {}, { params })
}
export function listFeedback(sessionId?: string) { const params = sessionId ? { params: { sessionId } } : {}; return client.get<ApiResponse<GovernanceOperatorFeedbackItem[]>>('/api/governance-copilot/feedback', params) }

// Adaptive Signal APIs
export function refreshAdaptiveSignals() { return client.post<ApiResponse<string>>('/api/governance-copilot/signals/refresh', {}) }
export function listAdaptiveSignals() { return client.get<ApiResponse<GovernanceAdaptiveGuidanceSignalItem[]>>('/api/governance-copilot/signals') }
export function getAdaptiveSignalDashboard() { return client.get<ApiResponse<any>>('/api/governance-copilot/signals/dashboard') }

// Tuning APIs
export function refreshCopilotTuning() { return client.post<ApiResponse<string>>('/api/governance-copilot/tuning/refresh', {}) }
export function listCopilotTuningSnapshots() { return client.get<ApiResponse<GovernanceCopilotTuningSnapshotItem[]>>('/api/governance-copilot/tuning/snapshots') }
export function getCopilotTuningDashboard() { return client.get<ApiResponse<any>>('/api/governance-copilot/tuning/dashboard') }
export function getCopilotTuningReport() { return client.get<ApiResponse<string>>('/api/governance-copilot/tuning/report') }

// ========== Governance Draft Planning (44A) ==========

export interface GovernanceDraftPlanItem {
  id: string; recommendationId: string | null; sessionId: string | null; operatorId: string | null
  operatorName: string | null; planStatus: string; planTitle: string; scopeType: string
  summaryText: string | null; goalText: string | null; proposedStepsJson: string
  linkedBundleId: string | null; linkedPlaybookKey: string | null; linkedRecipeKey: string | null
  riskLevel: string; humanConfirmationRequired: boolean; createTime: string; updateTime: string
}

export interface GovernanceAssistiveActionItem {
  id: string; draftPlanId: string; actionType: string; actionStatus: string; actionTitle: string
  actionSummary: string | null; safetyLevel: string; confirmationRequired: boolean
  checklistJson: string; prefillPayloadJson: string | null; actionOrder: number
  createTime: string; updateTime: string
}

export interface GovernanceRecommendationPackageItem {
  id: string; recommendationId: string | null; draftPlanId: string | null; packageStatus: string
  packageTitle: string; packageSummary: string | null; recommendationContextJson: string
  attachmentsJson: string | null; reviewNotesText: string | null
  submitReadyFlag: boolean; submittedFlag: boolean; createTime: string; updateTime: string
}

// Draft Plan APIs
export function createDraftPlan(planTitle: string, scopeType?: string) {
  const params: Record<string, string> = { planTitle }; if (scopeType) params.scopeType = scopeType
  return client.post<ApiResponse<GovernanceDraftPlanItem>>('/api/governance-draft-plans', {}, { params })
}
export function listDraftPlans() { return client.get<ApiResponse<GovernanceDraftPlanItem[]>>('/api/governance-draft-plans') }
export function updateDraftPlanStatus(planId: string, status: string) { return client.post<ApiResponse<GovernanceDraftPlanItem>>(`/api/governance-draft-plans/${planId}/status`, {}, { params: { status } }) }
export function refreshDraftPlan(planId: string) { return client.post<ApiResponse<GovernanceDraftPlanItem>>(`/api/governance-draft-plans/${planId}/refresh`, {}) }
export function generateAssistiveActions(planId: string) { return client.post<ApiResponse<GovernanceAssistiveActionItem[]>>(`/api/governance-draft-plans/${planId}/assistive-actions/generate`, {}) }
export function listAssistiveActions(planId: string) { return client.get<ApiResponse<GovernanceAssistiveActionItem[]>>(`/api/governance-draft-plans/${planId}/assistive-actions`) }
export function updateAssistiveActionStatus(actionId: string, status: string) { return client.post<ApiResponse<GovernanceAssistiveActionItem>>(`/api/governance-assistive-actions/${actionId}/status`, {}, { params: { status } }) }
export function listRecommendationPackages() { return client.get<ApiResponse<GovernanceRecommendationPackageItem[]>>('/api/governance-recommendation-packages') }
export function updatePackageStatus(packageId: string, status: string) { return client.post<ApiResponse<GovernanceRecommendationPackageItem>>(`/api/governance-recommendation-packages/${packageId}/status`, {}, { params: { status } }) }
export function getDraftPlanningDashboard() { return client.get<ApiResponse<any>>('/api/governance-draft-planning/dashboard') }
export function getDraftPlanningReport() { return client.get<ApiResponse<string>>('/api/governance-draft-planning/report') }

// ========== Governance Outcome Review (44B) ==========

export interface GovernanceDraftAdoptionReviewItem {
  id: string; draftPlanId: string; recommendationId: string | null; operatorId: string | null
  operatorName: string | null; adoptionResult: string; modificationLevel: string; usefulnessRating: number
  reasonCode: string | null; outcomeNoteText: string | null; reviewedAt: string; createTime: string; updateTime: string
}

export interface GovernanceAssistiveQualityReviewItem {
  id: string; assistiveActionId: string; draftPlanId: string | null; operatorId: string | null
  operatorName: string | null; outcomeResult: string; usefulnessRating: number; reasonCode: string | null
  feedbackText: string | null; reviewedAt: string; createTime: string; updateTime: string
}

export interface GovernancePackageEvaluationItem {
  id: string; packageId: string; draftPlanId: string | null; operatorId: string | null
  operatorName: string | null; evaluationResult: string; completenessScore: number; accuracyScore: number
  overallScore: number; reasonCode: string | null; reviewNotesText: string | null; reviewedAt: string
  createTime: string; updateTime: string
}

// Draft Adoption APIs
export function recordDraftAdoptionReview(draftPlanId: string, adoptionResult: string, usefulnessRating: number, modificationLevel?: string, reasonCode?: string, outcomeNoteText?: string) {
  const p: Record<string, any> = { draftPlanId, adoptionResult, usefulnessRating }; if (modificationLevel) p.modificationLevel = modificationLevel; if (reasonCode) p.reasonCode = reasonCode; if (outcomeNoteText) p.outcomeNoteText = outcomeNoteText
  return client.post<ApiResponse<GovernanceDraftAdoptionReviewItem>>('/api/governance-outcome-review/draft-adoption', {}, { params: p })
}
export function listDraftAdoptionReviews() { return client.get<ApiResponse<GovernanceDraftAdoptionReviewItem[]>>('/api/governance-outcome-review/draft-adoption') }

// Assistive Quality APIs
export function recordAssistiveQualityReview(assistiveActionId: string, outcomeResult: string, usefulnessRating: number, draftPlanId?: string, reasonCode?: string) {
  const p: Record<string, any> = { assistiveActionId, outcomeResult, usefulnessRating }; if (draftPlanId) p.draftPlanId = draftPlanId; if (reasonCode) p.reasonCode = reasonCode
  return client.post<ApiResponse<GovernanceAssistiveQualityReviewItem>>('/api/governance-outcome-review/assistive-quality', {}, { params: p })
}
export function listAssistiveQualityReviews() { return client.get<ApiResponse<GovernanceAssistiveQualityReviewItem[]>>('/api/governance-outcome-review/assistive-quality') }

// Package Evaluation APIs
export function recordPackageEvaluation(packageId: string, evaluationResult: string, completenessScore: number, accuracyScore: number, reasonCode?: string) {
  const p: Record<string, any> = { packageId, evaluationResult, completenessScore, accuracyScore }; if (reasonCode) p.reasonCode = reasonCode
  return client.post<ApiResponse<GovernancePackageEvaluationItem>>('/api/governance-outcome-review/package-evaluation', {}, { params: p })
}
export function listPackageEvaluations() { return client.get<ApiResponse<GovernancePackageEvaluationItem[]>>('/api/governance-outcome-review/package-evaluation') }

// Dashboard
export function getOutcomeReviewDashboard() { return client.get<ApiResponse<any>>('/api/governance-outcome-review/dashboard') }
export function getOutcomeReviewReport() { return client.get<ApiResponse<string>>('/api/governance-outcome-review/report') }

// ========== Governance Draft Optimization (44C) ==========

export interface GovernanceDraftOptimizationSignalItem {
  id: string; signalType: string; scopeType: string; scopeKey: string | null
  adoptionRate: number; rejectionRate: number; avgUsefulnessRating: number; sampleCount: number
  signalLevel: string; suggestionText: string | null
}

export interface GovernanceAssistiveOrderingItem {
  id: string; actionType: string; avgUsefulnessRating: number; avgActionOrder: number
  usefulnessCount: number; notUsefulCount: number; optimizationLevel: string
  suggestedNewOrder: number; rationaleText: string | null
}

export interface GovernancePackageCompositionItem {
  id: string; scoreRange: string; avgCompleteness: number; avgAccuracy: number
  avgOverall: number; sampleCount: number; tuningLevel: string; suggestionText: string | null
}

export function refreshDraftOptimizationSignals() { return client.post<ApiResponse<string>>('/api/governance-draft-optimization/signals/refresh', {}) }
export function listDraftOptimizationSignals() { return client.get<ApiResponse<GovernanceDraftOptimizationSignalItem[]>>('/api/governance-draft-optimization/signals') }
export function refreshAssistiveOrdering() { return client.post<ApiResponse<string>>('/api/governance-draft-optimization/assistive-ordering/refresh', {}) }
export function listAssistiveOrdering() { return client.get<ApiResponse<GovernanceAssistiveOrderingItem[]>>('/api/governance-draft-optimization/assistive-ordering') }
export function refreshPackageComposition() { return client.post<ApiResponse<string>>('/api/governance-draft-optimization/package-composition/refresh', {}) }
export function listPackageComposition() { return client.get<ApiResponse<GovernancePackageCompositionItem[]>>('/api/governance-draft-optimization/package-composition') }
export function getDraftOptimizationDashboard() { return client.get<ApiResponse<any>>('/api/governance-draft-optimization/dashboard') }
export function getDraftOptimizationReport() { return client.get<ApiResponse<string>>('/api/governance-draft-optimization/report') }

// ========== Governance Portfolio Benchmark (45A) ==========

export interface GovernanceBenchmarkItem {
  id: string; snapshotDate: string; benchmarkWindow: string; metricKey: string; metricValue: number
  percentileRank: number; peerAvg: number; peerP90: number; sampleCount: number
  signalLevel: string; summaryText: string
}

export interface GovernanceAlignmentItem {
  id: string; snapshotDate: string; projectId: string; projectName: string; practiceType: string
  alignmentLevel: string; currentScore: number; targetScore: number; gap: number; suggestionText: string | null
}

export interface GovernanceMaturityScorecardItem {
  id: string; snapshotDate: string; projectId: string; projectName: string; maturityLevel: string
  totalScore: number; draftAdoptionScore: number; assistiveQualityScore: number; packageQualityScore: number
  outcomeReviewScore: number; operatorProductivityScore: number; summaryText: string
}

// Benchmark APIs
export function refreshBenchmarks() { return client.post<ApiResponse<string>>('/api/governance-benchmark/benchmarks/refresh', {}) }
export function listBenchmarks() { return client.get<ApiResponse<GovernanceBenchmarkItem[]>>('/api/governance-benchmark/benchmarks') }
export function refreshAlignments() { return client.post<ApiResponse<string>>('/api/governance-benchmark/alignments/refresh', {}) }
export function listAlignments() { return client.get<ApiResponse<GovernanceAlignmentItem[]>>('/api/governance-benchmark/alignments') }
export function refreshScorecards() { return client.post<ApiResponse<string>>('/api/governance-benchmark/scorecards/refresh', {}) }
export function listScorecards() { return client.get<ApiResponse<GovernanceMaturityScorecardItem[]>>('/api/governance-benchmark/scorecards') }
export function getBenchmarkDashboard() { return client.get<ApiResponse<any>>('/api/governance-benchmark/dashboard') }
export function getBenchmarkReport() { return client.get<ApiResponse<string>>('/api/governance-benchmark/report') }

