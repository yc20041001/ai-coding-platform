<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listPlaybookTemplates, createPlaybookTemplate, updatePlaybookTemplateStatus, type GovernancePlaybookTemplateItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption } from 'element-plus'

const items = ref<GovernancePlaybookTemplateItem[]>([]); const loading = ref(false); const error = ref(false); const dialogVisible = ref(false)
const form = ref({ templateKey: '', displayName: '', recommendationCategory: '', priority: '', templateStepsJson: '[{"stepKey":"s1","title":"步骤1","status":"TODO","required":true}]' })
function loadData() { loading.value = true; error.value = false; listPlaybookTemplates().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function openCreate() { form.value = { templateKey: '', displayName: '', recommendationCategory: 'CONFIDENCE', priority: 'P1', templateStepsJson: '[{"stepKey":"s1","title":"步骤1","status":"TODO","required":true}]' }; dialogVisible.value = true }
async function handleSave() { try { await createPlaybookTemplate(form.value); ElMessage.success('已创建'); dialogVisible.value = false; loadData() } catch { ElMessage.error('创建失败') } }
async function toggleStatus(t: GovernancePlaybookTemplateItem) { try { await updatePlaybookTemplateStatus(t.id, !t.enabled); ElMessage.success(t.enabled ? '已禁用' : '已启用'); loadData() } catch { ElMessage.error('操作失败') } }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Playbook 模板</span><ElButton size="small" type="primary" @click="openCreate">新建</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="t in items" :key="t.id" style="padding:8px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">
            <span style="font-weight:600;font-size:13px;color:var(--app-text-bright)">{{ t.displayName }}</span>
            <ElTag size="small">{{ t.recommendationCategory || '-' }}</ElTag>
            <ElTag size="small" :type="t.enabled ? 'success' : 'info'">{{ t.enabled ? '启用' : '禁用' }}</ElTag>
          </div>
          <ElButton size="small" link @click="toggleStatus(t)">{{ t.enabled ? '禁用' : '启用' }}</ElButton>
        </div>
        <div style="font-size:11px;color:var(--app-text-muted);margin-top:2px">Key: {{ t.templateKey }} | 优先级: {{ t.priority || '-' }}</div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无模板" />
    </div>
    <ElDialog v-model="dialogVisible" title="新建模板" width="450px" destroy-on-close>
      <ElForm label-position="top" size="small">
        <ElFormItem label="Key"><ElInput v-model="form.templateKey" /></ElFormItem>
        <ElFormItem label="名称"><ElInput v-model="form.displayName" /></ElFormItem>
        <ElFormItem label="类别"><ElInput v-model="form.recommendationCategory" /></ElFormItem>
        <ElFormItem label="优先级"><ElInput v-model="form.priority" /></ElFormItem>
        <ElFormItem label="步骤(JSON)"><ElInput v-model="form.templateStepsJson" :rows="3" type="textarea" /></ElFormItem>
      </ElForm>
      <template #footer><ElButton @click="dialogVisible = false">取消</ElButton><ElButton type="primary" @click="handleSave">创建</ElButton></template>
    </ElDialog>
  </TechPanel>
</template>
