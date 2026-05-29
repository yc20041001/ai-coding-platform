<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listSlaPolicies, createSlaPolicy, updateSlaPolicyStatus, type GovernanceSlaPolicyItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption } from 'element-plus'

const policies = ref<GovernanceSlaPolicyItem[]>([]); const loading = ref(false); const error = ref(false)
const dialogVisible = ref(false)
const form = ref({ policyKey: '', displayName: '', priority: 'P0', slaHours: 24, warningHours: 12, notes: '' })

function loadData() {
  loading.value = true; error.value = false
  listSlaPolicies().then(res => { policies.value = res.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false })
}
function openCreate() { form.value = { policyKey: '', displayName: '', priority: 'P0', slaHours: 24, warningHours: 12, notes: '' }; dialogVisible.value = true }
async function handleSave() {
  try {
    await createSlaPolicy({ ...form.value, slaHours: form.value.slaHours, warningHours: form.value.warningHours })
    ElMessage.success('SLA 策略已创建'); dialogVisible.value = false; loadData()
  } catch { ElMessage.error('创建失败') }
}
async function toggleStatus(p: GovernanceSlaPolicyItem) {
  try { await updateSlaPolicyStatus(p.id, !p.enabled); ElMessage.success(p.enabled ? '已禁用' : '已启用'); loadData() } catch { ElMessage.error('操作失败') }
}
onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">SLA 策略</span>
      <ElButton size="small" type="primary" @click="openCreate">新建</ElButton>
    </div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="p in policies" :key="p.id" style="padding:10px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <div style="display:flex;align-items:center;gap:6px">
            <span style="font-weight:600;font-size:13px;color:var(--app-text-bright)">{{ p.displayName }}</span>
            <ElTag size="small">{{ p.priority }}</ElTag>
            <span style="font-size:11px;color:var(--app-text-muted)">SLA: {{ p.slaHours }}h / Warn: {{ p.warningHours }}h</span>
            <ElTag size="small" :type="p.enabled ? 'success' : 'info'">{{ p.enabled ? '启用' : '禁用' }}</ElTag>
          </div>
          <ElButton size="small" link @click="toggleStatus(p)">{{ p.enabled ? '禁用' : '启用' }}</ElButton>
        </div>
      </div>
      <EmptyState v-if="!loading && policies.length === 0 && !error" description="暂无 SLA 策略" />
    </div>
    <ElDialog v-model="dialogVisible" title="新建 SLA 策略" width="450px" destroy-on-close>
      <ElForm label-position="top" size="small">
        <ElFormItem label="策略 Key"><ElInput v-model="form.policyKey" /></ElFormItem>
        <ElFormItem label="名称"><ElInput v-model="form.displayName" /></ElFormItem>
        <ElFormItem label="优先级"><ElSelect v-model="form.priority"><ElOption label="P0" value="P0" /><ElOption label="P1" value="P1" /><ElOption label="P2" value="P2" /><ElOption label="P3" value="P3" /></ElSelect></ElFormItem>
        <ElFormItem label="SLA 小时"><ElInput v-model.number="form.slaHours" type="number" /></ElFormItem>
        <ElFormItem label="警告小时"><ElInput v-model.number="form.warningHours" type="number" /></ElFormItem>
        <ElFormItem label="备注"><ElInput v-model="form.notes" :rows="2" type="textarea" /></ElFormItem>
      </ElForm>
      <template #footer><ElButton @click="dialogVisible = false">取消</ElButton><ElButton type="primary" @click="handleSave">保存</ElButton></template>
    </ElDialog>
  </TechPanel>
</template>
