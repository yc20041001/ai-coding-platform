<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePagination } from '@/shared/composables/usePagination'
import {
  listTasks, createTask, executeTask, getTaskLogs, getTaskArtifacts,
  type TaskItem, type TaskLog, type TaskArtifact,
} from '@/modules/task/api'
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import SignalStrip from '@/shared/components/SignalStrip.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import MarkdownRenderer from '@/shared/components/MarkdownRenderer.vue'
import { formatDateTime } from '@/shared/utils/format'

const route = useRoute()
const router = useRouter()
const projectId = route.params.projectId as string

const { loading, records, pagination, load } = usePagination<TaskItem>(
  (page, pageSize) => listTasks(projectId, page, pageSize),
)

const createVisible = ref(false)
const creating = ref(false)
const createForm = ref({
  title: '', description: '', taskType: 'FEATURE', priority: 'MEDIUM', agentId: '300002',
})

const executeVisible = ref(false)
const executing = ref(false)
const executeForm = ref({ instruction: '', agentId: '300002', useRag: false, ragLimit: 5 })
const executingTaskId = ref('')

const logsVisible = ref(false)
const logs = ref<TaskLog[]>([])
const logsLoading = ref(false)

const artifactVisible = ref(false)
const artifacts = ref<TaskArtifact[]>([])
const artifactLoading = ref(false)
const selectedArtifact = ref<TaskArtifact | null>(null)

const statusTone = (status: string) => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'primary' | 'muted'> = {
    PENDING: 'muted',
    RUNNING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger',
    CANCELLED: 'muted',
  }
  return map[status] || 'muted'
}

const pipelineStats = computed(() => {
  const total = records.value.length
  const running = records.value.filter(r => r.status === 'RUNNING').length
  const pending = records.value.filter(r => r.status === 'PENDING').length
  const completed = records.value.filter(r => r.status === 'COMPLETED').length
  const failed = records.value.filter(r => r.status === 'FAILED').length
  return { total, running, pending, completed, failed }
})

async function handleCreate() {
  creating.value = true
  try {
    await createTask(projectId, {
      title: createForm.value.title,
      description: createForm.value.description,
      taskType: createForm.value.taskType,
      priority: createForm.value.priority,
      agentId: createForm.value.agentId,
    })
    ElMessage.success('Task created')
    createVisible.value = false
    createForm.value = { title: '', description: '', taskType: 'FEATURE', priority: 'MEDIUM', agentId: '300002' }
    load(1)
  } catch { /* handled */ } finally { creating.value = false }
}

function openExecute(taskId: string) {
  executingTaskId.value = taskId
  executeVisible.value = true
}

async function handleExecute() {
  executing.value = true
  try {
    await executeTask(executingTaskId.value, executeForm.value)
    ElMessage.success('Task executed')
    executeVisible.value = false
    load()
  } catch { /* handled */ } finally { executing.value = false }
}

async function handleViewLogs(taskId: string) {
  logsLoading.value = true
  logsVisible.value = true
  try {
    const res = await getTaskLogs(taskId)
    logs.value = res.data.data
  } catch { /* handled */ } finally { logsLoading.value = false }
}

async function handleViewArtifacts(taskId: string) {
  artifactLoading.value = true
  artifactVisible.value = true
  try {
    const res = await getTaskArtifacts(taskId)
    artifacts.value = res.data.data
    selectedArtifact.value = res.data.data[0] || null
  } catch { /* handled */ } finally { artifactLoading.value = false }
}

function viewDetail(taskId: string) {
  router.push(`/projects/${projectId}/tasks/${taskId}`)
}

load(1)
</script>

