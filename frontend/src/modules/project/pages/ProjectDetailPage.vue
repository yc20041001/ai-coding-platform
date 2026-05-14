<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProject, getProjectOverview, type ProjectDetail, type ProjectOverview } from '@/modules/project/api'
import PageHeader from '@/shared/components/PageHeader.vue'
import StatusTag from '@/shared/components/StatusTag.vue'
import TechPanel from '@/shared/components/TechPanel.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import { formatDateTime } from '@/shared/utils/format'

const route = useRoute()
const router = useRouter()
const projectId = route.params.projectId as string

const project = ref<ProjectDetail | null>(null)
const overview = ref<ProjectOverview | null>(null)
const loading = ref(false)

const activeTab = ref('overview')

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

const tabs = [
  { name: 'overview', label: 'Overview' },
  { name: 'tasks', label: 'Tasks' },
  { name: 'chat', label: 'Chat' },
  { name: 'knowledge', label: 'Knowledge' },
  { name: 'repository', label: 'Repository' },
  { name: 'members', label: 'Members' },
]

function onTabClick(pane: any) {
  const tabName = typeof pane === 'string' ? pane : pane?.props?.name || pane?.paneName || pane?.name
  const tabRoutes: Record<string, string> = {
    overview: `/projects/${projectId}`,
    tasks: `/projects/${projectId}/tasks`,
    chat: `/projects/${projectId}/chat`,
    knowledge: `/projects/${projectId}/knowledge`,
    repository: `/projects/${projectId}/repository`,
    members: `/projects/${projectId}/members`,
  }
  router.push(tabRoutes[tabName] || `/projects/${projectId}`)
}
</script>

<template>
  <div class="page-container" v-loading="loading">
    <PageHeader v-if="project" :title="project.name" :description="project.description">
      <template #actions>
        <StatusTag :status="project.status" />
      </template>
    </PageHeader>

    <el-tabs v-model="activeTab" @tab-click="onTabClick">
      <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" />
    </el-tabs>

    <div v-if="activeTab === 'overview' && overview" style="margin-top:16px">
      <TechPanel title="Project Overview" glow>
        <div class="card-grid">
          <MetricTile :value="overview.taskCount" label="Tasks" />
          <MetricTile :value="overview.runningTaskCount" label="Running" accent="warning" />
          <MetricTile :value="overview.completedTaskCount" label="Completed" accent="success" />
          <MetricTile :value="overview.documentCount" label="Documents" />
          <MetricTile :value="overview.agentCount" label="Agents" accent="accent" />
          <MetricTile :value="overview.memberCount" label="Members" accent="warning" />
        </div>
      </TechPanel>
    </div>

    <router-view v-else />
  </div>
</template>
