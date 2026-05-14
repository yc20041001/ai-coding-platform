<script setup lang="ts">
import { ref, onMounted } from 'vue'
import client from '@/shared/api/client'
import type { ApiResponse } from '@/shared/api/types'
import MetricTile from '@/shared/components/MetricTile.vue'
import TechPanel from '@/shared/components/TechPanel.vue'
import RuntimeBadge from '@/shared/components/RuntimeBadge.vue'

interface Overview {
  projectCount: number
  userCount: number
  taskCount: number
  runningTaskCount: number
  completedTaskCount: number
  agentCount: number
  knowledgeBaseCount: number
  documentCount: number
  chatMessageCount: number
  modelRequestCount: number
  todayModelRequestCount: number
  todayTokenUsage: number
}

const overview = ref<Overview | null>(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await client.get<ApiResponse<Overview>>('/api/observability/overview')
    overview.value = res.data.data
  } catch {
    // handled by client interceptor
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-container">
    <div class="dash-header">
      <div>
        <h1 class="dash-title">Dashboard</h1>
        <p class="dash-sub">AI Coding Platform · 系统概览</p>
      </div>
      <RuntimeBadge status="online" label="System Online" />
    </div>

    <div v-if="loading" v-loading="loading" style="min-height:200px;border-radius:10px" />

    <template v-else-if="overview">
      <TechPanel title="Platform Overview" glow style="margin-bottom:20px">
        <div class="card-grid">
          <MetricTile :value="overview.projectCount" label="Projects" />
          <MetricTile :value="overview.userCount" label="Users" accent="accent" />
          <MetricTile :value="overview.taskCount" label="Tasks" accent="success" />
          <MetricTile :value="overview.agentCount" label="Agents" accent="accent" />
          <MetricTile :value="overview.knowledgeBaseCount" label="Knowledge Bases" accent="warning" />
          <MetricTile :value="overview.documentCount" label="Documents" />
        </div>
      </TechPanel>

      <TechPanel title="Task Pipeline" glow style="margin-bottom:20px">
        <div class="card-grid">
          <MetricTile :value="overview.runningTaskCount" label="Running" accent="warning" />
          <MetricTile :value="overview.completedTaskCount" label="Completed" accent="success" />
          <MetricTile :value="overview.taskCount - overview.runningTaskCount - overview.completedTaskCount" label="Pending" />
        </div>
      </TechPanel>

      <TechPanel title="Model Usage" glow style="margin-bottom:20px">
        <div class="card-grid">
          <MetricTile :value="overview.modelRequestCount" label="Total Calls" />
          <MetricTile :value="overview.todayModelRequestCount" label="Today Calls" accent="success" />
          <MetricTile :value="overview.todayTokenUsage?.toLocaleString() ?? '0'" label="Today Tokens" accent="accent" />
          <MetricTile :value="overview.chatMessageCount" label="Chat Messages" accent="warning" />
        </div>
      </TechPanel>
    </template>
  </div>
</template>

<style scoped>
.dash-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.dash-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text);
  margin: 0;
}
.dash-sub {
  font-size: 13px;
  color: var(--app-text-muted);
  margin-top: 4px;
}
</style>
