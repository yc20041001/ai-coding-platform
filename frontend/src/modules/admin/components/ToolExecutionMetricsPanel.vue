<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getGlobalToolExecutionMetrics, type ToolExecutionMetrics } from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { formatNumber } from '@/shared/utils/format'

const emit = defineEmits<{
  loaded: []
}>()

const loading = ref(false)
const error = ref(false)
const metrics = ref<ToolExecutionMetrics | null>(null)

async function loadMetrics() {
  loading.value = true
  error.value = false
  try {
    const res = await getGlobalToolExecutionMetrics()
    metrics.value = res.data.data
    emit('loaded')
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadMetrics()
})

function pct(value: number): string {
  return (value * 100).toFixed(1) + '%'
}

function msDisplay(value: number): string {
  if (value < 1000) return value.toFixed(0) + 'ms'
  return (value / 1000).toFixed(1) + 's'
}

function barHeight(count: number, all: { totalJobs: number }[]): number {
  const max = Math.max(...all.map(d => d.totalJobs), 1)
  return Math.max((count / max) * 100, 1)
}
</script>

<template>
  <TechPanel
    v-loading="loading"
    title="Tool Worker Metrics"
    data-testid="tool-metrics-panel"
  >
    <ErrorState
      v-if="error"
      title="无法加载工具执行指标"
      message="工具执行指标加载失败，但不影响其他面板"
      retry-text="重试"
      @retry="loadMetrics"
    />

    <EmptyState
      v-if="!error && !loading && !metrics"
      description="暂无工具执行指标数据"
    />

    <template v-if="metrics">
      <!-- Summary Cards -->
      <div class="metrics-grid" data-testid="tool-metrics-summary">
        <MetricTile :value="formatNumber(metrics.summary.totalJobs)" label="Job 总数" />
        <MetricTile :value="pct(metrics.summary.successRate)" label="成功率" accent="success" />
        <MetricTile :value="formatNumber(metrics.summary.failedJobs + metrics.summary.deadLetteredJobs)" label="失败 / DLQ" accent="danger" />
        <MetricTile :value="formatNumber(metrics.summary.retryPendingJobs)" label="待重试" accent="warning" />
        <MetricTile :value="msDisplay(metrics.summary.avgDurationMs)" label="平均耗时" accent="accent" />
        <MetricTile :value="formatNumber(metrics.summary.totalRetries)" label="总重试次数" accent="warning" />
      </div>

      <!-- Tool Metrics Table -->
      <div v-if="metrics.tools.length > 0" class="metrics-section" data-testid="tool-metrics-table">
        <div class="metrics-section-title">工具维度统计</div>
        <el-table :data="metrics.tools" size="small" style="width:100%">
          <el-table-column prop="toolKey" label="工具" min-width="140" />
          <el-table-column label="总量" width="80">
            <template #default="{ row }">{{ formatNumber(row.totalJobs) }}</template>
          </el-table-column>
          <el-table-column label="成功率" width="80">
            <template #default="{ row }">
              <el-tag :type="row.successRate >= 0.9 ? 'success' : row.successRate >= 0.5 ? 'warning' : 'danger'" size="small">
                {{ pct(row.successRate) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="失败" width="70">
            <template #default="{ row }">{{ formatNumber(row.failedJobs) }}</template>
          </el-table-column>
          <el-table-column label="DLQ" width="60">
            <template #default="{ row }">{{ formatNumber(row.deadLetteredJobs) }}</template>
          </el-table-column>
          <el-table-column label="平均耗时" width="100">
            <template #default="{ row }">{{ msDisplay(row.avgDurationMs) }}</template>
          </el-table-column>
          <el-table-column label="重试" width="60">
            <template #default="{ row }">{{ formatNumber(row.totalRetries) }}</template>
          </el-table-column>
          <el-table-column prop="topErrorCode" label="主要错误" min-width="120" />
          <el-table-column prop="topFailureStage" label="主要阶段" min-width="100" />
        </el-table>
      </div>

      <!-- Daily Trend -->
      <div v-if="metrics.daily.length > 0" class="metrics-section" data-testid="tool-metrics-daily">
        <div class="metrics-section-title">近 30 天趋势</div>
        <div class="daily-bars">
          <div
            v-for="day in metrics.daily"
            :key="day.date"
            class="daily-bar-col"
            :title="day.date + ': ' + formatNumber(day.totalJobs) + ' jobs'"
          >
            <div class="daily-bar-stack">
              <div
                class="daily-bar daily-bar-failed"
                :style="{ height: barHeight(day.failedJobs, metrics.daily) + '%' }"
              />
              <div
                class="daily-bar daily-bar-success"
                :style="{ height: barHeight(day.completedJobs, metrics.daily) + '%' }"
              />
            </div>
            <div class="daily-bar-label">{{ day.date.slice(5) }}</div>
          </div>
        </div>
      </div>

      <!-- Failure Metrics -->
      <div class="metrics-row" data-testid="tool-metrics-failures">
        <div v-if="metrics.errorCodes.length > 0" class="metrics-half">
          <div class="metrics-section-title">错误码分布</div>
          <el-table :data="metrics.errorCodes" size="small" style="width:100%">
            <el-table-column prop="errorCode" label="错误码" min-width="140" />
            <el-table-column label="次数" width="70">
              <template #default="{ row }">{{ formatNumber(row.count) }}</template>
            </el-table-column>
            <el-table-column prop="latestTime" label="最近时间" min-width="160" />
          </el-table>
        </div>
        <div v-if="metrics.failureStages.length > 0" class="metrics-half">
          <div class="metrics-section-title">失败阶段分布</div>
          <el-table :data="metrics.failureStages" size="small" style="width:100%">
            <el-table-column prop="errorCode" label="阶段" min-width="140">
              <template #default="{ row }">{{ row.errorCode }}</template>
            </el-table-column>
            <el-table-column label="次数" width="70">
              <template #default="{ row }">{{ formatNumber(row.count) }}</template>
            </el-table-column>
            <el-table-column prop="latestTime" label="最近时间" min-width="160" />
          </el-table>
        </div>
        <div
          v-if="metrics.errorCodes.length === 0 && metrics.failureStages.length === 0"
          class="metrics-half"
          data-testid="tool-metrics-failures-empty"
        >
          <EmptyState description="暂无失败或问题 Job" />
        </div>
      </div>
    </template>
  </TechPanel>
</template>

<style scoped>
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.metrics-section {
  margin-bottom: 20px;
}

.metrics-section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 12px;
}

.metrics-row {
  display: flex;
  gap: 16px;
}

.metrics-half {
  flex: 1;
  min-width: 0;
}

.daily-bars {
  display: flex;
  gap: 2px;
  align-items: flex-end;
  height: 120px;
  padding: 8px 0;
  overflow-x: auto;
}

.daily-bar-col {
  flex: 1;
  min-width: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.daily-bar-stack {
  width: 100%;
  height: 100px;
  display: flex;
  flex-direction: column-reverse;
  align-items: center;
  gap: 1px;
}

.daily-bar {
  width: 100%;
  min-height: 2px;
  border-radius: 2px;
  transition: height 0.3s;
}

.daily-bar-success {
  background: var(--el-color-success, #67c23a);
}

.daily-bar-failed {
  background: var(--el-color-danger, #f56c6c);
}

.daily-bar-label {
  font-size: 9px;
  color: var(--app-text-muted);
  white-space: nowrap;
}

@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .metrics-row {
    flex-direction: column;
  }
}
</style>
