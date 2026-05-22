<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElDrawer, ElTag, ElCollapse, ElCollapseItem, ElButton, ElMessage, ElDropdown, ElDropdownItem, ElDropdownMenu } from 'element-plus'
import { getToolExecutionTrace, exportExecutionAudit, exportRunEvidence, exportTaskToolAudit } from '@/modules/task/api'
import type { ToolExecutionTrace, ToolExecutionTraceEvent, ToolAuditExport } from '@/modules/task/api'
import ToolOperatorReviewDialog from '@/modules/task/components/ToolOperatorReviewDialog.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import { formatDateTime } from '@/shared/utils/format'


const props = defineProps<{
  modelValue: boolean
  executionId?: string
  runId?: string
  taskId?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const trace = ref<ToolExecutionTrace | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const exporting = ref(false)

// Review dialog
const reviewDialogVisible = ref(false)

watch(() => props.modelValue, async (val) => {
  if (val && props.executionId) {
    await loadTrace()
  } else if (!val) {
    trace.value = null
    error.value = null
  }
})

async function loadTrace() {
  if (!props.executionId) return
  loading.value = true
  error.value = null
  try {
    const res = await getToolExecutionTrace(props.executionId)
    trace.value = res.data.data
  } catch (e: any) {
    error.value = e?.response?.data?.message || '加载证据链失败'
  } finally {
    loading.value = false
  }
}

function closeDrawer() {
  emit('update:modelValue', false)
}

function statusTone(status: string | undefined) {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    COMPLETED: 'success',
    FAILED: 'danger',
    RUNNING: 'warning',
    PENDING: 'info',
    RETRY_PENDING: 'warning',
    BLOCKED: 'danger',
    REJECTED: 'danger',
    CANCELED: 'info',
    WAITING_APPROVAL: 'warning',
    APPROVED: 'success',
    DEAD_LETTERED: 'danger',
  }
  return (map[status || ''] || 'info') as 'primary' | 'success' | 'warning' | 'info' | 'danger'
}

function eventIcon(eventType: string): string {
  const map: Record<string, string> = {
    EXECUTION_CREATED: '▶',
    POLICY_CHECKED: '✓',
    APPROVAL_CREATED: '◷',
    APPROVAL_ACCEPTED: '✓',
    APPROVAL_REJECTED: '✗',
    JOB_CREATED: '●',
    JOB_QUEUED: '◉',
    JOB_RUNNING: '◎',
    JOB_RETRY_PENDING: '↻',
    JOB_COMPLETED: '✓',
    JOB_FAILED: '✗',
    JOB_DEAD_LETTERED: '☠',
    OUTPUT_CAPTURED: '⤵',
    ARTIFACT_CREATED: '◆',
    PATCH_REVIEW_CREATED: '◈',
    PATCH_REVIEW_DECIDED: '◇',
    READ_ONLY_CONTRACT_WARNING: '⚠',
    OUTPUT_PARSE_WARNING: '⚠',
  }
  return map[eventType] || '○'
}

function eventTone(eventType: string) {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    APPROVAL_REJECTED: 'danger',
    JOB_FAILED: 'danger',
    JOB_DEAD_LETTERED: 'danger',
    READ_ONLY_CONTRACT_WARNING: 'warning',
    OUTPUT_PARSE_WARNING: 'warning',
    JOB_RETRY_PENDING: 'warning',
    BLOCKED: 'danger',
    APPROVAL_ACCEPTED: 'success',
    JOB_COMPLETED: 'success',
    OUTPUT_CAPTURED: 'success',
  }
  return (map[eventType] || 'primary') as 'primary' | 'success' | 'warning' | 'info' | 'danger'
}

function statusLabel(status: string | undefined): string {
  const map: Record<string, string> = {
    COMPLETED: '已完成',
    FAILED: '失败',
    RUNNING: '运行中',
    PENDING: '待执行',
    RETRY_PENDING: '待重试',
    BLOCKED: '已阻止',
    REJECTED: '已驳回',
    CANCELED: '已取消',
    WAITING_APPROVAL: '等待审批',
    DEAD_LETTERED: '死信',
    APPROVED: '已通过',
  }
  return map[status || ''] || status || ''
}

function riskLabel(level: string | undefined): string {
  if (level === 'LOW') return '低风险'
  if (level === 'MEDIUM') return '中风险'
  if (level === 'HIGH') return '高风险'
  return level || ''
}

