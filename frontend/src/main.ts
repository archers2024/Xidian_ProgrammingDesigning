import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import { setUnauthorizedHandler } from './api/http'
import { router } from './router'
import { useAuthStore } from './stores/auth'

import 'element-plus/dist/index.css'
import './styles/tokens.css'
import './styles/global.css'
import './styles/element-theme.css'
import './styles/transitions.css'

async function bootstrap(): Promise<void> {
  // 模拟接口按需加载：生产构建中该分支被静态移除，演示数据不会进入正式产物
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    const { installMocks } = await import('./mocks')
    installMocks()
  }

  const app = createApp(App)
  const pinia = createPinia()
  app.use(pinia)

  // 会话失效统一处理：清空前端身份并回到登录页，保留原地址便于登录后返回
  setUnauthorizedHandler(() => {
    const auth = useAuthStore(pinia)
    auth.clearSession()
    const current = router.currentRoute.value
    if (current.name !== 'admin-login') {
      void router.replace({ name: 'admin-login', query: { redirect: current.fullPath } })
    }
  })

  app.use(router)
  await router.isReady()
  app.mount('#app')
}

void bootstrap()
