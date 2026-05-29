<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listSimulationScenarios, createSimulationScenario, updateSimulationScenarioStatus, runSimulationScenario, type GovernanceSimulationScenarioItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption } from 'element-plus'

const items = ref<GovernanceSimulationScenarioItem[]>([]); const loading = ref(false); const error = ref(false)
const dialogVisible = ref(false); const running = ref<string | null>(null)
const form = ref({ scenarioName: '', scenarioType: 'SLA_TUNING', inputJson: '{}', notes: '' })

function loadData() { loading.value = true; error.value = false; listSimulationScenarios().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function openCreate() { form.value = { scenarioName: '', scenarioType: 'SLA_TUNING', inputJson: '{}', notes: '' }; dialogVisible.value = true }
async function handleSave() { try { await createSimulationScenario(form.value); ElMessage.success('已创建'); dialogVisible.value = false; loadData() } catch { ElMessage.error('创建失败') } }
async function handleRun(id: string) { running.value = id; try { await runSimulationScenario(id); ElMessage.success('已运行'); loadData() } catch { ElMessage.error('运行失败') } finally { running.value = null } }
async function handleStatus(id: string, status: string) { try { await updateSimulationScenarioStatus(id, status); ElMessage.success('状态已更新'); loadData() } catch { ElMessage.error('操作失败') } }
function typeLabel(t: string) { const m: Record<string, string> = { SLA_TUNING: 'SLA调优', OWNER_REBALANCING: 'Owner重平衡', WAIVER_REDUCTION: 'Waiver缩减', POLICY_THRESHOLD_TUNING: '策略阈值' }; return m[t] || t }
function statusTag(s: string) { if (s === 'DRAFT') return 'info' as const; if (s === 'READY') return 'warning' as const; if (s === 'SIMULATED') return 'success' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">模拟场景</span><ElButton size="small" type="primary" @click="openCreate">新建</ElButton></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="s in items" :key="s.id" style="padding:8px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.06)">
        <div style="display:flex;align-items:center;justify-content:space-between">
          <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">
            <span style="font-weight:600;font-size:13px;color:var(--app-text-bright)">{{ s.scenarioName }}</span>
            <ElTag size="small">{{ typeLabel(s.scenarioType) }}</ElTag>
            <ElTag size="small" :type="statusTag(s.scenarioStatus)">{{ s.scenarioStatus }}</ElTag>
          </div>
          <div style="display:flex;gap:4px">
            <ElButton v-if="s.scenarioStatus === 'DRAFT'" size="small" link @click="handleStatus(s.id, 'READY')">就绪</ElButton>
            <ElButton v-if="s.scenarioStatus === 'READY' || s.scenarioStatus === 'SIMULATED'" size="small" link :loading="running === s.id" @click="handleRun(s.id)">运行</ElButton>
            <ElButton v-if="s.scenarioStatus === 'SIMULATED'" size="small" link @click="handleStatus(s.id, 'ARCHIVED')">归档</ElButton>
          </div>
        </div>
        <div style="font-size:11px;color:var(--app-text-muted);margin-top:2px">{{ s.notes || '' }}</div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading" description="暂无模拟场景" />
    </div>
    <ElDialog v-model="dialogVisible" title="新建场景" width="400px" destroy-on-close>
      <ElForm label-position="top" size="small">
        <ElFormItem label="名称"><ElInput v-model="form.scenarioName" /></ElFormItem>
        <ElFormItem label="类型"><ElSelect v-model="form.scenarioType"><ElOption label="SLA调优" value="SLA_TUNING" /><ElOption label="Owner重平衡" value="OWNER_REBALANCING" /><ElOption label="Waiver缩减" value="WAIVER_REDUCTION" /><ElOption label="策略阈值" value="POLICY_THRESHOLD_TUNING" /></ElSelect></ElFormItem>
        <ElFormItem label="输入(JSON)"><ElInput v-model="form.inputJson" :rows="2" type="textarea" /></ElFormItem>
        <ElFormItem label="备注"><ElInput v-model="form.notes" :rows="2" type="textarea" /></ElFormItem>
      </ElForm>
      <template #footer><ElButton @click="dialogVisible = false">取消</ElButton><ElButton type="primary" @click="handleSave">创建</ElButton></template>
    </ElDialog>
  </TechPanel>
</template>
