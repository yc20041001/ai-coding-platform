import type { Router } from 'vue-router'
import { getToken } from '@/shared/utils/auth'

export function setupAuthGuard(router: Router) {
  router.beforeEach((to, _from, next) => {
    const isAuthenticated = !!getToken()

    if (to.path === '/login') {
      if (isAuthenticated) {
        next('/')
      } else {
        next()
      }
      return
    }

    if (!isAuthenticated) {
      next('/login')
    } else {
      next()
    }
  })
}
