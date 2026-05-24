<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  ElTag,
  ElButton,
  ElMessage,
  ElMessageBox,
  ElTable,
  ElTableColumn,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElSelect,
  ElOption,
  ElSwitch,
} from 'element-plus'
import {
  listProjectKnownIssueTemplates,
  createKnownIssueTemplate,
  updateKnownIssueTemplate,
  type KnownIssueTemplateItem,
  type CreateKnownIssueTemplateRequest,
  type UpdateKnownIssueTemplateRequest,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId: string
}>()

const emit = defineEmits<{
  'template-applied': [template: KnownIssueTemplateItem]
}>()

const loading = ref(false)
const error = ref(false)
const templates = ref<KnownIssueTemplateItem[]>([])
const saving = ref(false)
const showDialog = ref(false)
const editingTemplate = ref<KnownIssueTemplateItem | null>(null)
const filterCategory = ref('')

const categoryOptions = [
  { value: 'TOOL_POLICY', label: '工具策略' },
  { value: 'READ_ONLY_ADAPTER', label: '只读适配器' },
  { value: 'CODE_INDEX', label: '代码索引' },
  { value: 'WORKER_QUEUE', label: '工作队列' },
  { value: 'RABBITMQ', label: 'RabbitMQ' },
  { value: 'REDIS', label: 'Redis' },
  { value: 'MODEL_GATEWAY', label: '模型网关' },
  { value: 'GITHUB', label: 'GitHub' },
  { value: 'RAG', label: 'RAG' },
  { value: 'FRONTEND', label: '前端' },
  { value: 'CONFIGURATION', label: '配置' },
  { value: 'UNKNOWN', label: '未知' },
]

const severityOptions = [
  { value: 'CRITICAL', label: '严重' },
  { value: 'HIGH', label: '高' },
  { value: 'MEDIUM', label: '中' },
  { value: 'LOW', label: '低' },
  { value: 'INFO', label: '提示' },
]

const form = ref<CreateKnownIssueTemplateRequest>({
  title: '',
  category: undefined,
  severity: undefined,
  rootCauseTemplate: '',
  impactTemplate: '',
  resolutionTemplate: '',
  preventionTemplate: '',
  tags: '',
})

const isEditing = computed(() => !!editingTemplate.value)
const dialogTitle = computed(() => (isEditing.value ? '编辑已知问题模板' : '新建已知问题模板'))

function resetForm() {
  form.value = {
    title: '',
    category: undefined,
    severity: undefined,
    rootCauseTemplate: '',
    impactTemplate: '',
    resolutionTemplate: '',
    preventionTemplate: '',
    tags: '',
  }
}

function openCreateDialog() {
  editingTemplate.value = null
  resetForm()
  showDialog.value = true
}

function openEditDialog(template: KnownIssueTemplateItem) {
  editingTemplate.value = template
  form.value = {
    title: template.title,
    category: template.category ?? undefined,
    severity: template.severity ?? undefined,
    rootCauseTemplate: template.rootCauseTemplate ?? '',
    impactTemplate: template.impactTemplate ?? '',
    resolutionTemplate: template.resolutionTemplate ?? '',
    preventionTemplate: template.preventionTemplate ?? '',
    tags: template.tags ?? '',
  }
  showDialog.value = true
}

function closeDialog() {
  showDialog.value = false
  editingTemplate.value = null
}

const filteredTemplates = computed(() => {
  if (!filterCategory.value) return templates.value
  return templates.value.filter((t) => t.category === filterCategory.value)
})

