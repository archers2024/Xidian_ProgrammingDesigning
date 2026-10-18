import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { fetchCurrentAdmin, loginAdmin, logoutAdmin } from '@/api/admin/auth'
import { ensureCsrfToken, resetCsrfToken } from '@/api/csrf'
import type { AdminUser, LoginPayload } from '@/types/auth'

type SessionStatus = 'idle' | 'loading' | 'ready'

/**
 * 管理员会话。刷新页面后由 /auth/me 恢复；登录失效时由 http 层的 401 处理清空。
 * 登录态只保存内存，不写 localStorage，避免与 HttpOnly 会话 Cookie 形成两套真相。
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<AdminUser | null>(null)
  const status = ref<SessionStatus>('idle')

  const initialized = computed(() => status.value === 'ready')
  const isAuthenticated = computed(() => user.value !== null)
  const displayName = computed(() => user.value?.displayName ?? '未登录')

  async function ensureSession(): Promise<void> {
    if (status.value !== 'idle') return
    status.value = 'loading'
    try {
      user.value = await fetchCurrentAdmin()
    } catch {
      // 401 表示未登录；网络异常同样按未登录处理，页面会给出重试入口
      user.value = null
    } finally {
      status.value = 'ready'
    }
  }

  async function signIn(payload: LoginPayload): Promise<AdminUser> {
    const result = await loginAdmin(payload)
    user.value = result.user
    status.value = 'ready'
    // 登录后会话标识已更新，必须换用新的 CSRF 令牌
    resetCsrfToken()
    await ensureCsrfToken(true).catch(() => undefined)
    return result.user
  }

  async function signOut(): Promise<void> {
    try {
      await logoutAdmin()
    } catch {
      // 会话已失效时同样视为退出成功
    }
    clearSession()
  }

  function clearSession(): void {
    user.value = null
    status.value = 'idle'
    resetCsrfToken()
  }

  return {
    user,
    status,
    initialized,
    isAuthenticated,
    displayName,
    ensureSession,
    signIn,
    signOut,
    clearSession,
  }
})
