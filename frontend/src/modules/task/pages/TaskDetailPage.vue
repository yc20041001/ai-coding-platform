<script setup lang="ts">
import { ref, onMounted, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getTaskDetail, getTaskLogs, getTaskArtifacts, getTaskExecutions,
  executeTask, cancelTask, retryTask, startTask,
  type TaskDetail, type TaskLog, type TaskArtifact, type AgentExecution,
} from '@/modules/task/api'
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import SectionRail from '@/shared/components/SectionRail.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import SignalStrip from '@/shared/components/SignalStrip.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
import AgentExecutionDrawer from '@/modules/task/components/AgentExecutionDrawer.vue'
import { formatDateTime } from '@/shared/utils/format'

const MarkdownRenderer = defineAsyncComponent(() => import('@/shared/components/MarkdownRenderer.vue'))

const route = useRoute()
const router = useRouter()
const taskId = route.params.taskId as string
const projectId = route.params.projectId as string | undefined

const task = ref<TaskDetail | null>(null)
const loading = ref(false)

const activeTab = ref('overview')

const logs = ref<TaskLog[]>([])
const loadingLogs = ref(false)

const artifacts = ref<TaskArtifact[]>([])
const selectedArtifact = ref<TaskArtifact | null>(null)
const loadingArtifacts = ref(false)

const executions = ref<AgentExecution[]>([])
const loadingExecutions = ref(false)

const executeVisible = ref(false)
const executing = ref(false)
const executeForm = ref({ instruction: '', agentId: '300002', useRag: false, ragLimit: 5 })

const executionDrawerVisible = ref(false)
const selectedExecutionId = ref<string | null>(null)

const railItems = [
  { key: 'overview', label: 'Overview', icon: '◆' },
  { key: 'logs', label: 'Logs', icon: '▤' },
  { key: 'artifacts', label: 'Artifacts', icon: '◈' },
  { key: 'executions', label: 'Executions', icon: '◎' },
]

const statusTone = (status: string) => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'primary' | 'muted'> = {
    PENDING: 'muted', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger', CANCELLED: 'muted',
  }
  return map[status] || 'muted'
}

async function loadTask() {
  loading.value = true
  try { const res = await getTaskDetail(taskId); task.value = res.data.data } catch { ElMessage.error('Failed to load task') } finally { loading.value = false }
}

async function loadLogs() {
  loadingLogs.value = true
  try { const res = await getTaskLogs(taskId); logs.value = res.data.data } catch { ElMessage.error('Failed to load logs') } finally { loadingLogs.value = false }
}

async function loadArtifacts() {
  loadingArtifacts.value = true
  try { const res = await getTaskArtifacts(taskId); artifacts.value = res.data.data; selectedArtifact.value = res.data.data[0] || null } catch { ElMessage.error('Failed to load artifacts') } finally { loadingArtifacts.value = false }
}

async function loadExecutions() {
  loadingExecutions.value = true
  try { const res = await getTaskExecutions(taskId); executions.value = res.data.data.records } catch { ElMessage.error('Failed to load executions') } finally { loadingExecutions.value = false }
}

function onRailSelect(key: string) {
  activeTab.value = key
  if (key === 'logs' && logs.value.length === 0) loadLogs()
  else if (key === 'artifacts' && artifacts.value.length === 0) loadArtifacts()
  else if (key === 'executions' && executions.value.length === 0) loadExecutions()
}

async function handleExecute() {
  executing.value = true
  try { await executeTask(taskId, executeForm.value); ElMessage.success('Task executed'); executeVisible.value = false; loadTask(); loadExecutions() } catch { ElMessage.error('Execution failed') } finally { executing.value = false }
}

async function handleCancel() { try { await cancelTask(taskId, 'User cancelled'); ElMessage.success('Task cancelled'); loadTask() } catch { ElMessage.error('Cancel failed') } }
async function handleRetry() { try { await retryTask(taskId); ElMessage.success('Task retrying'); loadTask(); loadExecutions() } catch { ElMessage.error('Retry failed') } }
async function handleStart() { try { await startTask(taskId); ElMessage.success('Task started'); loadTask() } catch { ElMessage.error('Start failed') } }

function openExecution(executionId: string) {
  selectedExecutionId.value = executionId
  executionDrawerVisible.value = true
}

function goBack() {
  if (projectId) router.push(`/projects/${projectId}/tasks`)
  else router.back()
}