async function loadTemplates() {
  loading.value = true
  error.value = false
  try {
    const params: { category?: string } = {}
    if (filterCategory.value) {
      params.category = filterCategory.value
    }
    const res = await listProjectKnownIssueTemplates(props.projectId, params)
    templates.value = res.data.data
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入模板标题')
    return
  }

  saving.value = true
  try {
    if (isEditing.value && editingTemplate.value) {
      const updateData: UpdateKnownIssueTemplateRequest = {
        title: form.value.title.trim(),
        category: form.value.category || undefined,
        severity: form.value.severity || undefined,
        rootCauseTemplate: form.value.rootCauseTemplate || undefined,
        impactTemplate: form.value.impactTemplate || undefined,
        resolutionTemplate: form.value.resolutionTemplate || undefined,
        preventionTemplate: form.value.preventionTemplate || undefined,
        tags: form.value.tags || undefined,
      }
      await updateKnownIssueTemplate(editingTemplate.value.id, updateData)
      ElMessage.success('模板已更新')
    } else {
      const createData: CreateKnownIssueTemplateRequest = {
        title: form.value.title.trim(),
        category: form.value.category || undefined,
        severity: form.value.severity || undefined,
        rootCauseTemplate: form.value.rootCauseTemplate || undefined,
        impactTemplate: form.value.impactTemplate || undefined,
        resolutionTemplate: form.value.resolutionTemplate || undefined,
        preventionTemplate: form.value.preventionTemplate || undefined,
        tags: form.value.tags || undefined,
      }
      await createKnownIssueTemplate(props.projectId, createData)
      ElMessage.success('模板已创建')
    }
    closeDialog()
    await loadTemplates()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || (isEditing.value ? '更新失败' : '创建失败'))
  } finally {
    saving.value = false
  }
}

