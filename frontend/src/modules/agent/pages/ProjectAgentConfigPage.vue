<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  listProjectAgents,
  enableProjectAgent,
  disableProjectAgent,
  getAgentVersions,
  defaultRuntimeConfig,
  runtimeConfigSummary,
  type ProjectAgentConfig,
  type ProjectAgentRuntimeConfig,
  type AgentVersion,
} from '@/modules/agent/api'
import { getModelConfigs, type ModelConfigItem } from '@/modules/model/api'
import { listKnowledgeBases, type KnowledgeBaseItem } from '@/modules/knowledge/api'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import { formatDateTime } from '@/shared/utils/format'

const route = useRoute()
const projectId = route.params.projectId as string

const agents = ref<ProjectAgentConfig[]>([])
const loading = ref(false)
const actionLoading = ref<Record<string, boolean>>({})
const error = ref(false)
const actionError = ref('')

// Dialog state
const dialogVisible = ref(false)
const dialogAgent = ref<ProjectAgentConfig | null>(null)
const dialogVersionId = ref('')
const dialogModelConfigId = ref('')
const dialogConfig = ref<ProjectAgentRuntimeConfig>(defaultRuntimeConfig())
const dialogLoading = ref(false)

const isEditing = computed(() => dialogAgent.value?.enabled === true)

// Model config options
const modelConfigs = ref<ModelConfigItem[]>([])
const modelConfigsLoading = ref(false)

// Agent version options for enable dialog
const publishedVersions = ref<AgentVersion[]>([])
const versionsLoading = ref(false)

// Knowledge base options
const knowledgeBases = ref<KnowledgeBaseItem[]>([])
const knowledgeBasesLoading = ref(false)

onMounted(async () => {
  await loadAgents()
})