function riskTone(level: string | undefined) {
  if (level === 'LOW') return 'info' as const
  if (level === 'MEDIUM') return 'warning' as const
  if (level === 'HIGH') return 'danger' as const
  return 'info' as const
}

// ---- Export / Copy / Review ----

async function handleExport(cmd: string) {
  if (cmd === 'execution') {
    if (!props.executionId) return
    exporting.value = true
    try {
      const res = await exportExecutionAudit(props.executionId)
      downloadMarkdown(res.data.data.fileName, res.data.data.markdown)
      ElMessage.success('审计报告已导出')
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '导出失败')
    } finally {
      exporting.value = false
    }
  } else if (cmd.startsWith('run:') && trace.value?.runId) {
    const runId = cmd.split(':')[1]
    exporting.value = true
    try {
      const res = await exportRunEvidence(runId)
      downloadMarkdown(res.data.data.fileName, res.data.data.markdown)
      ElMessage.success('Run 证据已导出')
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '导出失败')
    } finally {
      exporting.value = false
    }
  } else if (cmd.startsWith('task:') && trace.value?.taskId) {
    const taskId = cmd.split(':')[1]
    exporting.value = true
    try {
      const res = await exportTaskToolAudit(taskId)
      downloadMarkdown(res.data.data.fileName, res.data.data.markdown)
      ElMessage.success('Task 审计报告已导出')
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '导出失败')
    } finally {
      exporting.value = false
    }
  }
}

