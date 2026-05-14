import client from '@/shared/api/client'
import type { ApiResponse, PageResult } from '@/shared/api/types'

export interface KnowledgeBaseItem {
  id: string
  projectId: string
  name: string
  description: string
  documentCount: number
  chunkCount: number
  status: string
  createTime: string
}

export interface KnowledgeDocument {
  id: string
  title: string
  documentType: string
  sourceType: string
  status: string
  chunkCount: number
  createTime: string
}

export interface DocumentChunk {
  id: string
  chunkIndex: number
  content: string
  tokenCount: number
}

export interface RagSearchResult {
  total: number
  references: RagReference[]
}

export interface RagReference {
  referenceType: string
  chunkId: string
  title: string
  filePath: string
  snippet: string
  score: number
  startLine: number | null
  endLine: number | null
}

export async function listKnowledgeBases(projectId: string, page: number, pageSize: number) {
  return client.get<ApiResponse<PageResult<KnowledgeBaseItem>>>(`/api/projects/${projectId}/knowledge-bases`, {
    params: { page, pageSize },
  })
}

export async function createKnowledgeBase(projectId: string, name: string, description: string) {
  return client.post<ApiResponse<KnowledgeBaseItem>>(`/api/projects/${projectId}/knowledge-bases`, {
    name,
    description,
    chunkSize: 200,
    chunkOverlap: 20,
  })
}

export async function uploadDocument(projectId: string, data: {
  knowledgeBaseId: string
  title: string
  documentType: string
  content: string
}) {
  return client.post<ApiResponse<KnowledgeDocument>>(`/api/projects/${projectId}/knowledge-documents`, {
    knowledgeBaseId: data.knowledgeBaseId,
    title: data.title,
    documentType: data.documentType,
    sourceType: 'MANUAL',
    fileName: data.title + '.md',
    content: data.content,
  })
}

export async function listDocuments(knowledgeBaseId: string, page: number, pageSize: number) {
  return client.get<ApiResponse<PageResult<KnowledgeDocument>>>(`/api/knowledge-bases/${knowledgeBaseId}/documents`, {
    params: { page, pageSize },
  })
}

export async function listChunks(documentId: string) {
  return client.get<ApiResponse<DocumentChunk[]>>(`/api/knowledge-documents/${documentId}/chunks`)
}

export async function searchRag(projectId: string, query: string) {
  return client.post<ApiResponse<RagSearchResult>>(`/api/projects/${projectId}/rag/search`, {
    query,
    knowledgeBaseId: '',
    limit: 5,
    includeContent: true,
  })
}
