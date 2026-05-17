import client from '@/shared/api/client'
import type { ApiResponse } from '@/shared/api/types'

export interface LoginRequest {
  email: string
  password: string
  captchaId?: string
  captchaCode?: string
}

export interface CaptchaResponse {
  captchaId: string
  imageBase64: string
  expireSeconds: number
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: LoginUser
}

export interface UserInfo {
  id: string
  username: string
  email: string
  status?: string
  roles: string[]
  permissions: string[]
}

export interface LoginUser {
  id: string
  username: string
  email: string
  roles: string[]
}

export async function login(data: LoginRequest) {
  return client.post('/api/auth/login', data)
}

export async function refreshToken(token: string) {
  return client.post('/api/auth/refresh', { refreshToken: token })
}

export async function getMe() {
  return client.get('/api/auth/me')
}

export function getCaptcha() {
  return client.get<ApiResponse<CaptchaResponse>>('/api/auth/captcha')
}