<template>
  <div class="page-container">
    <DynamicWorkspace
      title="Tasks"
      subtitle="Agent Task Pipeline"
      eyebrow="Execution"
      :status="`${pipelineStats.total} total`"
    >
      <template #actions>
        <GlowButton accent="primary" @click="createVisible = true" data-testid="btn-create-task">+ New Task</GlowButton>
      </template>

      <template #metrics>
        <div class="task-pipeline">
          <div class="task-pipe-item">
            <SignalStrip tone="muted" :active="pipelineStats.pending > 0" />
            <span class="task-pipe-label">Pending</span>
            <span class="task-pipe-val">{{ pipelineStats.pending }}</span>
          </div>
          <div class="task-pipe-arrow">→</div>
          <div class="task-pipe-item">
            <SignalStrip tone="warning" :active="pipelineStats.running > 0" />
            <span class="task-pipe-label">Running</span>
            <span class="task-pipe-val">{{ pipelineStats.running }}</span>
          </div>
          <div class="task-pipe-arrow">→</div>
          <div class="task-pipe-item">
            <SignalStrip tone="success" :active="pipelineStats.completed > 0" />
            <span class="task-pipe-label">Completed</span>
            <span class="task-pipe-val">{{ pipelineStats.completed }}</span>
          </div>
          <div class="task-pipe-arrow">→</div>
          <div class="task-pipe-item">
            <SignalStrip tone="danger" :active="pipelineStats.failed > 0" />
            <span class="task-pipe-label">Failed</span>
            <span class="task-pipe-val">{{ pipelineStats.failed }}</span>
          </div>
        </div>
      </template>

      <NeonDivider tone="primary" />

      <el-table :data="records" v-loading="loading" style="width:100%;margin-top:8px" data-testid="task-table">
        <el-table-column prop="title" label="Title" min-width="180">
          <template #default="{ row }">
            <span style="font-weight:600;color:var(--app-text)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Type" width="100">
          <template #default="{ row }">{{ row.taskType }}</template>
        </el-table-column>
        <el-table-column label="Priority" width="100">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.priority === 'HIGH' ? 'danger' : row.priority === 'MEDIUM' ? 'warning' : 'info'"
            >{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Status" width="130">
          <template #default="{ row }">
            <StatusPulse :status="row.status" :tone="statusTone(row.status)" />
          </template>
        </el-table-column>
        <el-table-column label="Created" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="viewDetail(row.id)" data-testid="btn-task-detail">Detail</el-button>
            <el-button size="small" @click="openExecute(row.id)" :disabled="row.status !== 'PENDING'" data-testid="btn-execute-task">Execute</el-button>
            <el-button size="small" @click="handleViewLogs(row.id)" data-testid="btn-task-logs">Logs</el-button>
            <el-button size="small" @click="handleViewArtifacts(row.id)" data-testid="btn-task-artifacts">Artifacts</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && records.length === 0" description="No tasks yet" />

      <el-pagination
        v-if="pagination.total > 0" v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize" :total="pagination.total"
        layout="total, prev, pager, next" style="margin-top:16px;justify-content:flex-end"
        @change="load()"
      />

      <!-- Create Task Dialog -->
      <el-dialog v-model="createVisible" title="Create Task" width="500px">
        <div data-testid="dialog-create-task">
          <el-form label-position="top">
          <el-form-item label="Title" required>
            <el-input v-model="createForm.title" placeholder="Task title" data-testid="input-task-title" />
          </el-form-item>
          <el-form-item label="Description">
            <el-input v-model="createForm.description" type="textarea" :rows="3" data-testid="input-task-description" />
          </el-form-item>
          <el-form-item label="Type">
            <el-select v-model="createForm.taskType" style="width:100%" data-testid="select-task-type">
              <el-option label="Feature" value="FEATURE" />
              <el-option label="Bug" value="BUG" />
              <el-option label="Refactor" value="REFACTOR" />
              <el-option label="Docs" value="DOCS" />
            </el-select>
          </el-form-item>
          <el-form-item label="Priority">
            <el-select v-model="createForm.priority" style="width:100%" data-testid="select-task-priority">
              <el-option label="High" value="HIGH" />
              <el-option label="Medium" value="MEDIUM" />
              <el-option label="Low" value="LOW" />
            </el-select>
          </el-form-item>
        </el-form>
        </div>
        <template #footer>
          <el-button @click="createVisible = false" data-testid="btn-cancel-task">Cancel</el-button>
          <el-button type="primary" :loading="creating" @click="handleCreate" data-testid="btn-submit-task">Create</el-button>
        </template>
      </el-dialog>

      <!-- Execute Task Dialog -->
      <el-dialog v-model="executeVisible" title="Execute Task" width="500px">
        <div data-testid="dialog-execute-task">
          <el-form label-position="top">
          <el-form-item label="Instruction">
            <el-input v-model="executeForm.instruction" type="textarea" :rows="3" placeholder="Enter execution instruction" data-testid="input-execute-instruction" />
          </el-form-item>
          <el-form-item label="Use RAG">
            <el-switch v-model="executeForm.useRag" data-testid="switch-execute-rag" />
          </el-form-item>
        </el-form>
        </div>
        <template #footer>
          <el-button @click="executeVisible = false" data-testid="btn-cancel-execute">Cancel</el-button>
          <el-button type="primary" :loading="executing" @click="handleExecute" data-testid="btn-submit-execute">Execute</el-button>
        </template>
      </el-dialog>

      <!-- Logs Drawer -->
      <el-drawer v-model="logsVisible" title="Task Logs" size="500px">
        <div v-loading="logsLoading">
          <div v-for="log in logs" :key="log.id" class="log-line">
            <span class="log-stage">{{ log.stage }}</span>
            <span class="log-level" :class="log.level?.toLowerCase()">{{ log.level }}</span>
            <span class="log-msg">{{ log.message }}</span>
            <span class="log-time">{{ formatDateTime(log.createTime) }}</span>
          </div>
          <EmptyState v-if="!logsLoading && logs.length === 0" description="No logs" />
        </div>
      </el-drawer>

      <!-- Artifacts Drawer -->
      <el-drawer v-model="artifactVisible" title="Artifacts" size="600px">
        <div v-loading="artifactLoading">
          <div v-if="selectedArtifact">
            <el-tag size="small" style="margin-bottom:12px">{{ selectedArtifact.artifactType }}</el-tag>
            <MarkdownRenderer :content="selectedArtifact.content || ''" />
          </div>
          <EmptyState v-else-if="!artifactLoading" description="No artifacts" />
        </div>
      </el-drawer>
    </DynamicWorkspace>
  </div>
</template>

<style scoped>
.task-pipeline {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.task-pipe-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 70px;
}

.task-pipe-label {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--app-text-muted);
  font-weight: 600;
}

.task-pipe-val {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text);
  font-variant-numeric: tabular-nums;
}

.task-pipe-arrow {
  color: var(--app-text-muted);
  font-size: 14px;
  margin-bottom: 12px;
}

.log-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px solid var(--app-border);
  font-size: 13px;
}
.log-stage { font-weight: 600; color: var(--app-text-soft); min-width: 120px; }
.log-level { padding: 1px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; text-transform: uppercase; }
.log-level.info { background: var(--app-primary-soft); color: var(--app-primary); }
.log-level.warn { background: var(--app-warning-soft); color: var(--app-warning); }
.log-level.error { background: var(--app-danger-soft); color: var(--app-danger); }
.log-msg { color: var(--app-text-soft); flex: 1; }
.log-time { color: var(--app-text-muted); font-size: 11px; }
</style>
