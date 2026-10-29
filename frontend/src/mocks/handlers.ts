import type { ActivityDisplayStatus, PageResult } from '@/types/common'
import type { RegistrationSummary } from '@/types/registration'
import type { CheckinSummary } from '@/types/checkin'
import { type MockActivity, colleges, activityTypes, demoAdmin } from './fixtures'
import {
  resolveDisplayStatus,
  resolveUnavailableReason,
  toActivityDetail,
  toActivitySummary,
  toPublicActivity,
} from './fixtures'
import { demoCredentials, mockDb } from './db'

export class MockHttpError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.name = 'MockHttpError'
    this.status = status
    this.code = code
  }
}

export interface MockContext {
  method: string
  path: string
  params: Record<string, string>
  query: URLSearchParams
  body: Record<string, unknown>
}

export interface MockRoute {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  path: string
  /** 后台接口要求已登录 */
  requiresAdmin?: boolean
  handle: (ctx: MockContext) => unknown
}

const nowIso = (): string => new Date().toISOString()
const createId = (prefix: string): string =>
  `${prefix}-${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`

function readString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}

function readDate(value: unknown, label: string): string {
  const raw = readString(value)
  const time = Date.parse(raw)
  if (!raw || Number.isNaN(time)) {
    throw new MockHttpError(400, 'VALIDATION_ERROR', `${label}格式不正确`)
  }
  return new Date(time).toISOString()
}

function parsePageParam(raw: string | null, fallback: number, max: number): number {
  const value = Number(raw)
  if (!Number.isInteger(value) || value < 1) return fallback
  return Math.min(value, max)
}

function findActivity(id: string): MockActivity {
  const activity = mockDb.activities.find((item) => item.id === id)
  if (!activity) throw new MockHttpError(404, 'ACTIVITY_NOT_FOUND', '活动不存在或已被删除')
  return activity
}

function findPublishedActivity(id: string): MockActivity {
  const activity = findActivity(id)
  if (activity.deletedAt) throw new MockHttpError(410, 'ACTIVITY_UNAVAILABLE', '活动已失效')
  return activity
}

function collegeNameOf(collegeId: string): string {
  return colleges.find((item) => item.id === collegeId)?.name ?? '未知学院'
}

interface ActivityInput {
  name: string
  typeId: string
  description: string
  location: string
  startAt: string
  endAt: string
  registrationDeadline: string
  capacity: number
  liveUrl: string | null
}

/** 与后端一致的基础校验；真实规则以服务端为准，模拟层只保证页面可开发 */
function validateActivityInput(body: Record<string, unknown>, minCapacity = 1): ActivityInput {
  const name = readString(body.name)
  if (name.length < 2 || name.length > 80) {
    throw new MockHttpError(400, 'VALIDATION_ERROR', '活动名称需为 2 至 80 个字符')
  }

  const typeId = readString(body.typeId)
  if (!activityTypes.some((item) => item.id === typeId)) {
    throw new MockHttpError(400, 'VALIDATION_ERROR', '请选择有效的活动类型')
  }

  const location = readString(body.location)
  if (location.length === 0 || location.length > 120) {
    throw new MockHttpError(400, 'VALIDATION_ERROR', '活动地点需为 1 至 120 个字符')
  }

  const description = readString(body.description)
  if (description.length > 2000) {
    throw new MockHttpError(400, 'VALIDATION_ERROR', '活动简介不能超过 2000 个字符')
  }

  const startAt = readDate(body.startAt, '开始时间')
  const endAt = readDate(body.endAt, '结束时间')
  const registrationDeadline = readDate(body.registrationDeadline, '报名截止时间')

  if (Date.parse(startAt) >= Date.parse(endAt)) {
    throw new MockHttpError(400, 'VALIDATION_ERROR', '开始时间必须早于结束时间')
  }
  if (Date.parse(registrationDeadline) > Date.parse(startAt)) {
    throw new MockHttpError(400, 'VALIDATION_ERROR', '报名截止时间不能晚于活动开始时间')
  }

  const capacity = Number(body.capacity)
  if (!Number.isInteger(capacity) || capacity < 1 || capacity > 100000) {
    throw new MockHttpError(400, 'VALIDATION_ERROR', '人数上限需为正整数')
  }
  if (capacity < minCapacity) {
    throw new MockHttpError(400, 'VALIDATION_ERROR', `人数上限不能小于已有报名人数 ${minCapacity}`)
  }

  const liveUrlRaw = readString(body.liveUrl)
  let liveUrl: string | null = null
  if (liveUrlRaw.length > 0) {
    if (!/^https?:\/\/\S+$/i.test(liveUrlRaw)) {
      throw new MockHttpError(400, 'VALIDATION_ERROR', '直播链接需以 http:// 或 https:// 开头')
    }
    liveUrl = liveUrlRaw
  }

  return { name, typeId, description, location, startAt, endAt, registrationDeadline, capacity, liveUrl }
}

