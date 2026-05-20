<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listWorkflowTemplates,
  updateWorkflowTemplateStatus,
  type WorkflowTemplateResponse,
} from '@/modules/workflow/api'
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import TechPanel from '@/shared/components/TechPanel.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { formatDateTime } from '@/shared/utils/format'

const templates = ref<WorkflowTemplateResponse[]>([])
const loading = ref(false)
const statusFilter = ref('')
const detailDrawer = ref(false)
const detailTemplate = ref<WorkflowTemplateResponse | null>(null)
const toggling = ref<Record<string, boolean>>({})

const filteredTemplates = computed(() => {
  if (!statusFilter.value) return templates.value
  return templates.value.filter(t => t.status === statusFilter.value)
})

onMounted(() => {
  loadTemplates()
})

async function loadTemplates() {
  loading.value = true
  try {
    const res = await listWorkflowTemplates(statusFilter.value || undefined)
    templates.value = res.data.data
  } catch {
    ElMessage.error('加载模板列表失败')
  } finally {
    loading.value = false
  }
}

function handleFilterChange(val: string) {
  statusFilter.value = val
  loadTemplates()
}

function showDetail(template: WorkflowTemplateResponse) {
  detailTemplate.value = template
  detailDrawer.value = true
}

async function toggleStatus(template: WorkflowTemplateResponse) {
  const newStatus = template.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  toggling.value[template.id] = true
  try {
    await updateWorkflowTemplateStatus(template.id, newStatus)
    template.status = newStatus
    ElMessage.success(newStatus === 'ENABLED' ? '模板已启用' : '模板已停用')
  } catch {
    ElMessage.error('操作失败')
  } finally {
    toggling.value[template.id] = false
  }
}

function statusTone(status: string): 'success' | 'muted' {
  return status === 'ENABLED' ? 'success' : 'muted'
}

function statusText(status: string) {
  return status === 'ENABLED' ? '启用' : '停用'
}

function parseApprovalGates(templateJson: string): Array<{ gateKey: string; title: string; description: string; afterPhaseOrder: number }> {
  try {
    const parsed = JSON.parse(templateJson)
    return parsed.approvalGates || []
  } catch {
    return []
  }
}

function formatJson(raw: string): string {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}
</script>

