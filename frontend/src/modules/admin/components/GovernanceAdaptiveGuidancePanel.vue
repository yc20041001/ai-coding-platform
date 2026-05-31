<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listAdaptiveSignals, type GovernanceAdaptiveGuidanceSignalItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<GovernanceAdaptiveGuidanceSignalItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listAdaptiveSignals().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function levelTag(l: string) { if (l === 'BOOST') return 'success' as const; if (l === 'DOWNRANK') return 'danger' as const; if (l === 'WATCH') return 'warning' as const; return 'info' as const }
function typeLabel(t: string) { return { SUGGESTION_TYPE_WEIGHT:'建议类型权重', FOCUS_MODE_WEIGHT:'聚焦模式权重', CATEGORY_WEIGHT:'类别权重', BUNDLE_REUSE_SIGNAL:'Bundle复用', DISMISSAL_RISK_SIGNAL:'忽略风险' }[t]||t }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">自适应引导</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="s in items" :key="s.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px">
        <div style="display:flex;align-items:center;gap:6px">
          <ElTag size="small" :type="levelTag(s.signalLevel)" effect="dark">{{ s.signalLevel }}</ElTag>
          <ElTag size="small">{{ typeLabel(s.signalType) }}</ElTag>
          <span style="color:var(--app-text-bright)">{{ s.suggestionType || s.focusMode || '-' }}</span>
          <span style="color:var(--app-text-muted)">权重: {{ s.weightScore }}</span>
        </div>
        <div style="color:var(--app-text-muted);margin-top:2px;font-size:11px">{{ s.rationaleText }}</div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无自适应信号" />
    </div>
  </TechPanel>
</template>
