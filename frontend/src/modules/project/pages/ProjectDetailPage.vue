<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProject, getProjectOverview, type ProjectDetail, type ProjectOverview } from '@/modules/project/api'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import SectionRail from '@/shared/components/SectionRail.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import { formatDateTime } from '@/shared/utils/format'

const route = useRoute()
const router = useRouter()
const projectId = route.params.projectId as string

const project = ref<ProjectDetail | null>(null)
const overview = ref<ProjectOverview | null>(null)
const loading = ref(false)

const activeTab = ref('overview')

const railItems = [
  { key: 'overview', label: 'Overview', icon: '◆' },
  { key: 'tasks', label: 'Tasks', icon: '◈' },
  { key: 'chat', label: 'Chat', icon: '◇' },
  { key: 'knowledge', label: 'Knowledge', icon: '◉' },
  { key: 'repository', label: 'Repository', icon: '⬡' },
  { key: 'members', label: 'Members', icon: '◎' },
]

onMounted(async () => {
  loading.value = true
  try {
    const [pRes, oRes] = await Promise.all([getProject(projectId), getProjectOverview(projectId)])
    project.value = pRes.data.data
    overview.value = oRes.data.data
  } catch {
    // handled
  } finally {
    loading.value = false
  }
})

watch(
  () => route.name,
  (name) => {
    const routeTabMap: Record<string, string> = {
      ProjectDetail: 'overview',
      TaskList: 'tasks',
      TaskDetail: 'tasks',
      Chat: 'chat',
      Knowledge: 'knowledge',
      Repository: 'repository',
      Members: 'members',
      PrReview: 'repository',
    }
    activeTab.value = routeTabMap[String(name)] || 'overview'
  },
  { immediate: true },
)

function onRailSelect(key: string) {
  const tabRoutes: Record<string, string> = {
    overview: `/projects/${projectId}`,
    tasks: `/projects/${projectId}/tasks`,
    chat: `/projects/${projectId}/chat`,
    knowledge: `/projects/${projectId}/knowledge`,
    repository: `/projects/${projectId}/repository`,
    members: `/projects/${projectId}/members`,
  }
  router.push(tabRoutes[key] || `/projects/${projectId}`)
}
</script>

<template>
  <div class="page-container" v-loading="loading">
    <DynamicWorkspace
      v-if="project"
      :title="project.name"
      :subtitle="project.description || 'Project Command Center'"
      eyebrow="Project"
    >
      <template #actions>
        <StatusPulse
          :status="project.status"
          :tone="project.status === 'ACTIVE' ? 'success' : 'muted'"
        />
      </template>

      <SectionRail
        :items="railItems"
        :active-key="activeTab"
        style="margin-bottom:20px"
        @select="onRailSelect"
      />

      <NeonDivider tone="primary" />

      <div v-if="activeTab === 'overview' && overview" style="margin-top:20px">
        <h2 class="pd-section-title">Project Telemetry</h2>
        <div class="card-grid">
          <MetricTile :value="overview.taskCount" label="Tasks" />
          <MetricTile :value="overview.runningTaskCount" label="Running" accent="warning" />
          <MetricTile :value="overview.completedTaskCount" label="Completed" accent="success" />
          <MetricTile :value="overview.documentCount" label="Documents" />
          <MetricTile :value="overview.agentCount" label="Agents" accent="accent" />
          <MetricTile :value="overview.memberCount" label="Members" accent="warning" />
        </div>
      </div>

      <router-view v-else />
    </DynamicWorkspace>
  </div>
</template>

<style scoped>
.pd-section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 14px;
}
</style>
