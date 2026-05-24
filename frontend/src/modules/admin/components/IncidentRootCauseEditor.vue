<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  ElTag,
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElSelect,
  ElOption,
  ElMessage,
  ElMessageBox,
  ElDialog,
} from 'element-plus'
import {
  getIncidentRootCauseNote,
  createRootCauseNote,
  updateRootCauseNote,
  exportRootCauseNoteMarkdown,
  applyKnownIssueTemplate,
  listProjectKnownIssueTemplates,
  type IncidentRootCauseNoteItem,
  type CreateIncidentRootCauseNoteRequest,
  type UpdateIncidentRootCauseNoteRequest,
  type KnownIssueTemplateItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  incidentId: string
  projectId?: string
}>()

const emit = defineEmits<{
  saved: []
}>()

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------
const loading = ref(false)
const error = ref(false)
const note = ref<IncidentRootCauseNoteItem | null>(null)
const isEditing = ref(false)

// ---------------------------------------------------------------------------
// Form
// ---------------------------------------------------------------------------
const form = ref({
  rootCause: '',
  impact: '',
  resolution: '',
  prevention: '',
  followUpActions: '',
  tags: '',
  confidence: 'MEDIUM',
})

const saving = ref(false)

// ---------------------------------------------------------------------------
// Templates
// ---------------------------------------------------------------------------
const noteProjectId = ref('')
const templates = ref<KnownIssueTemplateItem[]>([])
const templatesLoading = ref(false)
const selectedTemplateId = ref('')
const applyingTemplate = ref(false)

const effectiveProjectId = computed(() => props.projectId || noteProjectId.value || '')

// ---------------------------------------------------------------------------
// Markdown export
// ---------------------------------------------------------------------------
const markdownDialogVisible = ref(false)
const markdownContent = ref('')
const exportingMarkdown = ref(false)

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------
const confidenceOptions = [
  { value: 'LOW', label: '低' },
  { value: 'MEDIUM', label: '中' },
  { value: 'HIGH', label: '高' },
  { value: 'CONFIRMED', label: '已确认' },
]

const hasNote = computed(() => note.value !== null)

function statusTagType(status: string): 'info' | 'warning' | 'success' {
  const map: Record<string, 'info' | 'warning' | 'success'> = {
    DRAFT: 'info',
    REVIEWED: 'warning',
    PUBLISHED: 'success',
    ARCHIVED: 'info',
  }
  return map[status] || 'info'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    REVIEWED: '审核中',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档',
  }
  return map[status] || status
}

// ---------------------------------------------------------------------------
// Note loading
// ---------------------------------------------------------------------------
function populateFormFromNote() {
  if (!note.value) return
  form.value = {
    rootCause: note.value.rootCause || '',
    impact: note.value.impact || '',
    resolution: note.value.resolution || '',
    prevention: note.value.prevention || '',
    followUpActions: note.value.followUpActions || '',
    tags: note.value.tags || '',
    confidence: note.value.confidence || 'MEDIUM',
  }
}

async function loadNote() {
  loading.value = true
  error.value = false
  try {
    const res = await getIncidentRootCauseNote(props.incidentId)
    note.value = res.data.data
    noteProjectId.value = note.value.projectId
    populateFormFromNote()
    isEditing.value = false
  } catch (e: any) {
    if (e?.response?.status === 404) {
      note.value = null
      error.value = false
    } else {
      error.value = true
    }
  } finally {
    loading.value = false
  }
}

async function loadTemplates() {
  if (!effectiveProjectId.value) return
  templatesLoading.value = true
  try {
    const res = await listProjectKnownIssueTemplates(effectiveProjectId.value)
    templates.value = res.data.data
  } catch {
    // Non-critical
  } finally {
    templatesLoading.value = false
  }
}

// ---------------------------------------------------------------------------
// Actions
// ---------------------------------------------------------------------------
function startEditing() {
  if (note.value) populateFormFromNote()
  isEditing.value = true
}

function cancelEditing() {
  if (note.value) {
    populateFormFromNote()
    isEditing.value = false
  } else {
    form.value = {
      rootCause: '',
      impact: '',
      resolution: '',
      prevention: '',
      followUpActions: '',
      tags: '',
      confidence: 'MEDIUM',
    }
    isEditing.value = false
  }
}

