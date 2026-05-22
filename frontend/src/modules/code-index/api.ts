import client from '@/shared/api/client'
import type { ApiResponse } from '@/shared/api/types'

export interface CodeIndexSummaryResponse {
  projectId: string
  fileCount: number
  symbolCount: number
  chunkCount: number
  indexedAt: string
  mock: boolean
}

export interface CodeIndexFileResponse {
  id: string
  projectId: string
  filePath: string
  language: string
  fileSize: number
  lineCount: number
  status: string
  indexedAt: string
}

export interface CodeIndexSymbolResponse {
  id: string
  projectId: string
  fileId: string
  symbolName: string
  symbolType: string
  language: string
  filePath: string
  startLine: number
  endLine: number
  snippet: string
}

export interface CodeSearchRequest {
  keyword: string
  searchType?: string
  branch?: string
  language?: string
  pathPrefix?: string
  limit?: number
}

export interface CodeSearchResultResponse {
  resultType: string
  filePath: string
  symbolName: string
  symbolType: string
  startLine: number
  endLine: number
  snippet: string
}

export interface CodeSearchResponse {
  results: CodeSearchResultResponse[]
  totalCount: number
  keyword: string
  searchType: string
}

export async function buildCodeIndex(projectId: string, data: { branch?: string; pathPrefix?: string; maxFiles?: number }) {
  return client.post<ApiResponse<CodeIndexSummaryResponse>>(`/api/projects/${projectId}/code-index/build`, data)
}

export async function getCodeIndexSummary(projectId: string) {
  return client.get<ApiResponse<CodeIndexSummaryResponse>>(`/api/projects/${projectId}/code-index/summary`)
}

export async function listCodeIndexFiles(projectId: string, params: { branch?: string; pathPrefix?: string; limit?: number }) {
  return client.get<ApiResponse<CodeIndexFileResponse[]>>(`/api/projects/${projectId}/code-index/files`, { params })
}

export async function listCodeIndexSymbols(projectId: string, params: { branch?: string; symbolType?: string; limit?: number }) {
  return client.get<ApiResponse<CodeIndexSymbolResponse[]>>(`/api/projects/${projectId}/code-index/symbols`, { params })
}

export async function searchCodeIndex(projectId: string, data: CodeSearchRequest) {
  return client.post<ApiResponse<CodeSearchResponse>>(`/api/projects/${projectId}/code-index/search`, data)
}
