<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { createWorkspaceSession, getWorkspaceNextSteps, type GovernanceNextStepItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag, ElButton } from 'element-plus'

const steps = ref<GovernanceNextStepItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; createWorkspaceSession().then(s => getWorkspaceNextSteps(s.data.data.id)).then(r => { steps.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function typeLabel(t: string) { return { OPEN_PLAYBOOK:'打开Playbook', OPEN_RECIPE:'打开Recipe', OPEN_KNOWLEDGE:'查看知识', START_HANDOFF:'开始交接', REVIEW_WAIVER:'审查Waiver', REVIEW_FORECAST:'检查预测' }[t]||t }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">下一步</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="s in steps" :key="s.id" style="margin-bottom:8px">
        <div style="padding:10px;background:rgba(255,255,255,0.03);border-radius:6px;border:1px solid rgba(255,255,255,0.08);border-left:3px solid var(--app-accent)">
          <div style="display:flex;align-items:center;gap:6px;margin-bottom:4px"><ElTag size="small">{{ typeLabel(s.suggestionType) }}</ElTag><span style="font-weight:500;font-size:13px;color:var(--app-text-bright)">{{ s.title }}</span></div>
          <div v-if="s.rationaleText" style="font-size:11px;color:var(--app-text-muted)">{{ s.rationaleText }}</div>
          <div v-if="s.expectedOutcomeText" style="font-size:11px;color:var(--color-success);margin-top:2px">预期: {{ s.expectedOutcomeText }}</div>
        </div>
      </div>
      <EmptyState v-if="steps.length === 0 && !loading && !error" description="暂无下一步建议" />
    </div>
  </TechPanel>
</template>
