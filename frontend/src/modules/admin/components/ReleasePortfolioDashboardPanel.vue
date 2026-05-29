<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import {
  refreshPortfolio,
  getPortfolioDashboard,
  getPortfolioRanking,
  getGovernanceSummary,
  type ReleasePortfolioDashboardItem,
  type ReleasePortfolioRankingItem,
  type MultiProjectGovernanceSummaryItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import { ElTag, ElButton, ElMessage, ElTooltip } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId?: string | null
}>()

const dashboard = ref<ReleasePortfolioDashboardItem | null>(null)
const ranking = ref<ReleasePortfolioRankingItem[]>([])
const summary = ref<MultiProjectGovernanceSummaryItem | null>(null)
const loading = ref(false)
const error = ref(false)
const refreshing = ref(false)

function loadData() {
  loading.value = true
  error.value = false
  Promise.all([
    getPortfolioDashboard(),
    getPortfolioRanking(),
    getGovernanceSummary(),
  ])
    .then(([dashRes, rankRes, summaryRes]) => {
      dashboard.value = dashRes.data.data
      ranking.value = rankRes.data.data
      summary.value = summaryRes.data.data
    })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleRefresh() {
  refreshing.value = true
  try {
    await refreshPortfolio()
    loadData()
    ElMessage.success('Portfolio 快照已刷新')
  } catch {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

function levelTag(level: string) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    HIGH: 'success',
    MEDIUM: 'warning',
    LOW: 'warning',
    CRITICAL: 'danger',
    NONE: 'info',
  }
  return map[level] || 'info'
}

function levelLabel(level: string): string {
  const map: Record<string, string> = {
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低',
    CRITICAL: '严重',
    NONE: '无',
  }
  return map[level] || level
}

function recTag(rec: string) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    EXPAND_NOW: 'success',
    EXPAND_WITH_GUARDRAILS: 'warning',
    HOLD: 'info',
    BLOCK: 'danger',
  }
  return map[rec] || 'info'
}

function recLabel(rec: string): string {
  const map: Record<string, string> = {
    EXPAND_NOW: '立即扩大',
    EXPAND_WITH_GUARDRAILS: '有条件扩大',
    HOLD: '暂缓',
    BLOCK: '阻止',
  }
  return map[rec] || rec
}

function statusPulseTone(ready: boolean | null | undefined): 'success' | 'danger' {
  return ready ? 'success' : 'danger'
}

onMounted(() => { loadData() })
</script>

<template>
  <TechPanel>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">发布组合看板</span>
      <div style="display:flex;gap:4px">
        <ElButton size="small" type="primary" :loading="refreshing" @click="handleRefresh">刷新快照</ElButton>
      </div>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取组合看板" retry-text="重试" @retry="loadData" />

    <div v-if="!props.projectId" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">请先选择一个项目</div>

    <div v-if="dashboard && props.projectId" v-loading="loading">
      <!-- Overview Metrics -->
      <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(80px,1fr));gap:8px;margin-bottom:12px">
        <MetricTile :value="dashboard.projectCount" label="项目数" accent="primary" />
        <MetricTile :value="dashboard.highConfidenceCount" label="高信心" accent="success" />
        <MetricTile :value="dashboard.criticalConfidenceCount" label="严重" accent="danger" />
        <MetricTile :value="dashboard.expandNowCount" label="可扩大" accent="success" />
        <MetricTile :value="dashboard.blockCount" label="阻止" accent="danger" />
      </div>

      <div style="font-size:11px;color:var(--app-text-muted);margin-bottom:8px">
        平均信心分: <strong>{{ dashboard.averageConfidenceScore }}</strong>
        <span v-if="dashboard.snapshotDate"> | 快照日期: {{ dashboard.snapshotDate }}</span>
      </div>

      <NeonDivider tone="muted" style="margin:12px 0" />

      <!-- Ranking Table -->
      <div v-if="ranking.length > 0">
        <div style="font-size:11px;color:var(--app-text-muted);margin-bottom:6px">项目排名 (按信心分)</div>
        <div v-for="(item, idx) in ranking" :key="item.projectId ?? 'rank-' + idx" style="display:flex;gap:8px;padding:4px 0;font-size:11px;border-bottom:1px solid rgba(56,189,248,0.03);align-items:center">
          <span style="min-width:24px;font-weight:600;color:var(--app-text-muted)">#{{ item.portfolioRank }}</span>
          <span style="min-width:120px;color:var(--app-text-bright);font-weight:500">{{ item.projectName }}</span>
          <span style="min-width:60px;font-weight:600;color:var(--app-text-bright)">{{ item.confidenceScore }}</span>
          <ElTag :type="levelTag(item.confidenceLevel)" size="small" effect="dark">{{ levelLabel(item.confidenceLevel) }}</ElTag>
          <ElTag :type="recTag(item.expansionRecommendation)" size="small">{{ recLabel(item.expansionRecommendation) }}</ElTag>
          <StatusPulse :status="item.rollbackReady ? '就绪' : '未就绪'" :tone="statusPulseTone(item.rollbackReady)" />
          <ElTooltip :content="item.summaryText || ''">
            <span style="color:var(--app-text-muted);cursor:help;overflow:hidden;text-overflow:ellipsis;max-width:180px;white-space:nowrap">{{ item.summaryText }}</span>
          </ElTooltip>
        </div>
      </div>

      <NeonDivider tone="muted" style="margin:12px 0" />

      <!-- Summary -->
      <div v-if="summary">
        <div style="font-size:11px;color:var(--app-text-muted);margin-bottom:6px">组合概要</div>
        <div style="font-size:11px;color:var(--app-text-soft);margin-bottom:4px">
          <span>可扩大: {{ summary.expandNowCount }} | 有条件: {{ summary.expandWithGuardrailsCount }} | 暂缓: {{ summary.holdCount }} | 阻止: {{ summary.blockCount }}</span>
        </div>
        <div v-if="summary.riskiestProjects.length > 0" style="font-size:11px;color:var(--app-text-muted)">
          风险最高: <strong style="color:var(--color-error)">{{ summary.riskiestProjects.join(', ') }}</strong>
        </div>
        <div v-if="summary.improvingProject" style="font-size:11px;color:var(--color-success);margin-top:2px">
          ↑ 提升: {{ summary.improvingProject }}
        </div>
        <div v-if="summary.decliningProject" style="font-size:11px;color:var(--color-error);margin-top:2px">
          ↓ 下降: {{ summary.decliningProject }}
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && !dashboard && props.projectId && !error" description="暂无组合数据" />
  </TechPanel>
</template>
