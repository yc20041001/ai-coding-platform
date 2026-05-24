<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElTag, ElButton, ElInput, ElSelect, ElOption, ElMessage, ElCollapse, ElCollapseItem } from 'element-plus'
import { getIncidentRetrospective, updateRetrospective, createRetrospectiveDraft, checkRegression, type IncidentRetrospectiveItem, type SimilarIncidentRegressionCheck } from '@/modules/admin/api'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  incidentId: string
}>()

const emit = defineEmits<{
  saved: []
}>()

const loading = ref(false)
const saving = ref(false)
const retrospective = ref<IncidentRetrospectiveItem | null>(null)
const regressionCheck = ref<SimilarIncidentRegressionCheck | null>(null)
const editing = ref(false)
const activeTab = ref('edit')

const editForm = ref({
  title: '',
  summary: '',
  whatHappened: '',
  impactSummary: '',
  responseSummary: '',
  lessonsLearned: '',
  preventionPlan: '',
  actionItems: '',
  regressionRisk: '',
  repeatedIncident: false,
})

const statusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'REVIEWED', label: '已审查' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'ARCHIVED', label: '已归档' },
]

const riskOptions = [
  { value: 'LOW', label: '低' },
  { value: 'MEDIUM', label: '中' },
  { value: 'HIGH', label: '高' },
  { value: 'CRITICAL', label: '严重' },
]

function statusTag(status: string) {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = { DRAFT: 'info', REVIEWED: 'warning', PUBLISHED: 'success', ARCHIVED: 'info' }
  return map[status] || 'info' as const
}

function statusText(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', REVIEWED: '已审查', PUBLISHED: '已发布', ARCHIVED: '已归档' }
  return map[status] || status
}

function riskTag(risk: string) {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = { LOW: 'info', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }
  return map[risk] || 'info' as const
}

async function loadRetrospective() {
  loading.value = true
  try {
    const res = await getIncidentRetrospective(props.incidentId)
    retrospective.value = res.data.data
    editForm.value = {
      title: res.data.data.title || '',
      summary: res.data.data.summary || '',
      whatHappened: res.data.data.whatHappened || '',
      impactSummary: res.data.data.impactSummary || '',
      responseSummary: res.data.data.responseSummary || '',
      lessonsLearned: res.data.data.lessonsLearned || '',
      preventionPlan: res.data.data.preventionPlan || '',
      actionItems: res.data.data.actionItems || '',
      regressionRisk: res.data.data.regressionRisk || 'LOW',
      repeatedIncident: res.data.data.repeatedIncident || false,
    }
  } catch {
    retrospective.value = null
  } finally {
    loading.value = false
  }
}

async function loadRegressionCheck() {
  try {
    const res = await checkRegression(props.incidentId)
    regressionCheck.value = res.data.data
  } catch {
    regressionCheck.value = null
  }
}

async function handleCreateDraft() {
  saving.value = true
  try {
    const res = await createRetrospectiveDraft(props.incidentId)
    retrospective.value = res.data.data
    editForm.value = {
      title: res.data.data.title || '',
      summary: res.data.data.summary || '',
      whatHappened: res.data.data.whatHappened || '',
      impactSummary: res.data.data.impactSummary || '',
      responseSummary: res.data.data.responseSummary || '',
      lessonsLearned: res.data.data.lessonsLearned || '',
      preventionPlan: res.data.data.preventionPlan || '',
      actionItems: res.data.data.actionItems || '',
      regressionRisk: res.data.data.regressionRisk || 'LOW',
      repeatedIncident: res.data.data.repeatedIncident || false,
    }
    editing.value = true
    ElMessage.success('草稿创建成功')
    emit('saved')
    loadRegressionCheck()
  } catch {
    ElMessage.error('创建草稿失败')
  } finally {
    saving.value = false
  }
}

