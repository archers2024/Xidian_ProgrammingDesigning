import type { IsoDateTime } from './common'

export interface AdminUser {
  id: string
  username: string
  displayName: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  user: AdminUser
  sessionExpiresAt: IsoDateTime
}

/** GET /auth/csrf 的返回；请求头名称由后端下发，前端不写死 */
export interface CsrfToken {
  token: string
  headerName: string
}
