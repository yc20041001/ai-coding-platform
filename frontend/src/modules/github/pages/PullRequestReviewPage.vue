<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  listRepositories,
  listPullRequests,
  getPullRequestFiles,
  getPullRequestPatch,
  createPrReview,
  listPrReviews,
  getPrReviewDetail,
  getPrReviewFindings,
  type GithubRepository,
  type GithubPullRequest,
  type GithubPullRequestFile,
  type PrReviewJob,
  type PrReviewFinding
} from '@/modules/github/api'
import TechPanel from '@/shared/components/TechPanel.vue'

const route = useRoute()
const projectId = computed(() => route.params.projectId as string)

// State
const repos = ref<GithubRepository[]>([])
const selectedRepo = ref<GithubRepository | null>(null)
const pullRequests = ref<GithubPullRequest[]>([])
const selectedPr = ref<GithubPullRequest | null>(null)
const prFiles = ref<GithubPullRequestFile[]>([])
const patch = ref('')
const reviewMode = ref('FULL')
const reviewLoading = ref(false)
const currentReview = ref<PrReviewJob | null>(null)
const findings = ref<PrReviewFinding[]>([])
const reviewHistory = ref<PrReviewJob[]>([])
const loadingRepos = ref(false)
const loadingPRs = ref(false)

// Derived
const reviewModes = ['SUMMARY', 'SECURITY', 'QUALITY', 'FULL']
const severityColors: Record<string, string> = {
  INFO: '#60a5fa',
  WARNING: '#f59e0b',
  ERROR: '#ef4444',
  CRITICAL: '#dc2626'
}
const riskColors: Record<string, string> = {
  LOW: '#22c55e',
  MEDIUM: '#f59e0b',
  HIGH: '#ef4444',
  CRITICAL: '#dc2626'
}

// Load repos on mount
async function loadRepos() {
  loadingRepos.value = true
  try {
    const { data } = await listRepositories()
    repos.value = data ?? []
  } catch {
    repos.value = []
    ElMessage.warning('请先在 GitHub 集成页面同步仓库')
  } finally {
    loadingRepos.value = false
  }
}

async function onRepoSelect(repo: GithubRepository) {
  selectedRepo.value = repo
  selectedPr.value = null
  prFiles.value = []
  patch.value = ''
  currentReview.value = null
  findings.value = []

  loadingPRs.value = true
  try {
    const { data } = await listPullRequests(repo.owner, repo.repoName)
    pullRequests.value = data ?? []
  } catch {
    pullRequests.value = []
    ElMessage.error('获取 PR 列表失败')
  } finally {
    loadingPRs.value = false
  }
}

async function onPrSelect(pr: GithubPullRequest) {
  selectedPr.value = pr
  currentReview.value = null
  findings.value = []

  if (!selectedRepo.value) return
  const { owner, repoName } = selectedRepo.value

  try {
    const [filesRes, patchRes] = await Promise.all([
      getPullRequestFiles(owner, repoName, pr.number),
      getPullRequestPatch(owner, repoName, pr.number)
    ])
    prFiles.value = filesRes.data ?? []
    patch.value = patchRes.data ?? ''
  } catch {
    ElMessage.error('获取 PR 详情失败')
  }
}

async function handleRunReview() {
  if (!selectedRepo.value || !selectedPr.value) return
  reviewLoading.value = true
  try {
    const { data } = await createPrReview(projectId.value, {
      owner: selectedRepo.value.owner,
      repo: selectedRepo.value.repoName,
      pullRequestNumber: selectedPr.value.number,
      reviewMode: reviewMode.value
    })
    currentReview.value = data
    await loadFindings(data.id)
    await loadReviewHistory()
    ElMessage.success('Review 完成')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || 'Review 失败')
  } finally {
    reviewLoading.value = false
  }
}

async function loadFindings(reviewJobId: string) {
  try {
    const { data } = await getPrReviewFindings(reviewJobId)
    findings.value = data ?? []
  } catch {
    findings.value = []
  }
}

async function loadReviewHistory() {
  try {
    const { data } = await listPrReviews(projectId.value)
    reviewHistory.value = data ?? []
  } catch {
    reviewHistory.value = []
  }
}

function selectHistoryReview(review: PrReviewJob) {
  currentReview.value = review
  if (review.id) loadFindings(review.id)
}

loadRepos()
loadReviewHistory()
</script>

