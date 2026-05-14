import { ref, reactive } from 'vue'
import type { PageResult } from '@/shared/api/types'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from '@/shared/api/types'

export function usePagination<T>(
  fetchFn: (page: number, pageSize: number) => Promise<AxiosResponse<ApiResponse<PageResult<T>>>>,
) {
  const loading = ref(false)
  const records = ref<T[]>([])
  const pagination = reactive({
    page: 1,
    pageSize: 10,
    total: 0,
  })

  async function load(page?: number, pageSize?: number) {
    if (page !== undefined) pagination.page = page
    if (pageSize !== undefined) pagination.pageSize = pageSize
    loading.value = true
    try {
      const res = await fetchFn(pagination.page, pagination.pageSize)
      const data = res.data.data
      records.value = data.records
      pagination.total = data.total
    } catch (e: unknown) {
      const err = e as { message?: string }
      console.error('Pagination load failed:', err.message)
    } finally {
      loading.value = false
    }
  }

  return { loading, records, pagination, load }
}
