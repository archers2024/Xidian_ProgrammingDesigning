/** 通用类型：分页、响应信封、活动展示状态 */

/** 带时区的 ISO 8601 时间，例如 2026-10-20T19:00:00+08:00 */
export type IsoDateTime = string

/** 普通成功响应信封（文件下载等无正文接口除外） */
export interface ApiEnvelope<T> {
  code: string
  message: string
  data: T
  requestId: string
}

export interface PageQuery {
  page?: number
  pageSize?: number
}

/** 分页从 1 开始 */
export interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

export interface ItemList<T> {
  items: T[]
}

/** 数据库只保存 DRAFT / PUBLISHED，这里是由后端按服务器时间计算的展示状态 */
export type PublicationState = 'DRAFT' | 'PUBLISHED'

export type ActivityDisplayStatus = 'DRAFT' | 'REGISTERING' | 'CLOSED' | 'ENDED'

/** 名额已满等“不可报名原因”，与四种主要状态分开返回 */
export type UnavailableReason =
  | 'NOT_PUBLISHED'
  | 'ACTIVITY_DELETED'
  | 'ACTIVITY_ENDED'
  | 'REGISTRATION_CLOSED'
  | 'CAPACITY_FULL'

export interface SelectOption<T = string> {
  label: string
  value: T
  disabled?: boolean
}
