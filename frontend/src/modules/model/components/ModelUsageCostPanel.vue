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
  <TechPanel title="模型用量与成本" glow>
    <div v-loading="loading">
      <template v-if="costData">
        <div class="card-grid" style="margin-bottom:16px">
          <MetricTile :value="costData.totalRequests" label="请求总数" />
          <MetricTile :value="costData.successCount" label="成功" accent="success" />
          <MetricTile :value="costData.failureCount" label="失败" accent="danger" />
          <MetricTile :value="costData.fallbackCount" label="降级" accent="warning" />
          <MetricTile :value="costData.successRate.toFixed(1) + '%'" label="成功率" accent="success" />
          <MetricTile :value="costData.totalTokens.toLocaleString()" label="Token 总量" accent="accent" />
          <MetricTile :value="costData.promptTokens.toLocaleString()" label="提示词 Token" />
          <MetricTile :value="costData.completionTokens.toLocaleString()" label="补全 Token" />
          <MetricTile :value="formatCost(costData.estimatedCost)" label="预估成本" accent="accent" />
        </div>

        <!-- 供应商分布 -->
        <div v-if="costData.providerBreakdowns && costData.providerBreakdowns.length > 0" style="margin-bottom:16px">
          <h4 style="margin-bottom:10px;color:var(--app-text);font-size:14px">供应商分布</h4>
          <el-table :data="costData.providerBreakdowns" size="small" style="width:100%">
            <el-table-column prop="provider" label="供应商" min-width="120" />
            <el-table-column prop="requestCount" label="请求数" width="100" />
            <el-table-column prop="successCount" label="成功" width="100" />
            <el-table-column label="Token" width="120">
              <template #default="{ row }">{{ row.tokenCount.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="成本" width="120">
              <template #default="{ row }">{{ formatCost(row.cost) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 模型分布 -->
        <div v-if="costData.modelBreakdowns && costData.modelBreakdowns.length > 0">
          <h4 style="margin-bottom:10px;color:var(--app-text);font-size:14px">模型分布</h4>
          <el-table :data="costData.modelBreakdowns" size="small" style="width:100%">
            <el-table-column prop="modelName" label="模型" min-width="160" />
            <el-table-column prop="requestCount" label="请求数" width="100" />
            <el-table-column label="Token" width="120">
              <template #default="{ row }">{{ row.tokenCount.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="成本" width="120">
              <template #default="{ row }">{{ formatCost(row.cost) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </template>
      <EmptyState v-else-if="!loading" description="暂无统计数据（需要管理员权限）" />
    </div>
  </TechPanel>
</template>
