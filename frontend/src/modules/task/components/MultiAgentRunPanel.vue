<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, defineAsyncComponent } from 'vue'
import { ElMessage } from 'element-plus'
import {
  startMultiAgentRun,
  listMultiAgentRuns,
  getMultiAgentStrategies,
  approveMultiAgentGate,
  rejectMultiAgentGate,
  approveToolExecution,
  rejectToolExecution,
  retryToolExecutionJob,
  cancelToolExecutionJob,
  manualRetryToolExecutionJob,
  type MultiAgentRunResponse,
  type MultiAgentStepResponse,
  type MultiAgentMessageResponse,
  type MultiAgentPhaseResponse,
  type MultiAgentApprovalGateResponse,
  type MultiAgentApprovalDecisionRequest,
  type WorkflowStrategyResponse,
  type ToolSandboxExecutionResponse,
  type ToolExecutionApprovalResponse,
  type ToolExecutionJob,
} from '@/modules/task/api'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { formatDateTime } from '@/shared/utils/format'
import { getPatchProposalReview } from '@/modules/task/api'
import ToolExecutionTraceDrawer from '@/modules/task/components/ToolExecutionTraceDrawer.vue'

const MarkdownRenderer = defineAsyncComponent(() => import('@/shared/components/MarkdownRenderer.vue'))

const props = defineProps<{ taskId: string }>()

const runs = ref<MultiAgentRunResponse[]>([])
const loading = ref(false)
const starting = ref(false)
const selectedRun = ref<MultiAgentRunResponse | null>(null)
const expandedSteps = ref<Record<string, boolean>>({})
const showInputContext = ref<Record<string, boolean>>({})
const showMessages = ref(false)

// Job polling
const pollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const POLL_INTERVAL_MS = 1500

const hasNonTerminalJobs = computed(() => {
  if (!selectedRun.value?.toolExecutions) return false
  return selectedRun.value.toolExecutions.some(te => {
    const status = te.job?.status
    return status === 'PENDING' || status === 'RUNNING'
  })
})

function startJobPolling() {
  stopJobPolling()
  if (!hasNonTerminalJobs.value) return
  pollTimer.value = setInterval(async () => {
    if (!selectedRun.value?.id) return
    try {
      const res = await listMultiAgentRuns(props.taskId)
      runs.value = res.data.data || []
      const updated = runs.value.find(r => r.id === selectedRun.value?.id)
      if (updated) {
        selectedRun.value = updated
      }
      // Stop polling when all jobs are terminal
      const stillPending = runs.value.some(r =>
        r.toolExecutions?.some(te => {
          const s = te.job?.status
          return s === 'PENDING' || s === 'RUNNING'
        })
      )
      if (!stillPending) {
        stopJobPolling()
      }
    } catch {
      // Silently retry on next interval
    }
  }, POLL_INTERVAL_MS)
}

function stopJobPolling() {
  if (pollTimer.value !== null) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

onBeforeUnmount(() => {
  stopJobPolling()
})

// Strategy selection
const strategies = ref<WorkflowStrategyResponse[]>([])
const selectedStrategy = ref('')
const loadingStrategies = ref(false)
const previewStrategy = computed(() =>
  strategies.value.find(s => s.strategyKey === selectedStrategy.value) || null
)

onMounted(() => {
  loadRuns()
  loadStrategies()
})

async function loadRuns() {
  loading.value = true
  try {
    const res = await listMultiAgentRuns(props.taskId)
    runs.value = res.data.data || []
    if (runs.value.length > 0 && !selectedRun.value) {
      selectedRun.value = runs.value[0]
    }
  } catch {
    ElMessage.error('加载多智能体编排记录失败')
  } finally {
    loading.value = false
    startJobPolling()
  }
}

async function loadStrategies() {
  loadingStrategies.value = true
  try {
    const res = await getMultiAgentStrategies()
    strategies.value = res.data.data || []
    if (strategies.value.length > 0 && !selectedStrategy.value) {
      selectedStrategy.value = strategies.value[0].strategyKey
    }
  } catch {
    // strategies are optional; keep default empty
  } finally {
    loadingStrategies.value = false
  }
}

async function handleStart() {
  starting.value = true
  try {
    const res = await startMultiAgentRun(props.taskId, {
      strategy: selectedStrategy.value || undefined,
    })
    const runData = res.data.data
    if (runData.status === 'WAITING_APPROVAL') {
      ElMessage.info('编排已暂停，等待审批')
    } else {
      ElMessage.success('多智能体编排完成')
    }
    await loadRuns()
    selectedRun.value = runs.value.find(r => r.id === runData.id) || runs.value[0] || null
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '启动多智能体编排失败')
  } finally {
    starting.value = false
  }
}

// Approval gate state
const approvalComment = ref('')
const approving = ref(false)
const rejecting = ref(false)

const pendingApprovalGate = computed(() => selectedRun.value?.pendingApprovalGate || null)

async function handleApprove() {
  if (!selectedRun.value || !pendingApprovalGate.value) return
  approving.value = true
  try {
    const payload: MultiAgentApprovalDecisionRequest = { comment: approvalComment.value || undefined }
    const res = await approveMultiAgentGate(selectedRun.value.id, pendingApprovalGate.value.id, payload)
    ElMessage.success('审批已通过，编排继续执行')
    await loadRuns()
    selectedRun.value = runs.value.find(r => r.id === res.data.data.id) || runs.value[0] || null
    approvalComment.value = ''
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '审批操作失败')
  } finally {
    approving.value = false
  }
}

async function handleReject() {
  if (!selectedRun.value || !pendingApprovalGate.value) return
  rejecting.value = true
  try {
    const payload: MultiAgentApprovalDecisionRequest = { comment: approvalComment.value || undefined }
    const res = await rejectMultiAgentGate(selectedRun.value.id, pendingApprovalGate.value.id, payload)
    ElMessage.info('审批已驳回，编排已取消')
    await loadRuns()
    selectedRun.value = runs.value.find(r => r.id === res.data.data.id) || runs.value[0] || null
    approvalComment.value = ''
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '审批操作失败')
  } finally {
    rejecting.value = false
  }
}

function toggleStep(stepId: string) {
  expandedSteps.value[stepId] = !expandedSteps.value[stepId]
}

function toggleInputContext(stepId: string) {
  showInputContext.value[stepId] = !showInputContext.value[stepId]
}

const phases = computed(() => selectedRun.value?.phases || [])

const completedStepCount = computed(() =>
  selectedRun.value?.steps.filter(s => s.status === 'COMPLETED').length || 0
)

const skippedStepCount = computed(() =>
  selectedRun.value?.steps.filter(s => s.status === 'SKIPPED').length || 0
)

const messageCount = computed(() =>
  selectedRun.value?.messages?.length || 0
)

const parallelStepCount = computed(() => {
  const phaseSteps = new Map<number, number>()
  for (const s of selectedRun.value?.steps || []) {
    if (s.phaseOrder != null && s.status !== 'SKIPPED') {
      phaseSteps.set(s.phaseOrder, (phaseSteps.get(s.phaseOrder) || 0) + 1)
    }
  }
  let count = 0
  for (const n of phaseSteps.values()) {
    if (n > 1) count += n
  }
  return count
})

const stepMessages = computed(() => {
  const msgs = selectedRun.value?.messages || []
  const map: Record<string, MultiAgentMessageResponse[]> = {}
  for (const m of msgs) {
    const key = m.toStepId || m.fromStepId || ''
    if (key) {
      if (!map[key]) map[key] = []
      map[key].push(m)
    }
  }
  return map
})

// Tool sandbox stats
const toolExecCount = computed(() =>
  selectedRun.value?.toolExecutions?.length || 0
)
const mockExecCount = computed(() =>
  selectedRun.value?.toolExecutions?.filter(t => t.executionMode === 'MOCK_EXECUTE').length || 0
)
const blockedExecCount = computed(() =>
  selectedRun.value?.toolExecutions?.filter(t => t.status === 'BLOCKED').length || 0
)
const stepToolExecs = computed(() => {
  const execs = selectedRun.value?.toolExecutions || []
  const map: Record<string, ToolSandboxExecutionResponse[]> = {}
  for (const te of execs) {
    const key = te.stepId || ''
    if (key) {
      if (!map[key]) map[key] = []
      map[key].push(te)
    }
  }
  return map
})
const showToolOutput = ref<Record<string, boolean>>({})

function toggleToolOutput(execId: string) {
  showToolOutput.value[execId] = !showToolOutput.value[execId]
}

// Repository read-only tool display
const showFilesRead = ref<Record<string, boolean>>({})

function toggleFilesRead(execId: string) {
  showFilesRead.value[execId] = !showFilesRead.value[execId]
}

