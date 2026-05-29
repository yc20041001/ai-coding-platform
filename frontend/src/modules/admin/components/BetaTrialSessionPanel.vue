<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import {
  listBetaSessions, getBetaSession, createBetaSession, updateBetaSession,
  exportBetaSessionMarkdown,
  type BetaTrialSessionItem, type BetaTrialSessionSummary,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import { ElTag, ElButton, ElDialog, ElInput, ElSelect, ElOption, ElMessage } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = withDefaults(defineProps<{
  projectId?: string | null
  loading?: boolean
}>(), {
  projectId: null,
  loading: false,
})

const emit = defineEmits<{
  dashboardRefresh: []
  selectSession: [id: string]
}>()

const sessions = ref<BetaTrialSessionSummary[]>([])
const selectedSession = ref<BetaTrialSessionItem | null>(null)
const loadingSessions = ref(false)
const error = ref(false)
const sessionDetailVisible = ref(false)
const createDialogVisible = ref(false)
const markdownVisible = ref(false)
const markdownContent = ref('')
const createForm = ref({ title: '', participantRole: '', environmentType: '', providerMode: '', githubOauthStatus: '' })
const transitionLoading = ref<string | null>(null)

async function loadSessions() {
  if (!props.projectId) return
  loadingSessions.value = true
  error.value = false
  try {
    const res = await listBetaSessions(props.projectId)
    sessions.value = res.data.data
  } catch {
    error.value = true
  } finally {
    loadingSessions.value = false
  }
}

async function viewSession(id: string) {
  try {
    const res = await getBetaSession(id)
    selectedSession.value = res.data.data
    sessionDetailVisible.value = true
  } catch {
    ElMessage.error('加载会话详情失败')
  }
}

async function handleCreate() {
  if (!createForm.value.title || !props.projectId) return
  try {
    await createBetaSession({ ...createForm.value, projectId: props.projectId })
    ElMessage.success('Beta 试用会话已创建')
    createDialogVisible.value = false
    createForm.value = { title: '', participantRole: '', environmentType: '', providerMode: '', githubOauthStatus: '' }
    await loadSessions()
    emit('dashboardRefresh')
  } catch {
    ElMessage.error('创建失败')
  }
}

async function handleTransition(id: string, status: string) {
  transitionLoading.value = id
  try {
    await updateBetaSession(id, { sessionStatus: status })
    ElMessage.success(`状态已变更为 ${status}`)
    await loadSessions()
    if (selectedSession.value?.id === id) {
      await viewSession(id)
    }
    emit('dashboardRefresh')
  } catch {
    ElMessage.error('状态变更失败')
  } finally {
    transitionLoading.value = null
  }
}

async function handleExportMarkdown(id: string) {
  try {
    const res = await exportBetaSessionMarkdown(id)
    markdownContent.value = res.data.data
    markdownVisible.value = true
  } catch {
    ElMessage.error('导出失败')
  }
}

function statusTag(status: string): 'info' | 'warning' | 'success' | 'danger' {
  const map: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
    PLANNED: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success', BLOCKED: 'danger', CANCELED: 'info',
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PLANNED: '待开始', IN_PROGRESS: '进行中', COMPLETED: '已完成', BLOCKED: '已阻塞', CANCELED: '已取消',
  }
  return map[status] || status
}

onMounted(() => { loadSessions() })

watch(() => props.projectId, () => { loadSessions() })
</script>

