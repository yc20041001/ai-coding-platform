<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  getGateDashboard,
  evaluateReleaseGate,
  listGateRules,
  updateGateRule,
  type BetaReleaseGateDashboard,
  type BetaReleaseGateRuleItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag, ElButton, ElDialog, ElMessage, ElInputNumber, ElSwitch } from 'element-plus'

const props = defineProps<{
  projectId?: string | null
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'decision-created'): void
}>()

const dashboard = ref<BetaReleaseGateDashboard | null>(null)
const rules = ref<BetaReleaseGateRuleItem[]>([])
const loadingDashboard = ref(false)
const error = ref(false)
const evaluating = ref(false)
const ruleDialogVisible = ref(false)
const editingRule = ref<BetaReleaseGateRuleItem | null>(null)
const editEnabled = ref(true)
const editBlocking = ref(true)
const editThreshold = ref<number | null>(null)

function loadDashboard() {
  if (!props.projectId) return
  loadingDashboard.value = true
  error.value = false
  getGateDashboard(props.projectId)
    .then(res => { dashboard.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loadingDashboard.value = false })
}

function loadRules() {
  if (!props.projectId) return
  listGateRules(props.projectId)
    .then(res => { rules.value = res.data.data })
    .catch(() => {})
}

async function handleEvaluate() {
  if (!props.projectId) return
  evaluating.value = true
  try {
    await evaluateReleaseGate(props.projectId)
    ElMessage.success('门禁评估完成')
    loadDashboard()
    loadRules()
  } catch {
    ElMessage.error('评估失败')
  } finally {
    evaluating.value = false
  }
}

function openRuleDialog(rule: BetaReleaseGateRuleItem) {
  editingRule.value = rule
  editEnabled.value = rule.enabled === 1
  editBlocking.value = rule.blocking === 1
  editThreshold.value = rule.thresholdValue
  ruleDialogVisible.value = true
}

async function handleSaveRule() {
  if (!props.projectId || !editingRule.value) return
  try {
    await updateGateRule(props.projectId, editingRule.value.id, {
      enabled: editEnabled.value ? 'true' : 'false',
      blocking: editBlocking.value ? 'true' : 'false',
      thresholdValue: editThreshold.value ?? undefined,
    })
    ElMessage.success('规则已更新')
    ruleDialogVisible.value = false
    loadRules()
  } catch {
    ElMessage.error('更新失败')
  }
}

function gateStatusTag(status: string) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    PASS: 'success',
    BLOCK: 'danger',
    WARN: 'warning',
    SKIP: 'info',
  }
  return map[status] || 'info'
}

function overallStatusTag(status: string) {
  const map: Record<string, 'success' | 'danger' | 'warning'> = {
    PASS: 'success',
    BLOCK: 'danger',
    WARN: 'warning',
  }
  return map[status] || 'info'
}

function categoryTag(category: string) {
  const map: Record<string, 'info' | 'success' | 'warning' | 'danger'> = {
    TRIAL_FEEDBACK: 'warning',
    ENVIRONMENT_READINESS: 'info',
    MODEL_COST: 'danger',
    PR_REVIEW_QUALITY: 'success',
    INCIDENT_RISK: 'danger',
    KNOWLEDGE_QUALITY: 'info',
  }
  return map[category] || 'info'
}

watch(() => props.projectId, (val) => {
  if (val) {
    loadDashboard()
    loadRules()
  }
}, { immediate: true })
</script>

