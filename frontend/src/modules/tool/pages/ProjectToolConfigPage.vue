<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  listProjectTools,
  enableProjectTool,
  disableProjectTool,
  type ProjectToolConfig,
} from '@/modules/tool/api'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import ToolParameterForm from '@/modules/tool/components/ToolParameterForm.vue'

const route = useRoute()
const projectId = route.params.projectId as string

const tools = ref<ProjectToolConfig[]>([])
const loading = ref(false)
const error = ref(false)
const actionLoading = ref<Record<string, boolean>>({})
const actionError = ref('')

// Parameter dialog state
const showParamDialog = ref(false)
const editingTool = ref<ProjectToolConfig | null>(null)
const editingParams = ref<Record<string, unknown>>({})
const savingParams = ref(false)
const paramSaveError = ref('')

onMounted(async () => {
  await loadTools()
})

async function loadTools() {
  loading.value = true
  error.value = false
  try {
    const res = await listProjectTools(projectId)
    tools.value = res.data.data
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function riskTagType(level: string): 'success' | 'warning' | 'danger' | 'info' {
  switch (level) {
    case 'LOW': return 'success'
    case 'MEDIUM': return 'warning'
    case 'HIGH': return 'danger'
    case 'DANGEROUS': return 'danger'
    default: return 'info'
  }
}

function riskLabel(level: string): string {
  switch (level) {
    case 'LOW': return '低'
    case 'MEDIUM': return '中'
    case 'HIGH': return '高'
    case 'DANGEROUS': return '危险'
    default: return level
  }
}

function typeLabel(type: string): string {
  switch (type) {
    case 'READ_ONLY': return '只读'
    case 'ANALYSIS': return '分析'
    case 'MOCK': return '模拟'
    default: return type
  }
}

async function handleEnable(tool: ProjectToolConfig) {
  actionLoading.value[tool.toolId] = true
  actionError.value = ''
  try {
    await enableProjectTool(projectId, tool.toolId, { parameters: {} })
    ElMessage.success(`已启用「${tool.name}」`)
    await loadTools()
  } catch {
    actionError.value = '操作失败：权限不足或服务异常'
  } finally {
    actionLoading.value[tool.toolId] = false
  }
}

async function handleDisable(tool: ProjectToolConfig) {
  actionLoading.value[tool.toolId] = true
  actionError.value = ''
  try {
    await disableProjectTool(projectId, tool.toolId)
    ElMessage.success(`已停用「${tool.name}」`)
    await loadTools()
  } catch {
    actionError.value = '操作失败：权限不足或服务异常'
  } finally {
    actionLoading.value[tool.toolId] = false
  }
}

// Parameter dialog
function openParamDialog(tool: ProjectToolConfig) {
  editingTool.value = tool
  paramSaveError.value = ''
  // Parse existing parameters or initialize empty
  if (tool.parametersJson) {
    try {
      editingParams.value = JSON.parse(tool.parametersJson)
    } catch {
      editingParams.value = {}
    }
  } else {
    editingParams.value = {}
  }
  showParamDialog.value = true
}

function closeParamDialog() {
  showParamDialog.value = false
  editingTool.value = null
  editingParams.value = {}
}

async function handleSaveParameters() {
  if (!editingTool.value) return
  savingParams.value = true
  paramSaveError.value = ''
  try {
    await enableProjectTool(projectId, editingTool.value.toolId, {
      parameters: editingParams.value as Record<string, unknown>,
    })
    ElMessage.success(`参数已保存`)
    await loadTools()
    closeParamDialog()
  } catch (e: any) {
    paramSaveError.value = e?.response?.data?.message || '参数保存失败'
  } finally {
    savingParams.value = false
  }
}

// Parameter summary
function getParamSummary(tool: ProjectToolConfig): string {
  if (!tool.parametersJson) return ''
  try {
    const params = JSON.parse(tool.parametersJson)
    const parts: string[] = []
    for (const [key, val] of Object.entries(params)) {
      if (Array.isArray(val)) {
        parts.push(`${key}=${val.length} 项`)
      } else {
        parts.push(`${key}=${val}`)
      }
    }
    return parts.join(', ')
  } catch {
    return ''
  }
}
</script>

<template>
  <div data-testid="project-tool-page" class="tool-page">
    <div class="tool-page-header">
      <h3 class="tool-page-title">工具策略</h3>
      <p class="tool-page-desc">
        工具策略控制 Agent 可调用的安全工具。本阶段仅支持 Mock / Read-only 工具，不执行真实 Shell 或 Git 写操作。
      </p>
    </div>

    <div
      v-if="actionError"
      data-testid="tool-policy-error"
      class="tool-error-banner"
    >
      {{ actionError }}
    </div>

    <div v-loading="loading" class="tool-content">
      <ErrorState v-if="error" @retry="loadTools" />

      <div v-else-if="tools.length === 0 && !loading" class="tool-empty-wrap">
        <EmptyState message="暂无工具配置" />
      </div>

      <table v-else data-testid="project-tool-table" class="tool-table">
        <thead>
          <tr>
            <th>工具名称</th>
            <th>toolKey</th>
            <th>类型</th>
            <th>风险等级</th>
            <th>执行模式</th>
            <th>全局状态</th>
            <th>项目启用</th>
            <th>参数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="tool in tools"
            :key="tool.toolId"
            data-testid="project-tool-row"
            class="tool-row"
          >
            <td>
              <span class="tool-name">{{ tool.name }}</span>
            </td>
            <td>
              <code class="tool-key">{{ tool.toolKey }}</code>
            </td>
            <td>
              <span class="tool-type-badge">{{ typeLabel(tool.toolType) }}</span>
            </td>
            <td>
              <el-tag
                :type="riskTagType(tool.riskLevel)"
                size="small"
                effect="dark"
              >
                {{ riskLabel(tool.riskLevel) }}
              </el-tag>
            </td>
            <td>
              <span class="tool-mode">{{ tool.executionMode }}</span>
            </td>
            <td>
              <span
                class="tool-status-dot"
                :class="tool.globalEnabled ? 'dot-on' : 'dot-off'"
              />
              {{ tool.globalEnabled ? '启用' : '禁用' }}
            </td>
            <td>
              <span
                class="tool-status-dot"
                :class="tool.projectEnabled ? 'dot-on' : 'dot-off'"
              />
              {{ tool.projectEnabled ? '启用' : '未启用' }}
            </td>
            <td>
              <div class="tool-param-cell">
                <span
                  v-if="tool.parameterSchemaJson"
                  class="tool-param-summary"
                  data-testid="tool-parameter-summary"
                >
                  {{ getParamSummary(tool) || '默认参数' }}
                </span>
                <span v-else class="tool-param-na">-</span>
              </div>
            </td>
            <td>
              <div class="tool-actions">
                <template v-if="tool.riskLevel === 'DANGEROUS'">
                  <el-tooltip content="当前阶段不允许启用 DANGEROUS 工具" placement="top">
                    <el-button
                      size="small"
                      type="info"
                      plain
                      disabled
                    >
                      当前阶段不允许启用
                    </el-button>
                  </el-tooltip>
                </template>
                <template v-else>
                  <el-button
                    v-if="!tool.projectEnabled"
                    data-testid="btn-tool-enable"
                    size="small"
                    type="primary"
                    plain
                    :loading="actionLoading[tool.toolId]"
                    @click="handleEnable(tool)"
                  >
                    启用
                  </el-button>
                  <el-button
                    v-else
                    data-testid="btn-tool-disable"
                    size="small"
                    type="danger"
                    plain
                    :loading="actionLoading[tool.toolId]"
                    @click="handleDisable(tool)"
                  >
                    停用
                  </el-button>
                  <el-button
                    v-if="tool.parameterSchemaJson"
                    size="small"
                    type="info"
                    plain
                    data-testid="btn-tool-configure"
                    @click="openParamDialog(tool)"
                  >
                    配置
                  </el-button>
                </template>
                <span v-if="tool.riskLevel === 'HIGH' && tool.projectEnabled" class="tool-approval-hint" data-testid="tool-approval-hint">
                  需要审批
                </span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Parameter Dialog -->
    <el-dialog
      v-if="showParamDialog && editingTool"
      v-model="showParamDialog"
      :title="'参数配置 - ' + editingTool.name"
      width="480px"
      data-testid="tool-parameter-dialog"
      destroy-on-close
    >
      <div class="param-dialog-body">
        <div class="param-dialog-meta">
          <code>{{ editingTool.toolKey }}</code>
          <el-tag
            size="small"
            :type="riskTagType(editingTool.riskLevel)"
            effect="dark"
          >
            {{ riskLabel(editingTool.riskLevel) }}
          </el-tag>
        </div>
        <div v-if="paramSaveError" class="param-dialog-error">
          {{ paramSaveError }}
        </div>
        <ToolParameterForm
          v-if="editingTool.parameterSchemaJson"
          :schema-json="editingTool.parameterSchemaJson"
          :model-value="editingParams"
          @update:model-value="editingParams = $event"
        />
      </div>
      <template #footer>
        <el-button size="small" @click="closeParamDialog">取消</el-button>
        <el-button
          size="small"
          type="primary"
          :loading="savingParams"
          data-testid="btn-save-tool-parameters"
          @click="handleSaveParameters"
        >
          保存参数
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tool-page {
  padding: 0;
  color: var(--app-text);
}

.tool-page-header {
  margin-bottom: 20px;
}

.tool-page-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--app-text);
}