onMounted(() => loadTask())
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div v-if="task">
      <DynamicWorkspace
        :title="task.title"
        :subtitle="task.description || 'Task Execution Detail'"
        eyebrow="Task"
      >
        <template #actions>
          <div style="display:flex;gap:8px;align-items:center">
            <StatusPulse :status="task.status" :tone="statusTone(task.status)" />
            <el-button size="small" @click="goBack">Back</el-button>
          </div>
        </template>

        <template #metrics>
          <div class="td-pipeline">
            <div class="td-pipe-item">
              <SignalStrip tone="muted" :active="task.status === 'PENDING'" />
              <span class="td-pipe-label">Pending</span>
            </div>
            <div class="td-pipe-arrow">→</div>
            <div class="td-pipe-item">
              <SignalStrip tone="warning" :active="task.status === 'RUNNING'" />
              <span class="td-pipe-label">Running</span>
            </div>
            <div class="td-pipe-arrow">→</div>
            <div class="td-pipe-item">
              <SignalStrip tone="primary" :active="false" />
              <span class="td-pipe-label">Model Gateway</span>
            </div>
            <div class="td-pipe-arrow">→</div>
            <div class="td-pipe-item">
              <SignalStrip tone="accent" :active="false" />
              <span class="td-pipe-label">Artifact</span>
            </div>
            <div class="td-pipe-arrow">→</div>
            <div class="td-pipe-item">
              <SignalStrip tone="success" :active="task.status === 'COMPLETED'" />
              <span class="td-pipe-label">Completed</span>
            </div>
          </div>
        </template>

        <div class="td-meta">
          <el-tag size="small" type="info">{{ task.taskType }}</el-tag>
          <el-tag size="small" :type="task.priority === 'HIGH' ? 'danger' : task.priority === 'MEDIUM' ? 'warning' : 'info'">
            {{ task.priority }}
          </el-tag>
          <span class="td-meta-text">Agent: {{ task.agentName || task.agentId }}</span>
          <span v-if="task.creatorName" class="td-meta-text">Creator: {{ task.creatorName }}</span>
          <span class="td-meta-text">Retry: {{ task.retryCount }}/{{ task.maxRetryCount }}</span>
        </div>

        <div class="td-actions">
          <GlowButton v-if="task.status === 'PENDING'" accent="primary" size="small" @click="executeVisible = true">Execute</GlowButton>
          <el-button v-if="task.status === 'PENDING'" size="small" @click="handleStart">Start</el-button>
          <GlowButton v-if="task.status === 'RUNNING'" accent="danger" size="small" @click="handleCancel">Cancel</GlowButton>
          <GlowButton v-if="task.status === 'FAILED'" accent="warning" size="small" @click="handleRetry">Retry</GlowButton>
        </div>

        <div v-if="task.errorMessage" style="margin-top:12px">
          <el-alert :title="task.errorMessage" type="error" :closable="false" />
        </div>

        <NeonDivider tone="primary" style="margin:16px 0" />

        <SectionRail :items="railItems" :active-key="activeTab" @select="onRailSelect" />

        <div style="margin-top:16px">
          <!-- Overview -->
          <div v-if="activeTab === 'overview'" class="td-overview">
            <div class="overview-grid">
              <div><span class="ov-label">ID</span><code>{{ task.id }}</code></div>
              <div><span class="ov-label">Project ID</span><code>{{ task.projectId }}</code></div>
              <div><span class="ov-label">Type</span><span>{{ task.taskType }}</span></div>
              <div><span class="ov-label">Priority</span><span>{{ task.priority }}</span></div>
              <div><span class="ov-label">Status</span><StatusPulse :status="task.status" :tone="statusTone(task.status)" /></div>
              <div><span class="ov-label">Source</span><span>{{ task.sourceType || '-' }}</span></div>
              <div v-if="task.branch"><span class="ov-label">Branch</span><code>{{ task.branch }}</code></div>
              <div><span class="ov-label">Created</span><span>{{ formatDateTime(task.createTime) }}</span></div>
              <div><span class="ov-label">Started</span><span>{{ task.startTime ? formatDateTime(task.startTime) : '-' }}</span></div>
              <div><span class="ov-label">Ended</span><span>{{ task.endTime ? formatDateTime(task.endTime) : '-' }}</span></div>
            </div>
            <div v-if="task.description" style="margin-top:16px">
              <h3 class="td-section-label">Description</h3>
              <div class="td-desc-panel">
                <MarkdownRenderer :content="task.description" />
              </div>
            </div>
          </div>

          <!-- Logs -->
          <div v-if="activeTab === 'logs'" v-loading="loadingLogs">
            <div v-for="log in logs" :key="log.id" class="log-line">
              <span class="log-stage">{{ log.stage }}</span>
              <span class="log-level" :class="log.level?.toLowerCase()">{{ log.level }}</span>
              <span class="log-msg">{{ log.message }}</span>
              <span class="log-time">{{ formatDateTime(log.createTime) }}</span>
            </div>
            <EmptyState v-if="!loadingLogs && logs.length === 0" description="No logs" />
          </div>

          <!-- Artifacts -->
          <div v-if="activeTab === 'artifacts'" v-loading="loadingArtifacts">
            <template v-if="artifacts.length > 0">
              <div class="artifact-tabs">
                <el-button
                  v-for="a in artifacts" :key="a.id"
                  size="small"
                  :type="selectedArtifact?.id === a.id ? 'primary' : ''"
                  @click="selectedArtifact = a"
                >{{ a.name || a.artifactType }}</el-button>
              </div>
              <div v-if="selectedArtifact" class="artifact-content">
                <el-tag size="small" style="margin-bottom:12px">{{ selectedArtifact.artifactType }}</el-tag>
                <MarkdownRenderer :content="selectedArtifact.content || ''" />
              </div>
            </template>
            <EmptyState v-else-if="!loadingArtifacts" description="No artifacts" />
          </div>

          <!-- Executions -->
          <div v-if="activeTab === 'executions'" v-loading="loadingExecutions">
            <el-table :data="executions" size="small" style="width:100%">
              <el-table-column prop="agentName" label="Agent" min-width="120" />
              <el-table-column label="Status" width="130">
                <template #default="{ row }">
                  <StatusPulse :status="row.status" :tone="row.status === 'COMPLETED' ? 'success' : row.status === 'RUNNING' ? 'warning' : 'muted'" />
                </template>
              </el-table-column>
              <el-table-column label="RAG" width="70">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.ragUsed ? 'success' : 'info'">{{ row.ragUsed ? 'On' : 'Off' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="Tokens" width="90">
                <template #default="{ row }">{{ row.tokenUsage || 0 }}</template>
              </el-table-column>
              <el-table-column label="Started" width="150">
                <template #default="{ row }">{{ formatDateTime(row.startedAt) }}</template>
              </el-table-column>
              <el-table-column label="Actions" width="100">
                <template #default="{ row }">
                  <el-button size="small" text type="primary" @click="openExecution(row.id)">Detail</el-button>
                </template>
              </el-table-column>
            </el-table>
            <EmptyState v-if="!loadingExecutions && executions.length === 0" description="No executions" />
          </div>
        </div>
      </DynamicWorkspace>

      <!-- Execute Dialog -->
      <el-dialog v-model="executeVisible" title="Execute Task" width="500px">
        <div data-testid="dialog-execute-task-detail">
          <el-form label-position="top">
          <el-form-item label="Instruction">
            <el-input v-model="executeForm.instruction" type="textarea" :rows="3" placeholder="Enter execution instruction" data-testid="input-execute-instruction-detail" />
          </el-form-item>
          <el-form-item label="Use RAG">
            <el-switch v-model="executeForm.useRag" data-testid="switch-execute-rag-detail" />
          </el-form-item>
        </el-form>
        </div>
        <template #footer>
          <el-button @click="executeVisible = false" data-testid="btn-cancel-execute-detail">Cancel</el-button>
          <el-button type="primary" :loading="executing" @click="handleExecute" data-testid="btn-submit-execute-detail">Execute</el-button>
        </template>
      </el-dialog>

      <AgentExecutionDrawer
        :execution-id="selectedExecutionId"
        :visible="executionDrawerVisible"
        @close="executionDrawerVisible = false"
      />
    </div>
    <ErrorState v-else-if="!loading" title="Task not found" message="Cannot load task details" retry-text="Retry" @retry="loadTask" />
  </div>
