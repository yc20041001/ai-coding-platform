<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listWorkspaceSessions } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElTag } from 'element-plus'
const items = ref<any[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listWorkspaceSessions().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">Operator 记忆</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="s in items.slice(0, 8)" :key="s.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px">
        <span style="color:var(--app-text-bright)">{{ s.operatorName || 'Admin' }}</span>
        <ElTag size="small">{{ s.sessionStatus }}</ElTag>
        <ElTag size="small">{{ s.focusMode }}</ElTag>
        <span style="color:var(--app-text-muted);margin-left:auto">{{ s.startedAt?.substring(0, 10) }}</span>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无行为记忆数据" />
    </div>
  </TechPanel>
</template>
