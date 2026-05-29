<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  listBaselineTemplates,
  createBaselineTemplate,
  updateBaselineTemplate,
  updateBaselineTemplateStatus,
  type GovernanceBaselineTemplateItem,
  type CreateGovernanceBaselineTemplateRequest,
  type UpdateGovernanceBaselineTemplateRequest,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag, ElButton, ElMessage, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElSwitch } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const templates = ref<GovernanceBaselineTemplateItem[]>([])
const loading = ref(false)
const error = ref(false)

// Create dialog
const showCreate = ref(false)
const creating = ref(false)
const createForm = ref<CreateGovernanceBaselineTemplateRequest>({
  templateKey: '',
  displayName: '',
  templateScope: 'GLOBAL',
  notes: '',
})

// Edit dialog
const showEdit = ref(false)
const editing = ref(false)
const editForm = ref<UpdateGovernanceBaselineTemplateRequest>({})
const editId = ref<string>('')

function loadData() {
  loading.value = true
  error.value = false
  listBaselineTemplates()
    .then(res => { templates.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleCreate() {
  creating.value = true
  try {
    await createBaselineTemplate(createForm.value)
    ElMessage.success('模板已创建')
    showCreate.value = false
    createForm.value = { templateKey: '', displayName: '', templateScope: 'GLOBAL', notes: '' }
    loadData()
  } catch {
    ElMessage.error('创建失败')
  } finally {
    creating.value = false
  }
}

function openEdit(tpl: GovernanceBaselineTemplateItem) {
  editId.value = tpl.id || ''
  editForm.value = {
    displayName: tpl.displayName,
    templateScope: tpl.templateScope,
    notes: tpl.notes,
  }
  showEdit.value = true
}

async function handleEdit() {
  editing.value = true
  try {
    await updateBaselineTemplate(editId.value, editForm.value)
    ElMessage.success('模板已更新')
    showEdit.value = false
    loadData()
  } catch {
    ElMessage.error('更新失败')
  } finally {
    editing.value = false
  }
}

async function handleToggleStatus(tpl: GovernanceBaselineTemplateItem) {
  try {
    await updateBaselineTemplateStatus(tpl.id || '', !tpl.enabled)
    ElMessage.success(tpl.enabled ? '模板已停用' : '模板已启用')
    loadData()
  } catch {
    ElMessage.error('状态变更失败')
  }
}

function scopeTag(scope: string) {
  const map: Record<string, 'success' | 'warning' | 'info'> = {
    GLOBAL: 'success',
    PROJECT_TYPE: 'warning',
    PROJECT_OVERRIDE: 'info',
  }
  return map[scope] || 'info'
}

onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">治理基线模板</span>
      <ElButton size="small" type="primary" @click="showCreate = true">新建模板</ElButton>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取基线模板" retry-text="重试" @retry="loadData" />

    <div v-if="templates.length === 0 && !loading && !error">
      <EmptyState description="暂无基线模板" />
    </div>

    <div v-if="templates.length > 0" v-loading="loading">
      <div v-for="(tpl, idx) in templates" :key="tpl.id ?? 'tpl-' + idx" style="padding:8px 0;border-bottom:1px solid rgba(56,189,248,0.05);font-size:12px">
        <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px">
          <span style="font-weight:600;color:var(--app-text-bright)">{{ tpl.displayName }}</span>
          <ElTag size="small" :type="scopeTag(tpl.templateScope)" effect="dark">{{ tpl.templateScope }}</ElTag>
          <ElTag size="small" :type="tpl.enabled ? 'success' : 'info'">{{ tpl.enabled ? '已启用' : '已停用' }}</ElTag>
        </div>
        <div style="font-size:11px;color:var(--app-text-muted);margin-bottom:4px">
          Key: {{ tpl.templateKey }}
          <span v-if="tpl.notes"> | {{ tpl.notes }}</span>
        </div>
        <div style="display:flex;gap:8px">
          <ElButton size="small" text @click="openEdit(tpl)">编辑</ElButton>
          <ElButton size="small" text @click="handleToggleStatus(tpl)">{{ tpl.enabled ? '停用' : '启用' }}</ElButton>
        </div>
      </div>
    </div>

    <!-- Create Dialog -->
    <ElDialog v-model="showCreate" title="新建基线模板" width="480px">
      <ElForm label-position="top" size="small">
        <ElFormItem label="模板 Key" required>
          <ElInput v-model="createForm.templateKey" placeholder="如: default-rollback" />
        </ElFormItem>
        <ElFormItem label="显示名称" required>
          <ElInput v-model="createForm.displayName" placeholder="如: 默认回滚模板" />
        </ElFormItem>
        <ElFormItem label="作用范围">
          <ElSelect v-model="createForm.templateScope">
            <ElOption label="全局" value="GLOBAL" />
            <ElOption label="项目类型" value="PROJECT_TYPE" />
            <ElOption label="项目覆盖" value="PROJECT_OVERRIDE" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput v-model="createForm.notes" type="textarea" :rows="3" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="showCreate = false">取消</ElButton>
        <ElButton type="primary" :loading="creating" @click="handleCreate">创建</ElButton>
      </template>
    </ElDialog>

    <!-- Edit Dialog -->
    <ElDialog v-model="showEdit" title="编辑基线模板" width="480px">
      <ElForm label-position="top" size="small">
        <ElFormItem label="显示名称">
          <ElInput v-model="editForm.displayName" />
        </ElFormItem>
        <ElFormItem label="作用范围">
          <ElSelect v-model="editForm.templateScope">
            <ElOption label="全局" value="GLOBAL" />
            <ElOption label="项目类型" value="PROJECT_TYPE" />
            <ElOption label="项目覆盖" value="PROJECT_OVERRIDE" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput v-model="editForm.notes" type="textarea" :rows="3" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="showEdit = false">取消</ElButton>
        <ElButton type="primary" :loading="editing" @click="handleEdit">保存</ElButton>
      </template>
    </ElDialog>
  </TechPanel>
</template>