async function loadAgents() {
  loading.value = true
  error.value = false
  try {
    const res = await listProjectAgents(projectId)
    agents.value = res.data.data
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

async function loadModelConfigs() {
  if (modelConfigs.value.length > 0) return
  modelConfigsLoading.value = true
  try {
    const res = await getModelConfigs()
    modelConfigs.value = (res.data.data || []).filter((m) => m.status === 'ENABLED')
  } catch {
    modelConfigs.value = []
  } finally {
    modelConfigsLoading.value = false
  }
}

async function loadKnowledgeBases() {
  if (knowledgeBases.value.length > 0) return
  knowledgeBasesLoading.value = true
  try {
    const res = await listKnowledgeBases(projectId, 1, 100)
    knowledgeBases.value = res.data.data?.records || res.data.data || []
  } catch {
    knowledgeBases.value = []
  } finally {
    knowledgeBasesLoading.value = false
  }
}

function initRuntimeConfig(agent: ProjectAgentConfig): ProjectAgentRuntimeConfig {
  if (agent.config) {
    return {
      temperature: agent.config.temperature ?? 0.2,
      maxTokens: agent.config.maxTokens ?? 4096,
      timeoutSeconds: agent.config.timeoutSeconds ?? 60,
      useRag: agent.config.useRag ?? false,
      knowledgeBaseId: agent.config.knowledgeBaseId ?? null,
      customInstruction: agent.config.customInstruction ?? '',
    }
  }
  return defaultRuntimeConfig()
}

async function openEnableDialog(agent: ProjectAgentConfig) {
  actionError.value = ''
  dialogAgent.value = agent
  dialogModelConfigId.value = agent.modelConfigId || ''
  dialogConfig.value = initRuntimeConfig(agent)
  dialogVisible.value = true
  loadModelConfigs()
  loadKnowledgeBases()

  // Load all published versions for this agent
  publishedVersions.value = []
  versionsLoading.value = true
  try {
    const res = await getAgentVersions(agent.agentId)
    publishedVersions.value = (res.data.data || []).filter((v) => v.status === 'PUBLISHED')
  } catch {
    publishedVersions.value = []
  } finally {
    versionsLoading.value = false
  }

  // Default select: current version, or first published, or empty
  if (agent.agentVersionId && publishedVersions.value.some((v) => v.id === agent.agentVersionId)) {
    dialogVersionId.value = agent.agentVersionId
  } else if (publishedVersions.value.length > 0) {
    dialogVersionId.value = publishedVersions.value[0].id
  } else {
    dialogVersionId.value = ''
  }
}

async function handleConfirmEnable() {
  if (!dialogAgent.value) return
  actionError.value = ''
  dialogLoading.value = true
  const agentId = dialogAgent.value.agentId
  try {
    await enableProjectAgent(projectId, agentId, {
      agentVersionId: dialogVersionId.value || undefined,
      modelConfigId: dialogModelConfigId.value || undefined,
      config: dialogConfig.value,
    })
    dialogVisible.value = false
    await loadAgents()
  } catch (e: any) {
    actionError.value = e?.response?.data?.message || '保存失败'
  } finally {
    dialogLoading.value = false
  }
}

async function handleDisable(agentId: string) {
  actionError.value = ''
  actionLoading.value[agentId] = true
  try {
    await disableProjectAgent(projectId, agentId)
    await loadAgents()
  } catch (e: any) {
    actionError.value = e?.response?.data?.message || '停用失败'
  } finally {
    actionLoading.value[agentId] = false
  }
}

function statusTone(status: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'primary' | 'muted'> = {
    ACTIVE: 'success', ENABLED: 'success', DISABLED: 'muted', ERROR: 'danger',
  }
  return map[status] || 'muted'
}

function modelConfigLabel(mc: ModelConfigItem): string {
  return `${mc.provider} / ${mc.modelName}`
}
</script>

<template>
  <div v-loading="loading">
    <div v-if="actionError" style="margin-bottom: 12px">
      <el-alert data-testid="agent-action-error" :title="actionError" type="error" :closable="true" @close="actionError = ''" />
    </div>

    <template v-if="!loading && !error">
      <el-table v-if="agents.length > 0" :data="agents" style="width:100%" data-testid="project-agent-table">
        <el-table-column label="智能体" min-width="140">
          <template #default="{ row }">
            <div class="pa-agent-cell">
              <div class="pa-agent-name">{{ row.agentName }}</div>
              <div class="pa-agent-code">{{ row.agentCode }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="agentType" label="类型" width="90" />
        <el-table-column label="全局状态" width="100">
          <template #default="{ row }">
            <StatusPulse :status="row.agentStatus" :tone="statusTone(row.agentStatus)" />
          </template>
        </el-table-column>
        <el-table-column label="项目启用" width="100">
          <template #default="{ row }">
            <StatusPulse
              :status="row.enabled ? 'ENABLED' : 'DISABLED'"
              :tone="row.enabled ? 'success' : 'muted'"
            />
          </template>
        </el-table-column>
        <el-table-column label="版本" width="90">
          <template #default="{ row }">
            <span v-if="row.agentVersionNo" class="pa-version">{{ row.agentVersionNo }}</span>
            <span v-else style="color:var(--app-text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="运行配置" min-width="160">
          <template #default="{ row }">
            <span
              v-if="row.config"
              class="pa-runtime-summary"
              data-testid="agent-runtime-summary"
            >{{ runtimeConfigSummary(row.config) }}</span>
            <span v-else-if="row.enabled" style="color:var(--app-text-muted);font-size:12px">默认</span>
            <span v-else style="color:var(--app-text-muted);font-size:12px">-</span>
          </template>
        </el-table-column>
        <el-table-column label="模型配置" min-width="140">
          <template #default="{ row }">
            <span v-if="row.modelProvider && row.modelName" style="font-size:12px;color:var(--app-text-soft)">{{ row.modelProvider }} / {{ row.modelName }}</span>
            <span v-else-if="row.modelConfigId" style="font-family:monospace;font-size:12px;color:var(--app-text-soft)">{{ row.modelConfigId }}</span>
            <span v-else style="color:var(--app-text-muted);font-size:12px">默认</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="150">
          <template #default="{ row }">
            <span v-if="row.updateTime">{{ formatDateTime(row.updateTime) }}</span>
            <span v-else style="color:var(--app-text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <template v-if="!row.enabled">
              <el-button
                data-testid="btn-agent-enable"
                type="primary"
                size="small"
                :loading="actionLoading[row.agentId]"
                :disabled="row.agentStatus === 'DISABLED'"
                @click="openEnableDialog(row)"
              >
                启用
              </el-button>
            </template>
            <template v-else>
              <el-button
                data-testid="btn-agent-configure"
                type="primary"
                size="small"
                text
                @click="openEnableDialog(row)"
              >
                配置
              </el-button>
              <el-button
                data-testid="btn-agent-disable"
                type="danger"
                size="small"
                :loading="actionLoading[row.agentId]"
                @click="handleDisable(row.agentId)"
              >
                停用
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else description="暂无可用的智能体" />
    </template>

    <ErrorState
      v-if="!loading && error"
      title="加载失败"
      message="无法加载项目智能体配置"
      retry-text="重试"
      @retry="loadAgents"
    />

    <!-- Enable / Configure Agent Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '配置智能体' : '启用智能体'"
      width="560px"
      :close-on-click-modal="false"
      data-testid="agent-enable-dialog"
    >
      <template v-if="dialogAgent">
        <div class="pa-dialog-info">
          <div class="pa-dialog-label">智能体</div>
          <div class="pa-dialog-value">{{ dialogAgent.agentName }}</div>
        </div>
        <div class="pa-dialog-info">
          <div class="pa-dialog-label">全局状态</div>
          <div class="pa-dialog-value">
            <StatusPulse :status="dialogAgent.agentStatus" :tone="statusTone(dialogAgent.agentStatus)" />
          </div>
        </div>

        <div class="pa-dialog-field">
          <div class="pa-dialog-label">Agent 版本</div>
          <el-select
            v-model="dialogVersionId"
            style="width:100%"
            data-testid="select-agent-version"
            placeholder="选择版本"
            :loading="versionsLoading"
          >
            <el-option
              v-for="v in publishedVersions"
              :key="v.id"
              :label="v.versionNo"
              :value="v.id"
            />
          </el-select>
          <div v-if="!versionsLoading && publishedVersions.length === 0" class="pa-dialog-hint" style="color:var(--app-danger)">
            该智能体暂无已发布版本，无法启用
          </div>
        </div>

        <div class="pa-dialog-field">
          <div class="pa-dialog-label">模型配置</div>
          <el-select
            v-model="dialogModelConfigId"
            style="width:100%"
            data-testid="select-model-config"
            placeholder="选择模型配置"
            :loading="modelConfigsLoading"
            clearable
          >
            <el-option
              v-for="mc in modelConfigs"
              :key="mc.id"
              :label="modelConfigLabel(mc)"
              :value="String(mc.id)"
            />
          </el-select>
          <div v-if="!modelConfigsLoading && modelConfigs.length === 0" class="pa-dialog-hint">
            暂无可用的模型配置，将使用默认配置
          </div>
        </div>

        <!-- Runtime Config Section -->
        <div class="pa-dialog-section" data-testid="agent-runtime-config-section">
          <div class="pa-section-title">运行配置</div>

          <div class="pa-dialog-field">
            <div class="pa-dialog-label">Temperature</div>
            <div class="pa-range-row">
              <el-slider
                v-model="dialogConfig.temperature"
                :min="0"
                :max="2"
                :step="0.1"
                style="flex:1"
                data-testid="input-agent-temperature"
              />
              <span class="pa-range-value">{{ dialogConfig.temperature }}</span>
            </div>
          </div>

          <div class="pa-dialog-field">
            <div class="pa-dialog-label">Max Tokens</div>
            <el-input-number
              v-model="dialogConfig.maxTokens"
              :min="256"
              :max="32768"
              :step="256"
              style="width:100%"
              data-testid="input-agent-max-tokens"
            />
          </div>

          <div class="pa-dialog-field">
            <div class="pa-dialog-label">Timeout (秒)</div>
            <el-input-number
              v-model="dialogConfig.timeoutSeconds"
              :min="5"
              :max="600"
              :step="5"
              style="width:100%"
              data-testid="input-agent-timeout"
            />
          </div>

          <div class="pa-dialog-field">
            <div class="pa-dialog-label">Use RAG</div>
            <el-switch
              v-model="dialogConfig.useRag"
              data-testid="switch-agent-use-rag"
            />
          </div>

          <div class="pa-dialog-field" v-if="dialogConfig.useRag">
            <div class="pa-dialog-label">知识库</div>
            <el-select
              v-model="dialogConfig.knowledgeBaseId"
              style="width:100%"
              data-testid="select-agent-knowledge-base"
              placeholder="选择知识库（可选）"
              :loading="knowledgeBasesLoading"
              clearable
            >
              <el-option
                v-for="kb in knowledgeBases"
                :key="kb.id"
                :label="kb.name"
                :value="kb.id"
              />
            </el-select>
            <div v-if="!knowledgeBasesLoading && knowledgeBases.length === 0" class="pa-dialog-hint">
              暂无知识库，将使用默认 RAG 搜索范围
            </div>
          </div>

          <div class="pa-dialog-field">
            <div class="pa-dialog-label">Custom Instruction</div>
            <el-input
              v-model="dialogConfig.customInstruction"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              placeholder="自定义指令（可选，最长 2000 字符）"
              data-testid="input-agent-custom-instruction"
            />
          </div>
        </div>
      </template>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="dialogLoading"
          :disabled="!versionsLoading && publishedVersions.length === 0"
          data-testid="btn-confirm-enable-agent"
          @click="handleConfirmEnable"
        >
          {{ isEditing ? '保存配置' : '确认启用' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pa-agent-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.pa-agent-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text);
}
.pa-agent-code {
  font-size: 11px;
  color: var(--app-text-muted);
  font-family: monospace;
}
.pa-version {
  font-family: monospace;
  font-size: 12px;
  color: var(--app-text-soft);
}
.pa-runtime-summary {
  font-size: 11px;
  color: var(--app-text-soft);
  font-family: monospace;
}

.pa-dialog-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.pa-dialog-field {
  margin-top: 14px;
}
.pa-dialog-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
  min-width: 80px;
}
.pa-dialog-value {
  font-size: 14px;
  color: var(--app-text);
}
.pa-dialog-hint {
  margin-top: 6px;
  font-size: 11px;
  color: var(--app-text-muted);
}
.pa-dialog-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--app-border);
}
.pa-section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--app-text);
  margin-bottom: 4px;
}
.pa-range-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pa-range-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-primary);
  min-width: 32px;
  text-align: right;
  font-family: monospace;
}
</style>
