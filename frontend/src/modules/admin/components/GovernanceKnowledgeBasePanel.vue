<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listKnowledgeEntries, createKnowledgeEntry, searchKnowledgeEntries, type GovernanceKnowledgeEntryItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElDialog, ElForm, ElFormItem, ElInput } from 'element-plus'

const items = ref<GovernanceKnowledgeEntryItem[]>([]); const loading = ref(false); const error = ref(false)
const keyword = ref(''); const dialogVisible = ref(false)
const form = ref({ title: '', category: 'CONFIDENCE', sourceType: 'RECOMMENDATION', summaryText: '', detailMarkdown: '' })
function loadData() { loading.value = true; error.value = false; (keyword.value ? searchKnowledgeEntries(keyword.value) : listKnowledgeEntries()).then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function openCreate() { form.value = { title: '', category: 'CONFIDENCE', sourceType: 'RECOMMENDATION', summaryText: '', detailMarkdown: '' }; dialogVisible.value = true }
async function handleSave() { try { await createKnowledgeEntry(form.value.title, form.value.category, form.value.sourceType, form.value.summaryText, form.value.detailMarkdown); ElMessage.success('已创建'); dialogVisible.value = false; loadData() } catch { ElMessage.error('创建失败') } }
function scoreTag(s: number) { if (s >= 80) return 'success' as const; if (s >= 50) return 'warning' as const; return 'info' as const }
function sourceLabel(t: string) { return { RECOMMENDATION: '建议', EXECUTION_PLAN: '执行', HANDOFF: '交接', WAIVER: 'Waiver', PLAYBOOK: 'Playbook' }[t] || t }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">知识库</span><ElButton size="small" type="primary" @click="openCreate">新建</ElButton></div>
    <div style="display:flex;gap:8px;margin-bottom:10px"><input v-model="keyword" placeholder="搜索..." style="flex:1;background:rgba(255,255,255,0.06);border:1px solid rgba(255,255,255,0.1);border-radius:4px;padding:4px 8px;color:var(--app-text-bright);font-size:12px" @keyup.enter="loadData" /><ElButton size="small" @click="loadData">搜索</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="e in items" :key="e.id" style="padding:8px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap"><ElTag size="small">{{ sourceLabel(e.sourceType) }}</ElTag><ElTag size="small">{{ e.category }}</ElTag><ElTag size="small" :type="scoreTag(e.effectivenessScore)">{{ e.effectivenessScore }}</ElTag><span style="font-weight:500;font-size:13px;color:var(--app-text-bright)">{{ e.title }}</span></div>
        <div style="font-size:11px;color:var(--app-text-muted);margin-top:2px">{{ e.summaryText }}<span v-if="e.reuseCount && e.reuseCount > 0" style="margin-left:8px">复用: {{ e.reuseCount }}次</span></div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无知识条目" />
    </div>
    <ElDialog v-model="dialogVisible" title="新建知识" width="450px" destroy-on-close>
      <ElForm label-position="top" size="small"><ElFormItem label="标题"><ElInput v-model="form.title" /></ElFormItem><ElFormItem label="类别"><ElInput v-model="form.category" /></ElFormItem><ElFormItem label="来源"><ElInput v-model="form.sourceType" /></ElFormItem><ElFormItem label="摘要"><ElInput v-model="form.summaryText" :rows="2" type="textarea" /></ElFormItem></ElForm>
      <template #footer><ElButton @click="dialogVisible = false">取消</ElButton><ElButton type="primary" @click="handleSave">创建</ElButton></template>
    </ElDialog>
  </TechPanel>
</template>