function getRepositoryInfo(te: ToolSandboxExecutionResponse): {
  filesRead: string[];
  skippedFiles: Array<{filePath: string; reason: string}>;
  redacted: boolean;
  truncated: boolean;
  branch: string;
  pathPrefix: string;
  isRepoReadTool: boolean
} {
  if (!te.outputPayload) return { filesRead: [], skippedFiles: [], redacted: false, truncated: false, branch: '', pathPrefix: '', isRepoReadTool: false }
  try {
    const parsed = JSON.parse(te.outputPayload)
    if (parsed.readOnly && Array.isArray(parsed.filesRead)) {
      return {
        filesRead: parsed.filesRead,
        skippedFiles: Array.isArray(parsed.skippedFiles) ? parsed.skippedFiles : [],
        redacted: parsed.redacted === true,
        truncated: parsed.truncated === true,
        branch: parsed.branch || '',
        pathPrefix: parsed.pathPrefix || '',
        isRepoReadTool: true,
      }
    }
    return { filesRead: [], skippedFiles: [], redacted: false, truncated: false, branch: '', pathPrefix: '', isRepoReadTool: false }
  } catch {
    return { filesRead: [], skippedFiles: [], redacted: false, truncated: false, branch: '', pathPrefix: '', isRepoReadTool: false }
  }
}

// Code search tool display
function getCodeSearchInfo(te: ToolSandboxExecutionResponse): {
  isCodeSearchTool: boolean; matchedFiles: number; matchedSymbols: number; matchedChunks: number;
  totalCount: number; keyword: string; searchType: string
} {
  if (!te.outputPayload) return { isCodeSearchTool: false, matchedFiles: 0, matchedSymbols: 0, matchedChunks: 0, totalCount: 0, keyword: '', searchType: '' }
  try {
    const parsed = JSON.parse(te.outputPayload)
    const toolName = te.toolName
    if (toolName === 'READ_CODE_INDEX' || toolName === 'SEARCH_CODE_SYMBOL' || toolName === 'SEARCH_CODE_CHUNK') {
      return {
        isCodeSearchTool: true,
        matchedFiles: parsed.matchedFiles || 0,
        matchedSymbols: parsed.matchedSymbols || 0,
        matchedChunks: parsed.matchedChunks || 0,
        totalCount: parsed.totalCount || 0,
        keyword: parsed.keyword || '',
        searchType: parsed.searchType || '',
      }
    }
    return { isCodeSearchTool: false, matchedFiles: 0, matchedSymbols: 0, matchedChunks: 0, totalCount: 0, keyword: '', searchType: '' }
  } catch {
    return { isCodeSearchTool: false, matchedFiles: 0, matchedSymbols: 0, matchedChunks: 0, totalCount: 0, keyword: '', searchType: '' }
  }
}

// Job state
const showJobDetail = ref<Record<string, boolean>>({})
const retryingJob = ref<Record<string, boolean>>({})
const cancelingJob = ref<Record<string, boolean>>({})

function toggleJobDetail(execId: string) {
  showJobDetail.value[execId] = !showJobDetail.value[execId]
}

async function handleRetryJob(executionId: string, jobId: string) {
  retryingJob.value[executionId] = true
  try {
    await retryToolExecutionJob(jobId)
    ElMessage.success('工具执行 Job 已重试')
    await loadRuns()
    if (selectedRun.value) {
      const updated = runs.value.find(r => r.id === selectedRun.value!.id)
      if (updated) selectedRun.value = updated
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '重试 Job 失败')
  } finally {
    retryingJob.value[executionId] = false
  }
}

async function handleCancelJob(executionId: string, jobId: string) {
  cancelingJob.value[executionId] = true
  try {
    await cancelToolExecutionJob(jobId)
    ElMessage.info('工具执行 Job 已取消')
    await loadRuns()
    if (selectedRun.value) {
      const updated = runs.value.find(r => r.id === selectedRun.value!.id)
      if (updated) selectedRun.value = updated
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '取消 Job 失败')
  } finally {
    cancelingJob.value[executionId] = false
  }
}

function jobStatusTone(status: string | undefined): 'success' | 'warning' | 'danger' | 'primary' | 'muted' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'primary' | 'muted'> = {
    PENDING: 'muted', RUNNING: 'warning', RETRY_PENDING: 'warning',
    COMPLETED: 'success', FAILED: 'danger', CANCELED: 'muted', DEAD_LETTERED: 'danger',
  }
  return map[status || ''] || 'muted'
}

function jobStatusText(status: string | undefined): string {
  const map: Record<string, string> = {
    PENDING: '待执行', RUNNING: '运行中', RETRY_PENDING: '待重试',
    COMPLETED: '已完成', FAILED: '失败', CANCELED: '已取消', DEAD_LETTERED: '死信',
  }
  return map[status || ''] || status || ''
}

async function handleManualRetry(executionId: string, jobId: string) {
  retryingJob.value[executionId] = true
  try {
    await manualRetryToolExecutionJob(jobId)
    ElMessage.success('工具执行 Job 已手动重试')
    await loadRuns()
    if (selectedRun.value) {
      const updated = runs.value.find(r => r.id === selectedRun.value!.id)
      if (updated) selectedRun.value = updated
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '手动重试 Job 失败')
  } finally {
    retryingJob.value[executionId] = false
  }
}

// Patch review status cache
const patchReviewStatus = ref<Record<string, { status: string; decision: string | null }>>({})

async function loadPatchReviewStatus(artifactId: string) {
  if (!artifactId || patchReviewStatus.value[artifactId]) return
  try {
    const res = await getPatchProposalReview(artifactId)
    const data = res.data.data
    patchReviewStatus.value[artifactId] = { status: data.status, decision: data.decision }
  } catch {
    // silently ignore
  }
}

function reviewStatusLabel(review: { status: string; decision: string | null } | undefined): string {
  if (!review || review.status === 'PENDING') return '审阅: 待审阅'
  const map: Record<string, string> = {
    ACCEPTED_AS_PLAN: '已接受',
    REJECTED: '已拒绝',
    NEEDS_CHANGES: '需修改',
    MARKED_REVIEWED: '已审阅',
  }
  return '审阅: ' + (map[review.decision || ''] || review.decision || '已完成')
}

function reviewStatusTone(review: { status: string; decision: string | null } | undefined): 'success' | 'warning' | 'info' | 'danger' {
  if (!review || review.status === 'PENDING') return 'warning'
  return 'success'
}

// Trace drawer state
const traceDrawerExecutionId = ref<string | undefined>(undefined)
const traceDrawerVisible = ref(false)

function openTraceDrawer(executionId: string) {
  traceDrawerExecutionId.value = executionId
  traceDrawerVisible.value = true
}

// Tool approval state
const toolApprovalComment = ref<Record<string, string>>({})
const approvingTool = ref<Record<string, boolean>>({})
const rejectingTool = ref<Record<string, boolean>>({})

const waitingApprovalCount = computed(() =>
  selectedRun.value?.toolExecutions?.filter(t => t.status === 'WAITING_APPROVAL').length || 0
)
const approvedToolCount = computed(() =>
  selectedRun.value?.toolExecutions?.filter(t => t.status === 'COMPLETED' && t.requiresApproval).length || 0
)
const rejectedToolCount = computed(() =>
  selectedRun.value?.toolExecutions?.filter(t => t.status === 'REJECTED').length || 0
)

async function handleToolApprove(executionId: string) {
  approvingTool.value[executionId] = true
  try {
    const comment = toolApprovalComment.value[executionId] || undefined
    await approveToolExecution(executionId, comment)
    ElMessage.success('工具审批已通过，Mock 执行完成')
    await loadRuns()
    if (selectedRun.value) {
      const updated = runs.value.find(r => r.id === selectedRun.value!.id)
      if (updated) selectedRun.value = updated
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '工具审批操作失败')
  } finally {
    approvingTool.value[executionId] = false
  }
}

async function handleToolReject(executionId: string) {
  rejectingTool.value[executionId] = true
  try {
    const comment = toolApprovalComment.value[executionId] || undefined
    await rejectToolExecution(executionId, comment)
    ElMessage.info('工具执行已驳回')
    await loadRuns()
    if (selectedRun.value) {
      const updated = runs.value.find(r => r.id === selectedRun.value!.id)
      if (updated) selectedRun.value = updated
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '工具审批操作失败')
  } finally {
    rejectingTool.value[executionId] = false
  }
}

function toolNameLabel(name: string): string {
  const map: Record<string, string> = {
    PROJECT_CONTEXT_SCAN: '项目上下文扫描',
    TASK_REQUIREMENT_ANALYSIS: '任务需求分析',
    MOCK_FILE_INSPECTION: '模拟文件检查',
    MOCK_TEST_PLAN_SCAN: '模拟测试计划扫描',
    MOCK_SECURITY_REVIEW: '模拟安全审查',
    MOCK_PATCH_PROPOSAL: 'Mock 补丁方案生成',
    READ_REPOSITORY_TREE: '仓库目录树读取',
    READ_FILE_SNIPPET: '文件片段读取',
    READ_DIFF_SUMMARY: 'Diff 摘要读取',
    READ_BRANCH_INFO: '分支信息读取',
    READ_CODE_INDEX: '代码索引读取',
    SEARCH_CODE_SYMBOL: '代码符号搜索',
    SEARCH_CODE_CHUNK: '代码片段搜索',
  }
  return map[name] || name
}

