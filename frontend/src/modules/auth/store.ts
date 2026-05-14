import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { setToken, setRefreshToken, clearAuth, getStoredUser, setStoredUser, getToken } from '@/shared/utils/auth'
import { login as loginApi, getMe, type LoginRequest, type LoginResponse, type UserInfo } from './api'
import type { ApiResponse } from '@/shared/api/types'
import type { AxiosResponse } from 'axios'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const refreshTokenVal = ref<string | null>(null)
  const user = ref<UserInfo | null>(getStoredUser() as UserInfo | null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => user.value?.username || '')
  const roles = computed(() => user.value?.roles || [])
  const isAdmin = computed(() => roles.value.includes('ADMIN'))

  async function loginAction(data: LoginRequest): Promise<boolean> {
    loading.value = true
    try {
      const res: AxiosResponse<ApiResponse<LoginResponse>> = await loginApi(data)
      const loginData = res.data.data
      token.value = loginData.accessToken
      refreshTokenVal.value = loginData.refreshToken
      setToken(loginData.accessToken)
      setRefreshToken(loginData.refreshToken)

      user.value = {
        id: String(loginData.user.id),
        username: loginData.user.username,
        email: loginData.user.email,
        roles: loginData.user.roles || [],
        permissions: [],
      }
      setStoredUser(user.value)
      await fetchMe()
      return true
    } catch {
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchMe(): Promise<void> {
    try {
      const res: AxiosResponse<ApiResponse<UserInfo>> = await getMe()
      user.value = res.data.data
      setStoredUser(user.value)
    } catch {
      // ignore
    }
  }

  function logout() {
    token.value = null
    refreshTokenVal.value = null
    user.value = null
    clearAuth()
  }

  return { token, refreshTokenVal, user, loading, isLoggedIn, username, roles, isAdmin, loginAction, fetchMe, logout }
})
