<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  refreshHeatmap,
  getHeatmap,
  type ReleaseRiskHeatmapItem,
  type ReleaseRiskHeatmapCellItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { ElButton, ElMessage, ElTooltip } from 'element-plus'

const heatmap = ref<ReleaseRiskHeatmapItem | null>(null)

const uniqueProjectIds = computed(() => {
  if (!heatmap.value) return []
  return [...new Set(heatmap.value.cells.map(c => c.projectId).filter((id): id is string => id !== null))]
})
const loading = ref(false)
const error = ref(false)
const refreshing = ref(false)

function loadData() {
  loading.value = true
  error.value = false
  getHeatmap()
    .then(res => { heatmap.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await refreshHeatmap()
    loadData()
    ElMessage.success('风险热力图已刷新')
  } catch {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

function riskColor(level: string): string {
  switch (level) {
    case 'CRITICAL': return 'var(--color-error)'
    case 'HIGH': return 'var(--color-warning)'
    case 'MEDIUM': return 'var(--color-caution)'
    case 'LOW': return 'var(--color-success)'
    default: return 'var(--app-text-muted)'
  }
}

function riskBg(level: string): string {
  switch (level) {
    case 'CRITICAL': return 'rgba(239,68,68,0.15)'
    case 'HIGH': return 'rgba(245,158,11,0.12)'
    case 'MEDIUM': return 'rgba(234,179,8,0.08)'
    case 'LOW': return 'rgba(34,197,94,0.08)'
    default: return 'transparent'
  }
}

function riskLabel(level: string): string {
  const map: Record<string, string> = {
    CRITICAL: '严重',
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低',
  }
  return map[level] || level
}

function categoryLabel(cat: string): string {
  const map: Record<string, string> = {
    INCIDENT: '事件',
    ALERT: '告警',
    VERIFICATION: '验证',
    ROLLOUT: '回滚',
    SIGNOFF: '签字',
    COST: '成本',
    PR_QUALITY: 'PR质量',
  }
  return map[cat] || cat
}

function getCell(category: string): ReleaseRiskHeatmapCellItem | undefined {
  return heatmap.value?.cells.find(c => c.riskCategory === category)
}

onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">发布风险热力图</span>
      <ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新快照</ElButton>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取风险热力图" retry-text="重试" @retry="loadData" />

    <div v-if="heatmap && heatmap.cells.length > 0" v-loading="loading">
      <div style="font-size:11px;color:var(--app-text-muted);margin-bottom:8px">
        快照日期: {{ heatmap.snapshotDate }} | 项目/类别数: {{ [...new Set(heatmap.cells.map(c => c.projectId))].length }} 个项目 x {{ heatmap.categories.length }} 个类别
      </div>

      <!-- Get unique projects -->
      <div v-for="pid in uniqueProjectIds" :key="pid" style="margin-bottom:10px">
        <div style="font-size:11px;font-weight:600;color:var(--app-text-bright);margin-bottom:4px">
          {{ heatmap.cells.find(c => c.projectId === pid)?.projectName || pid }}
        </div>
        <div style="display:flex;gap:6px;flex-wrap:wrap">
          <div v-for="cat in heatmap.categories" :key="pid + '-' + cat">
            <ElTooltip placement="top" :content="`${categoryLabel(cat)}: ${(heatmap.cells.find(c => c.projectId === pid && c.riskCategory === cat)?.riskScore ?? '-')} - ${riskLabel(heatmap.cells.find(c => c.projectId === pid && c.riskCategory === cat)?.riskLevel || '')}`">
              <div
                style="width:32px;height:32px;border-radius:4px;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:600;cursor:pointer;border:1px solid rgba(255,255,255,0.05)"
                :style="{
                  backgroundColor: riskBg(heatmap.cells.find(c => c.projectId === pid && c.riskCategory === cat)?.riskLevel || ''),
                  color: riskColor(heatmap.cells.find(c => c.projectId === pid && c.riskCategory === cat)?.riskLevel || ''),
                }"
              >
                {{ cat.substring(0, 3) }}
              </div>
            </ElTooltip>
          </div>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && (!heatmap || heatmap.cells.length === 0) && !error" description="暂无热力图数据，请先刷新快照" />
  </TechPanel>
</template>