function toolTypeTone(type: string): string {
  const map: Record<string, string> = {
    READ_ONLY: 'info',
    MOCK: 'warning',
    ANALYSIS: 'primary',
  }
  return map[type] || 'info'
}

function stepTypeLabel(type: string): string {
  const map: Record<string, string> = {
    ARCHITECTURE_ANALYSIS: '架构分析',
    BACKEND_IMPLEMENTATION_PLAN: '后端方案',
    FRONTEND_IMPLEMENTATION_PLAN: '前端方案',
    TEST_PLAN: '测试计划',
    CODE_REVIEW: '代码审查',
    FINAL_SUMMARY: '总结归档',
  }
  return map[type] || type
}

function stepTypeIcon(type: string): string {
  const map: Record<string, string> = {
    ARCHITECTURE_ANALYSIS: '◆',
    BACKEND_IMPLEMENTATION_PLAN: '◇',
    FRONTEND_IMPLEMENTATION_PLAN: '◈',
    TEST_PLAN: '◎',
    CODE_REVIEW: '◈',
    FINAL_SUMMARY: '◆',
  }
  return map[type] || '○'
}

function laneLabel(laneKey: string | null): string {
  const map: Record<string, string> = {
    architect: 'Architect',
    backend: 'Backend',
    frontend: 'Frontend',
    test: 'Test',
    review: 'Review',
    summary: 'Summary',
  }
  return map[laneKey || ''] || laneKey || 'unknown'
}

function messageTypeLabel(type: string): string {
  const map: Record<string, string> = {
    TASK_CONTEXT: '任务上下文',
    STEP_OUTPUT: '步骤输出',
    HANDOFF: '交接消息',
    REVIEW_FEEDBACK: '审查反馈',
    FINAL_CONTEXT: '汇总上下文',
    APPROVAL_REQUEST: '审批请求',
    APPROVAL_DECISION: '审批决定',
  }
  return map[type] || type
}

function messageTypeTone(type: string): string {
  const map: Record<string, string> = {
    TASK_CONTEXT: 'primary',
    STEP_OUTPUT: 'success',
    HANDOFF: 'warning',
    REVIEW_FEEDBACK: 'danger',
    FINAL_CONTEXT: 'primary',
    APPROVAL_REQUEST: 'warning',
    APPROVAL_DECISION: 'success',
  }
  return map[type] || 'info'
}

function statusTone(status: string): 'success' | 'warning' | 'danger' | 'primary' | 'muted' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'primary' | 'muted'> = {
    PENDING: 'muted', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger', SKIPPED: 'muted', WAITING_APPROVAL: 'warning', REJECTED: 'danger', BLOCKED: 'warning',
  }
  return map[status] || 'muted'
}

function statusText(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待执行', RUNNING: '运行中', COMPLETED: '已完成', FAILED: '失败', SKIPPED: '已跳过', WAITING_APPROVAL: '等待审批', REJECTED: '已驳回', BLOCKED: '已阻止',
  }
  return map[status] || status
}

function runStatusTone(status: string): 'success' | 'warning' | 'danger' | 'primary' | 'muted' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'primary' | 'muted'> = {
    PENDING: 'muted', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger', CANCELED: 'muted', WAITING_APPROVAL: 'warning',
  }
  return map[status] || 'muted'
}

function phaseStatusTone(status: string): 'success' | 'warning' | 'danger' | 'primary' | 'muted' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'primary' | 'muted'> = {
    PENDING: 'muted', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger', SKIPPED: 'muted', WAITING_APPROVAL: 'warning',
  }
  return map[status] || 'muted'
}

function formatToolOutput(payload: string): string {
  try {
    return JSON.stringify(JSON.parse(payload), null, 2)
  } catch {
    return payload
  }
}

function getParamSummaryFromOutput(payload: string | null): string {
  if (!payload) return ''
  try {
    const parsed = JSON.parse(payload)
    return parsed.parameterSummary || ''
  } catch {
    return ''
  }
}

function getInputParams(payload: string | null): string {
  if (!payload) return ''
  try {
    const parsed = JSON.parse(payload)
    if (parsed.parameters) {
      const parts: string[] = []
      for (const [key, val] of Object.entries(parsed.parameters)) {
        if (Array.isArray(val)) {
          parts.push(`${key}=${val.length} 项`)
        } else {
          parts.push(`${key}=${val}`)
        }
      }
      return parts.join(', ')
    }
    return ''
  } catch {
    return ''
  }
}

function getTargetFiles(payload: string | null): string[] {
  if (!payload) return []
  try {
    const parsed = JSON.parse(payload)
    const params = parsed.parameters
    if (params && Array.isArray(params.targetFiles)) {
      return params.targetFiles as string[]
    }
    return []
  } catch {
    return []
  }
}

function phaseKeyLabel(key: string): string {
  const map: Record<string, string> = {
    PLANNING: '规划',
    IMPLEMENTATION: '实现',
    BACKEND_IMPLEMENTATION: '后端实现',
    FRONTEND_IMPLEMENTATION: '前端实现',
    REVIEW: '审查',
    SUMMARY: '总结',
  }
  return map[key] || key
}
</script>

