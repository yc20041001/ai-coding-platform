<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getGlobalCostSummary, type ModelUsageCost, type ProviderBreakdown, type ModelBreakdown } from '@/modules/model/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import EmptyState from '@/shared/components/EmptyState.vue'

const costData = ref<ModelUsageCost | null>(null)
const loading = ref(false)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getGlobalCostSummary()
    costData.value = res.data.data
  } catch { /* handled - admin only */ } finally { loading.value = false }
}

function formatCost(cost: number): string {
  if (cost === 0) return '$0.00'
  if (cost < 0.01) return '< $0.01'
  return '$' + cost.toFixed(4)
}
</script>

<template>
  <TechPanel title="Model Usage & Cost" glow>
    <div v-loading="loading">
      <template v-if="costData">
        <div class="card-grid" style="margin-bottom:16px">
          <MetricTile :value="costData.totalRequests" label="Total Requests" />
          <MetricTile :value="costData.successCount" label="Success" accent="success" />
          <MetricTile :value="costData.failureCount" label="Failure" accent="danger" />
          <MetricTile :value="costData.fallbackCount" label="Fallback" accent="warning" />
          <MetricTile :value="costData.successRate.toFixed(1) + '%'" label="Success Rate" accent="success" />
          <MetricTile :value="costData.totalTokens.toLocaleString()" label="Total Tokens" accent="accent" />
          <MetricTile :value="costData.promptTokens.toLocaleString()" label="Prompt Tokens" />
          <MetricTile :value="costData.completionTokens.toLocaleString()" label="Completion Tokens" />
          <MetricTile :value="formatCost(costData.estimatedCost)" label="Est. Cost" accent="accent" />
        </div>

        <!-- Provider Breakdown -->
        <div v-if="costData.providerBreakdowns && costData.providerBreakdowns.length > 0" style="margin-bottom:16px">
          <h4 style="margin-bottom:10px;color:var(--app-text);font-size:14px">Provider Breakdown</h4>
          <el-table :data="costData.providerBreakdowns" size="small" style="width:100%">
            <el-table-column prop="provider" label="Provider" min-width="120" />
            <el-table-column prop="requestCount" label="Requests" width="100" />
            <el-table-column prop="successCount" label="Success" width="100" />
            <el-table-column label="Tokens" width="120">
              <template #default="{ row }">{{ row.tokenCount.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="Cost" width="120">
              <template #default="{ row }">{{ formatCost(row.cost) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <!-- Model Breakdown -->
        <div v-if="costData.modelBreakdowns && costData.modelBreakdowns.length > 0">
          <h4 style="margin-bottom:10px;color:var(--app-text);font-size:14px">Model Breakdown</h4>
          <el-table :data="costData.modelBreakdowns" size="small" style="width:100%">
            <el-table-column prop="modelName" label="Model" min-width="160" />
            <el-table-column prop="requestCount" label="Requests" width="100" />
            <el-table-column label="Tokens" width="120">
              <template #default="{ row }">{{ row.tokenCount.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="Cost" width="120">
              <template #default="{ row }">{{ formatCost(row.cost) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </template>
      <EmptyState v-else-if="!loading" description="暂无统计（需要 ADMIN 权限）" />
    </div>
  </TechPanel>
</template>
