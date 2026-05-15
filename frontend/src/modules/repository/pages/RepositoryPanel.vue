<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  type RepositoryBranch, type RepositoryDiff, type GithubRepository,
  bindRepository, cloneRepository, pullRepository, getBranches, getDiff, listGithubRepos,
} from '@/modules/repository/api'
import type { ApiError } from '@/shared/api/client'
import PageHeader from '@/shared/components/PageHeader.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
import MarkdownRenderer from '@/shared/components/MarkdownRenderer.vue'
import { usePagination } from '@/shared/composables/usePagination'
import { formatDateTime } from '@/shared/utils/format'

const route = useRoute()
const projectId = route.params.projectId as string

const branches = ref<RepositoryBranch[]>([])
const loadingBranches = ref(false)

const bindVisible = ref(false)
const bindForm = ref({ provider: 'GITHUB', repoFullName: '', repoUrl: '', cloneUrl: '', defaultBranch: 'main' })
const binding = ref(false)

const cloneVisible = ref(false)
const cloneForm = ref({ branch: 'main', force: false })
const cloning = ref(false)

const pullVisible = ref(false)
const pullForm = ref({ branch: 'main' })
const pulling = ref(false)

const diffVisible = ref(false)
const diffData = ref<RepositoryDiff | null>(null)
const loadingDiff = ref(false)
const diffForm = ref({ base: 'main', head: 'HEAD' })

const githubVisible = ref(false)
const { loading: loadingGithub, records: githubRepos, load: loadGithub } = usePagination<GithubRepository>(
  (page, pageSize) => listGithubRepos(undefined, page, pageSize),
)

async function loadBranches() {
  loadingBranches.value = true
  try {
    const res = await getBranches(projectId)
    branches.value = res.data.data
  } catch (e) {
    const err = e as ApiError
    if (err.code === 'NOT_FOUND') {
      branches.value = []
      return
    }
    ElMessage.error('获取分支列表失败')
  } finally {
    loadingBranches.value = false
  }
}

async function handleBind() {
  binding.value = true
  try {
    await bindRepository(projectId, bindForm.value)
    ElMessage.success('仓库绑定成功')
    bindVisible.value = false
    loadBranches()
  } catch {
    ElMessage.error('仓库绑定失败')
  } finally {
    binding.value = false
  }
}

async function handleClone() {
  cloning.value = true
  try {
    await cloneRepository(projectId, cloneForm.value)
    ElMessage.success('Clone 操作已提交')
    cloneVisible.value = false
    loadBranches()
  } catch {
    ElMessage.error('Clone 失败')
  } finally {
    cloning.value = false
  }
}

async function handlePull() {
  pulling.value = true
  try {
    await pullRepository(projectId, pullForm.value)
    ElMessage.success('Pull 操作已提交')
    pullVisible.value = false
  } catch {
    ElMessage.error('Pull 失败')
  } finally {
    pulling.value = false
  }
}

async function handleViewDiff() {
  loadingDiff.value = true
  diffVisible.value = true
  try {
    const res = await getDiff(projectId, diffForm.value.base, diffForm.value.head)
    diffData.value = res.data.data
  } catch {
    ElMessage.error('获取 diff 失败')
    diffData.value = null
  } finally {
    loadingDiff.value = false
  }
}

function onGithubRowClick(row: GithubRepository) {
  bindForm.value = {
    provider: row.provider || 'GITHUB',
    repoFullName: row.fullName,
    repoUrl: row.htmlUrl,
    cloneUrl: row.cloneUrl,
    defaultBranch: row.defaultBranch || 'main',
  }
  githubVisible.value = false
  bindVisible.value = true
}

onMounted(() => loadBranches())
</script>

