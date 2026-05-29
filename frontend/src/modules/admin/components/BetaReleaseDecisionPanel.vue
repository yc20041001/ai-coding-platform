<script setup lang="ts">
import { ref } from 'vue'
import {
  createReleaseDecision,
  updateReleaseDecision,
  getReadinessReport,
  listReleaseDecisions,
  getReleaseDecision,
  type BetaReleaseDecisionItem,
  type BetaReleaseReadinessReport,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { ElTag, ElButton, ElDialog, ElMessage, ElInput, ElSelect, ElOption } from 'element-plus'

const props = defineProps<{
  projectId?: string | null
}>()

const decisions = ref<BetaReleaseDecisionItem[]>([])
const loading = ref(false)
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const reportDialogVisible = ref(false)
const selectedDecision = ref<BetaReleaseDecisionItem | null>(null)
const readinessReport = ref<BetaReleaseReadinessReport | null>(null)

const formLabel = ref('')
const formStatus = ref('GO')
const formReason = ref('')

function loadDecisions() {
  if (!props.projectId) return
  loading.value = true
  listReleaseDecisions(props.projectId, { page: 1, size: 20 })
    .then(res => { decisions.value = res.data.data })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

async function handleCreate() {
  if (!props.projectId || !formLabel.value) {
    ElMessage.warning('请输入发布标签')
    return
  }
  try {
    await createReleaseDecision(props.projectId, {
      releaseLabel: formLabel.value,
      decisionStatus: formStatus.value,
      decisionReason: formReason.value || undefined,
    })
    ElMessage.success('决策已创建')
    createDialogVisible.value = false
    formLabel.value = ''
    formStatus.value = 'GO'
    formReason.value = ''
    loadDecisions()
  } catch {
    ElMessage.error('创建失败')
  }
}

async function handleViewDetail(decisionId: string) {
  if (!props.projectId) return
  try {
    const res = await getReleaseDecision(props.projectId, decisionId)
    selectedDecision.value = res.data.data
    detailDialogVisible.value = true
  } catch {
    ElMessage.error('获取决策详情失败')
  }
}

async function handleGenerateReport() {
  if (!props.projectId) return
  try {
    const res = await getReadinessReport(props.projectId)
    readinessReport.value = res.data.data
    reportDialogVisible.value = true
  } catch {
    ElMessage.error('生成报告失败')
  }
}

async function handleUpdateStatus(decisionId: string, newStatus: string) {
  if (!props.projectId) return
  try {
    await updateReleaseDecision(props.projectId, decisionId, {
      decisionStatus: newStatus,
    })
    ElMessage.success('决策状态已更新')
    loadDecisions()
    if (selectedDecision.value?.id === decisionId) {
      selectedDecision.value.decisionStatus = newStatus
    }
  } catch {
    ElMessage.error('更新失败')
  }
}

function decisionStatusTag(status: string) {
  const map: Record<string, 'success' | 'danger' | 'warning'> = {
    GO: 'success',
    NO_GO: 'danger',
    CONDITIONAL_GO: 'warning',
  }
  return map[status] || 'info'
}

defineExpose({ loadDecisions })
</script>

<template>
  <TechPanel title="Go/No-Go 决策中心" glow v-loading="loading" data-testid="beta-decision-panel">
    <div style="display:flex;gap:6px;margin-bottom:12px;flex-wrap:wrap">
      <ElButton size="small" type="primary" @click="createDialogVisible = true">创建决策</ElButton>
      <ElButton size="small" @click="handleGenerateReport">生成就绪报告</ElButton>
      <ElButton size="small" @click="loadDecisions">刷新</ElButton>
    </div>

    <div v-if="decisions.length === 0" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">暂无决策记录</div>
    <div v-for="d in decisions" :key="d.id" class="decision-item">
      <div class="decision-header">
        <ElTag :type="decisionStatusTag(d.decisionStatus)" size="small" effect="dark">{{ d.decisionStatus }}</ElTag>
        <span class="decision-label">{{ d.releaseLabel }}</span>
        <span class="decision-meta">{{ d.blockingIssueCount }} 阻塞 · {{ d.warningIssueCount }} 警告</span>
      </div>
      <div v-if="d.decisionReason" class="decision-reason">{{ d.decisionReason }}</div>
      <div class="decision-actions">
        <ElButton size="small" link type="primary" @click="handleViewDetail(d.id)">详情</ElButton>
        <ElButton v-if="d.decisionStatus === 'NO_GO'" size="small" link type="warning" @click="handleUpdateStatus(d.id, 'CONDITIONAL_GO')">改为有条件通过</ElButton>
        <ElButton v-if="d.decisionStatus === 'CONDITIONAL_GO'" size="small" link type="success" @click="handleUpdateStatus(d.id, 'GO')">改为通过</ElButton>
      </div>
    </div>

    <!-- Create Decision Dialog -->
    <ElDialog v-model="createDialogVisible" title="创建发布决策" width="450px" data-testid="create-decision-dialog">
      <div class="create-form">
        <div class="form-field">
          <label>发布标签</label>
          <ElInput v-model="formLabel" placeholder="例如: v2.3.0-rc1" />
        </div>
        <div class="form-field">
          <label>决策结果</label>
          <ElSelect v-model="formStatus" style="width:100%">
            <ElOption label="通过 (GO)" value="GO" />
            <ElOption label="不通过 (NO_GO)" value="NO_GO" />
            <ElOption label="有条件通过 (CONDITIONAL_GO)" value="CONDITIONAL_GO" />
          </ElSelect>
        </div>
        <div class="form-field">
          <label>决策理由</label>
          <ElInput v-model="formReason" type="textarea" :rows="3" placeholder="可选: 填写决策理由" />
        </div>
      </div>
      <template #footer>
        <ElButton @click="createDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleCreate">创建</ElButton>
      </template>
    </ElDialog>

    <!-- Decision Detail Dialog -->
    <ElDialog v-model="detailDialogVisible" title="决策详情" width="550px" data-testid="decision-detail-dialog">
      <div v-if="selectedDecision" class="detail-content">
        <div class="detail-field">
          <label>发布标签</label>
          <span>{{ selectedDecision.releaseLabel }}</span>
        </div>
        <div class="detail-field">
          <label>决策结果</label>
          <ElTag :type="decisionStatusTag(selectedDecision.decisionStatus)" size="small" effect="dark">{{ selectedDecision.decisionStatus }}</ElTag>
        </div>
        <div class="detail-field">
          <label>阻塞问题数</label>
          <span>{{ selectedDecision.blockingIssueCount }}</span>
        </div>
        <div class="detail-field">
          <label>警告数</label>
          <span>{{ selectedDecision.warningIssueCount }}</span>
        </div>
        <div class="detail-field" v-if="selectedDecision.decisionReason">
          <label>决策理由</label>
          <span>{{ selectedDecision.decisionReason }}</span>
        </div>
        <div class="detail-field" v-if="selectedDecision.approvedAt">
          <label>审批时间</label>
          <span>{{ selectedDecision.approvedAt }}</span>
        </div>
        <div v-if="selectedDecision.reportMarkdown" class="report-section">
          <label>报告内容</label>
          <pre class="report-markdown">{{ selectedDecision.reportMarkdown }}</pre>
        </div>
      </div>
      <template #footer>
        <ElButton @click="detailDialogVisible = false">关闭</ElButton>
      </template>
    </ElDialog>

    <!-- Readiness Report Dialog -->
    <ElDialog v-model="reportDialogVisible" title="发布就绪报告" width="80%" data-testid="readiness-report-dialog">
      <div v-if="readinessReport">
        <div class="report-header">
          <ElTag v-if="readinessReport.overallStatus === 'PASS'" type="success" size="large" effect="dark">通过</ElTag>
          <ElTag v-else-if="readinessReport.overallStatus === 'WARN'" type="warning" size="large" effect="dark">警告</ElTag>
          <ElTag v-else type="danger" size="large" effect="dark">阻塞</ElTag>
          <span class="report-label">{{ readinessReport.releaseLabel }}</span>
        </div>
        <pre class="report-markdown-full">{{ readinessReport.reportMarkdown }}</pre>
        <div v-if="readinessReport.evaluations && readinessReport.evaluations.length > 0" style="margin-top:12px">
          <div style="font-size:12px;font-weight:700;color:var(--app-text-soft);margin-bottom:8px">门禁评估详情</div>
          <div v-for="e in readinessReport.evaluations" :key="e.ruleKey" class="report-eval-item">
            <ElTag :type="e.gateStatus === 'PASS' ? 'success' : e.gateStatus === 'BLOCK' ? 'danger' : 'warning'" size="small">
              {{ e.gateStatus }}
            </ElTag>
            <span class="report-eval-name">{{ e.ruleKey }}</span>
            <span class="report-eval-summary">{{ e.summary }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <ElButton @click="reportDialogVisible = false">关闭</ElButton>
      </template>
    </ElDialog>
  </TechPanel>
</template>

<style scoped>
.decision-item {
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 6px;
}
.decision-header {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.decision-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-bright);
  flex: 1;
}
.decision-meta {
  font-size: 11px;
  color: var(--app-text-muted);
}
.decision-reason {
  font-size: 11px;
  color: var(--app-text-soft);
  margin-top: 4px;
}
.decision-actions {
  margin-top: 6px;
  display: flex;
  gap: 6px;
}
.create-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.form-field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-soft);
}
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.detail-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.detail-field label {
  font-size: 11px;
  font-weight: 700;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.detail-field span {
  font-size: 13px;
  color: var(--app-text-bright);
}
.report-section {
  margin-top: 8px;
}
.report-section label {
  font-size: 11px;
  font-weight: 700;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  display: block;
  margin-bottom: 4px;
}
.report-markdown {
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  padding: 12px;
  font-size: 11px;
  white-space: pre-wrap;
  max-height: 300px;
  overflow: auto;
  color: var(--app-text-soft);
}
.report-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.report-label {
  font-size: 14px;
  font-weight: 700;
  color: var(--app-text-bright);
}
.report-markdown-full {
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  padding: 16px;
  font-size: 12px;
  white-space: pre-wrap;
  max-height: 400px;
  overflow: auto;
  color: var(--app-text-soft);
}
.report-eval-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  margin-bottom: 4px;
}
.report-eval-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-bright);
}
.report-eval-summary {
  font-size: 11px;
  color: var(--app-text-soft);
  flex: 1;
}
</style>