const authRoutes: MockRoute[] = [
  {
    method: 'GET',
    path: '/auth/csrf',
    handle: () => ({ token: `mock-csrf-${Date.now().toString(36)}`, headerName: 'X-CSRF-TOKEN' }),
  },
  {
    method: 'POST',
    path: '/auth/login',
    handle: (ctx) => {
      const username = readString(ctx.body.username)
      const password = readString(ctx.body.password)
      if (username !== demoCredentials.username || password !== demoCredentials.password) {
        throw new MockHttpError(401, 'INVALID_CREDENTIALS', '账号或密码不正确')
      }
      mockDb.session = demoAdmin
      return {
        user: demoAdmin,
        sessionExpiresAt: new Date(Date.now() + 8 * 60 * 60 * 1000).toISOString(),
      }
    },
  },
  {
    method: 'GET',
    path: '/auth/me',
    handle: () => {
      if (!mockDb.session) {
        throw new MockHttpError(401, 'UNAUTHENTICATED', '登录状态已失效，请重新登录')
      }
      return mockDb.session
    },
  },
  {
    method: 'POST',
    path: '/auth/logout',
    handle: () => {
      mockDb.session = null
      return null
    },
  },
]

const dictionaryRoutes: MockRoute[] = [
  {
    method: 'GET',
    path: '/public/activity-types',
    handle: () => ({ items: activityTypes }),
  },
  {
    method: 'GET',
    path: '/public/colleges',
    handle: () => ({ items: colleges }),
  },
]

const dashboardRoutes: MockRoute[] = [
  {
    method: 'GET',
    path: '/admin/dashboard',
    requiresAdmin: true,
    handle: () => {
      const now = Date.now()
      const activities = mockDb.activities.filter((item) => item.deletedAt === null)
      const activityCounts = { total: activities.length, draft: 0, registering: 0, closed: 0, ended: 0 }
      for (const activity of activities) {
        const status = resolveDisplayStatus(activity, now)
        if (status === 'DRAFT') activityCounts.draft += 1
        else if (status === 'REGISTERING') activityCounts.registering += 1
        else if (status === 'CLOSED') activityCounts.closed += 1
        else activityCounts.ended += 1
      }
      const statusOrder: Record<ActivityDisplayStatus, number> = {
        REGISTERING: 0,
        CLOSED: 1,
        DRAFT: 2,
        ENDED: 3,
      }
      const recentActivities = [...activities]
        .sort((a, b) => {
          const diff =
            statusOrder[resolveDisplayStatus(a, now)] - statusOrder[resolveDisplayStatus(b, now)]
          if (diff !== 0) return diff
          return Date.parse(a.startAt) - Date.parse(b.startAt)
        })
        .slice(0, 5)
        .map((activity) => toActivitySummary(activity, now))

      return {
        activityCounts,
        registrationCount: activities.reduce((sum, item) => sum + item.registeredCount, 0),
        checkinCount: mockDb.registrations.filter((item) => item.checkedInAt !== null).length,
        recentActivities,
      }
    },
  },
]