<template>
  <TechPanel
    v-loading="loadingSessions || loading"
    title="Beta Trial Sessions"
    data-testid="beta-session-panel"
  >
    <ErrorState
      v-if="error"
      title="无法加载 Beta 试用会话"
      message="加载失败，但不影响其他面板"
    />
    <template v-else>
      <div class="beta-toolbar">
        <ElButton size="small" type="primary" @click="createDialogVisible = true" :disabled="!projectId">
          + 创建会话
        </ElButton>
        <span class="beta-count">{{ sessions.length }} 条记录</span>
      </div>

      <NeonDivider v-if="sessions.length > 0" />

      <div v-if="sessions.length === 0" class="beta-empty">
        <EmptyState title="暂无 Beta 试用会话" message="创建一个新的试用会话来开始记录" />
      </div>

      <div v-for="s in sessions" :key="s.id" class="beta-session-row" @click="viewSession(s.id)">
        <div class="beta-session-info">
          <span class="beta-session-title">{{ s.title }}</span>
          <span class="beta-session-meta">
            {{ s.participantRole || '-' }} · {{ s.environmentType || '-' }}
          </span>
        </div>
        <div class="beta-session-status">
          <ElTag :type="statusTag(s.sessionStatus)" size="small">{{ statusLabel(s.sessionStatus) }}</ElTag>
          <span v-if="s.satisfactionScore" class="beta-score">{{ s.satisfactionScore }}/10</span>
          <ElButton size="small" text type="primary" @click.stop="emit('selectSession', s.id)">选择</ElButton>
        </div>
      </div>

      <!-- Session Detail Dialog -->
      <ElDialog v-model="sessionDetailVisible" title="会话详情" width="600px" destroy-on-close>
        <template v-if="selectedSession">
          <div class="beta-detail-grid">
            <div class="beta-detail-item">
              <label>标题</label><span>{{ selectedSession.title }}</span>
            </div>
            <div class="beta-detail-item">
              <label>状态</label>
              <ElTag :type="statusTag(selectedSession.sessionStatus)" size="small">
                {{ statusLabel(selectedSession.sessionStatus) }}
              </ElTag>
            </div>
            <div class="beta-detail-item">
              <label>参与角色</label><span>{{ selectedSession.participantRole || '-' }}</span>
            </div>
            <div class="beta-detail-item">
              <label>环境类型</label><span>{{ selectedSession.environmentType || '-' }}</span>
            </div>
            <div class="beta-detail-item">
              <label>供应商模式</label><span>{{ selectedSession.providerMode || '-' }}</span>
            </div>
            <div class="beta-detail-item">
              <label>GitHub OAuth</label><span>{{ selectedSession.githubOauthStatus || '-' }}</span>
            </div>
            <div class="beta-detail-item">
              <label>满意度</label><span>{{ selectedSession.satisfactionScore != null ? selectedSession.satisfactionScore + '/10' : '-' }}</span>
            </div>
            <div class="beta-detail-item">
              <label>继续意向</label><span>{{ selectedSession.continueIntent || '-' }}</span>
            </div>
            <div class="beta-detail-item">
              <label>开始时间</label><span>{{ selectedSession.startedAt ? formatDateTime(selectedSession.startedAt) : '-' }}</span>
            </div>
            <div class="beta-detail-item">
              <label>结束时间</label><span>{{ selectedSession.endedAt ? formatDateTime(selectedSession.endedAt) : '-' }}</span>
            </div>
          </div>

          <div v-if="selectedSession.blockerSummary" class="beta-detail-section">
            <label>阻塞摘要</label>
            <p>{{ selectedSession.blockerSummary }}</p>
          </div>
          <div v-if="selectedSession.summary" class="beta-detail-section">
            <label>总结</label>
            <p>{{ selectedSession.summary }}</p>
          </div>

          <NeonDivider />
          <div class="beta-detail-actions">
            <span class="beta-detail-actions-label">状态操作：</span>
            <ElButton v-if="selectedSession.sessionStatus === 'PLANNED'" size="small" type="warning"
              :loading="transitionLoading === selectedSession.id"
              @click="handleTransition(selectedSession.id, 'IN_PROGRESS')">
              开始试用
            </ElButton>
            <ElButton v-if="selectedSession.sessionStatus === 'IN_PROGRESS'" size="small" type="success"
              :loading="transitionLoading === selectedSession.id"
              @click="handleTransition(selectedSession.id, 'COMPLETED')">
              完成
            </ElButton>
            <ElButton v-if="selectedSession.sessionStatus === 'IN_PROGRESS'" size="small" type="danger"
              :loading="transitionLoading === selectedSession.id"
              @click="handleTransition(selectedSession.id, 'BLOCKED')">
              标记阻塞
            </ElButton>
            <ElButton v-if="selectedSession.sessionStatus === 'BLOCKED'" size="small" type="warning"
              :loading="transitionLoading === selectedSession.id"
              @click="handleTransition(selectedSession.id, 'IN_PROGRESS')">
              恢复
            </ElButton>
            <ElButton v-if="selectedSession.sessionStatus === 'PLANNED'" size="small"
              :loading="transitionLoading === selectedSession.id"
              @click="handleTransition(selectedSession.id, 'CANCELED')">
              取消
            </ElButton>
            <ElButton size="small" @click="handleExportMarkdown(selectedSession.id)">
              导出 Markdown
            </ElButton>
          </div>
        </template>
      </ElDialog>

      <!-- Create Dialog -->
      <ElDialog v-model="createDialogVisible" title="创建 Beta 试用会话" width="500px" destroy-on-close>
        <div class="beta-create-form">
          <div class="beta-form-row">
            <label>标题 *</label>
            <ElInput v-model="createForm.title" placeholder="会话标题" />
          </div>
          <div class="beta-form-row">
            <label>参与角色</label>
            <ElSelect v-model="createForm.participantRole" placeholder="选择角色" clearable>
              <ElOption label="开发者" value="DEVELOPER" />
              <ElOption label="测试工程师" value="TEST_ENGINEER" />
              <ElOption label="产品经理" value="PRODUCT_MANAGER" />
              <ElOption label="运维工程师" value="OPS_ENGINEER" />
              <ElOption label="外部 Beta 用户" value="EXTERNAL_BETA_USER" />
            </ElSelect>
          </div>
          <div class="beta-form-row">
            <label>环境类型</label>
            <ElSelect v-model="createForm.environmentType" placeholder="选择环境" clearable>
              <ElOption label="本地" value="LOCAL" />
              <ElOption label="Docker Compose" value="DOCKER_COMPOSE" />
              <ElOption label="生产 Demo" value="PROD_DEMO" />
              <ElOption label="自托管" value="SELF_HOSTED" />
            </ElSelect>
          </div>
          <div class="beta-form-row">
            <label>供应商模式</label>
            <ElSelect v-model="createForm.providerMode" placeholder="选择模式" clearable>
              <ElOption label="Mock" value="MOCK" />
              <ElOption label="真实模型" value="REAL_MODEL" />
              <ElOption label="混合" value="MIXED" />
            </ElSelect>
          </div>
          <div class="beta-form-row">
            <label>GitHub OAuth</label>
            <ElSelect v-model="createForm.githubOauthStatus" placeholder="选择状态" clearable>
              <ElOption label="未配置" value="NOT_CONFIGURED" />
              <ElOption label="已配置未测试" value="CONFIGURED_NOT_TESTED" />
              <ElOption label="测试通过" value="TESTED_OK" />
              <ElOption label="测试失败" value="TESTED_FAILED" />
              <ElOption label="不适用" value="NOT_APPLICABLE" />
            </ElSelect>
          </div>
        </div>
        <template #footer>
          <ElButton @click="createDialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleCreate" :disabled="!createForm.title">创建</ElButton>
        </template>
      </ElDialog>

      <!-- Markdown Export Dialog -->
      <ElDialog v-model="markdownVisible" title="导出 Markdown" width="700px" destroy-on-close>
        <pre class="beta-markdown">{{ markdownContent }}</pre>
      </ElDialog>
    </template>
  </TechPanel>
