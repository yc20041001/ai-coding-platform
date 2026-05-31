<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listRecommendationPackages, updatePackageStatus, type GovernanceRecommendationPackageItem } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTag } from 'element-plus'
const items = ref<GovernanceRecommendationPackageItem[]>([]); const loading = ref(false); const error = ref(false)
function loadData() { loading.value = true; error.value = false; listRecommendationPackages().then(r => { items.value = r.data.data }).catch(() => { error.value = true }).finally(() => { loading.value = false }) }
async function updateStatus(id: string, s: string) { try { await updatePackageStatus(id, s); loadData() } catch { ElMessage.error('操作失败') } }
function statusTag(s: string) { if (s === 'READY') return 'success' as const; if (s === 'REVIEWED') return 'primary' as const; if (s === 'ARCHIVED') return 'info' as const; return 'info' as const }
onMounted(() => { loadData() })
</script>
<template>
  <TechPanel><div style="margin-bottom:12px"><span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">推荐提交包</span></div>
    <ErrorState v-if="error" title="加载失败" retry-text="重试" @retry="loadData" />
    <div v-if="!loading && !error">
      <div v-for="pkg in items.slice(0, 8)" :key="pkg.id" style="padding:6px;margin-bottom:4px;background:rgba(255,255,255,0.03);border-radius:4px;font-size:12px;display:flex;align-items:center;gap:6px;flex-wrap:wrap">
        <ElTag size="small" :type="statusTag(pkg.packageStatus)">{{ pkg.packageStatus }}</ElTag>
        <span style="color:var(--app-text-bright)">{{ pkg.packageTitle }}</span>
        <ElTag v-if="pkg.submitReadyFlag" size="small" type="success">可提交</ElTag>
        <div style="margin-left:auto;display:flex;gap:4px">
          <ElButton v-if="pkg.packageStatus === 'DRAFT'" size="small" link @click="updateStatus(pkg.id, 'READY')">就绪</ElButton>
          <ElButton v-if="pkg.packageStatus === 'READY'" size="small" link @click="updateStatus(pkg.id, 'REVIEWED')">已审阅</ElButton>
        </div>
      </div>
      <EmptyState v-if="items.length === 0 && !loading && !error" description="暂无提交包" />
    </div>
  </TechPanel>
</template>