</template>

<style scoped>
.td-pipeline {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}
.td-pipe-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}
.td-pipe-label {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--app-text-muted);
  font-weight: 600;
}
.td-pipe-arrow {
  color: var(--app-text-muted);
  font-size: 12px;
  margin-bottom: 10px;
}

.td-meta { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin: 12px 0; }
.td-meta-text { font-size: 13px; color: var(--app-text-muted); }
.td-actions { display: flex; gap: 8px; }

.td-overview .overview-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.ov-label { font-size: 11px; color: var(--app-text-muted); display: block; margin-bottom: 3px; text-transform: uppercase; letter-spacing: 0.4px; }
.overview-grid code { font-size: 12px; background: rgba(148,163,184,0.1); color: var(--app-text-soft); padding: 2px 8px; border-radius: 4px; }
.overview-grid span { font-size: 13px; color: var(--app-text-soft); }

.td-section-label { font-size: 12px; font-weight: 600; color: var(--app-text-muted); text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 10px; }
.td-desc-panel {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 20px;
}

.artifact-tabs { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.artifact-content {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 20px;
}

.log-line { display: flex; flex-wrap: wrap; gap: 8px; padding: 10px 0; border-bottom: 1px solid var(--app-border); font-size: 13px; }
.log-stage { font-weight: 600; color: var(--app-text-soft); min-width: 120px; }
.log-level { padding: 1px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; text-transform: uppercase; }
.log-level.info { background: var(--app-primary-soft); color: var(--app-primary); }
.log-level.warn { background: var(--app-warning-soft); color: var(--app-warning); }
.log-level.error { background: var(--app-danger-soft); color: var(--app-danger); }
.log-msg { color: var(--app-text-soft); flex: 1; }
.log-time { color: var(--app-text-muted); font-size: 11px; }
</style>
