<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProject, getProjectOverview, type ProjectDetail, type ProjectOverview } from '@/modules/project/api'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import SectionRail from '@/shared/components/SectionRail.vue'
import MetricTile from '@/shared/components/MetricTile.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import DemoBadge from '@/shared/components/DemoBadge.vue'
import { formatDateTime } from '@/shared/utils/format'

const route = useRoute()
const router = useRouter()
const projectId = route.params.projectId as string

const project = ref<ProjectDetail | null>(null)
const overview = ref<ProjectOverview | null>(null)
const loading = ref(false)

const activeTab = ref('overview')

const railItems = [
  { key: 'overview', label: '概览', icon: '◆' },
  { key: 'tasks', label: '任务', icon: '◈' },
  { key: 'chat', label: '对话', icon: '◇' },
  { key: 'knowledge', label: '知识库', icon: '◉' },
  { key: 'repository', label: '仓库', icon: '⬡' },
  { key: 'members', label: '成员', icon: '◎' },
  { key: 'agents', label: '智能体', icon: '◆' },
  { key: 'tools', label: '工具', icon: '⬢' },
  { key: 'code-index', label: '代码索引', icon: '◈' },
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
      ProjectAgents: 'agents',
      ProjectTools: 'tools',
      CodeIndex: 'code-index',
    }
    activeTab.value = routeTabMap[String(name)] || 'overview'
  },
  { immediate: true },
)

const isDemoProject = computed(() => {
  return project.value?.name?.includes('Demo') ?? false
})

function onRailSelect(key: string) {
  const tabRoutes: Record<string, string> = {
    overview: `/projects/${projectId}`,
    tasks: `/projects/${projectId}/tasks`,
    chat: `/projects/${projectId}/chat`,
    knowledge: `/projects/${projectId}/knowledge`,
    repository: `/projects/${projectId}/repository`,
    members: `/projects/${projectId}/members`,
    agents: `/projects/${projectId}/agents`,
    tools: `/projects/${projectId}/tools`,
    'code-index': `/projects/${projectId}/code-index`,
  }
  router.push(tabRoutes[key] || `/projects/${projectId}`)
}
</script>

<template>
  <div class="page-container" v-loading="loading">
    <DynamicWorkspace
      v-if="project"
      :title="project.name"
      :subtitle="project.description || '项目指挥中心'"
      eyebrow="项目"
    >
      <template #actions>
        <DemoBadge v-if="isDemoProject" mode="demo" style="margin-right:8px" />
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
        <div v-if="isDemoProject" class="pd-demo-hint">
          <span class="pd-demo-hint-icon">◆</span>
          <span>演示项目：建议先查看 <strong>知识库</strong>，再体验 <strong>Chat</strong> 与 <strong>任务</strong></span>
        </div>
        <h2 class="pd-section-title">项目遥测</h2>
        <div class="card-grid">
          <MetricTile :value="overview.taskCount" label="任务" />
          <MetricTile :value="overview.runningTaskCount" label="运行中" accent="warning" />
          <MetricTile :value="overview.completedTaskCount" label="已完成" accent="success" />
          <MetricTile :value="overview.documentCount" label="文档" />
          <MetricTile :value="overview.agentCount" label="智能体" accent="accent" />
          <MetricTile :value="overview.memberCount" label="成员" accent="warning" />
        </div>
      </div>

      <router-view v-else />
    </DynamicWorkspace>
  </div>
</template>

<style scoped>
.pd-demo-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  margin-bottom: 16px;
  border-radius: 6px;
  background: rgba(64, 158, 255, 0.06);
  border: 1px solid rgba(64, 158, 255, 0.15);
  font-size: 12px;
  color: var(--app-text-dim);
}

.pd-demo-hint-icon {
  color: var(--app-cyan, #409EFF);
  font-size: 12px;
  flex-shrink: 0;
}

.pd-demo-hint strong {
  color: var(--app-text);
  font-weight: 600;
}

.pd-section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 14px;
}
</style>