<template>
  <div class="mar-panel" v-loading="loading">
    <!-- Header Actions -->
    <div class="mar-header">
      <div class="mar-header-left">
        <el-select
          v-model="selectedStrategy"
          v-loading="loadingStrategies"
          size="small"
          placeholder="选择策略"
          data-testid="strategy-select"
          style="width:200px"
        >
          <el-option
            v-for="s in strategies"
            :key="s.strategyKey"
            :label="s.name"
            :value="s.strategyKey"
            :data-testid="'strategy-option-' + s.strategyKey"
          />
        </el-select>
        <GlowButton
          data-testid="btn-start-multi-agent"
          accent="primary"
          :loading="starting"
          @click="handleStart"
        >
          启动多智能体编排
        </GlowButton>
      </div>
      <span class="mar-hint">当前阶段为 Mock 编排，不执行真实代码修改。</span>
    </div>

    <!-- Strategy Preview -->
    <div v-if="previewStrategy" class="mar-strategy-preview" data-testid="strategy-preview">
      <div class="mar-strategy-preview-header">
        <span class="mar-strategy-preview-name">{{ previewStrategy.name }}</span>
        <span class="mar-strategy-preview-desc">{{ previewStrategy.description }}</span>
      </div>
      <div class="mar-strategy-preview-phases">
        <div
          v-for="phase in previewStrategy.phases"
          :key="phase.phaseKey"
          class="mar-strategy-preview-phase"
          :data-testid="'strategy-preview-phase-' + phase.phaseKey"
        >
          <span class="mar-strategy-preview-phase-order">P{{ phase.phaseOrder }}</span>
          <span class="mar-strategy-preview-phase-title">{{ phase.title }}</span>
          <span class="mar-strategy-preview-phase-steps">
            <el-tag
              v-for="step in phase.steps"
              :key="step.stepType"
              size="small"
              type="info"
              effect="plain"
              class="mar-strategy-preview-step-tag"
            >
              {{ step.title }}
            </el-tag>
          </span>
        </div>
      </div>
    </div>

    <NeonDivider tone="primary" style="margin:16px 0" />

    <EmptyState v-if="!loading && runs.length === 0" description="暂无多智能体编排记录" />

    <template v-if="runs.length > 0">
      <!-- Run Selector -->
      <div class="mar-run-tabs">
        <el-button
          v-for="run in runs"
          :key="run.id"
          size="small"
          :type="selectedRun?.id === run.id ? 'primary' : ''"
          data-testid="multi-agent-run-tab"
          @click="selectedRun = run"
        >
          <StatusPulse :status="run.status" :tone="runStatusTone(run.status)" :size="8" />
          <span style="margin-left:6px">Run {{ run.id.slice(-6) }}</span>
        </el-button>
      </div>

      <!-- Selected Run Detail -->
      <div v-if="selectedRun" class="mar-run-detail" data-testid="multi-agent-run-detail">
        <!-- Run Meta -->
        <div class="mar-run-meta">
          <span>状态: <StatusPulse :status="selectedRun.status" :tone="runStatusTone(selectedRun.status)" /></span>
          <span v-if="selectedRun.strategyName">
            策略: <strong>{{ selectedRun.strategyName }}</strong>
            <span v-if="selectedRun.strategyDescription" class="mar-strategy-desc">{{ selectedRun.strategyDescription }}</span>
          </span>
          <span v-else>策略: <code>{{ selectedRun.strategyKey || selectedRun.strategy }}</code></span>
          <span v-if="selectedRun.startedAt">开始: {{ formatDateTime(selectedRun.startedAt) }}</span>
          <span v-if="selectedRun.finishedAt">结束: {{ formatDateTime(selectedRun.finishedAt) }}</span>
        </div>

        <div v-if="selectedRun.errorMessage" style="margin-top:8px">
          <el-alert :title="selectedRun.errorMessage" type="error" :closable="false" />
        </div>

        <!-- Summary Stats -->
        <div class="mar-summary-stats" data-testid="multi-agent-summary-stats">
          <span v-if="hasNonTerminalJobs" class="mar-polling-indicator" data-testid="tool-job-polling-indicator">
            <StatusPulse status="RUNNING" tone="warning" :size="8" />
            <span style="margin-left:4px;font-size:11px;color:var(--el-color-warning)">Job 轮询中...</span>
          </span>
          <el-tag size="small" type="primary" effect="plain">Phases {{ phases.length }}</el-tag>
          <el-tag size="small" type="warning" effect="plain">并行步骤 {{ parallelStepCount }}</el-tag>
          <el-tag size="small" type="success" effect="plain">完成步骤 {{ completedStepCount }}</el-tag>
          <el-tag v-if="skippedStepCount > 0" size="small" type="info" effect="plain">跳过 {{ skippedStepCount }}</el-tag>
          <el-tag size="small" type="primary" effect="plain">消息 {{ messageCount }}</el-tag>
          <el-tag v-if="toolExecCount > 0" size="small" type="warning" effect="dark" data-testid="multi-agent-tool-summary">工具执行 {{ toolExecCount }}</el-tag>
          <el-tag v-if="mockExecCount > 0" size="small" type="info" effect="dark">Mock 执行 {{ mockExecCount }}</el-tag>
          <el-tag v-if="blockedExecCount > 0" size="small" type="danger" effect="dark" data-testid="multi-agent-blocked-summary">阻止 {{ blockedExecCount }}</el-tag>
          <el-tag v-if="waitingApprovalCount > 0" size="small" type="warning" effect="dark" data-testid="multi-agent-waiting-approval-summary">待审批工具 {{ waitingApprovalCount }}</el-tag>
          <el-tag v-if="approvedToolCount > 0" size="small" type="success" effect="dark">已批准 {{ approvedToolCount }}</el-tag>
          <el-tag v-if="rejectedToolCount > 0" size="small" type="danger" effect="dark">已驳回 {{ rejectedToolCount }}</el-tag>
        </div>

        <!-- Message Flow Mini View -->
        <div v-if="messageCount > 0" class="mar-message-flow" data-testid="multi-agent-message-flow">
          <div class="mar-section-title">
            消息链路
            <el-tag size="small" type="primary" effect="plain" style="margin-left:8px">{{ messageCount }} 条</el-tag>
          </div>
          <div class="mar-flow-chain">
            <template v-for="(step, idx) in selectedRun.steps" :key="step.id">
              <span class="mar-flow-step" :class="{ 'is-skipped': step.status === 'SKIPPED' }">
                <span class="mar-flow-icon">{{ stepTypeIcon(step.stepType) }}</span>
                <span class="mar-flow-label">{{ stepTypeLabel(step.stepType) }}</span>
                <el-badge
                  v-if="stepMessages[step.id]?.length"
                  :value="stepMessages[step.id]?.length || 0"
                  :type="step.status === 'COMPLETED' ? 'primary' : 'info'"
                  class="mar-flow-badge"
                />
              </span>
              <span v-if="idx < selectedRun.steps.length - 1" class="mar-flow-arrow">→</span>
            </template>
          </div>
          <div class="mar-flow-legend">
            <span v-for="type in ['TASK_CONTEXT','STEP_OUTPUT','HANDOFF','REVIEW_FEEDBACK','FINAL_CONTEXT','APPROVAL_REQUEST','APPROVAL_DECISION']" :key="type" class="mar-legend-item">
              <el-tag size="small" :type="messageTypeTone(type)" effect="plain">{{ messageTypeLabel(type) }}</el-tag>
            </span>
          </div>
        </div>

        <!-- Phase / Lane View -->
        <div class="mar-phases" data-testid="multi-agent-phases">
          <div
            v-for="phase in phases"
            :key="phase.id"
            class="mar-phase-card"
            :class="{ 'is-skipped': phase.status === 'SKIPPED', 'is-failed': phase.status === 'FAILED' }"
            :data-testid="'multi-agent-phase-' + phase.phaseKey"
          >
            <!-- Phase Header -->
            <div class="mar-phase-header">
              <div class="mar-phase-title-row">
                <StatusPulse :status="phase.status" :tone="phaseStatusTone(phase.status)" :size="10" />
                <span class="mar-phase-order">Phase {{ phase.phaseOrder }}</span>
                <span class="mar-phase-key">{{ phaseKeyLabel(phase.phaseKey) }}</span>
                <span class="mar-phase-title">{{ phase.title }}</span>
                <el-tag
                  size="small"
                  :type="phase.status === 'COMPLETED' ? 'success' : phase.status === 'FAILED' ? 'danger' : phase.status === 'RUNNING' ? 'warning' : 'info'"
                >
                  {{ statusText(phase.status) }}
                </el-tag>
              </div>
              <div v-if="phase.inputSummary" class="mar-phase-summaries">
                <span class="mar-phase-summary-label">输入: </span>
                <span class="mar-phase-summary-text">{{ phase.inputSummary.slice(0, 120) }}{{ phase.inputSummary.length > 120 ? '...' : '' }}</span>
              </div>
              <div v-if="phase.outputSummary" class="mar-phase-summaries">
                <span class="mar-phase-summary-label">输出: </span>
                <span class="mar-phase-summary-text">{{ phase.outputSummary }}</span>
              </div>
            </div>

            <!-- Phase Lanes -->
            <div class="mar-phase-lanes" :class="{ 'is-parallel': phase.steps.length > 1 }">
              <div
                v-for="step in phase.steps"
                :key="step.id"
                class="mar-lane-card"
                :class="{ 'is-active': step.status === 'COMPLETED', 'is-skipped': step.status === 'SKIPPED' }"
                :data-testid="'multi-agent-lane-' + step.laneKey"
              >
                <!-- Lane Header -->
                <div class="mar-lane-header" @click="toggleStep(step.id)" style="cursor:pointer">
                  <div class="mar-lane-identity">
                    <span class="mar-lane-icon">{{ stepTypeIcon(step.stepType) }}</span>
                    <span class="mar-lane-label">{{ laneLabel(step.laneKey) }}</span>
                    <span class="mar-lane-agent" v-if="step.agentName">{{ step.agentName }}</span>
                  </div>
                  <div class="mar-lane-badges">
                    <el-badge
                      v-if="stepMessages[step.id]?.length"
                      :value="stepMessages[step.id]?.length"
                      class="mar-lane-msg-badge"
                      type="primary"
                    />
                    <StatusPulse :status="step.status" :tone="statusTone(step.status)" :size="8" />
                    <el-tag size="small" :type="step.status === 'COMPLETED' ? 'success' : step.status === 'FAILED' ? 'danger' : 'info'">
                      {{ statusText(step.status) }}
                    </el-tag>
                    <span class="mar-step-expand">{{ expandedSteps[step.id] ? '▾' : '▸' }}</span>
                  </div>
                </div>

                <!-- Lane Detail -->
                <div v-if="expandedSteps[step.id]" class="mar-lane-detail">
                  <!-- Input Context -->
                  <div class="mar-step-context-section">
                    <div
                      class="mar-step-context-header"
                      @click="toggleInputContext(step.id)"
                      data-testid="multi-agent-step-input-toggle"
                    >
                      <span>{{ showInputContext[step.id] ? '▾' : '▸' }} 输入上下文</span>
                    </div>
                    <div v-if="showInputContext[step.id] && step.inputContext" class="mar-step-context-body" data-testid="multi-agent-step-input">
                      <MarkdownRenderer :content="step.inputContext" />
                    </div>
                    <div v-else-if="showInputContext[step.id] && !step.inputContext" class="mar-step-context-body" style="color:var(--app-text-muted);font-size:12px">
                      无输入上下文
                    </div>
                  </div>

                  <!-- Step Messages -->
                  <div v-if="stepMessages[step.id]?.length" class="mar-step-msgs">
                    <div class="mar-step-msgs-title">关联消息 ({{ stepMessages[step.id].length }})</div>
                    <div
                      v-for="msg in stepMessages[step.id]"
                      :key="msg.id"
                      class="mar-step-msg-card"
                      :data-testid="'multi-agent-message-' + msg.messageType"
                    >
                      <div class="mar-msg-header">
                        <el-tag size="small" :type="messageTypeTone(msg.messageType)" effect="dark">
                          {{ messageTypeLabel(msg.messageType) }}
                        </el-tag>
                        <span class="mar-msg-summary" v-if="msg.summary">{{ msg.summary }}</span>
                      </div>
                      <div class="mar-msg-content">
                        <MarkdownRenderer :content="msg.content" />
                      </div>
                    </div>
                  </div>

                  <!-- Tool Sandbox Section -->
                  <div
                    v-if="stepToolExecs[step.id]?.length"
                    class="mar-tool-sandbox-section"
                    data-testid="multi-agent-tool-section"
                  >
                    <div class="mar-tool-sandbox-title">工具沙箱 ({{ stepToolExecs[step.id].length }})</div>
                    <div
                      v-for="te in stepToolExecs[step.id]"
                      :key="te.id"
                      class="mar-tool-card"
                      data-testid="multi-agent-tool-card"
                    >
                      <div class="mar-tool-header">
                        <el-tag size="small" :type="toolTypeTone(te.toolType)" effect="dark">
                          {{ toolNameLabel(te.toolName) }}
                        </el-tag>
                        <el-tag size="small" type="warning" effect="plain">{{ te.executionMode }}</el-tag>
                        <StatusPulse :status="te.status" :tone="te.status === 'COMPLETED' ? 'success' : te.status === 'BLOCKED' ? 'warning' : te.status === 'WAITING_APPROVAL' ? 'warning' : te.status === 'REJECTED' ? 'danger' : 'danger'" :size="6" />
                        <span class="mar-tool-status">{{ statusText(te.status) }}</span>
                        <span v-if="te.durationMs > 0" class="mar-tool-duration">{{ te.durationMs }}ms</span>
                        <span v-if="te.requiresApproval" class="mar-tool-approval-badge">需审批</span>

                        <!-- Job status badge -->
                        <template v-if="te.job">
                          <el-tag
                            v-if="te.job.status"
                            size="small"
                            :type="jobStatusTone(te.job.status)"
                            effect="dark"
                            data-testid="tool-job-status"
                          >
                            Job {{ jobStatusText(te.job.status) }}
                          </el-tag>
                          <el-tag
                            v-if="te.job.status === 'PENDING'"
                            size="small"
                            type="info"
                            effect="plain"
                            data-testid="tool-job-queued-badge"
                          >
                            排队中
                          </el-tag>
                          <span v-if="te.job.retryCount > 0" class="mar-tool-job-retry" data-testid="tool-job-retry-count">
                            retry {{ te.job.retryCount }}/{{ te.job.maxRetryCount }}
                          </span>
                          <span v-if="te.job.durationMs > 0" class="mar-tool-duration" data-testid="tool-job-duration">
                            Job {{ te.job.durationMs }}ms
                          </span>
                        </template>
                      </div>
                      <div v-if="te.summary" class="mar-tool-summary" :class="{ 'mar-tool-summary-blocked': te.status === 'BLOCKED', 'mar-tool-summary-warning': te.status === 'WAITING_APPROVAL' }">{{ te.summary }}</div>

                      <!-- Job detail toggle & actions -->
                      <template v-if="te.job">
                        <div
                          class="mar-tool-output-toggle"
                          @click="toggleJobDetail(te.id)"
                          data-testid="tool-job-detail"
                        >
                          {{ showJobDetail[te.id] ? '▾' : '▸' }} Job 详情
                        </div>
                        <div v-if="showJobDetail[te.id]" class="mar-tool-job-detail-body">
                          <div v-if="te.job.requestPayload" class="mar-tool-job-payload" data-testid="tool-job-request-payload">
                            <div class="mar-tool-job-payload-label">请求载荷:</div>
                            <pre class="mar-tool-output-pre">{{ formatToolOutput(te.job.requestPayload) }}</pre>
                          </div>
                          <div v-if="te.job.resultPayload" class="mar-tool-job-payload" data-testid="tool-job-result-payload">
                            <div class="mar-tool-job-payload-label">结果载荷:</div>
                            <pre class="mar-tool-output-pre">{{ formatToolOutput(te.job.resultPayload) }}</pre>
                          </div>
                          <div v-if="te.job.lastError" class="mar-tool-job-error">
                            <div class="mar-tool-job-payload-label">错误信息:</div>
                            <pre class="mar-tool-output-pre" style="color:var(--el-color-danger)">{{ te.job.lastError }}</pre>
                          </div>
                        </div>
                        <div class="mar-tool-job-actions" v-if="te.job.status === 'FAILED'">
                          <el-button
                            size="small"
                            type="warning"
                            :loading="retryingJob[te.id]"
                            data-testid="btn-retry-tool-job"
                            @click="handleRetryJob(te.id, te.job!.id)"
                          >
                            重试 Job
                          </el-button>
                        </div>
                        <div class="mar-tool-job-actions" v-if="te.job.status === 'PENDING' || te.job.status === 'RUNNING'">
                          <el-button
                            size="small"
                            type="danger"
                            plain
                            :loading="cancelingJob[te.id]"
                            data-testid="btn-cancel-tool-job"
                            @click="handleCancelJob(te.id, te.job!.id)"
                          >
                            取消 Job
                          </el-button>
                        </div>
                        <div class="mar-tool-job-actions" v-if="te.job.status === 'DEAD_LETTERED' || te.job.status === 'FAILED' || te.job.status === 'RETRY_PENDING'">
                          <el-button
                            size="small"
                            type="warning"
                            plain
                            :loading="retryingJob[te.id]"
                            data-testid="btn-manual-retry-tool-job"
                            @click="handleManualRetry(te.id, te.job!.id)"
                          >
                            手动重试
                          </el-button>
                        </div>
                        <div v-if="te.job.errorCode || te.job.failureStage || te.job.nextRetryAt || te.job.deadLetterReason" class="mar-tool-job-dlq-info">
                          <span v-if="te.job.errorCode" class="mar-tool-dlq-tag" data-testid="tool-job-error-code">
                            错误码: {{ te.job.errorCode }}
                          </span>
                          <span v-if="te.job.failureStage" class="mar-tool-dlq-tag" data-testid="tool-job-failure-stage">
                            阶段: {{ te.job.failureStage }}
                          </span>
                          <span v-if="te.job.nextRetryAt" class="mar-tool-dlq-tag" data-testid="tool-job-next-retry-at">
                            下次重试: {{ te.job.nextRetryAt }}
                          </span>
                          <span v-if="te.job.deadLetterReason" class="mar-tool-dlq-tag" data-testid="tool-job-dead-lettered">
                            死信原因: {{ te.job.deadLetterReason }}
                          </span>
                        </div>
                      </template>

                      <!-- Trace Evidence Button -->
                      <div class="mar-tool-job-actions">
                        <el-button
                          size="small"
                          type="primary"
                          plain
                          data-testid="tool-trace-open-button"
                          @click="openTraceDrawer(te.id)"
                        >
                          查看证据链
                        </el-button>
                      </div>

                      <!-- Parameter summary from input payload -->
                      <div v-if="getInputParams(te.inputPayload)" class="mar-tool-params" data-testid="tool-input-parameters">
                        <span class="mar-tool-params-label">参数:</span>
                        <span class="mar-tool-params-value">{{ getInputParams(te.inputPayload) }}</span>
                      </div>

                      <!-- Target files display -->
                      <div v-if="getTargetFiles(te.inputPayload).length > 0" class="mar-tool-params" data-testid="tool-target-files">
                        <span class="mar-tool-params-label">目标文件:</span>
                        <div class="mar-target-files-list">
                          <span v-for="(tf, tfi) in getTargetFiles(te.inputPayload)" :key="tfi" class="mar-target-file-item">{{ tf }}</span>
                        </div>
                      </div>

                      <!-- Parameter summary from output payload -->
                      <div v-if="getParamSummaryFromOutput(te.outputPayload)" class="mar-tool-params" data-testid="tool-parameter-summary">
                        <span class="mar-tool-params-label">参数摘要:</span>
                        <span class="mar-tool-params-value">{{ getParamSummaryFromOutput(te.outputPayload) }}</span>
                      </div>

                      <!-- Repository Read-Only Tool Info -->
                      <template v-if="getRepositoryInfo(te).isRepoReadTool">
                        <div class="mar-tool-params" data-testid="tool-files-read-summary">
                          <span class="mar-tool-params-label">读取文件:</span>
                          <span class="mar-tool-params-value">{{ getRepositoryInfo(te).filesRead.length }} 个</span>
                          <span v-if="getRepositoryInfo(te).branch" class="mar-repo-meta-item">分支: {{ getRepositoryInfo(te).branch }}</span>
                          <span v-if="getRepositoryInfo(te).pathPrefix" class="mar-repo-meta-item">路径: {{ getRepositoryInfo(te).pathPrefix }}</span>
                          <el-tag v-if="getRepositoryInfo(te).redacted" size="small" type="warning" effect="dark" data-testid="tool-redacted-badge">已脱敏</el-tag>
                          <el-tag v-if="getRepositoryInfo(te).truncated" size="small" type="info" effect="dark" data-testid="tool-truncated-badge">已截断</el-tag>
                        </div>
                        <div
                          v-if="getRepositoryInfo(te).filesRead.length > 0"
                          class="mar-tool-output-toggle"
                          @click="toggleFilesRead(te.id)"
                          data-testid="tool-files-read-list"
                        >
                          {{ showFilesRead[te.id] ? '▾' : '▸' }} 已读取文件列表 ({{ getRepositoryInfo(te).filesRead.length }})
                        </div>
                        <div v-if="showFilesRead[te.id] && getRepositoryInfo(te).filesRead.length > 0" class="mar-tool-output-body">
                          <div v-for="file in getRepositoryInfo(te).filesRead" :key="file" class="mar-repo-file-item">
                            {{ file }}
                          </div>
                        </div>
                        <div
                          v-if="getRepositoryInfo(te).skippedFiles.length > 0"
                          class="mar-tool-params"
                          data-testid="tool-skipped-files-summary"
                        >
                          <span class="mar-tool-params-label">跳过文件:</span>
                          <span class="mar-tool-params-value">{{ getRepositoryInfo(te).skippedFiles.length }} 个</span>
                        </div>
                        <div v-if="getRepositoryInfo(te).skippedFiles.length > 0" class="mar-tool-output-body">
                          <div
                            v-for="(sf, sfi) in getRepositoryInfo(te).skippedFiles"
                            :key="sfi"
                            class="mar-repo-skipped-item"
                            :data-testid="`tool-skipped-file-${sfi}`"
                          >
                            <span class="mar-repo-skipped-path">{{ sf.filePath }}</span>
                            <el-tag size="small" type="info" effect="dark">{{ sf.reason }}</el-tag>
                          </div>
                        </div>
                        <div class="mar-repo-safety-note" data-testid="repository-readonly-safety-note">
                          只读仓库上下文：从工作区读取文件，未 checkout，未 pull，未写入文件，未执行 Git 写操作。
                          <span v-if="getRepositoryInfo(te).redacted" class="mar-repo-safety-detail">包含脱敏处理。</span>
                          <span v-if="getRepositoryInfo(te).truncated" class="mar-repo-safety-detail">部分内容已截断。</span>
                        </div>
                      </template>

                      <!-- Code Search Tool Info -->
                      <template v-if="getCodeSearchInfo(te).isCodeSearchTool">
                        <div class="mar-tool-params" data-testid="tool-code-search-summary">
                          <span class="mar-tool-params-label">搜索摘要:</span>
                          <template v-if="getCodeSearchInfo(te).keyword">
                            <span class="mar-tool-params-value">关键词: {{ getCodeSearchInfo(te).keyword }}</span>
                          </template>
                          <template v-if="getCodeSearchInfo(te).searchType">
                            <span class="mar-tool-params-value" style="margin-left:8px">类型: {{ getCodeSearchInfo(te).searchType }}</span>
                          </template>
                        </div>
                        <div class="mar-code-search-grid" data-testid="tool-code-search-stats">
                          <span v-if="getCodeSearchInfo(te).matchedFiles > 0" class="mar-code-search-stat" data-testid="tool-code-search-matched-files">
                            <span class="mar-code-search-stat-value">{{ getCodeSearchInfo(te).matchedFiles }}</span> 文件
                          </span>
                          <span v-if="getCodeSearchInfo(te).matchedSymbols > 0" class="mar-code-search-stat" data-testid="tool-code-search-matched-symbols">
                            <span class="mar-code-search-stat-value">{{ getCodeSearchInfo(te).matchedSymbols }}</span> 符号
                          </span>
                          <span v-if="getCodeSearchInfo(te).matchedChunks > 0" class="mar-code-search-stat" data-testid="tool-code-search-matched-chunks">
                            <span class="mar-code-search-stat-value">{{ getCodeSearchInfo(te).matchedChunks }}</span> 切片
                          </span>
                          <span v-if="getCodeSearchInfo(te).totalCount > 0" class="mar-code-search-stat" data-testid="tool-code-search-total-count">
                            <span class="mar-code-search-stat-value">{{ getCodeSearchInfo(te).totalCount }}</span> 结果
                          </span>
                        </div>
                        <div class="mar-repo-safety-note" data-testid="code-search-safety-note">
                          代码搜索索引：只读查询，未读取文件内容，未写入文件，未执行 Git 操作。
                        </div>
                      </template>

                      <!-- Artifact link for PATCH_PROPOSAL -->
                      <div v-if="te.artifactId" class="mar-tool-artifact-link" data-testid="tool-artifact-link">
                        <span class="mar-tool-patch-proposal-badge" data-testid="tool-patch-proposal-badge">PATCH_PROPOSAL</span>
                        <span class="mar-tool-artifact-name">{{ te.artifactName || 'Mock Patch Proposal' }}</span>
                        <el-tag
                          v-if="patchReviewStatus[te.artifactId]"
                          size="small"
                          :type="reviewStatusTone(patchReviewStatus[te.artifactId])"
                          effect="dark"
                          data-testid="tool-patch-review-status"
                        >{{ reviewStatusLabel(patchReviewStatus[te.artifactId]) }}</el-tag>
                        <span class="mar-tool-artifact-hint">请在任务产物中完成补丁提案审阅。</span>
                      </div>

                      <!-- Tool Approval Card for WAITING_APPROVAL -->
                      <div
                        v-if="te.status === 'WAITING_APPROVAL'"
                        class="mar-tool-approval-card"
                        data-testid="tool-approval-card"
                      >
                        <div class="mar-tool-approval-notice">
                          该工具需要人工审批。审批通过后仍只执行 Mock，不会执行真实 Shell、Git 或文件写入。
                        </div>
                        <div v-if="te.approval" class="mar-tool-approval-info">
                          <span class="mar-tool-approval-risk">
                            <el-tag size="small" type="danger" effect="dark">{{ te.approval.riskLevel }}</el-tag>
                          </span>
                          <span v-if="te.approval.requestedAt" class="mar-tool-approval-time">
                            请求时间: {{ formatDateTime(te.approval.requestedAt) }}
                          </span>
                        </div>
                        <div class="mar-tool-approval-actions">
                          <el-input
                            v-model="toolApprovalComment[te.id]"
                            size="small"
                            placeholder="审批意见（可选）"
                            data-testid="tool-approval-comment"
                            class="mar-tool-approval-input"
                          />
                          <el-button
                            size="small"
                            type="success"
                            :loading="approvingTool[te.id]"
                            data-testid="btn-approve-tool"
                            @click="handleToolApprove(te.id)"
                          >
                            批准并执行 Mock
                          </el-button>
                          <el-button
                            size="small"
                            type="danger"
                            :loading="rejectingTool[te.id]"
                            data-testid="btn-reject-tool"
                            @click="handleToolReject(te.id)"
                          >
                            驳回
                          </el-button>
                        </div>
                      </div>

                      <!-- Approval History for APPROVED / REJECTED -->
                      <div
                        v-if="te.approval && (te.approval.status === 'APPROVED' || te.approval.status === 'REJECTED')"
                        class="mar-tool-approval-history"
                        data-testid="tool-approval-history"
                      >
                        <div class="mar-tool-approval-history-title">
                          审批结果: <el-tag size="small" :type="te.approval.status === 'APPROVED' ? 'success' : 'danger'" effect="dark">{{ te.approval.status === 'APPROVED' ? '已批准' : '已驳回' }}</el-tag>
                        </div>
                        <div v-if="te.approval.decidedBy" class="mar-tool-approval-history-item">
                          审批人: {{ te.approval.decidedBy }}
                        </div>
                        <div v-if="te.approval.decisionComment" class="mar-tool-approval-history-item">
                          审批意见: {{ te.approval.decisionComment }}
                        </div>
                        <div v-if="te.approval.decidedAt" class="mar-tool-approval-history-item">
                          决策时间: {{ formatDateTime(te.approval.decidedAt) }}
                        </div>
                      </div>

                      <div
                        v-if="te.outputPayload"
                        class="mar-tool-output-toggle"
                        :class="{ 'mar-tool-output-toggle-blocked': te.status === 'BLOCKED' }"
                        @click="toggleToolOutput(te.id)"
                        data-testid="multi-agent-tool-output"
                      >
                        {{ showToolOutput[te.id] ? '▾' : '▸' }} 查看输出
                      </div>
                      <div v-if="showToolOutput[te.id] && te.outputPayload" class="mar-tool-output-body">
                        <pre class="mar-tool-output-pre">{{ formatToolOutput(te.outputPayload) }}</pre>
                      </div>
                    </div>
                    <div class="mar-tool-safety-notice">
                      当前为 Mock 沙箱执行：未执行真实 Shell，未执行 Git 写操作，未写入文件。
                    </div>
                  </div>

                  <!-- Output Content -->
                  <div class="mar-step-output" data-testid="multi-agent-step-output">
                    <MarkdownRenderer v-if="step.outputContent" :content="step.outputContent" />
                    <span v-else-if="step.status === 'SKIPPED'" style="color:var(--app-text-muted);font-size:12px">
                      该步骤已跳过：对应智能体未启用或不存在
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Approval Gate Card -->
        <div
          v-if="pendingApprovalGate"
          class="mar-approval-gate"
          data-testid="multi-agent-approval-gate"
        >
          <div class="mar-approval-header">
            <StatusPulse status="WAITING_APPROVAL" :tone="'warning'" :size="10" />
            <span class="mar-approval-title">审批闸门</span>
            <el-tag type="warning" effect="dark">待审批</el-tag>
          </div>
          <div class="mar-approval-body">
            <div class="mar-approval-gate-title">{{ pendingApprovalGate.title }}</div>
            <div v-if="pendingApprovalGate.description" class="mar-approval-gate-desc">
              {{ pendingApprovalGate.description }}
            </div>
            <div class="mar-approval-divider" />
            <div class="mar-approval-input-label">审批意见</div>
            <el-input
              v-model="approvalComment"
              type="textarea"
              :rows="2"
              placeholder="输入审批意见（可选）"
              data-testid="approval-comment-input"
            />
            <div class="mar-approval-actions">
              <el-button
                type="success"
                :loading="approving"
                :disabled="rejecting"
                @click="handleApprove"
                data-testid="btn-approve-gate"
              >
                批准 (Approve)
              </el-button>
              <el-button
                type="danger"
                :loading="rejecting"
                :disabled="approving"
                @click="handleReject"
                data-testid="btn-reject-gate"
              >
                驳回 (Reject)
              </el-button>
            </div>
          </div>
        </div>

        <!-- Approval Gates History -->
        <div
          v-if="selectedRun.approvalGates && selectedRun.approvalGates.length > 0 && !pendingApprovalGate"
          class="mar-approval-gates-history"
          data-testid="multi-agent-approval-gates-history"
        >
          <div class="mar-section-title">审批记录</div>
          <div
            v-for="gate in selectedRun.approvalGates"
            :key="gate.id"
            class="mar-approval-gate-record"
            :data-testid="'approval-gate-record-' + gate.status"
          >
            <div class="mar-approval-record-header">
              <el-tag
                size="small"
                :type="gate.status === 'APPROVED' ? 'success' : gate.status === 'REJECTED' ? 'danger' : 'warning'"
                effect="dark"
              >
                {{ gate.status === 'APPROVED' ? '已批准' : gate.status === 'REJECTED' ? '已驳回' : gate.status }}
              </el-tag>
              <span class="mar-approval-record-title">{{ gate.title }}</span>
            </div>
            <div v-if="gate.decisionComment" class="mar-approval-record-comment">
              意见: {{ gate.decisionComment }}
            </div>
            <div class="mar-approval-record-meta">
              <span v-if="gate.decidedAt">决定时间: {{ formatDateTime(gate.decidedAt) }}</span>
              <span v-if="gate.decidedBy">决定人ID: {{ gate.decidedBy }}</span>
            </div>
          </div>
        </div>

        <!-- Final Summary -->
        <div v-if="selectedRun.finalSummary" class="mar-final-summary" data-testid="multi-agent-final-summary">
          <div class="mar-section-title">最终总结</div>
          <div class="mar-summary-content">
            <MarkdownRenderer :content="selectedRun.finalSummary" />
          </div>
        </div>
      </div>
    </template>
  </div>
  <!-- Tool Execution Trace Drawer -->
  <ToolExecutionTraceDrawer
    v-model="traceDrawerVisible"
    :execution-id="traceDrawerExecutionId"
  />
