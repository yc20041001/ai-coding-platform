import client from '@/shared/api/client'
import type { ApiResponse, PageResult } from '@/shared/api/types'

export interface ProjectItem {
  id: string
  name: string
  description: string
  techStack: string[]
  status: string
  createTime: string
}

export interface ProjectDetail {
  id: string
  name: string
  description: string
  techStack: string[]
  status: string
  createTime: string
  updateTime: string
}

export interface ProjectOverview {
  taskCount: number
  completedTaskCount: number
  runningTaskCount: number
  memberCount: number
  documentCount: number
  agentCount: number
  tokenUsage: number
}

export interface CreateProjectRequest {
  name: string
  description: string
  techStack: string[]
}

export async function listProjects(page: number, pageSize: number) {
  return client.get<ApiResponse<PageResult<ProjectItem>>>('/api/projects', {
    params: { page, pageSize },
  })
}

export async function getProject(id: string) {
  return client.get<ApiResponse<ProjectDetail>>(`/api/projects/${id}`)
}

export async function getProjectOverview(id: string) {
  return client.get<ApiResponse<ProjectOverview>>(`/api/projects/${id}/overview`)
}

export async function createProject(data: CreateProjectRequest) {
  return client.post<ApiResponse<ProjectDetail>>('/api/projects', data)
}
