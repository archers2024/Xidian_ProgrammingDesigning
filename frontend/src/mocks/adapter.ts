import {
  AxiosError,
  CanceledError,
  type AxiosAdapter,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'

import { MockHttpError, routes, type MockContext, type MockRoute } from './handlers'
import { mockDb } from './db'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const MIN_LATENCY = 160
const MAX_LATENCY = 420

interface CompiledRoute {
  route: MockRoute
  regex: RegExp
  keys: string[]
}

const escapeRegExp = (value: string): string => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const compiledRoutes: CompiledRoute[] = routes.map((route) => {
  const keys: string[] = []
  const pattern = escapeRegExp(route.path).replace(/:([A-Za-z0-9_]+)/g, (_match, key: string) => {
    keys.push(key)
    return '([^/]+)'
  })
  return { route, regex: new RegExp(`^${pattern}$`), keys }
})

let requestCounter = 0

const randomLatency = (): number =>
  Math.round(MIN_LATENCY + Math.random() * (MAX_LATENCY - MIN_LATENCY))

function resolvePath(url: string): string {
  let path = url
  if (/^https?:\/\//i.test(path)) {
    try {
      path = new URL(path).pathname
    } catch {
      // 保持原值，由匹配失败返回“未实现”
    }
  }
  const queryIndex = path.indexOf('?')
  if (queryIndex >= 0) path = path.slice(0, queryIndex)
  if (API_BASE_URL && path.startsWith(API_BASE_URL)) path = path.slice(API_BASE_URL.length)
  return path.startsWith('/') ? path : `/${path}`
}

function buildQuery(config: InternalAxiosRequestConfig): URLSearchParams {
  const params = new URLSearchParams()
  const rawParams = config.params as Record<string, unknown> | undefined
  if (rawParams) {
    for (const [key, value] of Object.entries(rawParams)) {
      if (value === undefined || value === null) continue
      params.set(key, String(value))
    }
  }
  const urlQuery = config.url?.split('?')[1]
  if (urlQuery) {
    new URLSearchParams(urlQuery).forEach((value, key) => params.set(key, value))
  }
  return params
}

function parseBody(data: unknown): Record<string, unknown> {
  if (!data) return {}
  if (typeof data === 'string') {
    try {
      const parsed: unknown = JSON.parse(data)
      return parsed && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : {}
    } catch {
      return {}
    }
  }
  if (typeof data === 'object' && !(data instanceof FormData)) {
    return data as Record<string, unknown>
  }
  return {}
}

function waitFor(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new CanceledError())
      return
    }
    const onAbort = (): void => {
      clearTimeout(timer)
      reject(new CanceledError())
    }
    const timer = setTimeout(() => {
      signal?.removeEventListener('abort', onAbort)
      resolve()
    }, ms)
    signal?.addEventListener('abort', onAbort, { once: true })
  })
}

function successResponse(
  config: InternalAxiosRequestConfig,
  data: unknown,
  requestId: string,
): AxiosResponse {
  return {
    data: { code: 'OK', message: '成功', data: data ?? null, requestId },
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
    request: {},
  }
}

function failure(
  config: InternalAxiosRequestConfig,
  status: number,
  code: string,
  message: string,
  requestId: string,
): never {
  const response: AxiosResponse = {
    data: { code, message, data: null, requestId },
    status,
    statusText: code,
    headers: {},
    config,
    request: {},
  }
  throw new AxiosError(message, code, config, {}, response)
}

/**
 * 模拟适配器：与真实接口使用相同的路径、响应信封和业务码。
 * 只替换传输层，页面与 api/ 模块无需任何分支。
 */
export function createMockAdapter(): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    const method = (config.method ?? 'get').toUpperCase()
    const path = resolvePath(config.url ?? '')
    const requestId = `mock-${(++requestCounter).toString(36)}`
    const signal = config.signal instanceof AbortSignal ? config.signal : undefined

    await waitFor(randomLatency(), signal)

    const matched = compiledRoutes.find(
      (item) => item.route.method === method && item.regex.test(path),
    )
    if (!matched) {
      failure(config, 404, 'MOCK_NOT_FOUND', `模拟接口未实现：${method} ${path}`, requestId)
    }

    const execResult = matched.regex.exec(path)
    const params: Record<string, string> = {}
    matched.keys.forEach((key, index) => {
      const value = execResult?.[index + 1] ?? ''
      params[key] = decodeURIComponent(value)
    })

    if (matched.route.requiresAdmin && !mockDb.session) {
      failure(config, 401, 'UNAUTHENTICATED', '登录状态已失效，请重新登录', requestId)
    }

    const context: MockContext = {
      method,
      path,
      params,
      query: buildQuery(config),
      body: parseBody(config.data),
    }

    try {
      const data = await matched.route.handle(context)
      return successResponse(config, data, requestId)
    } catch (error) {
      if (error instanceof MockHttpError) {
        failure(config, error.status, error.code, error.message, requestId)
      }
      throw error
    }
  }
}

/** 是否处于模拟模式，由 .env 的 VITE_USE_MOCK 控制 */
export const mockEnabled = import.meta.env.VITE_USE_MOCK === 'true'