</template>

<style scoped>
.mar-panel {
  min-height: 200px;
}
.mar-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.mar-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mar-hint {
  font-size: 12px;
  color: var(--app-text-muted);
  margin-left: 4px;
}

/* Strategy Preview */
.mar-strategy-preview {
  margin-top: 12px;
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 12px 16px;
}
.mar-strategy-preview-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.mar-strategy-preview-name {
  font-weight: 700;
  font-size: 13px;
  color: var(--app-primary);
}
.mar-strategy-preview-desc {
  font-size: 12px;
  color: var(--app-text-muted);
}
.mar-strategy-preview-phases {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.mar-strategy-preview-phase {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 4px 10px;
  background: rgba(148,163,184,0.06);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
}
.mar-strategy-preview-phase-order {
  font-weight: 700;
  color: var(--app-primary);
  font-size: 11px;
}
.mar-strategy-preview-phase-title {
  color: var(--app-text-soft);
}
.mar-strategy-preview-phase-steps {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.mar-strategy-preview-step-tag {
  font-size: 10px;
}

.mar-strategy-desc {
  font-size: 11px;
  color: var(--app-text-muted);
  margin-left: 6px;
}
.mar-run-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  align-items: center;
}
.mar-run-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
  font-size: 12px;
  color: var(--app-text-muted);
}
.mar-run-meta code {
  font-size: 11px;
  background: rgba(148,163,184,0.1);
  color: var(--app-text-soft);
  padding: 2px 6px;
  border-radius: 4px;
}