async function handleSave() {
  if (!retrospective.value) return
  saving.value = true
  try {
    const res = await updateRetrospective(retrospective.value.id, editForm.value)
    retrospective.value = res.data.data
    editing.value = false
    ElMessage.success('保存成功')
    emit('saved')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleStatusTransition(status: string) {
  if (!retrospective.value) return
  saving.value = true
  try {
    const res = await updateRetrospective(retrospective.value.id, { status })
    retrospective.value = res.data.data
    ElMessage.success('状态更新成功')
    emit('saved')
  } catch {
    ElMessage.error('状态更新失败')
  } finally {
    saving.value = false
  }
}

function startEditing() {
  if (!retrospective.value) return
  editForm.value = {
    title: retrospective.value.title || '',
    summary: retrospective.value.summary || '',
    whatHappened: retrospective.value.whatHappened || '',
    impactSummary: retrospective.value.impactSummary || '',
    responseSummary: retrospective.value.responseSummary || '',
    lessonsLearned: retrospective.value.lessonsLearned || '',
    preventionPlan: retrospective.value.preventionPlan || '',
    actionItems: retrospective.value.actionItems || '',
    regressionRisk: retrospective.value.regressionRisk || 'LOW',
    repeatedIncident: retrospective.value.repeatedIncident || false,
  }
  editing.value = true
}

function cancelEditing() {
  editing.value = false
  if (retrospective.value) {
    editForm.value = {
      title: retrospective.value.title || '',
      summary: retrospective.value.summary || '',
      whatHappened: retrospective.value.whatHappened || '',
      impactSummary: retrospective.value.impactSummary || '',
      responseSummary: retrospective.value.responseSummary || '',
      lessonsLearned: retrospective.value.lessonsLearned || '',
      preventionPlan: retrospective.value.preventionPlan || '',
      actionItems: retrospective.value.actionItems || '',
      regressionRisk: retrospective.value.regressionRisk || 'LOW',
      repeatedIncident: retrospective.value.repeatedIncident || false,
    }
  }
}

watch(() => props.incidentId, () => {
  loadRetrospective()
  loadRegressionCheck()
}, { immediate: true })
</script>

<template>
  <div class="retro-editor" data-testid="retrospective-editor">
    <div v-if="loading" class="retro-loading">加载中...</div>

    <div v-else-if="!retrospective" class="retro-empty">
      <p>暂无事后回顾报告</p>
      <div class="retro-actions">
        <ElButton type="primary" :loading="saving" @click="handleCreateDraft" data-testid="create-retro-draft-btn">
          创建草稿
        </ElButton>
      </div>
    </div>

    <template v-else>
      <div class="retro-header">
        <div class="retro-title-row">
          <span class="retro-title">{{ retrospective.title }}</span>
          <ElTag :type="statusTag(retrospective.status)" size="small" effect="dark">
            {{ statusText(retrospective.status) }}
          </ElTag>
          <ElTag v-if="retrospective.repeatedIncident" type="danger" size="small">重复事件</ElTag>
          <ElTag :type="riskTag(retrospective.regressionRisk)" size="small">
            回归风险: {{ retrospective.regressionRisk }}
          </ElTag>
        </div>
        <div class="retro-meta">
          <span>创建: {{ formatDateTime(retrospective.createTime) }}</span>
          <span v-if="retrospective.publishedAt">发布: {{ formatDateTime(retrospective.publishedAt) }}</span>
        </div>
      </div>

      <ElCollapse v-model="activeTab">
        <ElCollapseItem title="编辑内容" name="edit">
          <div class="retro-field" v-if="editing">
            <label class="retro-label">标题</label>
            <ElInput v-model="editForm.title" placeholder="报告标题" />
          </div>

          <div class="retro-field">
            <label class="retro-label">发生了什么</label>
            <ElInput
              v-if="editing"
              v-model="editForm.whatHappened"
              type="textarea"
              :rows="3"
              placeholder="事件经过描述"
            />
            <p v-else class="retro-text">{{ retrospective.whatHappened || '暂无' }}</p>
          </div>

          <div class="retro-field">
            <label class="retro-label">影响总结</label>
            <ElInput
              v-if="editing"
              v-model="editForm.impactSummary"
              type="textarea"
              :rows="3"
              placeholder="事件影响范围与程度"
            />
            <p v-else class="retro-text">{{ retrospective.impactSummary || '暂无' }}</p>
          </div>

          <div class="retro-field">
            <label class="retro-label">响应总结</label>
            <ElInput
              v-if="editing"
              v-model="editForm.responseSummary"
              type="textarea"
              :rows="3"
              placeholder="事件响应过程总结"
            />
            <p v-else class="retro-text">{{ retrospective.responseSummary || '暂无' }}</p>
          </div>

          <div class="retro-field">
            <label class="retro-label">经验教训</label>
            <ElInput
              v-if="editing"
              v-model="editForm.lessonsLearned"
              type="textarea"
              :rows="3"
              placeholder="从事件中汲取的经验教训"
            />
            <p v-else class="retro-text">{{ retrospective.lessonsLearned || '暂无' }}</p>
          </div>

          <div class="retro-field">
            <label class="retro-label">预防措施</label>
            <ElInput
              v-if="editing"
              v-model="editForm.preventionPlan"
              type="textarea"
              :rows="2"
              placeholder="预防措施计划"
            />
            <p v-else class="retro-text">{{ retrospective.preventionPlan || '暂无' }}</p>
          </div>

          <div class="retro-field">
            <label class="retro-label">行动事项</label>
            <ElInput
              v-if="editing"
              v-model="editForm.actionItems"
              type="textarea"
              :rows="2"
              placeholder="后续行动事项"
            />
            <p v-else class="retro-text">{{ retrospective.actionItems || '暂无' }}</p>
          </div>

          <div class="retro-field" v-if="editing">
            <label class="retro-label">回归风险</label>
            <ElSelect v-model="editForm.regressionRisk" style="width:200px">
              <ElOption v-for="opt in riskOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </ElSelect>
          </div>

          <div class="retro-actions" v-if="editing">
            <ElButton type="primary" :loading="saving" @click="handleSave" data-testid="save-retro-btn">保存</ElButton>
            <ElButton @click="cancelEditing">取消</ElButton>
          </div>
          <div class="retro-actions" v-else>
            <ElButton v-if="retrospective.status !== 'ARCHIVED'" type="primary" @click="startEditing" data-testid="edit-retro-btn">
              编辑
            </ElButton>
          </div>
        </ElCollapseItem>

        <ElCollapseItem title="状态流转" name="status">
          <div class="retro-status-actions">
            <template v-if="retrospective.status === 'DRAFT'">
              <ElButton type="warning" :loading="saving" @click="handleStatusTransition('REVIEWED')">标记已审查</ElButton>
              <ElButton type="info" :loading="saving" @click="handleStatusTransition('ARCHIVED')">归档</ElButton>
            </template>
            <template v-else-if="retrospective.status === 'REVIEWED'">
              <ElButton type="success" :loading="saving" @click="handleStatusTransition('PUBLISHED')">发布</ElButton>
              <ElButton :loading="saving" @click="handleStatusTransition('DRAFT')">退回草稿</ElButton>
            </template>
            <template v-else-if="retrospective.status === 'PUBLISHED'">
              <ElButton type="info" :loading="saving" @click="handleStatusTransition('ARCHIVED')">归档</ElButton>
            </template>
            <span v-else class="retro-archived-note">已归档，不可变更状态</span>
          </div>
        </ElCollapseItem>

        <ElCollapseItem title="回归检查" name="regression">
          <div v-if="regressionCheck" class="retro-regression">
            <div class="retro-regression-item">
              <span>重复事件:</span>
              <ElTag :type="regressionCheck.repeatedIncident ? 'danger' : 'info'" size="small">
                {{ regressionCheck.repeatedIncident ? '是' : '否' }}
              </ElTag>
            </div>
            <div class="retro-regression-item">
              <span>回归风险:</span>
              <ElTag :type="riskTag(regressionCheck.regressionRisk)" size="small">{{ regressionCheck.regressionRisk }}</ElTag>
            </div>
            <div class="retro-regression-item">
              <span>最高相似度: {{ (regressionCheck.highestScore * 100).toFixed(0) }}%</span>
            </div>
            <div class="retro-regression-item">
              <span>相似事件数: {{ regressionCheck.similarCount }}</span>
            </div>
          </div>
          <ElButton v-else size="small" type="primary" @click="loadRegressionCheck">执行回归检查</ElButton>
        </ElCollapseItem>
      </ElCollapse>
    </template>
  </div>
</template>

<style scoped>
.retro-editor {
  padding: 4px 0;
}

.retro-loading, .retro-empty {
  padding: 20px;
  text-align: center;
  color: var(--app-text-muted);
  font-size: 13px;
}

.retro-header {
  margin-bottom: 12px;
}

.retro-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.retro-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--app-text-bright);
}

.retro-meta {
  font-size: 11px;
  color: var(--app-text-muted);
  display: flex;
  gap: 16px;
}

.retro-field {
  margin-bottom: 12px;
}

.retro-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-soft);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.retro-text {
  font-size: 13px;
  color: var(--app-text);
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
}

.retro-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.retro-status-actions {
  display: flex;
  gap: 8px;
  padding: 8px 0;
}

.retro-archived-note {
  font-size: 12px;
  color: var(--app-text-muted);
}

.retro-regression-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--app-text);
}
</style>
