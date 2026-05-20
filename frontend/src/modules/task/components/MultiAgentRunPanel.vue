<script setup lang="ts">
import { ref, computed, onMounted, defineAsyncComponent } from 'vue'
import { ElMessage } from 'element-plus'
import {
  startMultiAgentRun,
  listMultiAgentRuns,
  getMultiAgentStrategies,
  approveMultiAgentGate,
  rejectMultiAgentGate,
  type MultiAgentRunResponse,
  type MultiAgentStepResponse,
  type MultiAgentMessageResponse,
  type MultiAgentPhaseResponse,
  type MultiAgentApprovalGateResponse,
  type MultiAgentApprovalDecisionRequest,
  type WorkflowStrategyResponse,
} from '@/modules/task/api'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { formatDateTime } from '@/shared/utils/format'

const MarkdownRenderer = defineAsyncComponent(() => import('@/shared/components/MarkdownRenderer.vue'))

const props = defineProps<{ taskId: string }>()

const runs = ref<MultiAgentRunResponse[]>([])
const loading = ref(false)
const starting = ref(false)
const selectedRun = ref<MultiAgentRunResponse | null>(null)
const expandedSteps = ref<Record<string, boolean>>({})
const showInputContext = ref<Record<string, boolean>>({})
const showMessages = ref(false)

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
    PENDING: 'muted', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger', SKIPPED: 'muted', WAITING_APPROVAL: 'warning',
  }
  return map[status] || 'muted'
}

function statusText(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待执行', RUNNING: '运行中', COMPLETED: '已完成', FAILED: '失败', SKIPPED: '已跳过', WAITING_APPROVAL: '等待审批',
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
          <el-tag size="small" type="primary" effect="plain">Phases {{ phases.length }}</el-tag>
          <el-tag size="small" type="warning" effect="plain">并行步骤 {{ parallelStepCount }}</el-tag>
          <el-tag size="small" type="success" effect="plain">完成步骤 {{ completedStepCount }}</el-tag>
          <el-tag v-if="skippedStepCount > 0" size="small" type="info" effect="plain">跳过 {{ skippedStepCount }}</el-tag>
          <el-tag size="small" type="primary" effect="plain">消息 {{ messageCount }}</el-tag>
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
</style>