/* Summary Stats */
.mar-summary-stats {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

/* Message Flow */
.mar-message-flow {
  margin-top: 20px;
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 16px;
}
.mar-section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 12px;
}
.mar-flow-chain {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  padding: 8px 0;
}
.mar-flow-step {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: var(--app-radius);
  background: rgba(148,163,184,0.06);
  border: 1px solid var(--app-border);
  font-size: 12px;
}
.mar-flow-step.is-skipped {
  opacity: 0.4;
}
.mar-flow-icon {
  color: var(--app-primary);
  font-size: 10px;
}
.mar-flow-label {
  color: var(--app-text-soft);
  font-size: 11px;
}
.mar-flow-arrow {
  color: var(--app-primary);
  font-size: 14px;
  margin: 0 2px;
}
.mar-flow-badge {
  margin-left: 2px;
}
.mar-flow-legend {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.mar-legend-item {
  font-size: 10px;
}

/* Phases */
.mar-phases {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.mar-phase-card {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 16px;
  transition: border-color 0.2s;
}
.mar-phase-card.is-skipped {
  opacity: 0.5;
}
.mar-phase-card.is-failed {
  border-color: var(--el-color-danger);
}

.mar-phase-header {
  margin-bottom: 14px;
}
.mar-phase-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mar-phase-order {
  font-weight: 700;
  font-size: 14px;
  color: var(--app-primary);
}
.mar-phase-key {
  font-size: 11px;
  color: var(--app-text-muted);
  background: rgba(148,163,184,0.1);
  padding: 2px 6px;
  border-radius: 4px;
}
.mar-phase-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-soft);
}
.mar-phase-summaries {
  margin-top: 8px;
  font-size: 12px;
  color: var(--app-text-muted);
  line-height: 1.5;
}
.mar-phase-summary-label {
  font-weight: 600;
  color: var(--app-text-soft);
}
.mar-phase-summary-text {
  color: var(--app-text-muted);
}

