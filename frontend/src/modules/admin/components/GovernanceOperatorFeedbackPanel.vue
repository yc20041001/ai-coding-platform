<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listFeedback, type GovernanceOperatorFeedbackItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<GovernanceOperatorFeedbackItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listFeedback().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
function reasonLabel(r: string | null) { if (!r) return ''; return { HELPFUL:'有用', TOO_GENERIC:'太泛', NOT_RELEVANT:'不相关', TOO_COMPLEX:'太复杂', MISSING_CONTEXT:'缺上下文', LOW_IMPACT:'低影响', GOOD_BUNDLE:'好Bundle', BAD_ORDERING:'排序差' }[r] || r }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Operator 反馈</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="f in items.slice(0, 8)" :key="f.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small">{{ f.feedbackTargetType }}</ElTag>
        <ElTag size="small" :type="f.helpfulFlag ? 'success' : 'info'">{{ f.helpfulFlag ? '有帮助' : '无帮助' }}</ElTag>
        <span style="color:var(--app-text-bright)">评分: {{ f.feedbackRating }}/5</span>
        <span v-if="f.reasonCode" style="color:var(--app-text-muted)">{{ reasonLabel(f.reasonCode) }}</span>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无反馈数据" />
    </div>
  </TechPanel>
</template>
