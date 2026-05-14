<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getTaskDetail, getTaskLogs, getTaskArtifacts, getTaskExecutions,
  executeTask, cancelTask, retryTask, startTask,
  type TaskDetail, type TaskLog, type TaskArtifact, type AgentExecution,
} from '@/modules/task/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import StatusTag from '@/shared/components/StatusTag.vue'
import RuntimeBadge from '@/shared/components/RuntimeBadge.vue'
import MarkdownRenderer from '@/shared/components/MarkdownRenderer.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import AgentExecutionDrawer from '@/modules/task/components/AgentExecutionDrawer.vue'
import { formatDateTime } from '@/shared/utils/format'

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

const tabs = [
  { name: 'overview', label: 'Overview' },
  { name: 'logs', label: 'Logs' },
  { name: 'artifacts', label: 'Artifacts' },
  { name: 'executions', label: 'Executions' },
]

async function loadTask() {
  loading.value = true
  try {
    const res = await getTaskDetail(taskId)
    task.value = res.data.data
  } catch { ElMessage.error('加载任务详情失败') } finally { loading.value = false }
}

async function loadLogs() {
  loadingLogs.value = true
  try { const res = await getTaskLogs(taskId); logs.value = res.data.data } catch { ElMessage.error('加载日志失败') } finally { loadingLogs.value = false }
}

async function loadArtifacts() {
  loadingArtifacts.value = true
  try { const res = await getTaskArtifacts(taskId); artifacts.value = res.data.data; selectedArtifact.value = res.data.data[0] || null } catch { ElMessage.error('加载产物失败') } finally { loadingArtifacts.value = false }
}

async function loadExecutions() {
  loadingExecutions.value = true
  try { const res = await getTaskExecutions(taskId); executions.value = res.data.data.records } catch { ElMessage.error('加载执行记录失败') } finally { loadingExecutions.value = false }
}

function onTabChange(name: string) {
  if (name === 'logs' && logs.value.length === 0) loadLogs()
  else if (name === 'artifacts' && artifacts.value.length === 0) loadArtifacts()
  else if (name === 'executions' && executions.value.length === 0) loadExecutions()
}

async function handleExecute() {
  executing.value = true
  try { await executeTask(taskId, executeForm.value); ElMessage.success('任务执行完成'); executeVisible.value = false; loadTask(); loadExecutions() } catch { ElMessage.error('执行失败') } finally { executing.value = false }
}

