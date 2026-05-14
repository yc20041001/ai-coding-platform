import client from '@/shared/api/client'
import type { ApiResponse, PageResult } from '@/shared/api/types'

export interface ProjectMember {
  userId: string
  username: string
  email: string
  avatar: string | null
  role: string
  status: string
  joinedTime: string
}

export interface InviteMemberRequest {
  email: string
  role: string
}

export interface InviteMemberResponse {
  invitationId: string
  email: string
  role: string
  status: string
  expireTime: string
}

export interface UpdateMemberRoleRequest {
  role: string
}

export async function listMembers(projectId: string, page = 1, pageSize = 20) {
  return client.get<ApiResponse<PageResult<ProjectMember>>>(`/api/projects/${projectId}/members`, {
    params: { page, pageSize },
  })
}

export async function inviteMember(projectId: string, data: InviteMemberRequest) {
  return client.post<ApiResponse<InviteMemberResponse>>(`/api/projects/${projectId}/members`, data)
}

export async function updateMemberRole(projectId: string, userId: string, data: UpdateMemberRoleRequest) {
  return client.put<ApiResponse<boolean>>(`/api/projects/${projectId}/members/${userId}/role`, data)
}

export async function removeMember(projectId: string, userId: string) {
  return client.delete<ApiResponse<boolean>>(`/api/projects/${projectId}/members/${userId}`)
}