.tool-page-desc {
  font-size: 12px;
  color: var(--app-text-muted);
  margin: 0;
  line-height: 1.6;
}

.tool-error-banner {
  background: rgba(245, 108, 108, 0.08);
  border: 1px solid rgba(245, 108, 108, 0.2);
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 16px;
  font-size: 12px;
  color: var(--el-color-danger, #f56c6c);
}

.tool-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.tool-table th {
  text-align: left;
  padding: 10px 12px;
  font-size: 11px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--app-border, rgba(255, 255, 255, 0.06));
}

.tool-table td {
  padding: 12px;
  border-bottom: 1px solid var(--app-border, rgba(255, 255, 255, 0.04));
  vertical-align: middle;
}

.tool-row:hover {
  background: rgba(64, 158, 255, 0.03);
}

.tool-name {
  font-weight: 500;
  color: var(--app-text);
}

.tool-key {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 11px;
  color: var(--app-text-muted);
  background: rgba(255, 255, 255, 0.04);
  padding: 2px 6px;
  border-radius: 4px;
}

.tool-type-badge {
  font-size: 11px;
  color: var(--app-text-muted);
  background: rgba(255, 255, 255, 0.04);
  padding: 2px 8px;
  border-radius: 4px;
}

.tool-mode {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 11px;
  color: var(--app-text-muted);
}

