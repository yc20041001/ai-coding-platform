<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getOAuthAuthorize,
  getOAuthStatus,
  unbindOAuth,
  syncRepositories,
  listRepositories,
  type GithubOAuthStatusResponse,
  type GithubRepository
} from '@/modules/github/api'
import TechPanel from '@/shared/components/TechPanel.vue'

const oauthStatus = ref<GithubOAuthStatusResponse | null>(null)
const repositories = ref<GithubRepository[]>([])
const loadingRepo = ref(false)
const loadingOAuth = ref(false)

onMounted(async () => {
  await loadOAuthStatus()
  await loadRepositories()
})

async function loadOAuthStatus() {
  try {
    const { data } = await getOAuthStatus()
    oauthStatus.value = data
  } catch {
    // status fetch failed silently
  }
}

async function loadRepositories() {
  if (!oauthStatus.value?.bound) return
  loadingRepo.value = true
  try {
    const { data } = await listRepositories()
    repositories.value = data ?? []
  } catch {
    repositories.value = []
  } finally {
    loadingRepo.value = false
  }
}

async function handleAuthorize() {
  loadingOAuth.value = true
  try {
    const { data } = await getOAuthAuthorize()
    if (!data.configured) {
      ElMessage.warning('GitHub OAuth 未配置，请联系管理员')
      return
    }
    if (data.authorizeUrl) {
      window.open(data.authorizeUrl, '_blank', 'width=800,height=700')
    }
  } catch {
    ElMessage.error('获取授权链接失败')
  } finally {
    loadingOAuth.value = false
  }
}

async function handleSync() {
  loadingRepo.value = true
  try {
    const { data } = await syncRepositories()
    repositories.value = data ?? []
    ElMessage.success(`同步完成，共 ${repositories.value.length} 个仓库`)
  } catch {
    ElMessage.error('同步仓库失败，请确认已绑定 GitHub 账号')
  } finally {
    loadingRepo.value = false
  }
}

async function handleUnbind() {
  try {
    await ElMessageBox.confirm('确定要解绑 GitHub 账号吗？', '确认解绑', {
      confirmButtonText: '解绑',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // unbind needs bindingId; we don't store it, so just reload status
    ElMessage.info('解绑功能请通过 API 直接调用 DELETE /api/github/oauth/bindings/{bindingId}')
  } catch {
    // cancelled
  }
}
</script>

<template>
  <div class="github-page">
    <h2 class="page-title">GitHub 集成</h2>

    <TechPanel title="GitHub OAuth 授权" glow>
      <div v-if="!oauthStatus" class="status-row">
        <span class="status-dot status-dot--loading" />
        <span>加载中...</span>
      </div>

      <template v-else-if="!oauthStatus.configured">
        <div class="notice">
          <span class="notice-icon">&#9888;</span>
          <div>
            <p class="notice-title">GitHub OAuth 未配置</p>
            <p class="notice-desc">
              请在 <code>.env</code> 中配置 <code>GITHUB_CLIENT_ID</code> 和 <code>GITHUB_CLIENT_SECRET</code>，
              然后重启后端服务。
            </p>
          </div>
        </div>
      </template>

      <template v-else-if="oauthStatus.bound">
        <div class="status-row">
          <span class="status-dot status-dot--active" />
          <span>已绑定 GitHub 账号：<strong>{{ oauthStatus.githubLogin }}</strong></span>
        </div>
        <div class="action-row">
          <el-button type="primary" :loading="loadingRepo" @click="handleSync">
            同步仓库
          </el-button>
          <el-button type="default" @click="handleUnbind">
            解绑
          </el-button>
        </div>
      </template>

      <template v-else>
        <div class="status-row">
          <span class="status-dot status-dot--inactive" />
          <span>未绑定 GitHub 账号</span>
        </div>
        <el-button
          type="primary"
          :loading="loadingOAuth"
          @click="handleAuthorize"
        >
          授权 GitHub
        </el-button>
      </template>
    </TechPanel>

    <TechPanel v-if="oauthStatus?.bound" title="仓库列表" class="repo-panel">
      <div v-if="loadingRepo" class="status-row">
        <span class="status-dot status-dot--loading" />
        <span>加载中...</span>
      </div>

      <div v-else-if="repositories.length === 0" class="empty-hint">
        暂无仓库，请点击「同步仓库」从 GitHub 拉取
      </div>

      <div v-else class="repo-grid">
        <div
          v-for="repo in repositories"
          :key="repo.id"
          class="repo-card"
        >
          <div class="repo-name">
            <a :href="repo.htmlUrl ?? '#'" target="_blank" rel="noopener">{{ repo.fullName }}</a>
            <span v-if="repo.privateRepo" class="badge">私有</span>
          </div>
          <p v-if="repo.description" class="repo-desc">{{ repo.description }}</p>
          <div class="repo-meta">
            <span v-if="repo.language">{{ repo.language }}</span>
            <span v-if="repo.defaultBranch">分支: {{ repo.defaultBranch }}</span>
          </div>
        </div>
      </div>
    </TechPanel>
  </div>
</template>

<style scoped>
.github-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
  color: var(--app-text-primary, #e5e7eb);
}

.status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--app-text-secondary, #9ca3af);
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot--active { background: #22c55e; box-shadow: 0 0 6px #22c55e; }
.status-dot--inactive { background: #6b7280; }
.status-dot--loading { background: #f59e0b; animation: pulse 1.5s infinite; }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.action-row {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.notice {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--app-warning-bg, rgba(245, 158, 11, 0.08));
  border: 1px solid var(--app-warning-border, rgba(245, 158, 11, 0.3));
  border-radius: 8px;
}

.notice-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.notice-title {
  font-weight: 600;
  margin: 0 0 4px;
  color: var(--app-text-primary, #e5e7eb);
}

.notice-desc {
  margin: 0;
  font-size: 13px;
  color: var(--app-text-secondary, #9ca3af);
}

.notice-desc code {
  background: var(--app-code-bg, rgba(255,255,255,0.06));
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12px;
}

.repo-panel {
  margin-top: 20px;
}

.empty-hint {
  color: var(--app-text-tertiary, #6b7280);
  font-size: 14px;
  padding: 8px 0;
}

.repo-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.repo-card {
  padding: 12px 16px;
  background: var(--app-card-bg, rgba(255,255,255,0.03));
  border: 1px solid var(--app-border, rgba(255,255,255,0.08));
  border-radius: 8px;
  transition: border-color 0.2s;
}

.repo-card:hover {
  border-color: var(--app-border-strong, rgba(255,255,255,0.15));
}

.repo-name {
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.repo-name a {
  color: var(--app-primary, #60a5fa);
  text-decoration: none;
}

.repo-name a:hover {
  text-decoration: underline;
}

.badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--app-badge-bg, rgba(107, 114, 128, 0.2));
  color: var(--app-text-tertiary, #9ca3af);
}

.repo-desc {
  font-size: 13px;
  color: var(--app-text-secondary, #9ca3af);
  margin: 4px 0 0;
}

.repo-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--app-text-tertiary, #6b7280);
  margin-top: 6px;
}
</style>