const adminActivityRoutes: MockRoute[] = [
  {
    method: 'GET',
    path: '/admin/activities',
    requiresAdmin: true,
    handle: (ctx) => {
      const now = Date.now()
      let items = mockDb.activities.filter((item) => item.deletedAt === null)

      const keyword = (ctx.query.get('keyword') ?? '').trim()
      if (keyword) {
        items = items.filter(
          (item) => item.name.includes(keyword) || item.location.includes(keyword),
        )
      }

      const typeId = ctx.query.get('typeId')
      if (typeId) items = items.filter((item) => item.typeId === typeId)

      const status = ctx.query.get('status') as ActivityDisplayStatus | null
      if (status) items = items.filter((item) => resolveDisplayStatus(item, now) === status)

      const dateFrom = ctx.query.get('dateFrom')
      const dateTo = ctx.query.get('dateTo')
      if (dateFrom) {
        const from = Date.parse(dateFrom)
        if (!Number.isNaN(from)) items = items.filter((item) => Date.parse(item.startAt) >= from)
      }
      if (dateTo) {
        const to = Date.parse(dateTo)
        if (!Number.isNaN(to)) items = items.filter((item) => Date.parse(item.startAt) <= to)
      }

      items = [...items].sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))

      const page = parsePageParam(ctx.query.get('page'), 1, 1000)
      const pageSize = parsePageParam(ctx.query.get('pageSize'), 10, 50)
      const total = items.length
      const start = (page - 1) * pageSize

      const result: PageResult<ReturnType<typeof toActivitySummary>> = {
        items: items.slice(start, start + pageSize).map((item) => toActivitySummary(item, now)),
        page,
        pageSize,
        total,
      }
      return result
    },
  },
  {
    method: 'POST',
    path: '/admin/activities',
    requiresAdmin: true,
    handle: (ctx) => {
      const input = validateActivityInput(ctx.body)
      const activity: MockActivity = {
        id: createId('act'),
        ...input,
        registeredCount: 0,
        publicationState: 'DRAFT',
        publishedAt: null,
        coverUrl: null,
        version: 1,
        createdAt: nowIso(),
        deletedAt: null,
        attachments: [],
      }
      mockDb.activities.unshift(activity)
      return { id: activity.id }
    },
  },
  {
    method: 'GET',
    path: '/admin/activities/:id/checkin-summary',
    requiresAdmin: true,
    handle: (ctx) => {
      const activity = findPublishedActivity(ctx.params.id)
      const session = mockDb.sessions.find((item) => item.activityId === activity.id) ?? null
      const summary: CheckinSummary = {
        activityId: activity.id,
        registeredCount: activity.registeredCount,
        checkedInCount: mockDb.registrations.filter(
          (item) => item.activityId === activity.id && item.checkedInAt !== null,
        ).length,
        sessionId: session?.id ?? null,
        sessionMode: session?.mode ?? null,
        sessionEnabled: session?.enabled ?? false,
        windowStart: session?.windowStart ?? null,
        windowEnd: session?.windowEnd ?? null,
      }
      return summary
    },
  },
  {
    method: 'GET',
    path: '/admin/activities/:id/registrations',
    requiresAdmin: true,
    handle: (ctx) => {
      const activity = findPublishedActivity(ctx.params.id)
      const items = mockDb.registrations
        .filter((item) => item.activityId === activity.id)
        .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))

      const page = parsePageParam(ctx.query.get('page'), 1, 1000)
      const pageSize = parsePageParam(ctx.query.get('pageSize'), 10, 100)
      const start = (page - 1) * pageSize
      const rows: RegistrationSummary[] = items.slice(start, start + pageSize).map((item) => ({
        id: item.id,
        name: item.name,
        phone: item.phone,
        collegeId: item.collegeId,
        collegeName: collegeNameOf(item.collegeId),
        registeredAt: item.createdAt,
        checkedIn: item.checkedInAt !== null,
        checkedInAt: item.checkedInAt,
      }))

      return { items: rows, page, pageSize, total: items.length }
    },
  },
  {
    method: 'GET',
    path: '/admin/activities/:id',
    requiresAdmin: true,
    handle: (ctx) => toActivityDetail(findPublishedActivity(ctx.params.id)),
  },
  {
    method: 'PUT',
    path: '/admin/activities/:id',
    requiresAdmin: true,
    handle: (ctx) => {
      const activity = findPublishedActivity(ctx.params.id)
      const version = Number(ctx.body.version)
      if (!Number.isInteger(version)) {
        throw new MockHttpError(400, 'VALIDATION_ERROR', '缺少活动版本号')
      }
      if (version !== activity.version) {
        throw new MockHttpError(
          409,
          'VERSION_CONFLICT',
          '活动已被其他管理员修改，请重新读取后再保存',
        )
      }
      const input = validateActivityInput(ctx.body, activity.registeredCount)
      Object.assign(activity, input)
      activity.version += 1
      return toActivityDetail(activity)
    },
  },
  {
    method: 'DELETE',
    path: '/admin/activities/:id',
    requiresAdmin: true,
    handle: (ctx) => {
      const activity = findPublishedActivity(ctx.params.id)
      activity.deletedAt = nowIso()
      activity.version += 1
      return null
    },
  },
  {
    method: 'POST',
    path: '/admin/activities/:id/publish',
    requiresAdmin: true,
    handle: (ctx) => {
      const activity = findPublishedActivity(ctx.params.id)
      if (activity.publicationState !== 'PUBLISHED') {
        activity.publicationState = 'PUBLISHED'
        activity.publishedAt = nowIso()
        activity.version += 1
      }
      const base = typeof window === 'undefined' ? 'http://localhost:5173' : window.location.origin
      return {
        activityId: activity.id,
        shareUrl: `${base}/events/${activity.id}`,
        registrationUrl: `${base}/events/${activity.id}/register`,
        validFrom: activity.publishedAt ?? nowIso(),
        validUntil: activity.registrationDeadline,
      }
    },
  },
]

