import type { IsoDateTime } from './common'

export type CheckinMode = 'BASIC' | 'DYNAMIC'

export interface CheckinSession {
  id: string
  activityId: string
  mode: CheckinMode
  windowStart: IsoDateTime
  windowEnd: IsoDateTime
  enabled: boolean
  createdAt: IsoDateTime
}

/** 大屏汇总：不返回手机号与姓名 */
export interface CheckinSummary {
  activityId: string
  registeredCount: number
  checkedInCount: number
  sessionMode: CheckinMode | null
  sessionEnabled: boolean
  windowEnd: IsoDateTime | null
}

/** 动态码挑战凭证，30 秒为一段，由后端签发 */
export interface CheckinChallenge {
  token: string
  serverTime: IsoDateTime
  expiresAt: IsoDateTime
  refreshAfterSeconds: number
}

/** 扫码上下文：仅返回非敏感信息 */
export interface CheckinContext {
  sessionId: string
  activityId: string
  activityName: string
  mode: CheckinMode
  expiresInSeconds: number
  validUntil: IsoDateTime
}

export interface CheckinSubmitPayload {
  sessionId: string
  phone: string
  /** 动态模式下必须携带，静态签到不需要 */
  token?: string
}

export interface CheckinResult {
  checkinId: string
  checkedAt: IsoDateTime
  activityName: string
  /** 重复签到时为 true，不新增记录 */
  alreadyCheckedIn: boolean
}
