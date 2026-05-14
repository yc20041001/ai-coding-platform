import client from '@/shared/api/client'
import type { ApiResponse, PageResult } from '@/shared/api/types'

export interface ChatSession {
  id: string
  projectId: string
  title: string
  sessionType: string
  lastMessage: string | null
  lastMessageTime: string | null
  status: string
  createTime: string
}

export interface ChatMessage {
  id: string
  sessionId: string
  senderType: string
  senderName: string | null
  content: string
  status: string
  tokenUsage: number
  references: ChatReference[]
  createTime: string
}

export interface ChatReference {
  id: string
  referenceType: string
  referenceId: string | null
  title: string | null
  url: string | null
  filePath: string | null
  score: number | null
  snippet: string | null
  startLine: number | null
  endLine: number | null
}

export interface SendMessageRequest {
  content: string
  agentIds: string[]
  stream: boolean
  useRag: boolean
  knowledgeBaseId?: string
  ragLimit: number
}

export interface SendMessageResponse {
  userMessageId: string
  assistantMessageId: string
  streamUrl: string
  ragUsed: boolean
  references: ChatReference[]
}

export async function createSession(projectId: string, title: string) {
  return client.post<ApiResponse<ChatSession>>(`/api/projects/${projectId}/chat/sessions`, {
    title,
    sessionType: 'PROJECT',
  })
}

export async function listSessions(projectId: string, page: number, pageSize: number) {
  return client.get<ApiResponse<PageResult<ChatSession>>>(`/api/projects/${projectId}/chat/sessions`, {
    params: { page, pageSize },
  })
}

export async function getMessages(sessionId: string, limit = 50) {
  return client.get<ApiResponse<ChatMessage[]>>(`/api/chat/sessions/${sessionId}/messages`, {
    params: { limit },
  })
}

export async function sendMessage(sessionId: string, data: SendMessageRequest) {
  return client.post<ApiResponse<SendMessageResponse>>(`/api/chat/sessions/${sessionId}/messages`, data)
}
