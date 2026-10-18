/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 站点标题，用于浏览器标题与品牌区 */
  readonly VITE_APP_TITLE: string
  /** 接口基础路径，默认 /api/v1 */
  readonly VITE_API_BASE_URL: string
  /** CSRF 请求头名称，需与后端 Spring Security 配置一致 */
  readonly VITE_CSRF_HEADER: string
  /** 'true' 时启用 src/mocks 的模拟适配层 */
  readonly VITE_USE_MOCK: string
  /** 开发代理目标 */
  readonly VITE_API_PROXY_TARGET?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
