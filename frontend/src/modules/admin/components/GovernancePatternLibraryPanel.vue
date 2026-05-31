<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listPatterns, createPattern, updatePatternStatus, type GovernancePatternLibraryItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElDialog, ElForm, ElFormItem, ElInput } from 'element-plus'

const items = ref<GovernancePatternLibraryItem[]>([]); const loading = ref(false); const error = ref(false); const dialogVisible = ref(false)
const form = ref({ patternKey: '', displayName: '', recommendationCategory: 'CONFIDENCE', guardrailKey: 'MIN_CONFIDENCE_SCORE', priority: 'P1', patternJson: '{}' })
function loadData() { loading.value = true; error.value = false; listPatterns().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function openCreate() { form.value = { patternKey: '', displayName: '', recommendationCategory: 'CONFIDENCE', guardrailKey: 'MIN_CONFIDENCE_SCORE', priority: 'P1', patternJson: '{}' }; dialogVisible.value = true }
async function handleSave() { try { await createPattern(form.value.patternKey, form.value.displayName, form.value.recommendationCategory, form.value.guardrailKey, form.value.priority, form.value.patternJson); ElMessage.success('已创建'); dialogVisible.value = false; loadData() } catch { ElMessage.error('创建失败') } }
async function toggleStatus(p: GovernancePatternLibraryItem) { try { await updatePatternStatus(p.id, !p.enabled); ElMessage.success(p.enabled ? '已禁用' : '已启用'); loadData() } catch { ElMessage.error('操作失败') } }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">模式库</span><ElButton size="small" type="primary" @click="openCreate">新建</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="p in items" :key="p.id" style="padding:8px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;justify-content:space-between"><div style="display:flex;align-items:center;gap:6px"><span style="font-weight:600;font-size:13px;color:var(--app-text-bright)">{{ p.displayName }}</span><ElTag size="small">{{ p.recommendationCategory || '-' }}</ElTag><ElTag size="small" :type="p.enabled ? 'success' : 'info'">{{ p.enabled ? '启用' : '禁用' }}</ElTag></div><ElButton size="small" link @click="toggleStatus(p)">{{ p.enabled ? '禁用' : '启用' }}</ElButton></div>
        <div style="font-size:11px;color:var(--app-text-muted);margin-top:2px">Key: {{ p.patternKey }} | Guardrail: {{ p.guardrailKey || '-' }}</div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无模式" />
    </div>
    <ElDialog v-model="dialogVisible" title="新建模式" width="450px" destroy-on-close>
      <ElForm label-position="top" size="small"><ElFormItem label="Key"><ElInput v-model="form.patternKey" /></ElFormItem><ElFormItem label="名称"><ElInput v-model="form.displayName" /></ElFormItem><ElFormItem label="类别"><ElInput v-model="form.recommendationCategory" /></ElFormItem></ElForm>
      <template #footer><ElButton @click="dialogVisible = false">取消</ElButton><ElButton type="primary" @click="handleSave">创建</ElButton></template>
    </ElDialog>
  </TechPanel>
</template>
