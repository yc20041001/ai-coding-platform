<script setup lang="ts">
import { ref, onMounted, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  type RepositoryBranch, type RepositoryDiff, type GithubRepository,
  bindRepository, cloneRepository, pullRepository, getBranches, getDiff, listGithubRepos,
} from '@/modules/repository/api'
import type { ApiError } from '@/shared/api/client'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
import { usePagination } from '@/shared/composables/usePagination'
import { formatDateTime } from '@/shared/utils/format'

const MarkdownRenderer = defineAsyncComponent(() => import('@/shared/components/MarkdownRenderer.vue'))

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
    ElMessage.error('加载分支失败')
  } finally {
    loadingBranches.value = false
  }
}

async function handleBind() {
  binding.value = true
  try {
    await bindRepository(projectId, bindForm.value)
    ElMessage.success('仓库已绑定')
    bindVisible.value = false
    loadBranches()
  } catch {
    ElMessage.error('绑定仓库失败')
  } finally {
    binding.value = false
  }
}

async function handleClone() {
  cloning.value = true
  try {
    await cloneRepository(projectId, cloneForm.value)
    ElMessage.success('克隆任务已提交')
    cloneVisible.value = false
    loadBranches()
  } catch {
    ElMessage.error('克隆失败')
  } finally {
    cloning.value = false
  }
}

async function handlePull() {
  pulling.value = true
  try {
    await pullRepository(projectId, pullForm.value)
    ElMessage.success('拉取任务已提交')
    pullVisible.value = false
  } catch {
    ElMessage.error('拉取失败')
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
    ElMessage.error('加载 Diff 失败')
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
    <div class="repo-header">
      <div>
        <h3 class="repo-title">代码仓库</h3>
        <p class="repo-sub">代码仓库管理</p>
      </div>
      <div class="repo-actions">
        <GlowButton size="small" accent="primary" :loading="loadingBranches" @click="loadBranches">刷新</GlowButton>
        <GlowButton size="small" accent="accent" @click="githubVisible = true">浏览</GlowButton>
        <GlowButton size="small" accent="primary" @click="bindVisible = true">绑定</GlowButton>
        <GlowButton size="small" accent="success" @click="cloneVisible = true">克隆</GlowButton>
        <GlowButton size="small" accent="warning" @click="pullVisible = true">拉取</GlowButton>
      </div>
    </div>

    <div class="repo-section">
      <h4>分支</h4>
      <el-table :data="branches" v-loading="loadingBranches" size="small" style="width:100%">
        <el-table-column prop="name" label="分支" min-width="160" />
        <el-table-column prop="commitHash" label="提交哈希" width="280">
          <template #default="{ row }">
            <code>{{ row.commitHash?.substring(0, 12) || '-' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="受保护" width="100">
          <template #default="{ row }">
            <el-tag :type="row.protectedBranch ? 'warning' : 'info'" size="small">{{ row.protectedBranch ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近同步" width="150" class-name="nowrap-column">
          <template #default="{ row }">{{ formatDateTime(row.lastSyncTime) }}</template>
        </el-table-column>
      </el-table>
      <EmptyState v-if="!loadingBranches && branches.length === 0" title="暂无分支" description="绑定并克隆仓库后可查看分支。" />
    </div>

    <div class="repo-section" style="margin-top:20px">
      <h4>Diff 差异</h4>
      <div style="display:flex;gap:8px;align-items:center;margin-bottom:12px">
        <el-input v-model="diffForm.base" placeholder="基准" size="small" style="width:140px" />
        <span style="color:var(--app-text-muted)">..</span>
        <el-input v-model="diffForm.head" placeholder="目标" size="small" style="width:140px" />
        <GlowButton size="small" accent="primary" @click="handleViewDiff">查看 Diff</GlowButton>
      </div>
    </div>

    <!-- Bind Dialog -->
    <el-dialog v-model="bindVisible" title="绑定仓库" width="500px">
      <el-form label-position="top">
        <el-form-item label="供应商">
          <el-select v-model="bindForm.provider" style="width:100%">
            <el-option label="GitHub" value="GITHUB" />
            <el-option label="GitLab" value="GITLAB" />
            <el-option label="Gitee" value="GITEE" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库全名" required>
          <el-input v-model="bindForm.repoFullName" placeholder="owner/repo" />
        </el-form-item>
        <el-form-item label="仓库地址" required>
          <el-input v-model="bindForm.repoUrl" placeholder="https://github.com/owner/repo" />
        </el-form-item>
        <el-form-item label="克隆地址" required>
          <el-input v-model="bindForm.cloneUrl" />
        </el-form-item>
        <el-form-item label="默认分支">
          <el-input v-model="bindForm.defaultBranch" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="binding" @click="handleBind">绑定</el-button>
      </template>
    </el-dialog>

    <!-- Clone Dialog -->
    <el-dialog v-model="cloneVisible" title="克隆仓库" width="450px">
      <el-form label-position="top">
        <el-form-item label="分支">
          <el-input v-model="cloneForm.branch" placeholder="main" />
        </el-form-item>
        <el-form-item label="强制克隆">
          <el-switch v-model="cloneForm.force" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cloneVisible = false">取消</el-button>
        <el-button type="primary" :loading="cloning" @click="handleClone">克隆</el-button>
      </template>
    </el-dialog>

    <!-- Pull Dialog -->
    <el-dialog v-model="pullVisible" title="拉取仓库" width="450px">
      <el-form label-position="top">
        <el-form-item label="分支">
          <el-input v-model="pullForm.branch" placeholder="main" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pullVisible = false">取消</el-button>
        <el-button type="primary" :loading="pulling" @click="handlePull">拉取</el-button>
      </template>
    </el-dialog>

    <!-- Github Browse Dialog -->
    <el-dialog v-model="githubVisible" title="浏览 GitHub 仓库" width="700px">
      <el-table :data="githubRepos" v-loading="loadingGithub" size="small" @row-click="onGithubRowClick" style="cursor:pointer">
        <el-table-column prop="fullName" label="全名" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="私有" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isPrivate ? 'warning' : 'success'" size="small">{{ row.isPrivate ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="defaultBranch" label="默认分支" width="120" />
      </el-table>
      <el-pagination
        v-if="githubRepos.length > 0"
        layout="prev, pager, next" size="small" style="margin-top:12px;justify-content:flex-end"
        @current-change="(p: number) => loadGithub(p)"
      />
    </el-dialog>

    <!-- Diff Drawer -->
    <el-drawer v-model="diffVisible" title="Diff 差异" size="700px">
      <div v-loading="loadingDiff">
        <template v-if="diffData">
          <div class="diff-summary">
            <span>基准：<code>{{ diffData.base }}</code></span>
            <span>目标：<code>{{ diffData.head }}</code></span>
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
        <ErrorState v-else-if="!loadingDiff" title="暂无 Diff" message="请先绑定并克隆仓库。" />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.repo-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.repo-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--app-text);
  margin: 0;
}
.repo-sub {
  font-size: 13px;
  color: var(--app-text-muted);
  margin-top: 2px;
}
.repo-actions {
  display: flex;
  gap: 8px;
}
.repo-section h4 {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 12px;
}
:deep(.nowrap-column .cell) {
  white-space: nowrap;
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
  background: var(--app-bg-soft);
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
