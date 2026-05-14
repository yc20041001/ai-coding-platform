<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePagination } from '@/shared/composables/usePagination'
import {
  listTasks, createTask, executeTask, getTaskLogs, getTaskArtifacts,
  type TaskItem, type TaskLog, type TaskArtifact,
} from '@/modules/task/api'
import RuntimeBadge from '@/shared/components/RuntimeBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusTag from '@/shared/components/StatusTag.vue'
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
    ElMessage.success('任务创建成功')
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
    ElMessage.success('任务执行完成')
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
    <div class="dash-header">
      <div>
        <h1 class="dash-title">Tasks</h1>
        <p class="dash-sub">任务管理与执行</p>
      </div>
      <el-button type="primary" @click="createVisible = true">新建任务</el-button>
    </div>

    <el-table :data="records" v-loading="loading" style="width:100%">
      <el-table-column prop="title" label="标题" min-width="160">
        <template #default="{ row }">
          <span style="font-weight:600;color:var(--app-text)">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ row.taskType }}</template>
      </el-table-column>
      <el-table-column label="优先级" width="90">
        <template #default="{ row }">
          <el-tag
            size="small"
            :type="row.priority === 'HIGH' ? 'danger' : row.priority === 'MEDIUM' ? 'warning' : 'info'"
          >{{ row.priority }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="viewDetail(row.id)">详情</el-button>
          <el-button size="small" @click="openExecute(row.id)" :disabled="row.status !== 'PENDING'">执行</el-button>
          <el-button size="small" @click="handleViewLogs(row.id)">日志</el-button>
          <el-button size="small" @click="handleViewArtifacts(row.id)">产物</el-button>
        </template>
      </el-table-column>
    </el-table>

    <EmptyState v-if="!loading && records.length === 0" description="暂无任务" />

    <el-pagination
      v-if="pagination.total > 0" v-model:current-page="pagination.page"
      v-model:page-size="pagination.pageSize" :total="pagination.total"
      layout="total, prev, pager, next" style="margin-top:16px;justify-content:flex-end"
      @change="load()"
    />

    <!-- Create Task Dialog -->
    <el-dialog v-model="createVisible" title="新建任务" width="500px">
      <el-form label-position="top">
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" placeholder="任务标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="createForm.taskType" style="width:100%">
            <el-option label="Feature" value="FEATURE" />
            <el-option label="Bug" value="BUG" />
            <el-option label="Refactor" value="REFACTOR" />
            <el-option label="Docs" value="DOCS" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="createForm.priority" style="width:100%">
            <el-option label="High" value="HIGH" />
            <el-option label="Medium" value="MEDIUM" />
            <el-option label="Low" value="LOW" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Execute Task Dialog -->
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

    <!-- Logs Drawer -->
    <el-drawer v-model="logsVisible" title="任务日志" size="500px">
      <div v-loading="logsLoading">
        <div v-for="log in logs" :key="log.id" class="log-line">
          <span class="log-stage">{{ log.stage }}</span>
          <span class="log-level" :class="log.level?.toLowerCase()">{{ log.level }}</span>
          <span class="log-msg">{{ log.message }}</span>
          <span class="log-time">{{ formatDateTime(log.createTime) }}</span>
        </div>
        <EmptyState v-if="!logsLoading && logs.length === 0" description="暂无日志" />
      </div>
    </el-drawer>

    <!-- Artifacts Drawer -->
    <el-drawer v-model="artifactVisible" title="产物" size="600px">
      <div v-loading="artifactLoading">
        <div v-if="selectedArtifact">
          <el-tag size="small" style="margin-bottom:12px">{{ selectedArtifact.artifactType }}</el-tag>
          <MarkdownRenderer :content="selectedArtifact.content || ''" />
        </div>
        <EmptyState v-else-if="!artifactLoading" description="暂无产物" />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.dash-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.dash-title { font-size: 24px; font-weight: 700; color: var(--app-text); margin: 0; }
.dash-sub { font-size: 13px; color: var(--app-text-muted); margin-top: 4px; }

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
