import type { IsoDateTime } from './common'

/** 匿名报名请求；手机号按大陆 11 位格式，后端以规范化手机号判重 */
export interface RegistrationCreatePayload {
  name: string
  phone: string
  collegeId: string
}

export interface RegistrationResult {
  registrationId: string
  registeredAt: IsoDateTime
  activityName: string
  startAt: IsoDateTime
  location: string
}

/** 后台报名名单条目；手机号仅对有权限的管理员返回 */
export interface RegistrationSummary {
  id: string
  name: string
  phone: string
  collegeId: string
  collegeName: string
  registeredAt: IsoDateTime
  checkedIn: boolean
  checkedInAt: IsoDateTime | null
}
