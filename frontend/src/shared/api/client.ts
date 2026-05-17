import axios, { type AxiosInstance, type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/shared/utils/auth'
import type { ApiResponse } from './types'

export interface ApiError {
  code: string
  message: string
  traceId?: string
  status?: number
}

const client: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

client.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body.code && body.code !== 'OK') {
      if (body.code === 'UNAUTHORIZED') {
        // Don't redirect for login failures — let the login page show the error
        if (!response.config.url?.includes('/auth/login')) {
          clearAuth()
          window.location.href = '/login'
        }
      }
      return Promise.reject({
        code: body.code,
        message: body.message || '请求失败',
        traceId: body.traceId,
      })
    }
    return response
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const data = error.response?.data
    if (data?.code === 'UNAUTHORIZED') {
      // Don't redirect for login failures — let the login page show the error
      if (!error.config?.url?.includes('/auth/login')) {
        clearAuth()
        window.location.href = '/login'
      }
    }
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时')
    } else if (!error.response) {
      ElMessage.error('网络错误，请检查后端是否已启动')
    }
    return Promise.reject({
      code: data?.code || 'NETWORK_ERROR',
      message: data?.message || error.message || '网络错误',
      traceId: data?.traceId,
    })
  },
)

export default client
