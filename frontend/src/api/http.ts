import axios, { AxiosError, type AxiosRequestConfig } from 'axios'

import { ensureCsrfToken, resetCsrfToken } from './csrf'
import type { ApiEnvelope } from '@/types/common'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

/** 后端成功业务码；契约确定后只保留实际使用的一个 */
const SUCCESS_CODES = new Set(['OK', 'SUCCESS', '0'])
const UNSAFE_METHODS = new Set(['post', 'put', 'patch', 'delete'])

/**
 * 统一请求实例：同源 Cookie 会话、超时、CSRF 请求头与错误归一化。
 * 页面不直接创建 Axios 实例，只调用 api/ 下的业务函数。
 */
export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  withCredentials: true,
  headers: { Accept: 'application/json' },
})

/** 统一业务错误：页面依据 code 分支，不依据中文提示文本判断 */
export class ApiError extends Error {
  readonly code: string
  readonly status: number
  readonly requestId: string | null

  constructor(code: string, message: string, status = 0, requestId: string | null = null) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.requestId = requestId
  }

  get isUnauthorized(): boolean {
    return this.status === 401
  }

  get isAborted(): boolean {
    return this.code === 'REQUEST_ABORTED'
  }

  get isNetworkIssue(): boolean {
    return this.code === 'NETWORK_ERROR'
  }
}

let unauthorizedHandler: (() => void) | null = null

/** 由 main.ts 注册：登录失效时清理前端会话并跳转登录页 */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler
}

function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  if (axios.isCancel(error)) return new ApiError('REQUEST_ABORTED', '请求已取消')
  if (error instanceof AxiosError) {
    const response = error.response
    const body = response?.data as Partial<ApiEnvelope<unknown>> | undefined
    if (response && body && typeof body.code === 'string' && body.code.length > 0) {
      return new ApiError(
        body.code,
        body.message || '请求失败',
        response.status,
        body.requestId ?? null,
      )
    }
    if (response) {
      return new ApiError(`HTTP_${response.status}`, `服务暂时不可用（${response.status}）`, response.status)
    }
    return new ApiError('NETWORK_ERROR', '网络连接失败，请检查网络后重试')
  }
  return new ApiError('UNKNOWN_ERROR', error instanceof Error ? error.message : '未知错误')
}

http.interceptors.request.use(async (config) => {
  const method = (config.method ?? 'get').toLowerCase()
  if (UNSAFE_METHODS.has(method)) {
    try {
      const csrf = await ensureCsrfToken()
      config.headers.set(csrf.headerName, csrf.token)
    } catch {
      // 令牌获取失败时仍发出请求，由后端返回 403，页面按业务码提示
    }
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    const normalized = toApiError(error)
    // 登录与会话接口的 401 由调用方处理，避免与路由守卫重复跳转
    const requestUrl = error instanceof AxiosError ? (error.config?.url ?? '') : ''
    if (normalized.isUnauthorized && !requestUrl.startsWith('/auth/')) {
      unauthorizedHandler?.()
    }
    return Promise.reject(normalized)
  },
)

function unwrap<T>(payload: unknown): T {
  if (payload && typeof payload === 'object' && 'code' in (payload as Record<string, unknown>)) {
    const envelope = payload as ApiEnvelope<T>
    if (SUCCESS_CODES.has(envelope.code)) return envelope.data
    throw new ApiError(
      envelope.code,
      envelope.message || '请求失败',
      200,
      envelope.requestId ?? null,
    )
  }
  return payload as T
}

/** 发请求并拆掉响应信封，返回 data */
export async function request<T>(config: AxiosRequestConfig, retried = false): Promise<T> {
  try {
    const response = await http.request<unknown>(config)
    return unwrap<T>(response.data)
  } catch (error) {
    const normalized = toApiError(error)
    // CSRF 令牌过期时刷新一次并重试，避免用户无感失败
    if (!retried && normalized.code === 'CSRF_INVALID') {
      resetCsrfToken()
      return request<T>(config, true)
    }
    throw normalized
  }
}

type RequestOptions = Pick<AxiosRequestConfig, 'params' | 'signal' | 'headers'>

export function apiGet<T>(url: string, options: RequestOptions = {}): Promise<T> {
  return request<T>({ ...options, method: 'GET', url })
}

export function apiPost<T>(url: string, data?: unknown, options: RequestOptions = {}): Promise<T> {
  return request<T>({ ...options, method: 'POST', url, data })
}

export function apiPut<T>(url: string, data?: unknown, options: RequestOptions = {}): Promise<T> {
  return request<T>({ ...options, method: 'PUT', url, data })
}

export function apiDelete<T>(url: string, options: RequestOptions = {}): Promise<T> {
  return request<T>({ ...options, method: 'DELETE', url })
}