function downloadMarkdown(fileName: string, content: string) {
  const blob = new Blob([content], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  a.click()
  URL.revokeObjectURL(url)
}

function copyMarkdown() {
  if (!trace.value) return
  const lines = [
    `# Tool Execution Audit Report`,
    ``,
    `**Tool**: ${trace.value.toolKey}`,
    `**Status**: ${trace.value.status}`,
    `**Execution ID**: ${trace.value.executionId}`,
    trace.value.riskLevel ? `**Risk**: ${trace.value.riskLevel}` : '',
    trace.value.createTime ? `**Time**: ${formatDateTime(trace.value.createTime)}` : '',
  ].filter(Boolean).join('\n')

  navigator.clipboard.writeText(lines).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function openReviewDialog() {
  reviewDialogVisible.value = true
}

function onReviewSaved(review: any) {
  ElMessage.success('审查记录已创建')
}
</script>

<template>
  <ElDrawer
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="trace ? '证据链: ' + trace.toolKey : '工具执行证据链'"
    size="60%"
    :before-close="closeDrawer"
    :data-testid="'tool-trace-drawer'"
  >
    <!-- Loading -->
    <div v-if="loading" class="tet-loading" data-testid="tool-trace-loading">加载证据链中...</div>

    <!-- Error -->
    <div v-else-if="error" class="tet-error" data-testid="tool-trace-error">{{ error }}</div>

    <!-- No executionId -->
    <div v-else-if="!executionId" class="tet-loading">未提供执行 ID</div>

    <!-- Trace Content -->
    <template v-else-if="trace">
      <div class="tet-container">
        <!-- Header -->
        <div class="tet-header" data-testid="tool-trace-header">
          <div class="tet-header-row">
            <span class="tet-header-key">{{ trace.toolKey }}</span>
            <ElTag :type="statusTone(trace.status)" size="small" effect="dark" data-testid="tool-trace-status">
              {{ statusLabel(trace.status) }}
            </ElTag>
            <ElTag v-if="trace.riskLevel" :type="riskTone(trace.riskLevel)" size="small" effect="plain" data-testid="tool-trace-risk">
              {{ riskLabel(trace.riskLevel) }}
            </ElTag>
            <ElTag v-if="trace.readOnly" type="info" size="small" effect="dark" data-testid="tool-trace-readonly-badge">
              只读
            </ElTag>
            <ElTag v-if="trace.mode" size="small" effect="plain">
              {{ trace.mode }}
            </ElTag>
          </div>
          <div class="tet-header-meta">
            <span>执行 ID: {{ trace.executionId }}</span>
            <span v-if="trace.createTime">创建时间: {{ formatDateTime(trace.createTime) }}</span>
          </div>
          <div class="tet-actions" data-testid="tool-trace-actions">
            <ElButton size="small" @click="copyMarkdown" data-testid="tet-copy-btn">复制摘要</ElButton>
            <ElDropdown trigger="click" @command="handleExport">
              <ElButton size="small" :loading="exporting" data-testid="tet-export-btn">
                {{ exporting ? '导出中...' : '导出 Markdown' }}
              </ElButton>
              <template #dropdown>
                <ElDropdownMenu>
                  <ElDropdownItem command="execution" data-testid="tet-export-exec">当前执行</ElDropdownItem>
                  <ElDropdownItem v-if="trace.runId" :command="'run:' + trace.runId" data-testid="tet-export-run">所属 Run</ElDropdownItem>
                  <ElDropdownItem v-if="trace.taskId" :command="'task:' + trace.taskId" data-testid="tet-export-task">所属 Task</ElDropdownItem>
                </ElDropdownMenu>
              </template>
            </ElDropdown>
            <ElButton size="small" type="warning" @click="openReviewDialog" data-testid="tet-review-btn">
              创建审查
            </ElButton>
          </div>
        </div>

        <!-- Safety Banner -->
        <div class="tet-safety-banner" data-testid="tool-trace-safety-banner">
          <div class="tet-safety-row">
            <span v-if="trace.readOnly" class="tet-safety-tag">🔒 只读执行</span>
            <span class="tet-safety-tag">✏️ 未写入文件</span>
            <span class="tet-safety-tag">🔧 未执行 Git 写操作</span>
            <ElTag v-if="trace.evidence?.redacted" size="small" type="warning" effect="dark" data-testid="tool-trace-redacted-badge">已脱敏</ElTag>
            <ElTag v-if="trace.evidence?.truncated" size="small" type="info" effect="dark" data-testid="tool-trace-truncated-badge">已截断</ElTag>
          </div>
        </div>

        <!-- Warning Events -->
        <div v-if="trace.events && trace.events.some(e => e.eventType === 'READ_ONLY_CONTRACT_WARNING' || e.eventType === 'OUTPUT_PARSE_WARNING')" class="tet-warning-banner" data-testid="tool-trace-warning">
          <div v-for="(evt, idx) in trace.events.filter(e => e.eventType === 'READ_ONLY_CONTRACT_WARNING' || e.eventType === 'OUTPUT_PARSE_WARNING')" :key="idx" class="tet-warning-item">
            ⚠️ {{ evt.title }}: {{ evt.description }}
          </div>
        </div>

        <!-- Timeline -->
        <div class="tet-section" data-testid="tool-trace-timeline">
          <h4 class="tet-section-title">时间线</h4>
          <div v-if="trace.events && trace.events.length > 0" class="tet-timeline">
            <div
              v-for="(evt, idx) in trace.events"
              :key="idx"
              class="tet-timeline-event"
              data-testid="tool-trace-event"
            >
              <div class="tet-event-icon" :class="`tet-event-icon--${eventTone(evt.eventType)}`">
                {{ eventIcon(evt.eventType) }}
              </div>
              <div class="tet-event-content">
                <div class="tet-event-title-row">
                  <span class="tet-event-title">{{ evt.title }}</span>
                  <ElTag v-if="evt.status" :type="statusTone(evt.status)" size="small" effect="dark">
                    {{ statusLabel(evt.status) }}
                  </ElTag>
                </div>
                <div v-if="evt.description" class="tet-event-desc">{{ evt.description }}</div>
                <div v-if="evt.eventTime" class="tet-event-time">{{ formatDateTime(evt.eventTime) }}</div>
              </div>
            </div>
          </div>
          <div v-else class="tet-empty">无事件记录</div>
        </div>

        <!-- Evidence Section -->
        <div class="tet-section" v-if="trace.evidence" data-testid="tool-trace-evidence">
          <h4 class="tet-section-title">执行证据</h4>
          <div class="tet-evidence-grid">
            <div class="tet-evidence-card" data-testid="tool-trace-files-read">
              <div class="tet-evidence-value">{{ trace.evidence.filesReadCount }}</div>
              <div class="tet-evidence-label">读取文件</div>
            </div>
            <div class="tet-evidence-card" data-testid="tool-trace-skipped-files">
              <div class="tet-evidence-value">{{ trace.evidence.skippedFilesCount }}</div>
              <div class="tet-evidence-label">跳过文件</div>
            </div>
            <div class="tet-evidence-card">
              <div class="tet-evidence-value-bool">{{ trace.evidence.redacted ? '是' : '否' }}</div>
              <div class="tet-evidence-label">已脱敏</div>
            </div>
            <div class="tet-evidence-card">
              <div class="tet-evidence-value-bool">{{ trace.evidence.truncated ? '是' : '否' }}</div>
              <div class="tet-evidence-label">已截断</div>
            </div>
            <div class="tet-evidence-card" v-if="trace.evidence.binarySkipped !== undefined">
              <div class="tet-evidence-value-bool">{{ trace.evidence.binarySkipped ? '是' : '否' }}</div>
              <div class="tet-evidence-label">二进制跳过</div>
            </div>
            <div class="tet-evidence-card" v-if="trace.evidence.sensitiveDenylistApplied !== undefined">
              <div class="tet-evidence-value-bool">{{ trace.evidence.sensitiveDenylistApplied ? '是' : '否' }}</div>
              <div class="tet-evidence-label">敏感路径过滤</div>
            </div>
          </div>

          <!-- Files Read detail -->
          <div v-if="trace.evidence.filesRead && trace.evidence.filesRead.length > 0" class="tet-evidence-detail">
            <h5 class="tet-sub-title">已读文件</h5>
            <div v-for="(f, idx) in trace.evidence.filesRead" :key="idx" class="tet-file-item">
              <span class="tet-file-path">{{ f.path }}</span>
              <span v-if="f.lineStart" class="tet-file-lines">L{{ f.lineStart }}-{{ f.lineEnd }}</span>
              <ElTag v-if="f.redacted" size="small" type="warning" effect="dark">脱敏</ElTag>
              <ElTag v-if="f.truncated" size="small" type="info" effect="dark">截断</ElTag>
            </div>
          </div>

          <!-- Skipped Files detail -->
          <div v-if="trace.evidence.skippedFiles && trace.evidence.skippedFiles.length > 0" class="tet-evidence-detail">
            <h5 class="tet-sub-title">跳过文件</h5>
            <div v-for="(sf, idx) in trace.evidence.skippedFiles" :key="idx" class="tet-file-item tet-file-item--skipped">
              <span class="tet-file-path">{{ sf.path }}</span>
              <ElTag v-if="sf.reason" size="small" type="info" effect="dark">{{ sf.reason }}</ElTag>
            </div>
          </div>
        </div>

        <!-- Approval Section -->
        <div v-if="trace.approval" class="tet-section" data-testid="tool-trace-approval">
          <h4 class="tet-section-title">审批</h4>
          <div class="tet-info-grid">
            <div class="tet-info-row">
              <span class="tet-info-label">审批 ID</span>
              <span class="tet-info-value">{{ trace.approval.approvalId }}</span>
            </div>
            <div class="tet-info-row">
              <span class="tet-info-label">状态</span>
              <ElTag :type="statusTone(trace.approval.status)" size="small" effect="dark">{{ statusLabel(trace.approval.status) }}</ElTag>
            </div>
            <div class="tet-info-row" v-if="trace.approval.comment">
              <span class="tet-info-label">备注</span>
              <span class="tet-info-value">{{ trace.approval.comment }}</span>
            </div>
            <div class="tet-info-row" v-if="trace.approval.createTime">
              <span class="tet-info-label">请求时间</span>
              <span class="tet-info-value">{{ formatDateTime(trace.approval.createTime) }}</span>
            </div>
            <div class="tet-info-row" v-if="trace.approval.decidedAt">
              <span class="tet-info-label">决定时间</span>
              <span class="tet-info-value">{{ formatDateTime(trace.approval.decidedAt) }}</span>
            </div>
          </div>
        </div>

        <!-- Job Section -->
        <div v-if="trace.job" class="tet-section" data-testid="tool-trace-job">
          <h4 class="tet-section-title">Job</h4>
          <div class="tet-info-grid">
            <div class="tet-info-row">
              <span class="tet-info-label">Job ID</span>
              <span class="tet-info-value">{{ trace.job.jobId }}</span>
            </div>
            <div class="tet-info-row">
              <span class="tet-info-label">状态</span>
              <ElTag :type="statusTone(trace.job.status)" size="small" effect="dark">{{ statusLabel(trace.job.status) }}</ElTag>
            </div>
            <div class="tet-info-row">
              <span class="tet-info-label">优先级</span>
              <span class="tet-info-value">{{ trace.job.priority || '-' }}</span>
            </div>
            <div class="tet-info-row">
              <span class="tet-info-label">尝试次数</span>
              <span class="tet-info-value">{{ trace.job.attemptCount || 1 }}</span>
            </div>
            <div class="tet-info-row" v-if="trace.job.errorCode">
              <span class="tet-info-label">错误码</span>
              <span class="tet-info-value tet-error-text">{{ trace.job.errorCode }}</span>
            </div>
            <div class="tet-info-row" v-if="trace.job.failureStage">
              <span class="tet-info-label">失败阶段</span>
              <span class="tet-info-value tet-error-text">{{ trace.job.failureStage }}</span>
            </div>
            <div class="tet-info-row" v-if="trace.job.nextRetryAt">
              <span class="tet-info-label">下次重试</span>
              <span class="tet-info-value">{{ formatDateTime(trace.job.nextRetryAt) }}</span>
            </div>
            <div class="tet-info-row" v-if="trace.job.deadLetteredAt">
              <span class="tet-info-label">死信时间</span>
              <span class="tet-info-value">{{ formatDateTime(trace.job.deadLetteredAt) }}</span>
            </div>
            <div class="tet-info-row" v-if="trace.job.deadLetterReason">
              <span class="tet-info-label">死信原因</span>
              <span class="tet-info-value tet-error-text">{{ trace.job.deadLetterReason }}</span>
            </div>
            <div class="tet-info-row" v-if="trace.job.sourceJobId">
              <span class="tet-info-label">源 Job ID</span>
              <span class="tet-info-value">{{ trace.job.sourceJobId }}</span>
            </div>
            <div class="tet-info-row" v-if="trace.job.startedAt">
              <span class="tet-info-label">开始时间</span>
              <span class="tet-info-value">{{ formatDateTime(trace.job.startedAt) }}</span>
            </div>
            <div class="tet-info-row" v-if="trace.job.finishedAt">
              <span class="tet-info-label">完成时间</span>
              <span class="tet-info-value">{{ formatDateTime(trace.job.finishedAt) }}</span>
            </div>
          </div>
        </div>

        <!-- Artifacts Section -->
        <div v-if="trace.evidence?.artifacts && trace.evidence.artifacts.length > 0" class="tet-section" data-testid="tool-trace-artifact">
          <h4 class="tet-section-title">产物</h4>
          <div v-for="(art, idx) in trace.evidence.artifacts" :key="idx" class="tet-artifact-item">
            <div class="tet-artifact-header">
              <ElTag size="small" type="primary" effect="dark" data-testid="tool-trace-artifact-type">{{ art.artifactType }}</ElTag>
              <span class="tet-artifact-title">{{ art.title }}</span>
            </div>
            <div v-if="art.patchReviewStatus" class="tet-artifact-review">
              <span class="tet-info-label">审查状态:</span>
              <ElTag size="small" :type="statusTone(art.patchReviewStatus)" effect="dark">{{ art.patchReviewStatus }}</ElTag>
              <span v-if="art.patchReviewDecision" class="tet-artifact-decision">
                决策: {{ art.patchReviewDecision }}
              </span>
            </div>
          </div>
        </div>

        <!-- Policy Info -->
        <div class="tet-section">
          <h4 class="tet-section-title">策略信息</h4>
          <div class="tet-info-grid">
            <div class="tet-info-row">
              <span class="tet-info-label">策略允许</span>
              <ElTag v-if="trace.policyAllowed" type="success" size="small" effect="dark">是</ElTag>
              <ElTag v-else type="danger" size="small" effect="dark">否</ElTag>
            </div>
            <div class="tet-info-row" v-if="trace.policyReason">
              <span class="tet-info-label">策略原因</span>
              <span class="tet-info-value">{{ trace.policyReason }}</span>
            </div>
            <div class="tet-info-row" v-if="trace.policyAllowed === false">
              <span class="tet-info-label">执行模式</span>
              <span class="tet-info-value">{{ trace.mode || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- Payload -->
        <div class="tet-section" data-testid="tool-trace-payload">
          <h4 class="tet-section-title">载荷 (Payload)</h4>
          <ElCollapse>
            <ElCollapseItem title="输入载荷" name="input-payload">
              <pre class="tet-payload-pre">{{ trace.inputPayload || '(空)' }}</pre>
            </ElCollapseItem>
            <ElCollapseItem title="输出载荷" name="output-payload">
              <pre class="tet-payload-pre">{{ trace.outputPayload || '(空)' }}</pre>
            </ElCollapseItem>
          </ElCollapse>
        </div>
      </div>
    </template>
  </ElDrawer>

  <!-- Operator Review Dialog -->
  <ToolOperatorReviewDialog
    v-model="reviewDialogVisible"
    :target-type="'TOOL_EXECUTION'"
    :target-id="props.executionId || ''"
    :project-id="trace?.projectId || ''"
    @saved="onReviewSaved"
  />
</template>

<style scoped>
.tet-loading, .tet-empty {
  padding: 40px;
  text-align: center;
  color: var(--app-text-muted);
  font-size: 14px;
}

.tet-error {
  padding: 40px;
  text-align: center;
  color: var(--el-color-danger);
  font-size: 14px;
}

.tet-container {
  padding: 0 4px;
}

.tet-header {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(56, 189, 248, 0.15);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.tet-header-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tet-header-key {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text-bright);
  font-family: 'SF Mono', 'Cascadia Code', monospace;
}

.tet-header-meta {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--app-text-muted);
}

.tet-actions {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(56, 189, 248, 0.1);
}

.tet-safety-banner {
  background: rgba(52, 211, 153, 0.08);
  border: 1px solid rgba(52, 211, 153, 0.2);
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 12px;
}

.tet-safety-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.tet-safety-tag {
  font-size: 12px;
  color: var(--app-text-soft);
}

.tet-warning-banner {
  background: rgba(251, 191, 36, 0.08);
  border: 1px solid rgba(251, 191, 36, 0.2);
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 12px;
}

.tet-warning-item {
  font-size: 12px;
  color: var(--app-warning);
  margin-bottom: 4px;
}

.tet-warning-item:last-child { margin-bottom: 0; }

.tet-section {
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}

.tet-section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--app-text-soft);
  margin: 0 0 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.tet-sub-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  margin: 12px 0 6px;
}

.tet-timeline {
  position: relative;
}

.tet-timeline-event {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  position: relative;
}

.tet-timeline-event::before {
  content: '';
  position: absolute;
  left: 11px;
  top: 24px;
  bottom: 0;
  width: 1px;
  background: rgba(56, 189, 248, 0.15);
}

.tet-timeline-event:last-child::before {
  display: none;
}

.tet-event-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  flex-shrink: 0;
  background: rgba(56, 189, 248, 0.15);
  color: #38bdf8;
}