<template>
  <div class="pr-review-page">
    <h2 class="page-title">Pull Request Review</h2>

    <div class="layout">
      <!-- Left: Repo + PR Selection -->
      <aside class="sidebar">
        <TechPanel title="选择仓库" glow>
          <div v-if="loadingRepos" class="loading-hint">加载仓库...</div>
          <div v-else-if="repos.length === 0" class="empty-hint">
            暂无仓库，请先在
            <router-link to="/github">GitHub 集成</router-link>
            页面同步
          </div>
          <div v-else class="repo-list">
            <div
              v-for="repo in repos"
              :key="repo.id"
              class="select-item"
              :class="{ 'select-item--active': selectedRepo?.id === repo.id }"
              @click="onRepoSelect(repo)"
            >
              {{ repo.fullName }}
            </div>
          </div>
        </TechPanel>

        <TechPanel v-if="selectedRepo" title="选择 PR" class="mt-16">
          <div v-if="loadingPRs" class="loading-hint">加载 PR...</div>
          <div v-else-if="pullRequests.length === 0" class="empty-hint">暂无 PR</div>
          <div v-else class="repo-list">
            <div
              v-for="pr in pullRequests"
              :key="pr.id"
              class="select-item"
              :class="{ 'select-item--active': selectedPr?.id === pr.id }"
              @click="onPrSelect(pr)"
            >
              <div class="pr-item-title">#{{ pr.number }} {{ pr.title }}</div>
              <div class="pr-item-meta">{{ pr.authorLogin }} · {{ pr.state }}</div>
            </div>
          </div>
        </TechPanel>

        <TechPanel v-if="currentReview" title="Review 历史" class="mt-16">
          <div v-if="reviewHistory.length === 0" class="empty-hint">暂无历史</div>
          <div v-else class="repo-list">
            <div
              v-for="review in reviewHistory"
              :key="review.id"
              class="select-item"
              :class="{ 'select-item--active': currentReview?.id === review.id }"
              @click="selectHistoryReview(review)"
            >
              <div class="pr-item-title">
                <span class="risk-dot" :style="{ background: riskColors[review.riskLevel ?? 'MEDIUM'] }" />
                {{ review.reviewMode }} Review
              </div>
              <div class="pr-item-meta">{{ review.status }} · {{ review.createTime }}</div>
            </div>
          </div>
        </TechPanel>
      </aside>

      <!-- Right: Diff + Review -->
      <section class="main">
        <!-- PR Info -->
        <TechPanel v-if="selectedPr" title="PR 信息" glow>
          <div class="pr-info">
            <h3>{{ selectedPr.title }}</h3>
            <div class="pr-meta-row">
              <span>作者: {{ selectedPr.authorLogin || 'unknown' }}</span>
              <span>{{ selectedPr.baseBranch }} ← {{ selectedPr.headBranch }}</span>
              <span class="diff-stat">+{{ selectedPr.additions }} -{{ selectedPr.deletions }}</span>
              <span>{{ selectedPr.changedFiles }} files</span>
            </div>
          </div>

          <div class="review-actions">
            <el-select v-model="reviewMode" size="small" style="width: 120px">
              <el-option
                v-for="mode in reviewModes"
                :key="mode"
                :label="mode"
                :value="mode"
              />
            </el-select>
            <el-button
              type="primary"
              size="small"
              :loading="reviewLoading"
              @click="handleRunReview"
            >
              Run AI Review
            </el-button>
          </div>
        </TechPanel>

        <!-- Review Result -->
        <TechPanel v-if="currentReview && currentReview.status !== 'PENDING' && currentReview.status !== 'RUNNING'" title="Review 结果" glow class="mt-16">
          <div v-if="currentReview.status === 'FAILED'" class="review-error">
            错误: {{ currentReview.errorMessage }}
          </div>
          <template v-else>
            <div class="review-summary-row">
              <span class="risk-badge" :style="{ background: riskColors[currentReview.riskLevel ?? 'MEDIUM'] }">
                {{ currentReview.riskLevel || 'MEDIUM' }}
              </span>
              <span class="review-meta">
                {{ currentReview.modelProvider }} / {{ currentReview.modelName }} ·
                {{ currentReview.tokenUsage ?? 0 }} tokens
              </span>
            </div>
            <p class="review-summary">{{ currentReview.summary }}</p>
          </template>
        </TechPanel>

        <!-- Findings -->
        <TechPanel v-if="findings.length > 0" title="Review Findings" class="mt-16">
          <div v-for="f in findings" :key="f.id" class="finding-card">
            <div class="finding-header">
              <span class="finding-severity" :style="{ background: severityColors[f.severity] ?? '#6b7280' }">
                {{ f.severity }}
              </span>
              <span class="finding-category">{{ f.category }}</span>
              <span v-if="f.filePath" class="finding-file">{{ f.filePath }}<template v-if="f.lineNumber">:{{ f.lineNumber }}</template></span>
            </div>
            <h4 class="finding-title">{{ f.title }}</h4>
            <p v-if="f.description" class="finding-desc">{{ f.description }}</p>
            <div v-if="f.suggestion" class="finding-suggestion">
              <strong>建议:</strong> {{ f.suggestion }}
            </div>
            <pre v-if="f.codeSnippet" class="finding-snippet"><code>{{ f.codeSnippet }}</code></pre>
          </div>
        </TechPanel>

        <!-- Diff Viewer -->
        <TechPanel v-if="patch" title="Diff / Patch" class="mt-16">
          <pre class="diff-view"><code>{{ patch }}</code></pre>
        </TechPanel>

        <!-- Empty state -->
        <div v-if="!selectedPr" class="empty-state">
          <p>选择一个 Pull Request 查看 Diff 并运行 AI Review</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.pr-review-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
  color: var(--app-text-primary, #e5e7eb);
}

.layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.sidebar {
  width: 300px;
  flex-shrink: 0;
  position: sticky;
  top: 24px;
}

.main {
  flex: 1;
  min-width: 0;
}

.mt-16 { margin-top: 16px; }

.loading-hint,
.empty-hint {
  font-size: 13px;
  color: var(--app-text-tertiary, #6b7280);
  padding: 8px 0;
}

.empty-hint a {
  color: var(--app-primary, #60a5fa);
}

.repo-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.select-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: var(--app-text-secondary, #9ca3af);
  transition: background 0.15s;
}

.select-item:hover {
  background: var(--app-hover-bg, rgba(255,255,255,0.04));
}

.select-item--active {
  background: var(--app-active-bg, rgba(96, 165, 250, 0.12));
  color: var(--app-text-primary, #e5e7eb);
}

.pr-item-title {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.pr-item-meta {
  font-size: 12px;
  color: var(--app-text-tertiary, #6b7280);
  margin-top: 2px;
}

.risk-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* PR Info */
.pr-info h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px;
  color: var(--app-text-primary, #e5e7eb);
}

.pr-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: var(--app-text-secondary, #9ca3af);
}

.diff-stat {
  color: var(--app-primary, #60a5fa);
}

.review-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

/* Review Result */
.review-error {
  color: #ef4444;
  font-size: 14px;
}

.review-summary-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.risk-badge {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}

.review-meta {
  font-size: 12px;
  color: var(--app-text-tertiary, #6b7280);
}

.review-summary {
  font-size: 14px;
  color: var(--app-text-secondary, #9ca3af);
  line-height: 1.6;
  white-space: pre-wrap;
}

/* Findings */
.finding-card {
  padding: 14px;
  margin-bottom: 10px;
  background: var(--app-card-bg, rgba(255,255,255,0.02));
  border: 1px solid var(--app-border, rgba(255,255,255,0.07));
  border-radius: 8px;
}

.finding-card:last-child { margin-bottom: 0; }

.finding-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.finding-severity {
  padding: 1px 8px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
}

.finding-category {
  font-size: 12px;
  color: var(--app-text-tertiary, #6b7280);
  padding: 1px 6px;
  background: var(--app-badge-bg, rgba(255,255,255,0.05));
  border-radius: 3px;
}

.finding-file {
  font-size: 12px;
  color: var(--app-primary, #60a5fa);
  font-family: monospace;
  margin-left: auto;
}

.finding-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 4px;
  color: var(--app-text-primary, #e5e7eb);
}

.finding-desc {
  font-size: 13px;
  color: var(--app-text-secondary, #9ca3af);
  margin: 0 0 8px;
  line-height: 1.5;
}

.finding-suggestion {
  font-size: 13px;
  color: #22c55e;
  padding: 8px;
  background: rgba(34, 197, 94, 0.06);
  border-radius: 6px;
  margin-bottom: 8px;
}

.finding-snippet {
  font-size: 12px;
  background: var(--app-code-bg, rgba(0,0,0,0.3));
  padding: 10px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 0;
}

.finding-snippet code {
  font-family: 'SF Mono', 'Fira Code', monospace;
}

/* Diff */
.diff-view {
  font-size: 12px;
  background: var(--app-code-bg, rgba(0,0,0,0.3));
  padding: 14px;
  border-radius: 8px;
  overflow-x: auto;
  max-height: 600px;
  margin: 0;
  line-height: 1.5;
}

.diff-view code {
  font-family: 'SF Mono', 'Fira Code', monospace;
  white-space: pre;
}

/* Empty */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: var(--app-text-tertiary, #6b7280);
  font-size: 15px;
}
</style>