.tool-status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.dot-on {
  background: var(--el-color-success, #67c23a);
  box-shadow: 0 0 4px rgba(103, 194, 58, 0.4);
}

.dot-off {
  background: var(--app-text-muted, #888);
}

.tool-desc {
  font-size: 11px;
  color: var(--app-text-muted);
  max-width: 200px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-actions {
  display: flex;
  gap: 6px;
  white-space: nowrap;
  align-items: center;
}

.tool-approval-hint {
  font-size: 10px;
  color: var(--el-color-warning, #e6a23c);
  background: rgba(230,162,60,0.12);
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
}

.tool-content {
  min-height: 200px;
}

.tool-empty-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

/* Parameter cell */
.tool-param-cell {
  max-width: 200px;
}

.tool-param-summary {
  font-size: 11px;
  color: var(--app-text-soft);
  font-family: monospace;
  word-break: break-all;
}

.tool-param-na {
  font-size: 11px;
  color: var(--app-text-muted);
}

/* Parameter dialog */
.param-dialog-body {
  min-height: 100px;
}

.param-dialog-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 16px;
  font-size: 12px;
}

.param-dialog-error {
  background: rgba(245, 108, 108, 0.08);
  border: 1px solid rgba(245, 108, 108, 0.2);
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--el-color-danger, #f56c6c);
}
</style>
