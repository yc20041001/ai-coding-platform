import { createRouter, createWebHistory } from 'vue-router'
import { setupAuthGuard } from '@/app/guards/authGuard'
import { getToken } from '@/shared/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/modules/auth/pages/LoginPage.vue'),
    },
    {
      path: '/public',
      name: 'PublicHome',
      component: () => import('@/modules/public/pages/PublicHomePage.vue'),
    },
    {
      path: '/',
      component: () => import('@/app/layouts/BasicLayout.vue'),
      children: [
        {
          path: '',
          redirect: () => getToken() ? '/dashboard' : '/public',
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/modules/dashboard/pages/DashboardPage.vue'),
        },
        {
          path: 'projects',
          name: 'ProjectList',
          component: () => import('@/modules/project/pages/ProjectListPage.vue'),
        },
        {
          path: 'projects/:projectId',
          name: 'ProjectDetail',
          component: () => import('@/modules/project/pages/ProjectDetailPage.vue'),
          children: [
            {
              path: 'tasks',
              name: 'TaskList',
              component: () => import('@/modules/task/pages/TaskListPage.vue'),
            },
            {
              path: 'chat',
              name: 'Chat',
              component: () => import('@/modules/chat/pages/ChatPage.vue'),
            },
            {
              path: 'knowledge',
              name: 'Knowledge',
              component: () => import('@/modules/knowledge/pages/KnowledgeBasePage.vue'),
            },
            {
              path: 'repository',
              name: 'Repository',
              component: () => import('@/modules/repository/pages/RepositoryPanel.vue'),
            },
            {
              path: 'members',
              name: 'Members',
              component: () => import('@/modules/member/pages/MemberPanel.vue'),
            },
            {
              path: 'tasks/:taskId',
              name: 'TaskDetail',
              component: () => import('@/modules/task/pages/TaskDetailPage.vue'),
            },
            {
              path: 'github/pr-review',
              name: 'PrReview',
              component: () => import('@/modules/github/pages/PullRequestReviewPage.vue'),
            },
            {
              path: 'agents',
              name: 'ProjectAgents',
              component: () => import('@/modules/agent/pages/ProjectAgentConfigPage.vue'),
            },
          ],
        },
        {
          path: 'agents',
          name: 'AgentList',
          component: () => import('@/modules/agent/pages/AgentListPage.vue'),
        },
        {
          path: 'github',
          name: 'GithubIntegration',
          component: () => import('@/modules/github/pages/GithubIntegrationPage.vue'),
        },
        {
          path: 'observability',
          name: 'Observability',
          component: () => import('@/modules/admin/pages/ObservabilityPage.vue'),
        },
        {
          path: 'model-gateway',
          name: 'ModelGateway',
          component: () => import('@/modules/model/pages/ModelConfigPage.vue'),
        },
      ],
    },
  ],
})

setupAuthGuard(router)

export default router
