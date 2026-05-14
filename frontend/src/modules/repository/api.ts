import client from '@/shared/api/client'
import type { ApiResponse, PageResult, PageQuery } from '@/shared/api/types'

export interface GithubRepository {
  provider: string
  fullName: string
  name: string
  description: string
  isPrivate: boolean
  defaultBranch: string
  htmlUrl: string
  cloneUrl: string
}

export interface RepositoryInfo {
  repositoryId: string
  status: string
}

export interface RepositoryBranch {
  name: string
  commitHash: string
  protectedBranch: boolean
  lastSyncTime: string
}

export interface DiffFile {
  path: string
  changeType: string
  additions: number
  deletions: number
  patch: string
}

export interface RepositoryDiff {
  base: string
  head: string
  files: DiffFile[]
}

export interface GitOperation {
  operationId: string
  status: string
  commitHash: string
  prUrl: string
}

export interface BindRepositoryRequest {
  provider: string
  repoFullName: string
  repoUrl: string
  cloneUrl: string
  defaultBranch?: string
}

export interface CloneRepositoryRequest {
  branch?: string
  force?: boolean
}

export interface PullRepositoryRequest {
  branch?: string
}

export async function listGithubRepos(keyword?: string, page = 1, pageSize = 10) {
  return client.get<ApiResponse<PageResult<GithubRepository>>>('/api/github/repositories', {
    params: { keyword, page, pageSize },
  })
}

export async function bindRepository(projectId: string, data: BindRepositoryRequest) {
  return client.post<ApiResponse<RepositoryInfo>>(`/api/projects/${projectId}/repository/bind`, data)
}

export async function cloneRepository(projectId: string, data: CloneRepositoryRequest) {
  return client.post<ApiResponse<GitOperation>>(`/api/projects/${projectId}/repository/clone`, data)
}

export async function pullRepository(projectId: string, data: PullRepositoryRequest) {
  return client.post<ApiResponse<GitOperation>>(`/api/projects/${projectId}/repository/pull`, data)
}

export async function getBranches(projectId: string) {
  return client.get<ApiResponse<RepositoryBranch[]>>(`/api/projects/${projectId}/repository/branches`)
}

export async function getDiff(projectId: string, base: string, head: string) {
  return client.get<ApiResponse<RepositoryDiff>>(`/api/projects/${projectId}/repository/diff`, {
    params: { base, head },
  })
}
