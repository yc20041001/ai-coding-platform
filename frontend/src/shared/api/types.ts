export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId?: string
  timestamp?: string
}

export interface PageResult<T> {
  records: T[]
  page: number
  pageSize: number
  total: number
  hasNext: boolean
}

export interface PageQuery {
  page: number
  pageSize: number
  sort?: string
}

export interface BizError {
  code: string
  message: string
  traceId?: string
}