<template>
  <DynamicWorkspace title="工作流模板">
    <div class="wftp-root">
      <!-- Header -->
      <div class="wftp-header">
        <div>
          <h1 class="wftp-title">工作流模板</h1>
          <p class="wftp-subtitle">管理多智能体编排策略，启用或停用模板以控制 Multi-Agent Run 可用选项。</p>
        </div>
      </div>

      <NeonDivider />

      <!-- Filter -->
      <div class="wftp-filters">
        <span class="wftp-filter-label">状态筛选</span>
        <button
          class="wftp-filter-chip"
          :class="{ active: statusFilter === '' }"
          @click="handleFilterChange('')"
        >
          全部
        </button>
        <button
          class="wftp-filter-chip"
          :class="{ active: statusFilter === 'ENABLED' }"
          @click="handleFilterChange('ENABLED')"
        >
          启用
        </button>
        <button
          class="wftp-filter-chip"
          :class="{ active: statusFilter === 'DISABLED' }"
          @click="handleFilterChange('DISABLED')"
        >
          停用
        </button>
      </div>

      <!-- Table -->
      <TechPanel data-testid="workflow-template-table">
        <table class="wftp-table">
          <thead>
            <tr>
              <th>模板名称</th>
              <th>Key</th>
              <th>状态</th>
              <th>内置</th>
              <th>阶段</th>
              <th>步骤</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredTemplates.length === 0 && !loading">
              <td colspan="8" style="text-align: center; padding: 32px 0;">
                <EmptyState message="暂无模板数据" />
              </td>
            </tr>
            <tr
              v-for="tpl in filteredTemplates"
              :key="tpl.id"
              :data-testid="`template-row-${tpl.templateKey}`"
            >
              <td>
                <span class="wftp-tpl-name">{{ tpl.name }}</span>
              </td>
              <td>
                <code class="wftp-tpl-key">{{ tpl.templateKey }}</code>
              </td>
              <td>
                <StatusPulse :status="statusText(tpl.status)" :tone="statusTone(tpl.status)">
                  {{ statusText(tpl.status) }}
                </StatusPulse>
              </td>
              <td>
                <span v-if="tpl.builtIn" class="wftp-badge-builtin">内置</span>
                <span v-else class="wftp-badge-custom">自定义</span>
              </td>
              <td>{{ tpl.phaseCount ?? tpl.strategy?.phaseCount ?? '-' }}</td>
              <td>{{ tpl.stepCount ?? tpl.strategy?.stepCount ?? '-' }}</td>
              <td class="wftp-time">{{ formatDateTime(tpl.updateTime || '') }}</td>
              <td>
                <div class="wftp-actions">
                  <GlowButton size="small" accent="accent" :data-testid="`btn-view-template-${tpl.templateKey}`" @click="showDetail(tpl)">
                    详情
                  </GlowButton>
                  <GlowButton
                    size="small"
                    :accent="tpl.status === 'ENABLED' ? 'warning' : 'success'"
                    :loading="toggling[tpl.id]"
                    :data-testid="`btn-toggle-template-${tpl.templateKey}`"
                    @click="toggleStatus(tpl)"
                  >
                    {{ tpl.status === 'ENABLED' ? '停用' : '启用' }}
                  </GlowButton>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </TechPanel>
    </div>

    <!-- Detail Drawer -->
    <el-drawer
      v-model="detailDrawer"
      title="模板详情"
      size="560px"
      :data-testid="'template-detail-drawer'"
    >
      <template v-if="detailTemplate">
        <div class="wftp-detail">
          <!-- Basic info -->
          <div class="wftp-detail-section">
            <h4 class="wftp-detail-head">基本信息</h4>
            <dl class="wftp-detail-dl">
              <dt>名称</dt><dd>{{ detailTemplate.name }}</dd>
              <dt>Key</dt><dd><code>{{ detailTemplate.templateKey }}</code></dd>
              <dt>描述</dt><dd>{{ detailTemplate.description || '-' }}</dd>
              <dt>状态</dt><dd><StatusPulse :status="statusText(detailTemplate.status)" :tone="statusTone(detailTemplate.status)">{{ statusText(detailTemplate.status) }}</StatusPulse></dd>
              <dt>类型</dt><dd>{{ detailTemplate.builtIn ? '内置模板' : '自定义模板' }}</dd>
              <dt>分类</dt><dd>{{ detailTemplate.category }}</dd>
              <dt>阶段数</dt><dd>{{ detailTemplate.phaseCount ?? '-' }}</dd>
              <dt>步骤数</dt><dd>{{ detailTemplate.stepCount ?? '-' }}</dd>
              <dt>创建时间</dt><dd>{{ formatDateTime(detailTemplate.createTime || '') }}</dd>
              <dt>更新时间</dt><dd>{{ formatDateTime(detailTemplate.updateTime || '') }}</dd>
            </dl>
          </div>

          <!-- Strategy Overview -->
          <NeonDivider />
          <div class="wftp-detail-section" v-if="detailTemplate.strategy">
            <h4 class="wftp-detail-head">策略概览</h4>
            <div class="wftp-detail-block">
              <p><strong>策略 Key:</strong> {{ detailTemplate.strategy.strategyKey }}</p>
              <p><strong>名称:</strong> {{ detailTemplate.strategy.name }}</p>
              <p><strong>描述:</strong> {{ detailTemplate.strategy.description }}</p>
              <p><strong>阶段:</strong> {{ detailTemplate.strategy.phaseCount }} / <strong>步骤:</strong> {{ detailTemplate.strategy.stepCount }}</p>
            </div>

            <!-- Phases & Steps -->
            <h4 class="wftp-detail-head" style="margin-top: 16px;">阶段与步骤</h4>
            <div
              v-for="phase in detailTemplate.strategy.phases"
              :key="phase.phaseKey"
              :data-testid="`detail-phase-${phase.phaseKey}`"
              class="wftp-detail-phase"
            >
              <div class="wftp-detail-phase-header">
                <span class="wftp-phase-badge">Phase {{ phase.phaseOrder }}</span>
                <span class="wftp-phase-key">{{ phase.phaseKey }}</span>
                <span class="wftp-phase-title">{{ phase.title }}</span>
              </div>
              <ul class="wftp-detail-steps">
                <li v-for="step in phase.steps" :key="step.agentCode + step.stepOrder" class="wftp-detail-step">
                  <code>{{ step.stepType }}</code>
                  <span class="wftp-step-meta">{{ step.agentCode }} / {{ step.laneKey }}</span>
                  <span>{{ step.title }}</span>
                </li>
              </ul>
            </div>
          </div>

          <!-- Approval Gates -->
          <NeonDivider />
          <div class="wftp-detail-section">
            <h4 class="wftp-detail-head">审批闸门</h4>
            <div v-if="detailTemplate.strategy?.phases">
              <!-- Parse gates from templateJson for display -->
              <template v-if="parseApprovalGates(detailTemplate.templateJson).length > 0">
                <div
                  v-for="gate in parseApprovalGates(detailTemplate.templateJson)"
                  :key="gate.gateKey"
                  class="wftp-detail-gate"
                >
                  <div class="wftp-detail-gate-header">
                    <span class="wftp-gate-key">{{ gate.gateKey }}</span>
                    <span class="wftp-gate-after">After Phase {{ gate.afterPhaseOrder }}</span>
                  </div>
                  <p class="wftp-gate-title">{{ gate.title }}</p>
                  <p class="wftp-gate-desc">{{ gate.description }}</p>
                </div>
              </template>
              <p v-else class="wftp-detail-muted">无审批闸门</p>
            </div>
          </div>

          <!-- Raw JSON -->
          <NeonDivider />
          <div class="wftp-detail-section">
            <h4 class="wftp-detail-head">Raw JSON</h4>
            <pre class="wftp-json">{{ formatJson(detailTemplate.templateJson) }}</pre>
          </div>
        </div>
      </template>
    </el-drawer>
  </DynamicWorkspace>
