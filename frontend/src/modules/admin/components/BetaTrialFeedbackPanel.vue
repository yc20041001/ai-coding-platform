<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import {
  listBetaFeedback, createBetaFeedback, updateBetaFeedback, deleteBetaFeedback,
  getBetaPassBlockSummary, getBetaSession,
  type BetaTrialFeedbackSummary, type BetaPassBlockSummary,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import { ElTag, ElButton, ElDialog, ElInput, ElSelect, ElOption, ElMessage, ElPopconfirm } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = withDefaults(defineProps<{
  sessionId?: string | null
  loading?: boolean
}>(), {
  sessionId: null,
  loading: false,
})

const emit = defineEmits<{
  dashboardRefresh: []
}>()

const feedbacks = ref<BetaTrialFeedbackSummary[]>([])
const passBlockSummary = ref<BetaPassBlockSummary | null>(null)
const loadingFeedback = ref(false)
const error = ref(false)
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const selectedSessionTitle = ref('')

const filterSeverity = ref('')
const filterTriage = ref('')

const createForm = ref({
  severity: 'P2', title: '', category: '', subcategory: '', detail: '',
  expectedBehavior: '', actualBehavior: '', suggestedAction: '', sourceType: 'MANUAL', releaseBlocking: false,
})

const editForm = ref({
  id: '', triageStatus: '', category: '', severity: '',
})

async function loadFeedback() {
  if (!props.sessionId) return
  loadingFeedback.value = true
  error.value = false
  try {
    const [fbRes, summaryRes, sessionRes] = await Promise.all([
      listBetaFeedback(props.sessionId, filterSeverity.value || undefined, filterTriage.value || undefined),
      getBetaPassBlockSummary(props.sessionId),
      getBetaSession(props.sessionId),
    ])
    feedbacks.value = fbRes.data.data
    passBlockSummary.value = summaryRes.data.data
    selectedSessionTitle.value = sessionRes.data.data.title
  } catch {
    error.value = true
  } finally {
    loadingFeedback.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.title || !props.sessionId) return
  try {
    await createBetaFeedback(props.sessionId, {
      severity: createForm.value.severity,
      title: createForm.value.title,
      category: createForm.value.category || undefined,
      subcategory: createForm.value.subcategory || undefined,
      detail: createForm.value.detail || undefined,
      expectedBehavior: createForm.value.expectedBehavior || undefined,
      actualBehavior: createForm.value.actualBehavior || undefined,
      suggestedAction: createForm.value.suggestedAction || undefined,
      sourceType: createForm.value.sourceType,
      releaseBlocking: createForm.value.releaseBlocking,
    })
    ElMessage.success('反馈已提交')
    createDialogVisible.value = false
    createForm.value = { severity: 'P2', title: '', category: '', subcategory: '', detail: '', expectedBehavior: '', actualBehavior: '', suggestedAction: '', sourceType: 'MANUAL', releaseBlocking: false }
    await loadFeedback()
    emit('dashboardRefresh')
  } catch {
    ElMessage.error('创建反馈失败')
  }
}

function startEdit(fb: BetaTrialFeedbackSummary) {
  editForm.value = {
    id: fb.id, triageStatus: fb.triageStatus, category: fb.category || '', severity: fb.severity,
  }
  editDialogVisible.value = true
}

async function handleEdit() {
  if (!editForm.value.id) return
  try {
    await updateBetaFeedback(editForm.value.id, {
      triageStatus: editForm.value.triageStatus || undefined,
      category: editForm.value.category || undefined,
      severity: editForm.value.severity || undefined,
    })
    ElMessage.success('反馈已更新')
    editDialogVisible.value = false
    await loadFeedback()
    emit('dashboardRefresh')
  } catch {
    ElMessage.error('更新失败')
  }
}

async function handleDelete(id: string) {
  try {
    await deleteBetaFeedback(id)
    ElMessage.success('反馈已删除')
    await loadFeedback()
    emit('dashboardRefresh')
  } catch {
    ElMessage.error('删除失败')
  }
}

function severityTag(s: string): 'info' | 'warning' | 'danger' {
  const map: Record<string, 'info' | 'warning' | 'danger'> = { P0: 'danger', P1: 'warning', P2: 'info', P3: 'info' }
  return map[s] || 'info'
}

function triageLabel(s: string) {
  const map: Record<string, string> = { NEW: '新建', TRIAGED: '已分类', SCHEDULED: '已排期', DONE: '已完成', WONT_FIX: '不修复' }
  return map[s] || s
}

onMounted(() => { loadFeedback() })
watch(() => props.sessionId, () => { loadFeedback() })
watch([filterSeverity, filterTriage], () => { loadFeedback() })
</script>

<template>
  <TechPanel
    v-loading="loadingFeedback || loading"
    :title="'Feedback — ' + (selectedSessionTitle || 'Beta Trial')"
    data-testid="beta-feedback-panel"
  >
    <ErrorState
      v-if="error"
      title="无法加载反馈"
      message="反馈加载失败"
    />
    <template v-else>
      <!-- Pass/Block Summary Tiles -->
      <div v-if="passBlockSummary" class="beta-summary-tiles">
        <MetricTile label="总反馈" :value="passBlockSummary.totalFeedback" />
        <MetricTile label="Release Blocking" :value="passBlockSummary.releaseBlockingCount" :accent="'danger'" />
        <MetricTile label="P0" :value="passBlockSummary.p0Count" :accent="'danger'" />
        <MetricTile label="P1" :value="passBlockSummary.p1Count" :accent="'warning'" />
        <MetricTile label="新建" :value="passBlockSummary.newCount" :accent="'warning'" />
        <MetricTile label="已分类" :value="passBlockSummary.triagedCount" />
        <MetricTile label="已排期" :value="passBlockSummary.scheduledCount" />
        <MetricTile label="已完成" :value="passBlockSummary.doneCount" :accent="'success'" />
        <MetricTile label="不修复" :value="passBlockSummary.wontFixCount" />
      </div>

      <NeonDivider />

      <div class="beta-toolbar">
        <ElButton size="small" type="primary" @click="createDialogVisible = true" :disabled="!sessionId">
          + 提交反馈
        </ElButton>
        <div class="beta-filters">
          <ElSelect v-model="filterSeverity" placeholder="严重程度" clearable size="small" style="width:110px">
            <ElOption label="P0" value="P0" />
            <ElOption label="P1" value="P1" />
            <ElOption label="P2" value="P2" />
            <ElOption label="P3" value="P3" />
          </ElSelect>
          <ElSelect v-model="filterTriage" placeholder="分类状态" clearable size="small" style="width:110px">
            <ElOption label="新建" value="NEW" />
            <ElOption label="已分类" value="TRIAGED" />
            <ElOption label="已排期" value="SCHEDULED" />
            <ElOption label="已完成" value="DONE" />
            <ElOption label="不修复" value="WONT_FIX" />
          </ElSelect>
        </div>
      </div>

      <div v-if="feedbacks.length === 0" class="beta-empty">
        <EmptyState title="暂无反馈" message="尚无 Beta 试用反馈" />
      </div>

      <div v-for="fb in feedbacks" :key="fb.id" class="beta-feedback-row">
        <div class="beta-feedback-info">
          <div class="beta-feedback-title-row">
            <ElTag :type="severityTag(fb.severity)" size="small">{{ fb.severity }}</ElTag>
            <span class="beta-fb-title">{{ fb.title }}</span>
            <ElTag v-if="fb.releaseBlocking" size="small" type="danger">Release Blocking</ElTag>
          </div>
          <div class="beta-fb-meta">
            {{ fb.category || '-' }} · {{ triageLabel(fb.triageStatus) }} · {{ formatDateTime(fb.createTime) }}
          </div>
        </div>
        <div class="beta-feedback-actions">
          <ElButton size="small" text @click="startEdit(fb)">分类</ElButton>
          <ElPopconfirm title="确认删除？" @confirm="handleDelete(fb.id)">
            <ElButton size="small" text type="danger">删除</ElButton>
          </ElPopconfirm>
        </div>
      </div>

      <!-- Create Feedback Dialog -->
      <ElDialog v-model="createDialogVisible" title="提交 Beta 反馈" width="550px" destroy-on-close>
        <div class="beta-fb-form">
          <div class="beta-form-row">
            <label>严重程度 *</label>
            <ElSelect v-model="createForm.severity">
              <ElOption label="P0 - 紧急" value="P0" />
              <ElOption label="P1 - 高" value="P1" />
              <ElOption label="P2 - 中" value="P2" />
              <ElOption label="P3 - 低" value="P3" />
            </ElSelect>
          </div>
          <div class="beta-form-row">
            <label>标题 *</label>
            <ElInput v-model="createForm.title" placeholder="反馈标题" />
          </div>
          <div class="beta-form-row">
            <label>分类</label>
            <ElSelect v-model="createForm.category" clearable>
              <ElOption label="缺陷" value="BUG" />
              <ElOption label="功能需求" value="FEATURE_REQUEST" />
              <ElOption label="性能" value="PERFORMANCE" />
              <ElOption label="文档" value="DOCUMENTATION" />
              <ElOption label="可用性" value="USABILITY" />
              <ElOption label="安全性" value="SECURITY" />
              <ElOption label="其他" value="OTHER" />
            </ElSelect>
          </div>
          <div class="beta-form-row">
            <label>子分类</label>
            <ElInput v-model="createForm.subcategory" placeholder="子分类（可选）" />
          </div>
          <div class="beta-form-row">
            <label>详情</label>
            <ElInput v-model="createForm.detail" type="textarea" :rows="3" placeholder="详细描述" />
          </div>
          <div class="beta-form-row">
            <ElSelect v-model="createForm.sourceType" style="width:200px">
              <ElOption label="手动录入" value="MANUAL" />
              <ElOption label="走查" value="WALKTHROUGH" />
              <ElOption label="冒烟测试" value="SMOKE_TEST" />
              <ElOption label="运营总结" value="OPERATOR_SUMMARY" />
            </ElSelect>
            <label style="display:flex;align-items:center;gap:6px;margin-left:16px">
              <input type="checkbox" v-model="createForm.releaseBlocking" />
              Release Blocking
            </label>
          </div>
        </div>
        <template #footer>
          <ElButton @click="createDialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleCreate" :disabled="!createForm.title">提交</ElButton>
        </template>
      </ElDialog>

      <!-- Edit Feedback Dialog -->
      <ElDialog v-model="editDialogVisible" title="分类反馈" width="400px" destroy-on-close>
        <div class="beta-fb-form">
          <div class="beta-form-row">
            <label>分类</label>
            <ElSelect v-model="editForm.category" clearable>
              <ElOption label="缺陷" value="BUG" />
              <ElOption label="功能需求" value="FEATURE_REQUEST" />
              <ElOption label="性能" value="PERFORMANCE" />
              <ElOption label="文档" value="DOCUMENTATION" />
              <ElOption label="可用性" value="USABILITY" />
              <ElOption label="安全性" value="SECURITY" />
              <ElOption label="其他" value="OTHER" />
            </ElSelect>
          </div>
          <div class="beta-form-row">
            <label>严重程度</label>
            <ElSelect v-model="editForm.severity">
              <ElOption label="P0" value="P0" />
              <ElOption label="P1" value="P1" />
              <ElOption label="P2" value="P2" />
              <ElOption label="P3" value="P3" />
            </ElSelect>
          </div>
          <div class="beta-form-row">
            <label>分类状态</label>
            <ElSelect v-model="editForm.triageStatus">
              <ElOption label="新建" value="NEW" />
              <ElOption label="已分类" value="TRIAGED" />
              <ElOption label="已排期" value="SCHEDULED" />
              <ElOption label="已完成" value="DONE" />
              <ElOption label="不修复" value="WONT_FIX" />
            </ElSelect>
          </div>
        </div>
        <template #footer>
          <ElButton @click="editDialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleEdit">保存</ElButton>
        </template>
      </ElDialog>
    </template>
  </TechPanel>
</template>

<style scoped>
.beta-summary-tiles {
  display: grid;
  grid-template-columns: repeat(9, 1fr);
  gap: 8px;
  margin-bottom: 8px;
}
.beta-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.beta-filters {
  display: flex;
  gap: 8px;
}
.beta-empty {
  padding: 16px 0;
}
.beta-feedback-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 8px 0;
  border-bottom: 1px solid var(--app-border);
}
.beta-feedback-row:last-child { border-bottom: none; }
.beta-feedback-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.beta-feedback-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.beta-fb-title {
  font-size: 13px;
  color: var(--app-text);
}
.beta-fb-meta {
  font-size: 11px;
  color: var(--app-text-secondary);
}
.beta-feedback-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
.beta-fb-form {
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
</style>