<template>
  <TechPanel title="Beta 发布门禁" glow v-loading="loadingDashboard" data-testid="beta-gate-panel">
    <ErrorState v-if="error" title="加载失败" message="无法获取门禁数据" retry-text="重试" @retry="loadDashboard" />
    <template v-else>
      <div style="display:flex;gap:6px;margin-bottom:12px;flex-wrap:wrap">
        <ElButton size="small" type="primary" :loading="evaluating" @click="handleEvaluate">执行门禁评估</ElButton>
      </div>

      <div class="card-grid" v-if="dashboard && dashboard.summary">
        <MetricTile :value="dashboard.summary.totalRules" label="规则总数" />
        <MetricTile :value="dashboard.summary.passCount" label="通过" accent="success" />
        <MetricTile :value="dashboard.summary.warningCount" label="警告" accent="warning" />
        <MetricTile :value="dashboard.summary.blockingFailures" label="阻塞失败" accent="danger" />
        <MetricTile :value="dashboard.summary.overallStatus" label="整体状态" :accent="overallStatusTag(dashboard.summary.overallStatus)" />
      </div>

      <!-- Gate Evaluations -->
      <div style="margin-top:16px">
        <div style="font-size:12px;font-weight:700;color:var(--app-text-soft);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.05em">门禁评估结果</div>
        <div v-if="!dashboard || !dashboard.evaluations || dashboard.evaluations.length === 0" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">暂无评估结果，请先执行门禁评估</div>
        <div v-for="e in dashboard?.evaluations || []" :key="e.id" class="gate-eval-item">
          <div class="gate-eval-header">
            <ElTag :type="gateStatusTag(e.gateStatus)" size="small" effect="dark">{{ e.gateStatus }}</ElTag>
            <ElTag :type="categoryTag(e.category)" size="small">{{ e.category }}</ElTag>
            <ElTag v-if="e.blocking === 1" size="small" type="danger" effect="plain">阻塞</ElTag>
            <span class="gate-eval-summary">{{ e.summary }}</span>
          </div>
          <div class="gate-eval-meta">
            <span>规则: {{ e.ruleKey }}</span>
            <span v-if="e.actualValue != null">实际值: {{ e.actualValue }}</span>
            <span v-if="e.thresholdValue != null">阈值: {{ e.thresholdValue }}</span>
          </div>
        </div>
      </div>

      <!-- Gate Rules -->
      <div style="margin-top:16px">
        <div style="font-size:12px;font-weight:700;color:var(--app-text-soft);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.05em">门禁规则配置</div>
        <div v-if="rules.length === 0" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">暂无规则</div>
        <div v-for="r in rules" :key="r.id" class="gate-rule-item">
          <div class="gate-rule-header">
            <ElTag :type="r.blocking === 1 ? 'danger' : 'warning'" size="small" effect="plain">
              {{ r.blocking === 1 ? '阻塞' : '警告' }}
            </ElTag>
            <span class="gate-rule-name">{{ r.displayName }}</span>
            <ElTag size="small" :type="r.enabled === 1 ? 'success' : 'info'">
              {{ r.enabled === 1 ? '启用' : '禁用' }}
            </ElTag>
          </div>
          <div class="gate-rule-meta">
            <span>{{ r.ruleKey }}</span>
            <span>{{ r.thresholdOperator }} {{ r.thresholdValue }}</span>
          </div>
          <ElButton size="small" link type="primary" @click="openRuleDialog(r)">编辑</ElButton>
        </div>
      </div>

      <!-- Recent Decisions -->
      <div style="margin-top:16px" v-if="dashboard && dashboard.recentDecisions && dashboard.recentDecisions.length > 0">
        <div style="font-size:12px;font-weight:700;color:var(--app-text-soft);margin-bottom:8px;text-transform:uppercase;letter-spacing:0.05em">最近决策</div>
        <div v-for="d in dashboard.recentDecisions" :key="d.id" class="gate-decision-item">
          <ElTag :type="gateStatusTag(d.decisionStatus)" size="small" effect="dark">{{ d.decisionStatus }}</ElTag>
          <span class="gate-decision-label">{{ d.releaseLabel }}</span>
          <span class="gate-decision-meta">{{ d.blockingIssueCount }} 阻塞 · {{ d.warningIssueCount }} 警告</span>
        </div>
      </div>
    </template>

    <!-- Rule Edit Dialog -->
    <ElDialog v-model="ruleDialogVisible" title="编辑门禁规则" width="400px" data-testid="gate-rule-edit-dialog">
      <div v-if="editingRule" class="rule-edit-form">
        <div class="rule-edit-field">
          <label>规则名称</label>
          <div class="rule-edit-value">{{ editingRule.displayName }}</div>
        </div>
        <div class="rule-edit-field">
          <label>启用</label>
          <ElSwitch v-model="editEnabled" />
        </div>
        <div class="rule-edit-field">
          <label>阻塞</label>
          <ElSwitch v-model="editBlocking" />
        </div>
        <div class="rule-edit-field">
          <label>阈值</label>
          <ElInputNumber v-model="editThreshold" :min="0" :step="0.01" style="width:100%" />
        </div>
      </div>
      <template #footer>
        <ElButton @click="ruleDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSaveRule">保存</ElButton>
      </template>
    </ElDialog>
  </TechPanel>
</template>

<style scoped>
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 8px;
}
.gate-eval-item {
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 6px;
}
.gate-eval-header {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.gate-eval-summary {
  font-size: 12px;
  color: var(--app-text-soft);
  flex: 1;
}
.gate-eval-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--app-text-muted);
  margin-top: 4px;
}
.gate-rule-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  margin-bottom: 4px;
}
.gate-rule-header {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}
.gate-rule-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-bright);
}
.gate-rule-meta {
  font-size: 11px;
  color: var(--app-text-muted);
  display: flex;
  gap: 8px;
}
.gate-decision-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(15, 23, 42, 0.3);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  margin-bottom: 4px;
}
.gate-decision-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-bright);
  flex: 1;
}
.gate-decision-meta {
  font-size: 11px;
  color: var(--app-text-muted);
}
.rule-edit-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.rule-edit-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.rule-edit-field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-soft);
}
.rule-edit-value {
  font-size: 13px;
  color: var(--app-text-bright);
  padding: 4px 0;
}
</style>