</template>

<style scoped>
.wftp-root {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 20px 100px;
}

.wftp-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.wftp-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--app-text-soft);
}

.wftp-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--app-text-muted);
}

.wftp-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0;
}

.wftp-filter-label {
  font-size: 12px;
  color: var(--app-text-muted);
  margin-right: 4px;
}

.wftp-filter-chip {
  padding: 4px 14px;
  border: 1px solid var(--app-border-strong);
  border-radius: 12px;
  background: var(--app-panel);
  color: var(--app-text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s var(--app-ease-out);
}

.wftp-filter-chip:hover {
  border-color: var(--app-primary);
  color: var(--app-text-soft);
}

.wftp-filter-chip.active {
  background: var(--app-primary-soft);
  border-color: var(--app-primary);
  color: var(--app-primary);
  box-shadow: 0 0 10px var(--app-primary-glow);
}

.wftp-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.wftp-table thead th {
  text-align: left;
  padding: 10px 12px;
  color: var(--app-text-muted);
  font-weight: 500;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  border-bottom: 1px solid var(--app-border-strong);
}

.wftp-table tbody td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--app-border);
  color: var(--app-text-soft);
  vertical-align: middle;
}

.wftp-table tbody tr:hover td {
  background: var(--app-panel-hover);
}

.wftp-tpl-name {
  font-weight: 500;
}

.wftp-tpl-key {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--app-panel-hover);
  border: 1px solid var(--app-border);
}

.wftp-time {
  font-size: 11px;
  color: var(--app-text-muted);
  white-space: nowrap;
}

.wftp-actions {
  display: flex;
  gap: 6px;
}

.wftp-badge-builtin,
.wftp-badge-custom {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 8px;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.wftp-badge-builtin {
  background: rgba(56, 189, 248, 0.08);
  color: var(--app-primary);
  border: 1px solid rgba(56, 189, 248, 0.2);
}

.wftp-badge-custom {
  background: rgba(168, 85, 247, 0.08);
  color: #a855f7;
  border: 1px solid rgba(168, 85, 247, 0.2);
}

/* Detail Drawer */
.wftp-detail {
  padding: 0 8px;
}

.wftp-detail-section {
  margin-bottom: 8px;
}

.wftp-detail-head {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text-soft);
}

.wftp-detail-dl {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 6px 12px;
  font-size: 13px;
}

.wftp-detail-dl dt {
  color: var(--app-text-muted);
  text-align: right;
  font-weight: 500;
}

.wftp-detail-dl dd {
  color: var(--app-text-soft);
  margin: 0;
  display: flex;
  align-items: center;
}

.wftp-detail-block {
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--app-panel-hover);
  border: 1px solid var(--app-border);
  font-size: 12px;
  line-height: 1.8;
}

.wftp-detail-block p {
  margin: 2px 0;
}

.wftp-detail-phase {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--app-panel);
  border: 1px solid var(--app-border);
}

.wftp-detail-phase-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.wftp-phase-badge {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--app-primary-soft);
  color: var(--app-primary);
  font-weight: 600;
}

.wftp-phase-key {
  font-size: 11px;
  color: var(--app-text-muted);
}

.wftp-phase-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--app-text-soft);
}

.wftp-detail-steps {
  list-style: none;
  padding: 0;
  margin: 0;
}

.wftp-detail-step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0 4px 12px;
  border-left: 2px solid var(--app-border);
  margin-left: 4px;
  font-size: 12px;
}

.wftp-detail-step code {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  background: var(--app-panel-hover);
}

.wftp-step-meta {
  font-size: 10px;
  color: var(--app-text-muted);
}

.wftp-detail-gate {
  margin-bottom: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(250, 204, 21, 0.06);
  border: 1px solid rgba(250, 204, 21, 0.2);
}

.wftp-detail-gate-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.wftp-gate-key {
  font-size: 11px;
  font-weight: 600;
  color: var(--app-warning);
}

.wftp-gate-after {
  font-size: 10px;
  color: var(--app-text-muted);
  background: var(--app-panel);
  padding: 2px 6px;
  border-radius: 4px;
}

.wftp-gate-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--app-text-soft);
  margin: 2px 0;
}

.wftp-gate-desc {
  font-size: 11px;
  color: var(--app-text-muted);
  margin: 0;
}

.wftp-detail-muted {
  font-size: 12px;
  color: var(--app-text-muted);
  font-style: italic;
}

.wftp-json {
  padding: 12px;
  border-radius: 8px;
  background: var(--app-panel-hover);
  border: 1px solid var(--app-border);
  font-size: 11px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  color: var(--app-text-soft);
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
  overflow-y: auto;
}
</style>
