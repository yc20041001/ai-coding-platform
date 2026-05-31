<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listRecipes, createRecipe, updateRecipeStatus, type GovernanceRemediationRecipeItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElDialog, ElForm, ElFormItem, ElInput } from 'element-plus'

const items = ref<GovernanceRemediationRecipeItem[]>([]); const loading = ref(false); const error = ref(false); const dialogVisible = ref(false)
const form = ref({ recipeKey: '', displayName: '', recipeType: 'REMEDIATION', recommendationCategory: '', stepsJson: '[{"stepKey":"s1","title":"步骤1","status":"TODO"}]' })
function loadData() { loading.value = true; error.value = false; listRecipes().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function openCreate() { form.value = { recipeKey: '', displayName: '', recipeType: 'REMEDIATION', recommendationCategory: '', stepsJson: '[{"stepKey":"s1","title":"步骤1","status":"TODO"}]' }; dialogVisible.value = true }
async function handleSave() { try { await createRecipe(form.value.recipeKey, form.value.displayName, form.value.recipeType, form.value.recommendationCategory, undefined, form.value.stepsJson); ElMessage.success('已创建'); dialogVisible.value = false; loadData() } catch { ElMessage.error('创建失败') } }
async function toggleStatus(r: GovernanceRemediationRecipeItem) { try { await updateRecipeStatus(r.id, !r.enabled); ElMessage.success(r.enabled ? '已禁用' : '已启用'); loadData() } catch { ElMessage.error('操作失败') } }
function typeLabel(t: string) { return { REMEDIATION: '修复', WAIVER_MITIGATION: 'Waiver缓解', HANDOFF_SUPPORT: '交接支持', ESCALATION_RESPONSE: '升级响应' }[t] || t }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Recipe 库</span><ElButton size="small" type="primary" @click="openCreate">新建</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="r in items" :key="r.id" style="padding:8px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;justify-content:space-between"><div style="display:flex;align-items:center;gap:6px"><span style="font-weight:600;font-size:13px;color:var(--app-text-bright)">{{ r.displayName }}</span><ElTag size="small">{{ typeLabel(r.recipeType) }}</ElTag><ElTag size="small" :type="r.enabled ? 'success' : 'info'">{{ r.enabled ? '启用' : '禁用' }}</ElTag></div><ElButton size="small" link @click="toggleStatus(r)">{{ r.enabled ? '禁用' : '启用' }}</ElButton></div>
        <div style="font-size:11px;color:var(--app-text-muted);margin-top:2px">效果: {{ r.effectivenessScore }} | 使用: {{ r.usageCount }}次</div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无 Recipe" />
    </div>
    <ElDialog v-model="dialogVisible" title="新建 Recipe" width="450px" destroy-on-close>
      <ElForm label-position="top" size="small"><ElFormItem label="Key"><ElInput v-model="form.recipeKey" /></ElFormItem><ElFormItem label="名称"><ElInput v-model="form.displayName" /></ElFormItem><ElFormItem label="类型"><ElInput v-model="form.recipeType" /></ElFormItem></ElForm>
      <template #footer><ElButton @click="dialogVisible = false">取消</ElButton><ElButton type="primary" @click="handleSave">创建</ElButton></template>
    </ElDialog>
  </TechPanel>
</template>