const publicRoutes: MockRoute[] = [
  {
    method: 'GET',
    path: '/public/activities/:id',
    handle: (ctx) => {
      const activity = findActivity(ctx.params.id)
      if (activity.deletedAt || activity.publicationState !== 'PUBLISHED') {
        throw new MockHttpError(404, 'ACTIVITY_UNAVAILABLE', '活动不存在或尚未发布')
      }
      return toPublicActivity(activity)
    },
  },
  {
    method: 'POST',
    path: '/public/activities/:id/registrations',
    handle: (ctx) => {
      const activity = findPublishedActivity(ctx.params.id)
      if (activity.publicationState !== 'PUBLISHED') {
        throw new MockHttpError(404, 'ACTIVITY_UNAVAILABLE', '活动尚未发布')
      }

      const reason = resolveUnavailableReason(activity)
      if (reason === 'REGISTRATION_CLOSED' || reason === 'ACTIVITY_ENDED') {
        throw new MockHttpError(409, 'REGISTRATION_CLOSED', '报名已截止')
      }
      if (reason) {
        throw new MockHttpError(409, 'ACTIVITY_UNAVAILABLE', '活动当前不可报名')
      }

      const name = readString(ctx.body.name)
      if (name.length < 2 || name.length > 30) {
        throw new MockHttpError(400, 'VALIDATION_ERROR', '姓名需为 2 至 30 个字符')
      }

      const phone = readString(ctx.body.phone)
      if (!/^1[3-9]\d{9}$/.test(phone)) {
        throw new MockHttpError(400, 'VALIDATION_ERROR', '请输入正确的 11 位手机号')
      }

      const collegeId = readString(ctx.body.collegeId)
      if (!colleges.some((item) => item.id === collegeId)) {
        throw new MockHttpError(400, 'VALIDATION_ERROR', '请选择所属学院')
      }

      const duplicated = mockDb.registrations.some(
        (item) => item.activityId === activity.id && item.phone === phone,
      )
      if (duplicated) {
        throw new MockHttpError(409, 'REGISTRATION_DUPLICATE', '该手机号已报名本活动')
      }

      if (activity.registeredCount >= activity.capacity) {
        throw new MockHttpError(409, 'REGISTRATION_FULL', '活动名额已满')
      }

      const registration = {
        id: createId('reg'),
        activityId: activity.id,
        name,
        phone,
        collegeId,
        createdAt: nowIso(),
        checkedInAt: null,
      }
      mockDb.registrations.push(registration)
      activity.registeredCount += 1

      return {
        registrationId: registration.id,
        registeredAt: registration.createdAt,
        activityName: activity.name,
        startAt: activity.startAt,
        location: activity.location,
      }
    },
  },
]

export const routes: MockRoute[] = [
  ...authRoutes,
  ...dictionaryRoutes,
  ...dashboardRoutes,
  ...adminActivityRoutes,
  ...publicRoutes,
]