/* Phase Lanes */
.mar-phase-lanes {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.mar-phase-lanes.is-parallel {
  position: relative;
}
.mar-phase-lanes.is-parallel::before {
  content: '并行';
  position: absolute;
  top: -8px;
  right: 4px;
  font-size: 10px;
  color: var(--app-primary);
  background: var(--app-panel);
  padding: 0 6px;
  z-index: 1;
}

.mar-lane-card {
  flex: 1;
  min-width: 200px;
  background: rgba(148,163,184,0.03);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  overflow: hidden;
  transition: border-color 0.2s;
}
.mar-lane-card.is-active {
  border-color: var(--app-primary);
}
.mar-lane-card.is-skipped {
  opacity: 0.4;
}

.mar-lane-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  gap: 8px;
  transition: background 0.15s;
}
.mar-lane-header:hover {
  background: rgba(148,163,184,0.05);
}
.mar-lane-identity {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mar-lane-icon {
  font-size: 11px;
  color: var(--app-primary);
}
.mar-lane-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-soft);
}
.mar-lane-agent {
  font-size: 11px;
  color: var(--app-text-muted);
  font-family: monospace;
}
.mar-lane-badges {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mar-lane-msg-badge {
  margin-right: 2px;
}
.mar-step-expand {
  color: var(--app-text-muted);
  font-size: 11px;
  margin-left: 4px;
}

/* Lane Detail */
.mar-lane-detail {
  padding: 0 14px 14px;
}

/* Input Context */
.mar-step-context-section {
  margin-bottom: 12px;
}
.mar-step-context-header {
  font-size: 12px;
  color: var(--app-text-muted);
  cursor: pointer;
  padding: 6px 0;
  user-select: none;
}
.mar-step-context-header:hover {
  color: var(--app-primary);
}
.mar-step-context-body {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 16px;
  margin-top: 4px;
}

/* Step Messages */
.mar-step-msgs {
  margin-bottom: 12px;
}
.mar-step-msgs-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  margin-bottom: 8px;
}
.mar-step-msg-card {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 12px;
  margin-bottom: 8px;
}
.mar-msg-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.mar-msg-summary {
  font-size: 12px;
  color: var(--app-text-soft);
}
.mar-msg-content {
  font-size: 12px;
  max-height: 300px;
  overflow-y: auto;
}