async function handleSave() {
  if (!form.value.rootCause.trim()) {
    ElMessage.warning('请输入根因分析')
    return
  }

  saving.value = true
  try {
    const data = {
      rootCause: form.value.rootCause || undefined,
      impact: form.value.impact || undefined,
      resolution: form.value.resolution || undefined,
      prevention: form.value.prevention || undefined,
      followUpActions: form.value.followUpActions || undefined,
      tags: form.value.tags || undefined,
      confidence: form.value.confidence,
    }

    if (note.value) {
      const res = await updateRootCauseNote(note.value.id, data)
      note.value = res.data.data
    } else {
      const res = await createRootCauseNote(props.incidentId, data)
      note.value = res.data.data
      noteProjectId.value = note.value.projectId
      await loadTemplates()
    }

    isEditing.value = false
    ElMessage.success('保存成功')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleCreateNew() {
  form.value = {
    rootCause: '',
    impact: '',
    resolution: '',
    prevention: '',
    followUpActions: '',
    tags: '',
    confidence: 'MEDIUM',
  }
  isEditing.value = true
}

async function confirmStatusTransition(newStatus: string) {
  const messages: Record<string, string> = {
    REVIEWED: '确认提交审核？',
    PUBLISHED: '确认发布？',
    ARCHIVED: '确认归档？',
    DRAFT: '确认返回草稿？',
  }
  try {
    await ElMessageBox.confirm(messages[newStatus] || '确认操作？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info',
    })
    await handleStatusTransition(newStatus)
  } catch {
    // User cancelled
  }
}

async function handleStatusTransition(newStatus: string) {
  if (!note.value) return
  saving.value = true
  try {
    const res = await updateRootCauseNote(note.value.id, { status: newStatus })
    note.value = res.data.data
    ElMessage.success('状态已更新')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '状态更新失败')
  } finally {
    saving.value = false
  }
}

async function handleExportMarkdown() {
  if (!note.value) return
  exportingMarkdown.value = true
  try {
    const res = await exportRootCauseNoteMarkdown(note.value.id)
    markdownContent.value = res.data.data
    markdownDialogVisible.value = true
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '导出失败')
  } finally {
    exportingMarkdown.value = false
  }
}

