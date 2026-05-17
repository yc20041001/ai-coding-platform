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
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import TechPanel from '@/shared/components/TechPanel.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import GlowButton from '@/shared/components/GlowButton.vue'

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
    ElMessage.error('获取授权地址失败')
  } finally {
    loadingOAuth.value = false
  }
}

async function handleSync() {
  loadingRepo.value = true
  try {
    const { data } = await syncRepositories()
    repositories.value = data ?? []
    ElMessage.success(`Sync complete: ${repositories.value.length} repos`)
  } catch {
    ElMessage.error('同步失败，请确认已绑定 GitHub 账号')
  } finally {
    loadingRepo.value = false
  }
}

async function handleUnbind() {
  try {
    await ElMessageBox.confirm('Unbind GitHub account?', 'Confirm', {
      confirmButtonText: 'Unbind',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    ElMessage.info('Please use API: DELETE /api/github/oauth/bindings/{bindingId}')
  } catch {
    // cancelled
  }
}
</script>

<template>
  <div class="page-container">
    <DynamicWorkspace
      title="GitHub 集成"
      subtitle="仓库同步与 OAuth 绑定"
      eyebrow="研发运维"
    >
      <template #actions>
        <StatusPulse
          :status="oauthStatus?.bound ? 'Bound' : 'Not Bound'"
          :tone="oauthStatus?.bound ? 'success' : 'muted'"
        />
      </template>

      <NeonDivider tone="primary" style="margin-bottom:20px" />

      <TechPanel title="GitHub OAuth 配置" glow style="margin-bottom:20px">
        <div v-if="!oauthStatus" class="status-row">
          <StatusPulse status="Checking..." tone="warning" />
        </div>

        <template v-else-if="!oauthStatus.configured">
          <div class="notice">
            <span class="notice-icon">&#9888;</span>
            <div>
              <p class="notice-title">GitHub OAuth 未配置</p>
              <p class="notice-desc">
                请设置 <code>GITHUB_CLIENT_ID</code> 和 <code>GITHUB_CLIENT_SECRET</code>
                in <code>.env</code>, then restart the backend.
              </p>
            </div>
          </div>
        </template>

        <template v-else-if="oauthStatus.bound">
          <div class="status-row">
            <StatusPulse :status="`Bound: ${oauthStatus.githubLogin}`" tone="success" />
          </div>
          <div class="action-row">
            <GlowButton accent="primary" size="small" :loading="loadingRepo" @click="handleSync">
              Sync Repos
            </GlowButton>
            <el-button size="small" @click="handleUnbind">解绑</el-button>
          </div>
        </template>

        <template v-else>
          <div class="status-row">
            <StatusPulse status="Not Bound" tone="muted" />
          </div>
          <GlowButton
            accent="primary"
            size="small"
            :loading="loadingOAuth"
            @click="handleAuthorize"
          >
            Authorize GitHub
          </GlowButton>
        </template>
      </TechPanel>

      <TechPanel v-if="oauthStatus?.bound" title="仓库">
        <div v-if="loadingRepo" style="padding:8px 0">
          <StatusPulse status="Loading..." tone="warning" />
        </div>

        <div v-else-if="repositories.length === 0" class="empty-hint">
          No repositories. Click "Sync Repos" to pull from GitHub.
        </div>

        <div v-else class="repo-grid">
          <div
            v-for="repo in repositories"
            :key="repo.id"
            class="repo-card"
          >
            <div class="repo-name">
              <span class="repo-icon">◈</span>
              <a :href="repo.htmlUrl ?? '#'" target="_blank" rel="noopener">{{ repo.fullName }}</a>
              <span v-if="repo.privateRepo" class="badge">私有</span>
            </div>
            <p v-if="repo.description" class="repo-desc">{{ repo.description }}</p>
            <div class="repo-meta">
              <span v-if="repo.language">{{ repo.language }}</span>
              <span v-if="repo.defaultBranch">分支：{{ repo.defaultBranch }}</span>
            </div>
          </div>
        </div>
      </TechPanel>
    </DynamicWorkspace>
  </div>
</template>

<style scoped>
.status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
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
  background: var(--app-warning-soft, rgba(245, 158, 11, 0.08));
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: var(--app-radius);
}

.notice-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.notice-title {
  font-weight: 600;
  margin: 0 0 4px;
  color: var(--app-text);
}

.notice-desc {
  margin: 0;
  font-size: 13px;
  color: var(--app-text-muted);
}

.notice-desc code {
  background: var(--app-panel);
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12px;
}

.empty-hint {
  color: var(--app-text-muted);
  font-size: 14px;
  padding: 8px 0;
}

.repo-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.repo-card {
  padding: 14px 16px;
  background: var(--app-bg-soft);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  transition: border-color 0.2s;
}

.repo-card:hover {
  border-color: var(--app-border-strong);
}

.repo-name {
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.repo-icon {
  font-size: 14px;
  color: var(--app-primary);
  opacity: 0.7;
}

.repo-name a {
  color: var(--app-primary);
  text-decoration: none;
}

.repo-name a:hover {
  text-decoration: underline;
}

.badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--app-panel);
  color: var(--app-text-muted);
}

.repo-desc {
  font-size: 13px;
  color: var(--app-text-muted);
  margin: 4px 0 0;
}

.repo-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--app-text-muted);
  margin-top: 6px;
}
</style>
