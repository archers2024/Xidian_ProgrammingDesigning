import { apiGet, apiPost, request } from '@/api/http'
import type { AdminUser, LoginPayload, LoginResult } from '@/types/auth'

/** 刷新页面后先查当前身份；401 表示未登录 */
export function fetchCurrentAdmin(signal?: AbortSignal): Promise<AdminUser> {
  return apiGet<AdminUser>('/auth/me', { signal })
}

export function loginAdmin(payload: LoginPayload): Promise<LoginResult> {
  return apiPost<LoginResult>('/auth/login', payload)
}

export function logoutAdmin(): Promise<void> {
  return request<void>({ method: 'POST', url: '/auth/logout' })
}