<template>
  <div class="page-container">
    <PageHeader title="Repository" description="代码仓库管理">
      <template #actions>
        <GlowButton size="small" accent="primary" :loading="loadingBranches" @click="loadBranches">Refresh</GlowButton>
        <GlowButton size="small" accent="accent" @click="githubVisible = true">Browse</GlowButton>
        <GlowButton size="small" accent="primary" @click="bindVisible = true">Bind</GlowButton>
        <GlowButton size="small" accent="success" @click="cloneVisible = true">Clone</GlowButton>
        <GlowButton size="small" accent="warning" @click="pullVisible = true">Pull</GlowButton>
      </template>
    </PageHeader>

    <div class="repo-section">
      <h4>Branches</h4>
      <el-table :data="branches" v-loading="loadingBranches" size="small" style="width:100%">
        <el-table-column prop="name" label="Branch" min-width="160" />
        <el-table-column prop="commitHash" label="Commit Hash" width="280">
          <template #default="{ row }">
            <code>{{ row.commitHash?.substring(0, 12) || '-' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="Protected" width="100">
          <template #default="{ row }">
            <el-tag :type="row.protectedBranch ? 'warning' : 'info'" size="small">{{ row.protectedBranch ? 'Yes' : 'No' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Last Sync" width="150" class-name="nowrap-column">
          <template #default="{ row }">{{ formatDateTime(row.lastSyncTime) }}</template>
        </el-table-column>
      </el-table>
      <EmptyState v-if="!loadingBranches && branches.length === 0" title="No Branches" description="Bind and clone a repository to inspect branches." />
    </div>

    <div class="repo-section" style="margin-top:20px">
      <h4>Diff</h4>
      <div style="display:flex;gap:8px;align-items:center;margin-bottom:12px">
        <el-input v-model="diffForm.base" placeholder="Base" size="small" style="width:140px" />
        <span>..</span>
        <el-input v-model="diffForm.head" placeholder="Head" size="small" style="width:140px" />
        <GlowButton size="small" accent="primary" @click="handleViewDiff">View Diff</GlowButton>
      </div>
    </div>

    <!-- Bind Dialog -->
    <el-dialog v-model="bindVisible" title="绑定仓库" width="500px">
      <el-form label-position="top">
        <el-form-item label="Provider">
          <el-select v-model="bindForm.provider" style="width:100%">
            <el-option label="GitHub" value="GITHUB" />
            <el-option label="GitLab" value="GITLAB" />
            <el-option label="Gitee" value="GITEE" />
          </el-select>
        </el-form-item>
        <el-form-item label="Repo Full Name" required>
          <el-input v-model="bindForm.repoFullName" placeholder="owner/repo" />
        </el-form-item>
        <el-form-item label="Repo URL" required>
          <el-input v-model="bindForm.repoUrl" placeholder="https://github.com/owner/repo" />
        </el-form-item>
        <el-form-item label="Clone URL" required>
          <el-input v-model="bindForm.cloneUrl" />
        </el-form-item>
        <el-form-item label="Default Branch">
          <el-input v-model="bindForm.defaultBranch" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="binding" @click="handleBind">绑定</el-button>
      </template>
    </el-dialog>

    <!-- Clone Dialog -->
    <el-dialog v-model="cloneVisible" title="Clone 仓库" width="450px">
      <el-form label-position="top">
        <el-form-item label="分支">
          <el-input v-model="cloneForm.branch" placeholder="main" />
        </el-form-item>
        <el-form-item label="强制 Clone">
          <el-switch v-model="cloneForm.force" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cloneVisible = false">取消</el-button>
        <el-button type="primary" :loading="cloning" @click="handleClone">Clone</el-button>
      </template>
    </el-dialog>

    <!-- Pull Dialog -->
    <el-dialog v-model="pullVisible" title="Pull 仓库" width="450px">
      <el-form label-position="top">
        <el-form-item label="分支">
          <el-input v-model="pullForm.branch" placeholder="main" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pullVisible = false">取消</el-button>
        <el-button type="primary" :loading="pulling" @click="handlePull">Pull</el-button>
      </template>
    </el-dialog>

    <!-- Github Browse Dialog -->
    <el-dialog v-model="githubVisible" title="浏览 GitHub 仓库" width="700px">
      <el-table :data="githubRepos" v-loading="loadingGithub" size="small" @row-click="onGithubRowClick" style="cursor:pointer">
        <el-table-column prop="fullName" label="Full Name" min-width="180" />
        <el-table-column prop="description" label="Description" min-width="200" show-overflow-tooltip />
        <el-table-column label="Private" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isPrivate ? 'warning' : 'success'" size="small">{{ row.isPrivate ? 'Yes' : 'No' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="defaultBranch" label="Default Branch" width="120" />
      </el-table>
      <el-pagination
        v-if="githubRepos.length > 0"
        layout="prev, pager, next" size="small" style="margin-top:12px;justify-content:flex-end"
        @current-change="(p: number) => loadGithub(p)"
      />
    </el-dialog>

    <!-- Diff Drawer -->
    <el-drawer v-model="diffVisible" title="Diff" size="700px">
      <div v-loading="loadingDiff">
        <template v-if="diffData">
          <div class="diff-summary">
            <span>Base: <code>{{ diffData.base }}</code></span>
            <span>Head: <code>{{ diffData.head }}</code></span>
            <span>{{ diffData.files?.length || 0 }} files changed</span>
          </div>
          <div v-for="f in diffData.files" :key="f.path" class="diff-file">
            <div class="diff-file-header">
              <el-tag size="small" :type="f.changeType === 'ADDED' ? 'success' : f.changeType === 'DELETED' ? 'danger' : 'warning'">
                {{ f.changeType }}
              </el-tag>
              <span class="diff-file-path">{{ f.path }}</span>
              <span class="diff-stats">+{{ f.additions }} -{{ f.deletions }}</span>
            </div>
            <MarkdownRenderer v-if="f.patch" :content="'```diff\n' + f.patch + '\n```'" />
          </div>
        </template>
        <ErrorState v-else-if="!loadingDiff" title="无法加载 Diff" message="请确认仓库已绑定并 Clone" />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.repo-section h4 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--app-text);
}
:deep(.nowrap-column .cell) {
  white-space: nowrap;
}
:deep(.page-header__actions) {
  gap: 8px;
}
.diff-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  padding: 10px 14px;
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  font-size: 13px;
  color: var(--app-text-soft);
}
.diff-summary code {
  background: rgba(148, 163, 184, 0.1);
  color: var(--app-text-soft);
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
}
.diff-file {
  margin-bottom: 20px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  overflow: hidden;
}
.diff-file-header {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 8px 12px;
  background: var(--app-panel);
  border-bottom: 1px solid var(--app-border);
  font-size: 13px;
}
.diff-file-path {
  font-weight: 500;
  color: var(--app-text-soft);
  flex: 1;
}
.diff-stats {
  font-size: 12px;
  color: var(--app-text-muted);
  font-family: monospace;
}
</style>