</template>

<style scoped>
.beta-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.beta-count {
  font-size: 12px;
  color: var(--app-text-secondary);
}
.beta-empty {
  padding: 16px 0;
}
.beta-session-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  cursor: pointer;
  border-bottom: 1px solid var(--app-border);
  transition: background 0.15s;
}
.beta-session-row:last-child { border-bottom: none; }
.beta-session-row:hover { background: var(--app-hover); }
.beta-session-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.beta-session-title {
  font-size: 14px;
  color: var(--app-text);
  font-weight: 500;
}
.beta-session-meta {
  font-size: 12px;
  color: var(--app-text-secondary);
}
.beta-session-status {
  display: flex;
  align-items: center;
  gap: 8px;
}
.beta-score {
  font-size: 12px;
  color: var(--app-accent);
  font-weight: 600;
}
.beta-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.beta-detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.beta-detail-item label {
  font-size: 11px;
  color: var(--app-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.beta-detail-item span {
  font-size: 14px;
  color: var(--app-text);
}
.beta-detail-section {
  margin-top: 16px;
}
.beta-detail-section label {
  font-size: 11px;
  color: var(--app-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.beta-detail-section p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--app-text);
  white-space: pre-wrap;
}
.beta-detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.beta-detail-actions-label {
  font-size: 12px;
  color: var(--app-text-secondary);
}
.beta-create-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.beta-form-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.beta-form-row label {
  font-size: 12px;
  color: var(--app-text-secondary);
}
.beta-markdown {
  background: var(--app-bg);
  padding: 16px;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  max-height: 400px;
  overflow-y: auto;
  color: var(--app-text);
}
</style>
