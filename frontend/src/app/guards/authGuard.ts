import type { Router } from 'vue-router'
import { getToken } from '@/shared/utils/auth'

const PUBLIC_PATHS = ['/login', '/public']

export function setupAuthGuard(router: Router) {
  router.beforeEach((to, _from, next) => {
    const isAuthenticated = !!getToken()

    if (PUBLIC_PATHS.includes(to.path)) {
      if (to.path === '/login' && isAuthenticated) {
        next('/dashboard')
        return
      }
      next()
      return
    }

    if (to.path === '/' || to.path === '') {
      next(isAuthenticated ? '/dashboard' : '/public')
      return
    }

    if (!isAuthenticated) {
      next('/login')
    } else {
      next()
    }
  })
}