async function handleCopyMarkdown() {
  try {
    await window.navigator.clipboard.writeText(markdownContent.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

async function handleApplyTemplate() {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请选择一个模板')
    return
  }
  applyingTemplate.value = true
  try {
    const res = await applyKnownIssueTemplate(props.incidentId, selectedTemplateId.value)
    note.value = res.data.data
    noteProjectId.value = note.value.projectId
    populateFormFromNote()
    isEditing.value = true
    ElMessage.success('模板已应用')
    selectedTemplateId.value = ''
    await loadTemplates()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '应用模板失败')
  } finally {
    applyingTemplate.value = false
  }
}

// ---------------------------------------------------------------------------
// Lifecycle & watchers
// ---------------------------------------------------------------------------
onMounted(() => {
  loadNote()
})

watch(effectiveProjectId, (val) => {
  if (val) loadTemplates()
})
</script>

<template>
  <TechPanel
    v-loading="loading"
    title="根因分析"
    class="incident-root-cause-editor"
    data-testid="incident-root-cause-editor"
  >
    <!-- Error -->
    <ErrorState
      v-if="error && !loading"
      title="加载失败"
      message="无法加载根因分析数据"
      retry-text="重试"
      @retry="loadNote"
    />

    <!-- Empty state: no note yet -->
    <div
      v-else-if="!hasNote && !isEditing && !loading"
      class="rce-empty"
      data-testid="rce-empty"
    >
      <EmptyState
        title="暂无分析"
        description="尚未创建根因分析记录"
      />
      <ElButton
        type="primary"
        size="small"
        @click="handleCreateNew"
        data-testid="rce-create-btn"
      >
        创建分析
      </ElButton>
    </div>

    <!-- Note content (viewing or editing) -->
    <template v-if="(hasNote || isEditing) && !loading">
      <!-- Status bar (viewing only) -->
      <div
        v-if="hasNote && !isEditing"
        class="rce-status-bar"
        data-testid="rce-status-bar"
      >
        <ElTag
          :type="statusTagType(note!.status)"
          size="small"
          effect="dark"
          data-testid="rce-status-tag"
        >
          {{ statusLabel(note!.status) }}
        </ElTag>

        <div class="rce-status-actions">
          <ElButton
            v-if="note!.status === 'DRAFT'"
            size="small"
            type="primary"
            @click="confirmStatusTransition('REVIEWED')"
            data-testid="rce-submit-btn"
          >
            提交审核
          </ElButton>
          <ElButton
            v-if="note!.status === 'REVIEWED'"
            size="small"
            type="success"
            @click="confirmStatusTransition('PUBLISHED')"
            data-testid="rce-publish-btn"
          >
            发布
          </ElButton>
          <ElButton
            v-if="note!.status === 'REVIEWED'"
            size="small"
            @click="confirmStatusTransition('DRAFT')"
            data-testid="rce-draft-btn"
          >
            返回草稿
          </ElButton>
          <ElButton
            v-if="note!.status === 'PUBLISHED'"
            size="small"
            @click="confirmStatusTransition('ARCHIVED')"
            data-testid="rce-archive-btn"
          >
            归档
          </ElButton>
        </div>
      </div>

      <!-- Template section -->
      <div
        v-if="effectiveProjectId && templates.length > 0"
        class="rce-template-section"
        data-testid="rce-template-section"
      >
        <span class="rce-section-label">应用模板</span>
        <div class="rce-template-row">
          <ElSelect
            v-model="selectedTemplateId"
            placeholder="选择已知问题模板"
            size="small"
            :loading="templatesLoading"
            style="width: 280px"
            data-testid="rce-template-select"
          >
            <ElOption
              v-for="tpl in templates"
              :key="tpl.id"
              :label="tpl.title"
              :value="tpl.id"
            />
          </ElSelect>
          <ElButton
            size="small"
            type="primary"
            :loading="applyingTemplate"
            :disabled="!selectedTemplateId"
            @click="handleApplyTemplate"
            data-testid="rce-apply-template-btn"
          >
            应用
          </ElButton>
        </div>
      </div>

      <!-- Viewing mode: fields as read-only -->
      <template v-if="!isEditing && hasNote">
        <div class="rce-field" data-testid="rce-field-root-cause">
          <span class="rce-field-label">根因分析</span>
          <div class="rce-field-value">{{ note!.rootCause || '-' }}</div>
        </div>
        <div class="rce-field-divider" />
        <div class="rce-field" data-testid="rce-field-impact">
          <span class="rce-field-label">影响范围</span>
          <div class="rce-field-value">{{ note!.impact || '-' }}</div>
        </div>
        <div class="rce-field-divider" />
        <div class="rce-field" data-testid="rce-field-resolution">
          <span class="rce-field-label">解决方案</span>
          <div class="rce-field-value">{{ note!.resolution || '-' }}</div>
        </div>
        <div class="rce-field-divider" />
        <div class="rce-field" data-testid="rce-field-prevention">
          <span class="rce-field-label">预防措施</span>
          <div class="rce-field-value">{{ note!.prevention || '-' }}</div>
        </div>
        <div class="rce-field-divider" />
        <div class="rce-field" data-testid="rce-field-followup">
          <span class="rce-field-label">后续行动</span>
          <div class="rce-field-value">{{ note!.followUpActions || '-' }}</div>
        </div>
        <div class="rce-field-divider" />
        <div class="rce-field" data-testid="rce-field-tags">
          <span class="rce-field-label">标签</span>
          <div class="rce-field-value">{{ note!.tags || '-' }}</div>
        </div>
        <div class="rce-field-divider" />
        <div class="rce-field" data-testid="rce-field-confidence">
          <span class="rce-field-label">置信度</span>
          <div class="rce-field-value">{{ note!.confidence }}</div>
        </div>
        <div class="rce-field-divider" />
        <div class="rce-timestamps">
          <span class="rce-timestamp">创建时间：{{ formatDateTime(note!.createTime) }}</span>
          <span class="rce-timestamp">更新时间：{{ formatDateTime(note!.updateTime) }}</span>
          <span v-if="note!.publishedAt" class="rce-timestamp">发布时间：{{ formatDateTime(note!.publishedAt) }}</span>
        </div>

        <div class="rce-view-actions" data-testid="rce-view-actions">
          <ElButton
            type="primary"
            size="small"
            @click="startEditing"
            data-testid="rce-edit-btn"
          >
            编辑
          </ElButton>
          <ElButton
            size="small"
            :loading="exportingMarkdown"
            @click="handleExportMarkdown"
            data-testid="rce-export-btn"
          >
            导出 Markdown
          </ElButton>
        </div>
      </template>

      <!-- Editing mode: form fields -->
      <template v-else-if="isEditing">
        <ElForm
          :model="form"
          label-position="top"
          size="small"
          class="rce-form"
          data-testid="rce-form"
        >
          <ElFormItem label="根因分析" data-testid="rce-form-root-cause">
            <ElInput
              v-model="form.rootCause"
              type="textarea"
              :rows="4"
              placeholder="描述问题的根本原因"
            />
          </ElFormItem>
          <ElFormItem label="影响范围" data-testid="rce-form-impact">
            <ElInput
              v-model="form.impact"
              type="textarea"
              :rows="3"
              placeholder="描述事件造成的影响"
            />
          </ElFormItem>
          <ElFormItem label="解决方案" data-testid="rce-form-resolution">
            <ElInput
              v-model="form.resolution"
              type="textarea"
              :rows="3"
              placeholder="描述已采取的解决方案"
            />
          </ElFormItem>
          <ElFormItem label="预防措施" data-testid="rce-form-prevention">
            <ElInput
              v-model="form.prevention"
              type="textarea"
              :rows="3"
              placeholder="描述如何防止再次发生"
            />
          </ElFormItem>
          <ElFormItem label="后续行动" data-testid="rce-form-followup">
            <ElInput
              v-model="form.followUpActions"
              type="textarea"
              :rows="3"
              placeholder="列出后续需要执行的行动项"
            />
          </ElFormItem>
          <ElFormItem label="标签" data-testid="rce-form-tags">
            <ElInput
              v-model="form.tags"
              placeholder="逗号分隔的标签，如：网络,超时"
            />
          </ElFormItem>
          <ElFormItem label="置信度" data-testid="rce-form-confidence">
            <ElSelect
              v-model="form.confidence"
              placeholder="选择置信度"
              data-testid="rce-confidence-select"
            >
              <ElOption
                v-for="opt in confidenceOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </ElSelect>
          </ElFormItem>
        </ElForm>

        <div class="rce-edit-actions" data-testid="rce-edit-actions">
          <ElButton
            type="primary"
            size="small"
            :loading="saving"
            @click="handleSave"
            data-testid="rce-save-btn"
          >
            保存
          </ElButton>
          <ElButton
            size="small"
            :disabled="saving"
            @click="cancelEditing"
            data-testid="rce-cancel-btn"
          >
            取消
          </ElButton>
        </div>
      </template>
    </template>
  </TechPanel>

  <!-- Markdown export dialog -->
  <ElDialog
    v-model="markdownDialogVisible"
    title="Markdown 导出"
    width="700px"
    :close-on-click-modal="false"
    destroy-on-close
    data-testid="rce-markdown-dialog"
  >
    <div class="rce-markdown-header">
      <ElButton
        size="small"
        @click="handleCopyMarkdown"
        data-testid="rce-markdown-copy-btn"
      >
        复制
      </ElButton>
    </div>
    <textarea
      class="rce-markdown-content"
      :value="markdownContent"
      readonly
      rows="20"
      data-testid="rce-markdown-textarea"
    />
    <template #footer>
      <ElButton @click="markdownDialogVisible = false">关闭</ElButton>
    </template>
  </ElDialog>
</template>

<style scoped>
.incident-root-cause-editor {
  position: relative;
}

/* ------------------------------------------------------------------ */
/* Empty state                                                         */
/* ------------------------------------------------------------------ */
.rce-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

/* ------------------------------------------------------------------ */
/* Status bar                                                          */
/* ------------------------------------------------------------------ */
.rce-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  margin-bottom: 16px;
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  flex-wrap: wrap;
}