.tet-event-icon--success { background: rgba(52, 211, 153, 0.15); color: #34d399; }
.tet-event-icon--danger { background: rgba(239, 68, 68, 0.15); color: #ef4444; }
.tet-event-icon--warning { background: rgba(251, 191, 36, 0.15); color: #fbbf24; }
.tet-event-icon--primary { background: rgba(56, 189, 248, 0.15); color: #38bdf8; }

.tet-event-content {
  flex: 1;
  min-width: 0;
}

.tet-event-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.tet-event-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-bright);
}

.tet-event-desc {
  font-size: 12px;
  color: var(--app-text-muted);
  margin-top: 2px;
}

.tet-event-time {
  font-size: 11px;
  color: var(--app-text-muted);
  margin-top: 2px;
  opacity: 0.7;
}

.tet-evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.tet-evidence-card {
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(56, 189, 248, 0.12);
  border-radius: 6px;
  padding: 12px;
  text-align: center;
}

.tet-evidence-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--app-text-bright);
  line-height: 1.2;
}

.tet-evidence-value-bool {
  font-size: 14px;
  font-weight: 700;
  color: var(--app-text-soft);
  line-height: 1.2;
}

.tet-evidence-label {
  font-size: 10px;
  color: var(--app-text-muted);
  margin-top: 4px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.tet-evidence-detail {
  margin-top: 8px;
}

.tet-file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: rgba(15, 23, 42, 0.3);
  border-radius: 4px;
  margin-bottom: 4px;
}

.tet-file-item--skipped {
  opacity: 0.7;
}

.tet-file-path {
  font-size: 12px;
  font-weight: 500;
  color: var(--app-text-soft);
  font-family: 'SF Mono', 'Cascadia Code', monospace;
  flex: 1;
}

.tet-file-lines {
  font-size: 11px;
  color: var(--app-text-muted);
  font-family: 'SF Mono', 'Cascadia Code', monospace;
}

.tet-info-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tet-info-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tet-info-label {
  font-size: 12px;
  color: var(--app-text-muted);
  min-width: 80px;
  flex-shrink: 0;
}

.tet-info-value {
  font-size: 12px;
  color: var(--app-text-soft);
}

.tet-error-text {
  color: var(--el-color-danger);
}

.tet-artifact-item {
  background: rgba(15, 23, 42, 0.3);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 6px;
}

.tet-artifact-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tet-artifact-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-bright);
}

.tet-artifact-review {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--app-text-muted);
}

.tet-artifact-decision {
  color: var(--app-text-soft);
}

.tet-payload-pre {
  margin: 0;
  padding: 10px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
  font-size: 11px;
  line-height: 1.5;
  color: var(--app-text-soft);
  max-height: 300px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
