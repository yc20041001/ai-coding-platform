<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import client from '@/shared/api/client'
import type { ApiResponse } from '@/shared/api/types'
import MetricTile from '@/shared/components/MetricTile.vue'
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import SignalStrip from '@/shared/components/SignalStrip.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import DemoBadge from '@/shared/components/DemoBadge.vue'
import DemoGuidePanel from '@/shared/components/DemoGuidePanel.vue'

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

const router = useRouter()
const overview = ref<Overview | null>(null)
const loading = ref(false)
const demoProjectId = ref<string | null>(null)

onMounted(async () => {
  loading.value = true
  try {
    const [oRes, pRes] = await Promise.all([
      client.get<ApiResponse<Overview>>('/api/observability/overview'),
      client.get<ApiResponse<{ records: { id: string; name: string }[] }>>('/api/projects?page=1&pageSize=50'),
    ])
    overview.value = oRes.data.data

    const records = pRes.data.data?.records ?? []
    const demoProj = records.find((r: { name: string }) =>
      r.name.includes('Demo')
    )
    if (demoProj) {
      demoProjectId.value = demoProj.id
    }
  } catch {
    // handled by client interceptor
  } finally {
    loading.value = false
  }
})

function goToDemoProject() {
  if (demoProjectId.value) {
    router.push(`/projects/${demoProjectId.value}`)
  } else {
    router.push('/projects')
  }
}
</script>

<template>
  <div class="page-container">
    <DynamicWorkspace
      title="System Dashboard"
      subtitle="AI Coding Platform · Real-time Telemetry"
      eyebrow="Command Center"
      status="Online"
    >
      <template #actions>
        <DemoBadge
          :mode="overview && overview.modelRequestCount > 0 ? 'demo' : 'demo'"
        />
      </template>

      <template #metrics>
        <div class="dash-flow">
          <div class="dash-flow-item">
            <SignalStrip tone="primary" active />
            <span class="dash-flow-label">Projects</span>
            <span class="dash-flow-value">{{ overview?.projectCount ?? '--' }}</span>
          </div>
          <div class="dash-flow-connector" />
          <div class="dash-flow-item">
            <SignalStrip tone="accent" active />
            <span class="dash-flow-label">Tasks</span>
            <span class="dash-flow-value">{{ overview?.taskCount ?? '--' }}</span>
          </div>
          <div class="dash-flow-connector" />
          <div class="dash-flow-item">
            <SignalStrip tone="success" active />
            <span class="dash-flow-label">Chat</span>
            <span class="dash-flow-value">{{ overview?.chatMessageCount ?? '--' }}</span>
          </div>
          <div class="dash-flow-connector" />
          <div class="dash-flow-item">
            <SignalStrip tone="warning" active />
            <span class="dash-flow-label">RAG</span>
            <span class="dash-flow-value">{{ overview?.documentCount ?? '--' }}</span>
          </div>
          <div class="dash-flow-connector" />
          <div class="dash-flow-item">
            <SignalStrip tone="primary" active />
            <span class="dash-flow-label">Model Calls</span>
            <span class="dash-flow-value">{{ overview?.modelRequestCount ?? '--' }}</span>
          </div>
        </div>
      </template>

      <div v-if="loading" v-loading="loading" style="min-height:200px;border-radius:10px" />

      <template v-else-if="overview">
        <DemoGuidePanel
          :has-demo-data="overview.projectCount > 0"
          :demo-project-id="demoProjectId ?? undefined"
          style="margin-bottom: 12px"
        >
          <template #badge>
            <DemoBadge mode="demo" style="margin-left:auto" />
          </template>
        </DemoGuidePanel>

        <NeonDivider tone="primary" />

        <section class="dash-section">
          <h2 class="dash-section-title">Platform Overview</h2>
          <div class="card-grid">
            <MetricTile :value="overview.projectCount" label="Projects" />
            <MetricTile :value="overview.userCount" label="Users" accent="accent" />
            <MetricTile :value="overview.agentCount" label="Agents" accent="accent" />
            <MetricTile :value="overview.knowledgeBaseCount" label="Knowledge Bases" accent="warning" />
            <MetricTile :value="overview.documentCount" label="Documents" />
            <MetricTile :value="overview.chatMessageCount" label="Chat Messages" accent="warning" />
          </div>
        </section>

        <NeonDivider tone="accent" />

        <section class="dash-section">
          <h2 class="dash-section-title">Task Pipeline</h2>
          <div class="card-grid">
            <MetricTile :value="overview.taskCount" label="Total Tasks" />
            <MetricTile :value="overview.runningTaskCount" label="Running" accent="warning" />
            <MetricTile :value="overview.completedTaskCount" label="Completed" accent="success" />
            <MetricTile :value="overview.taskCount - overview.runningTaskCount - overview.completedTaskCount" label="Pending" accent="primary" />
          </div>
        </section>

        <NeonDivider tone="primary" />

        <section class="dash-section">
          <h2 class="dash-section-title">Model Usage</h2>
          <div class="card-grid">
            <MetricTile :value="overview.modelRequestCount" label="Total Calls" />
            <MetricTile :value="overview.todayModelRequestCount" label="Today Calls" accent="success" />
            <MetricTile :value="overview.todayTokenUsage?.toLocaleString() ?? '0'" label="Today Tokens" accent="accent" />
            <MetricTile :value="overview.chatMessageCount" label="Messages" accent="warning" />
          </div>
        </section>
      </template>
    </DynamicWorkspace>
  </div>
</template>

<style scoped>
.dash-flow {
  display: flex;
  align-items: center;
  gap: 0;
  width: 100%;
  padding: 8px 0;
  flex-wrap: wrap;
  justify-content: center;
}

.dash-flow-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 80px;
}

.dash-flow-label {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--app-text-muted);
  font-weight: 600;
}

.dash-flow-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text);
  font-variant-numeric: tabular-nums;
}

.dash-flow-connector {
  width: 24px;
  height: 1px;
  background: var(--app-border-strong);
  margin: 0 4px;
  align-self: center;
  margin-bottom: 16px;
}

.dash-section {
  margin: 16px 0;
}

.dash-section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 14px;
}
</style>