/* Tool Sandbox */
.mar-tool-sandbox-section {
  margin-bottom: 12px;
  background: rgba(230,162,60,0.04);
  border: 1px solid rgba(230,162,60,0.2);
  border-radius: var(--app-radius);
  padding: 12px;
}
.mar-tool-sandbox-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-warning);
  margin-bottom: 10px;
}
.mar-tool-card {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 10px 12px;
  margin-bottom: 8px;
}
.mar-tool-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.mar-tool-status {
  font-size: 11px;
  color: var(--app-text-muted);
}
.mar-tool-duration {
  font-size: 11px;
  color: var(--app-text-muted);
  font-family: monospace;
}
.mar-tool-summary {
  margin-top: 6px;
  font-size: 12px;
  color: var(--app-text-soft);
}
.mar-tool-output-toggle {
  margin-top: 6px;
  font-size: 12px;
  color: var(--app-text-muted);
  cursor: pointer;
  user-select: none;
}
.mar-tool-output-toggle:hover {
  color: var(--app-primary);
}
.mar-tool-summary-blocked {
  color: var(--el-color-warning, #e6a23c) !important;
}
.mar-tool-output-toggle-blocked {
  color: var(--el-color-warning, #e6a23c);
}
.mar-tool-params {
  margin-top: 4px;
  font-size: 11px;
  color: var(--app-text-muted);
  display: flex;
  gap: 4px;
  align-items: flex-start;
}
.mar-tool-params-label {
  color: var(--app-text-soft);
  font-weight: 500;
  white-space: nowrap;
}
.mar-tool-params-value {
  font-family: monospace;
  word-break: break-all;
}
.mar-target-files-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.mar-target-file-item {
  font-family: monospace;
  font-size: 11px;
  color: var(--app-primary);
  background: rgba(64,158,255,0.06);
  padding: 1px 6px;
  border-radius: 3px;
  display: inline-block;
}
.mar-tool-output-body {
  margin-top: 4px;
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 10px;
  max-height: 200px;
  overflow: auto;
}
.mar-tool-output-pre {
  font-size: 11px;
  font-family: monospace;
  color: var(--app-text-soft);
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
.mar-tool-safety-notice {
  margin-top: 10px;
  font-size: 11px;
  color: var(--app-text-muted);
  padding: 6px 10px;
  background: rgba(148,163,184,0.06);
  border-radius: var(--app-radius);
  text-align: center;
}

/* Tool Approval */
.mar-tool-approval-badge {
  font-size: 10px;
  color: var(--el-color-warning);
  background: rgba(230,162,60,0.15);
  padding: 1px 6px;
  border-radius: 10px;
}
.mar-tool-summary-warning {
  color: var(--el-color-warning, #e6a23c) !important;
}
.mar-tool-approval-card {
  margin-top: 8px;
  background: rgba(230,162,60,0.06);
  border: 1px solid rgba(230,162,60,0.25);
  border-radius: var(--app-radius);
  padding: 10px 12px;
}
.mar-tool-approval-notice {
  font-size: 11px;
  color: var(--el-color-warning);
  margin-bottom: 8px;
  line-height: 1.5;
}
.mar-tool-approval-info {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
  font-size: 11px;
  color: var(--app-text-muted);
}
.mar-tool-approval-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.mar-tool-approval-input {
  flex: 1;
  min-width: 140px;
}
.mar-tool-approval-history {
  margin-top: 8px;
  background: rgba(148,163,184,0.04);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 8px 12px;
  font-size: 11px;
  color: var(--app-text-muted);
}
.mar-tool-approval-history-title {
  font-weight: 600;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.mar-tool-approval-history-item {
  margin-top: 2px;
  line-height: 1.5;
}

/* Output */
.mar-step-output {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 16px;
}

/* Final Summary */
.mar-final-summary {
  margin-top: 24px;
}
.mar-summary-content {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 20px;
}

/* Approval Gate */
.mar-approval-gate {
  margin-top: 24px;
  background: var(--app-panel);
  border: 2px solid var(--el-color-warning);
  border-radius: var(--app-radius);
  overflow: hidden;
}
.mar-approval-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(230,162,60,0.08);
  border-bottom: 1px solid rgba(230,162,60,0.2);
}
.mar-approval-title {
  font-weight: 700;
  font-size: 14px;
  color: var(--el-color-warning);
}
.mar-approval-body {
  padding: 16px;
}
.mar-approval-gate-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text);
  margin-bottom: 6px;
}
.mar-approval-gate-desc {
  font-size: 12px;
  color: var(--app-text-muted);
  line-height: 1.5;
}
.mar-approval-divider {
  margin: 14px 0;
  border-top: 1px solid var(--app-border);
}
.mar-approval-input-label {
  font-size: 12px;
  color: var(--app-text-soft);
  margin-bottom: 6px;
}
.mar-approval-actions {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}

/* Approval Gates History */
.mar-approval-gates-history {
  margin-top: 24px;
}
.mar-approval-gate-record {
  margin-top: 12px;
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 12px 16px;
}
.mar-approval-record-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mar-approval-record-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
}
.mar-approval-record-comment {
  margin-top: 8px;
  font-size: 12px;
  color: var(--app-text-soft);
}
.mar-approval-record-meta {
  margin-top: 6px;
  font-size: 11px;
  color: var(--app-text-muted);
  display: flex;
  gap: 16px;
}

/* Tool Artifact Link */
.mar-tool-artifact-link {
  margin-top: 8px;
  padding: 8px 10px;
  background: rgba(64,158,255,0.06);
  border: 1px solid rgba(64,158,255,0.15);
  border-radius: var(--app-radius);
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  font-size: 12px;
}
.mar-tool-patch-proposal-badge {
  font-size: 10px;
  color: var(--el-color-warning, #e6a23c);
  background: rgba(230,162,60,0.15);
  padding: 1px 6px;
  border-radius: 10px;
  font-weight: 500;
}
.mar-tool-artifact-name {
  color: var(--app-text);
  font-weight: 500;
}
.mar-tool-artifact-hint {
  color: var(--app-text-muted);
  font-size: 11px;
}
.mar-tool-job-retry {
  font-size: 10px;
  color: var(--el-color-danger);
  font-family: monospace;
}
.mar-tool-job-detail-body {
  margin-top: 6px;
  background: rgba(64,158,255,0.03);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 8px 10px;
}
.mar-tool-job-payload {
  margin-bottom: 6px;
}
.mar-tool-job-payload:last-child {
  margin-bottom: 0;
}
.mar-tool-job-payload-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--app-text-muted);
  margin-bottom: 2px;
}
.mar-tool-job-error {
  margin-top: 6px;
}
.mar-tool-job-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

/* Repository Read-Only Tool */
.mar-repo-meta-item {
  font-size: 11px;
  font-family: monospace;
  color: var(--app-text-muted);
  margin-left: 8px;
}
.mar-repo-file-item {
  font-size: 11px;
  font-family: monospace;
  color: var(--app-text-soft);
  padding: 2px 0;
  border-bottom: 1px solid var(--app-border);
}
.mar-repo-file-item:last-child {
  border-bottom: none;
}
.mar-repo-safety-note {
  margin-top: 6px;
  font-size: 11px;
  color: var(--app-text-muted);
  background: rgba(148,163,184,0.06);
  padding: 4px 10px;
  border-radius: var(--app-radius);
}
.mar-repo-safety-detail {
  color: var(--app-warning);
  font-weight: 600;
}
.mar-repo-skipped-item {
  font-size: 11px;
  font-family: monospace;
  color: var(--app-text-muted);
  padding: 2px 0;
  border-bottom: 1px solid var(--app-border);
  display: flex;
  align-items: center;
  gap: 6px;
}
.mar-repo-skipped-item:last-child {
  border-bottom: none;
}
.mar-repo-skipped-path {
  color: var(--app-text-muted);
  text-decoration: line-through;
  opacity: 0.7;
}
.mar-polling-indicator {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  background: rgba(230,162,60,0.1);
  border: 1px solid rgba(230,162,60,0.25);
  border-radius: var(--app-radius);
}

.mar-code-search-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
}

.mar-code-search-stat {
  font-size: 11px;
  color: var(--app-text-muted);
}

.mar-code-search-stat-value {
  font-weight: 700;
  color: var(--app-text-soft);
  font-family: 'SF Mono', 'Cascadia Code', monospace;
}

.mar-tool-job-dlq-info {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  padding: 6px 8px;
  background: rgba(230,162,60,0.08);
  border-radius: var(--app-radius);
}

.mar-tool-dlq-tag {
  font-size: 11px;
  color: var(--el-color-warning, #e6a23c);
  font-family: 'SF Mono', 'Cascadia Code', monospace;
}
</style>