async function handleCancel() { try { await cancelTask(taskId, '用户取消'); ElMessage.success('任务已取消'); loadTask() } catch { ElMessage.error('取消失败') } }
async function handleRetry() { try { await retryTask(taskId); ElMessage.success('任务已重试'); loadTask(); loadExecutions() } catch { ElMessage.error('重试失败') } }
async function handleStart() { try { await startTask(taskId); ElMessage.success('任务已开始'); loadTask() } catch { ElMessage.error('启动失败') } }

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
      <div class="dash-header">
        <div>
          <h1 class="dash-title">{{ task.title }}</h1>
          <p class="dash-sub">{{ task.description || '任务详情' }}</p>
        </div>
        <div style="display:flex;gap:8px;align-items:center">
          <StatusTag :status="task.status" />
          <el-button size="small" @click="goBack">返回</el-button>
        </div>
      </div>

      <div class="task-meta">
        <el-tag size="small" type="info">{{ task.taskType }}</el-tag>
        <el-tag size="small" :type="task.priority === 'HIGH' ? 'danger' : task.priority === 'MEDIUM' ? 'warning' : 'info'">
          {{ task.priority }}
        </el-tag>
        <span class="task-meta-text">Agent: {{ task.agentName || task.agentId }}</span>
        <span v-if="task.creatorName" class="task-meta-text">Creator: {{ task.creatorName }}</span>
        <span class="task-meta-text">Retry: {{ task.retryCount }}/{{ task.maxRetryCount }}</span>
        <span v-if="task.branch" class="task-meta-text">Branch: {{ task.branch }}</span>
      </div>

      <div class="task-actions">
        <el-button v-if="task.status === 'PENDING'" size="small" type="primary" @click="executeVisible = true">执行</el-button>
        <el-button v-if="task.status === 'PENDING'" size="small" @click="handleStart">开始</el-button>
        <el-button v-if="task.status === 'RUNNING'" size="small" type="danger" @click="handleCancel">取消</el-button>
        <el-button v-if="task.status === 'FAILED'" size="small" type="warning" @click="handleRetry">重试</el-button>
      </div>

      <div v-if="task.errorMessage" style="margin-top:12px">
        <el-alert :title="task.errorMessage" type="error" :closable="false" />
      </div>

      <el-tabs v-model="activeTab" style="margin-top:20px" @tab-click="(t: any) => onTabChange(t.panelName || t.name)">
        <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" />
      </el-tabs>

      <div class="tab-content">
        <!-- Overview -->
        <div v-if="activeTab === 'overview'" class="task-overview">
          <TechPanel title="基本信息" glow>
            <div class="overview-grid">
              <div><span class="ov-label">ID</span><code>{{ task.id }}</code></div>
              <div><span class="ov-label">项目ID</span><code>{{ task.projectId }}</code></div>
              <div><span class="ov-label">类型</span><span>{{ task.taskType }}</span></div>
              <div><span class="ov-label">优先级</span><span>{{ task.priority }}</span></div>
              <div><span class="ov-label">状态</span><StatusTag :status="task.status" /></div>
              <div><span class="ov-label">来源</span><span>{{ task.sourceType || '-' }}</span></div>
              <div v-if="task.branch"><span class="ov-label">分支</span><code>{{ task.branch }}</code></div>
              <div><span class="ov-label">创建时间</span><span>{{ formatDateTime(task.createTime) }}</span></div>
              <div><span class="ov-label">开始时间</span><span>{{ task.startTime ? formatDateTime(task.startTime) : '-' }}</span></div>
              <div><span class="ov-label">结束时间</span><span>{{ task.endTime ? formatDateTime(task.endTime) : '-' }}</span></div>
            </div>
          </TechPanel>
          <TechPanel v-if="task.description" title="描述" style="margin-top:16px">
            <MarkdownRenderer :content="task.description" />
          </TechPanel>
        </div>

        <!-- Logs -->
        <div v-if="activeTab === 'logs'" v-loading="loadingLogs">
          <div v-for="log in logs" :key="log.id" class="log-line">
            <span class="log-stage">{{ log.stage }}</span>
            <span class="log-level" :class="log.level?.toLowerCase()">{{ log.level }}</span>
            <span class="log-msg">{{ log.message }}</span>
            <span class="log-time">{{ formatDateTime(log.createTime) }}</span>
          </div>
          <EmptyState v-if="!loadingLogs && logs.length === 0" description="暂无日志" />
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
          <EmptyState v-else-if="!loadingArtifacts" description="暂无产物" />
        </div>

        <!-- Executions -->
        <div v-if="activeTab === 'executions'" v-loading="loadingExecutions">
          <el-table :data="executions" size="small" style="width:100%">
            <el-table-column prop="agentName" label="Agent" min-width="120" />
            <el-table-column label="Status" width="110">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
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
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="openExecution(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <EmptyState v-if="!loadingExecutions && executions.length === 0" description="暂无执行记录" />
        </div>
      </div>

      <!-- Execute Dialog -->
      <el-dialog v-model="executeVisible" title="执行任务" width="500px">
        <el-form label-position="top">
          <el-form-item label="指令">
            <el-input v-model="executeForm.instruction" type="textarea" :rows="3" placeholder="请输入执行指令" />
          </el-form-item>
          <el-form-item label="使用 RAG">
            <el-switch v-model="executeForm.useRag" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="executeVisible = false">取消</el-button>
          <el-button type="primary" :loading="executing" @click="handleExecute">执行</el-button>
        </template>
      </el-dialog>

      <AgentExecutionDrawer
        :execution-id="selectedExecutionId"
        :visible="executionDrawerVisible"
        @close="executionDrawerVisible = false"
      />
    </div>
    <ErrorState v-else-if="!loading" title="任务不存在" message="无法加载任务详情" retry-text="重试" @retry="loadTask" />
  </div>
</template>

<style scoped>
.dash-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.dash-title { font-size: 22px; font-weight: 700; color: var(--app-text); margin: 0; }
.dash-sub { font-size: 13px; color: var(--app-text-muted); margin-top: 4px; }

.task-meta { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 12px; }
.task-meta-text { font-size: 13px; color: var(--app-text-muted); }
.task-actions { display: flex; gap: 8px; }

.task-overview .overview-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.ov-label { font-size: 11px; color: var(--app-text-muted); display: block; margin-bottom: 3px; text-transform: uppercase; letter-spacing: 0.4px; }
.overview-grid code { font-size: 12px; background: rgba(148,163,184,0.1); color: var(--app-text-soft); padding: 2px 8px; border-radius: 4px; }
.overview-grid span { font-size: 13px; color: var(--app-text-soft); }

.tab-content { margin-top: 16px; }

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
