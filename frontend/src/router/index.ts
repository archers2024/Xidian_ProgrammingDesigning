import { createRouter, createWebHistory } from 'vue-router'

import { setupRouterGuards } from './guards'
import { routes } from './routes'
import './meta'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, _from, savedPosition) {
    // 后台内容区由 AdminLayout 自己回到顶部，这里不干预窗口滚动
    if (to.meta.layout === 'admin') return false
    return savedPosition ?? { top: 0 }
  },
})

setupRouterGuards(router)

export default router