.rce-status-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* ------------------------------------------------------------------ */
/* Template section                                                    */
/* ------------------------------------------------------------------ */
.rce-template-section {
  margin-bottom: 16px;
  padding: 12px 14px;
  background: rgba(15, 23, 42, 0.3);
  border: 1px dashed var(--app-border);
  border-radius: 6px;
}

.rce-section-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-soft);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 8px;
}

.rce-template-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ------------------------------------------------------------------ */
/* Read-only fields                                                    */
/* ------------------------------------------------------------------ */
.rce-field {
  padding: 10px 0;
}

.rce-field-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 6px;
}

.rce-field-value {
  font-size: 13px;
  color: var(--app-text-bright);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.rce-field-divider {
  height: 1px;
  background: var(--app-border);
  opacity: 0.5;
}

.rce-timestamps {
  display: flex;
  gap: 20px;
  padding: 10px 0;
  flex-wrap: wrap;
}

.rce-timestamp {
  font-size: 11px;
  color: var(--app-text-muted);
}

/* ------------------------------------------------------------------ */
/* Action buttons                                                      */
/* ------------------------------------------------------------------ */
.rce-view-actions,
.rce-edit-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--app-border);
}

.rce-view-actions {
  margin-top: 8px;
}

.rce-edit-actions {
  margin-top: 4px;
}

/* ------------------------------------------------------------------ */
/* Form                                                                */
/* ------------------------------------------------------------------ */
.rce-form {
  margin-bottom: 4px;
}

/* ------------------------------------------------------------------ */
/* Markdown dialog                                                     */
/* ------------------------------------------------------------------ */
.rce-markdown-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.rce-markdown-content {
  width: 100%;
  padding: 12px;
  font-family: 'SF Mono', 'Fira Code', 'Fira Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-text-bright);
  background: var(--app-bg-card);
  border: 1px solid var(--app-border);
  border-radius: 6px;
  resize: vertical;
  outline: none;
  box-sizing: border-box;
}

.rce-markdown-content:focus {
  border-color: var(--el-color-primary);
}
</style>