async function handleToggleEnabled(template: KnownIssueTemplateItem) {
  try {
    await updateKnownIssueTemplate(template.id, { enabled: !template.enabled })
    template.enabled = !template.enabled
    ElMessage.success(template.enabled ? '模板已启用' : '模板已禁用')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function handleDeleteTemplate(template: KnownIssueTemplateItem) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${template.title}」？`, '确认删除', { type: 'warning' })
    await updateKnownIssueTemplate(template.id, { enabled: false })
    ElMessage.success('模板已禁用')
    await loadTemplates()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '操作失败')
    }
  }
}

function handleApplyTemplate(template: KnownIssueTemplateItem) {
  emit('template-applied', template)
}

function categoryLabel(category: string | null): string {
  if (!category) return '-'
  const opt = categoryOptions.find((o) => o.value === category)
  return opt ? opt.label : category
}

function severityTagType(severity: string | null): 'danger' | 'warning' | 'info' | 'success' {
  const map: Record<string, 'danger' | 'warning' | 'info' | 'success'> = {
    CRITICAL: 'danger',
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'info',
    INFO: 'info',
  }
  return map[severity ?? ''] || 'info'
}

function severityLabel(severity: string | null): string {
  if (!severity) return '-'
  const opt = severityOptions.find((o) => o.value === severity)
  return opt ? opt.label : severity
}

onMounted(() => {
  loadTemplates()
})
</script>

<template>
  <TechPanel
    v-loading="loading"
    title="已知问题模板 (Known Issue Templates)"
    data-testid="known-issue-template-panel"
  >
    <ErrorState
      v-if="error"
      title="加载失败"
      message="无法加载已知问题模板"
      retry-text="重试"
      @retry="loadTemplates"
    />

    <!-- Toolbar -->
    <div class="kit-toolbar">
      <div class="kit-toolbar-left">
        <ElSelect
          v-model="filterCategory"
          size="small"
          placeholder="分类筛选"
          clearable
          style="width:140px"
          @change="loadTemplates"
          data-testid="kit-category-filter"
        >
          <ElOption
            v-for="opt in categoryOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </ElSelect>
      </div>
      <div class="kit-toolbar-right">
        <ElButton
          size="small"
          type="primary"
          @click="openCreateDialog"
          data-testid="kit-add-btn"
        >
          新建模板
        </ElButton>
      </div>
    </div>

    <!-- Templates table -->
    <ElTable
      v-if="filteredTemplates.length > 0"
      :data="filteredTemplates"
      size="small"
      style="width:100%"
      data-testid="kit-table"
    >
      <ElTableColumn label="启用" width="60">
        <template #default="{ row }">
          <ElSwitch
            :model-value="row.enabled"
            size="small"
            @click.stop
            @change="handleToggleEnabled(row)"
            data-testid="kit-toggle"
          />
        </template>
      </ElTableColumn>
      <ElTableColumn label="标题" min-width="160" prop="title" show-overflow-tooltip />
      <ElTableColumn label="分类" width="120">
        <template #default="{ row }">
          <ElTag size="small">{{ categoryLabel(row.category) }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="严重级别" width="90">
        <template #default="{ row }">
          <ElTag
            v-if="row.severity"
            :type="severityTagType(row.severity)"
            size="small"
            effect="dark"
          >
            {{ severityLabel(row.severity) }}
          </ElTag>
          <span v-else>-</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="标签" width="140">
        <template #default="{ row }">
          <div v-if="row.tags" class="kit-tags">
            <ElTag
              v-for="tag in row.tags.split(',')"
              :key="tag"
              size="small"
              type="info"
              style="margin-right:4px;margin-bottom:2px"
            >
              {{ tag.trim() }}
            </ElTag>
          </div>
          <span v-else>-</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="创建时间" width="150">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <ElButton
            size="small"
            type="primary"
            link
            @click="openEditDialog(row)"
            data-testid="kit-edit-btn"
          >
            编辑
          </ElButton>
          <ElButton
            size="small"
            type="success"
            link
            @click="handleApplyTemplate(row)"
            data-testid="kit-apply-btn"
          >
            应用
          </ElButton>
          <ElButton
            size="small"
            type="danger"
            link
            @click="handleDeleteTemplate(row)"
            data-testid="kit-delete-btn"
          >
            删除
          </ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <EmptyState
      v-if="!error && !loading && filteredTemplates.length === 0"
      :description="filterCategory ? '当前分类暂无模板' : '暂无已知问题模板'"
    />
  </TechPanel>

  <!-- Create / Edit Dialog -->
  <ElDialog
    :model-value="showDialog"
    @update:model-value="closeDialog"
    :title="dialogTitle"
    width="640px"
    :close-on-click-modal="false"
    data-testid="kit-dialog"
  >
    <ElForm
      :model="form"
      label-position="top"
      size="small"
      data-testid="kit-form"
    >
      <div class="kit-form-row">
        <ElFormItem
          label="标题"
          required
          style="flex:1"
          data-testid="kit-form-title"
        >
          <ElInput
            v-model="form.title"
            placeholder="模板标题"
            data-testid="kit-title-input"
          />
        </ElFormItem>
      </div>
      <div class="kit-form-row kit-form-row--selects">
        <ElFormItem label="分类" style="flex:1" data-testid="kit-form-category">
          <ElSelect
            v-model="form.category"
            placeholder="选择分类"
            clearable
            data-testid="kit-category-select"
          >
            <ElOption
              v-for="opt in categoryOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="严重级别" style="flex:1" data-testid="kit-form-severity">
          <ElSelect
            v-model="form.severity"
            placeholder="选择级别"
            clearable
            data-testid="kit-severity-select"
          >
            <ElOption
              v-for="opt in severityOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </ElSelect>
        </ElFormItem>
      </div>
      <ElFormItem label="标签" data-testid="kit-form-tags">
        <ElInput
          v-model="form.tags"
          placeholder="用英文逗号分隔，例如：database,timeout"
          data-testid="kit-tags-input"
        />
      </ElFormItem>
      <ElFormItem label="根因模板" data-testid="kit-form-root-cause">
        <ElInput
          v-model="form.rootCauseTemplate"
          type="textarea"
          :rows="3"
          placeholder="根因分析模板文本"
          data-testid="kit-root-cause-input"
        />
      </ElFormItem>
      <ElFormItem label="影响模板" data-testid="kit-form-impact">
        <ElInput
          v-model="form.impactTemplate"
          type="textarea"
          :rows="3"
          placeholder="影响范围模板文本"
          data-testid="kit-impact-input"
        />
      </ElFormItem>
      <ElFormItem label="解决方案模板" data-testid="kit-form-resolution">
        <ElInput
          v-model="form.resolutionTemplate"
          type="textarea"
          :rows="3"
          placeholder="解决方案模板文本"
          data-testid="kit-resolution-input"
        />
      </ElFormItem>
      <ElFormItem label="预防措施模板" data-testid="kit-form-prevention">
        <ElInput
          v-model="form.preventionTemplate"
          type="textarea"
          :rows="3"
          placeholder="预防措施模板文本"
          data-testid="kit-prevention-input"
        />
      </ElFormItem>
    </ElForm>

    <template #footer>
      <ElButton
        size="small"
        @click="closeDialog"
        data-testid="kit-cancel-btn"
      >
        取消
      </ElButton>
      <ElButton
        size="small"
        type="primary"
        :loading="saving"
        @click="handleSave"
        data-testid="kit-save-btn"
      >
        {{ isEditing ? '更新' : '创建' }}
      </ElButton>
    </template>
  </ElDialog>
</template>

<style scoped>
.kit-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 8px;
}

.kit-toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.kit-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.kit-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.kit-form-row {
  display: flex;
  gap: 12px;
}

.kit-form-row--selects {
  display: flex;
  gap: 12px;
}
</style>
