import { csrfClient } from '@/api/csrf'
import { http } from '@/api/http'
import { createMockAdapter, mockEnabled } from './adapter'

let installed = false

/**
 * 安装模拟适配层。只在 VITE_USE_MOCK=true 时由 main.ts 调用，
 * 切换真实接口时无需改动 api/ 与页面代码。
 */
export function installMocks(): void {
  if (installed || !mockEnabled) return
  installed = true
  const adapter = createMockAdapter()
  http.defaults.adapter = adapter
  csrfClient.defaults.adapter = adapter
  console.info('[mocks] 已启用模拟接口，演示账号 admin / admin123')
}
