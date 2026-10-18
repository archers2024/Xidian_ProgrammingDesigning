import type { AdminUser } from '@/types/auth'
import {
  type MockActivity,
  type MockCheckinSession,
  type MockRegistration,
  mockActivities,
  mockRegistrations,
  mockSessions,
} from './fixtures'

/** 模拟接口的进程内数据，页面刷新后重置为初始演示数据 */
export interface MockDatabase {
  session: AdminUser | null
  activities: MockActivity[]
  registrations: MockRegistration[]
  sessions: MockCheckinSession[]
}

export const mockDb: MockDatabase = {
  session: null,
  activities: mockActivities,
  registrations: mockRegistrations,
  sessions: mockSessions,
}

/** 仅模拟模式使用的演示账号，真实环境由后端管理员表校验 */
export const demoCredentials = {
  username: 'admin',
  password: 'admin123',
}
