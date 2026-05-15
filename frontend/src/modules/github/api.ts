import client from '@/shared/api/client'

// === OAuth ===

export interface GithubOAuthAuthorizeResponse {
  configured: boolean
  authorizeUrl?: string
  state?: string
}

export interface GithubOAuthStatusResponse {
  configured: boolean
  bound?: boolean
  githubLogin?: string
  githubUserId?: number
}

// === Repository ===

export interface GithubRepository {
  id: string
  githubRepoId: number
  owner: string
  repoName: string
  fullName: string
  privateRepo: boolean
  defaultBranch: string | null
  htmlUrl: string | null
  description: string | null
  language: string | null
  githubUpdatedAt: string | null
}

// === Pull Request ===

export interface GithubPullRequest {
  id: string
  githubPrId: number
  githubRepoId: number
  number: number
  title: string
  state: string
  authorLogin: string | null
  baseBranch: string | null
  headBranch: string | null
  htmlUrl: string | null
  additions: number
  deletions: number
  changedFiles: number
  githubCreatedAt: string | null
  githubUpdatedAt: string | null
}

export interface GithubPullRequestFile {
  filename: string
  status: string
  additions: number
  deletions: number
  changes: number
  patch: string | null
}

// === PR Review ===

export interface CreatePrReviewRequest {
  owner: string
  repo: string
  pullRequestNumber: number
  reviewMode: string
  agentId?: string
}

export interface PrReviewJob {
  id: string
  projectId: string
  pullRequestId: string
  status: string
  reviewMode: string
  summary: string | null
  riskLevel: string | null
  modelProvider: string | null
  modelName: string | null
  tokenUsage: number | null
  errorMessage: string | null
  startedAt: string | null
  finishedAt: string | null
  createTime: string | null
}

export interface PrReviewFinding {
  id: string
  reviewJobId: string
  severity: string
  category: string
  filePath: string | null
  lineNumber: number | null
  title: string
  description: string | null
  suggestion: string | null
  codeSnippet: string | null
}

// === OAuth API ===

export function getOAuthAuthorize() {
  return client.get<GithubOAuthAuthorizeResponse>('/api/github/oauth/authorize')
}

export function getOAuthStatus() {
  return client.get<GithubOAuthStatusResponse>('/api/github/oauth/status')
}

export function unbindOAuth(bindingId: string) {
  return client.delete(`/api/github/oauth/bindings/${bindingId}`)
}

// === Repository API ===

export function syncRepositories() {
  return client.post<GithubRepository[]>('/api/github/repos/sync')
}

export function listRepositories() {
  return client.get<GithubRepository[]>('/api/github/repos')
}

// === Pull Request API ===

export function listPullRequests(owner: string, repo: string, state = 'open') {
  return client.get<GithubPullRequest[]>(`/api/github/repos/${owner}/${repo}/pull-requests`, { params: { state } })
}

export function getPullRequest(owner: string, repo: string, number: number) {
  return client.get<GithubPullRequest>(`/api/github/repos/${owner}/${repo}/pull-requests/${number}`)
}

export function getPullRequestFiles(owner: string, repo: string, number: number) {
  return client.get<GithubPullRequestFile[]>(`/api/github/repos/${owner}/${repo}/pull-requests/${number}/files`)
}

export function getPullRequestPatch(owner: string, repo: string, number: number) {
  return client.get<string>(`/api/github/repos/${owner}/${repo}/pull-requests/${number}/patch`)
}

// === PR Review API ===

export function createPrReview(projectId: string, data: CreatePrReviewRequest) {
  return client.post<PrReviewJob>(`/api/projects/${projectId}/github/pr-reviews`, data)
}

export function listPrReviews(projectId: string, page = 1, pageSize = 20) {
  return client.get<PrReviewJob[]>(`/api/projects/${projectId}/github/pr-reviews`, { params: { page, pageSize } })
}

export function getPrReviewDetail(reviewJobId: string) {
  return client.get<PrReviewJob>(`/api/github/pr-reviews/${reviewJobId}`)
}

export function getPrReviewFindings(reviewJobId: string) {
  return client.get<PrReviewFinding[]>(`/api/github/pr-reviews/${reviewJobId}/findings`)
}
