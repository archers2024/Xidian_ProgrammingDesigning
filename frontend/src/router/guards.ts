import type { Router } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const APP_TITLE = import.meta.env.VITE_APP_TITLE || '校园活动管理'

/**
 * 路由守卫：
 * 1. 后台页面需要已登录会话，未登录跳到登录页并记住来源；
 * 2. 已登录再访问登录页时回到工作台；
 * 3. 每次导航后更新浏览器标题。
 */
export function setupRouterGuards(router: Router): void {
  router.beforeEach(async (to) => {
    const auth = useAuthStore()

    if (to.meta.requiresAuth) {
      if (!auth.initialized) await auth.ensureSession()
      if (!auth.isAuthenticated) {
        return {
          name: 'admin-login',
          query: { redirect: to.fullPath },
          replace: true,
        }
      }
    }

    if (to.name === 'admin-login' && auth.isAuthenticated) {
      return { name: 'admin-dashboard', replace: true }
    }

    return true
  })

  router.afterEach((to) => {
    const title = typeof to.meta.title === 'string' ? to.meta.title : ''
    document.title = title ? `${title} · ${APP_TITLE}` : APP_TITLE
  })
}
