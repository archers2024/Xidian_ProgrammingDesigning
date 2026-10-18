import axios from 'axios'

import type { ApiEnvelope } from '@/types/common'
import type { CsrfToken } from '@/types/auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const FALLBACK_HEADER = import.meta.env.VITE_CSRF_HEADER || 'X-CSRF-TOKEN'

/**
 * 独立的 CSRF 客户端：不经过业务拦截器，避免“取令牌失败 → 再次取令牌”的循环。
 * 模拟模式下同样由 src/mocks 接管。
 */
export const csrfClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  withCredentials: true,
  headers: { Accept: 'application/json' },
})

let cachedToken: CsrfToken | null = null
let inflight: Promise<CsrfToken> | null = null

function normalizeToken(payload: unknown): CsrfToken {
  if (typeof payload === 'string' && payload.length > 0) {
    return { token: payload, headerName: FALLBACK_HEADER }
  }
  if (payload && typeof payload === 'object') {
    const record = payload as Record<string, unknown>
    const token = typeof record.token === 'string' ? record.token : ''
    const headerName = typeof record.headerName === 'string' ? record.headerName : FALLBACK_HEADER
    if (token) return { token, headerName }
  }
  throw new Error('CSRF 令牌响应格式不正确')
}

/** 登录、退出或令牌失效后必须清空缓存 */
export function resetCsrfToken(): void {
  cachedToken = null
  inflight = null
}

/** 取当前令牌；并发调用共用同一个请求 */
export async function ensureCsrfToken(force = false): Promise<CsrfToken> {
  if (!force && cachedToken) return cachedToken
  if (!force && inflight) return inflight

  const request = csrfClient
    .get<ApiEnvelope<unknown> | unknown>('/auth/csrf')
    .then((response) => {
      const body = response.data as ApiEnvelope<unknown> | unknown
      const data =
        body && typeof body === 'object' && 'data' in (body as Record<string, unknown>)
          ? (body as ApiEnvelope<unknown>).data
          : body
      cachedToken = normalizeToken(data)
      return cachedToken
    })
    .finally(() => {
      inflight = null
    })

  inflight = request
  return request
}
