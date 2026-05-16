import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from '@/shared/api/types'

export interface UseRequestReturn<T> {
  loading: import('vue').Ref<boolean>
  error: import('vue').Ref<string | null>
  execute: () => Promise<T | null>
}

export function useRequest<T>(
  fn: () => Promise<AxiosResponse<ApiResponse<T>>>,
): UseRequestReturn<T> {
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function execute(): Promise<T | null> {
    loading.value = true
    error.value = null
    try {
      const res = await fn()
      return res.data.data
    } catch (e: unknown) {
      const err = e as { code?: string; message?: string }
      error.value = err.message || 'Request failed'
      if (err.code !== 'UNAUTHORIZED') {
        ElMessage.error(err.message || 'Request failed')
      }
      return null
    } finally {
      loading.value = false
    }
  }

  return { loading, error, execute }
}
