import type {
  ActivityDisplayStatus,
  IsoDateTime,
  PageQuery,
  PublicationState,
  UnavailableReason,
} from './common'

export type {
  ActivityDisplayStatus,
  PublicationState,
  UnavailableReason,
} from './common'

export interface AttachmentSummary {
  id: string
  originalName: string
  contentType: string
  sizeBytes: number
  createdAt: IsoDateTime
}

/** 列表与详情的公共字段，展示状态与可报名性由后端计算 */
export interface ActivitySummary {
  id: string
  name: string
  typeId: string
  typeName: string
  location: string
  startAt: IsoDateTime
  endAt: IsoDateTime
  registrationDeadline: IsoDateTime
  capacity: number
  registeredCount: number
  publicationState: PublicationState
  publishedAt: IsoDateTime | null
  displayStatus: ActivityDisplayStatus
  canRegister: boolean
  unavailableReason: UnavailableReason | null
  version: number
  createdAt: IsoDateTime
  updatedAt: IsoDateTime | null
}

export interface ActivityDetail extends ActivitySummary {
  description: string
  liveUrl: string | null
  coverUrl: string | null
  attachments: AttachmentSummary[]
  /** 发布后才有的公开地址 */
  shareUrl: string | null
  registrationUrl: string | null
}

export interface ActivityListQuery extends PageQuery {
  keyword?: string
  typeId?: string
  status?: ActivityDisplayStatus
  /** 按活动开始时间筛选，包含边界 */
  dateFrom?: IsoDateTime
  dateTo?: IsoDateTime
}

/** 创建与编辑共用的可写字段，时间是带时区的 ISO 8601 */
export interface ActivityFormPayload {
  name: string
  typeId: string
  description: string
  location: string
  startAt: IsoDateTime
  endAt: IsoDateTime
  registrationDeadline: IsoDateTime
  capacity: number
  liveUrl: string | null
}

export type ActivityCreatePayload = ActivityFormPayload

/** 编辑保留原活动 ID，并携带 version 做乐观锁更新 */
export interface ActivityUpdatePayload extends ActivityFormPayload {
  version: number
}

export interface PublishResult {
  activityId: string
  shareUrl: string
  registrationUrl: string
  validFrom: IsoDateTime
  validUntil: IsoDateTime
}

/** 公开宣传页数据：不包含报名名单等敏感信息 */
export interface PublicActivityView {
  id: string
  name: string
  typeName: string
  description: string
  location: string
  startAt: IsoDateTime
  endAt: IsoDateTime
  registrationDeadline: IsoDateTime
  capacity: number
  registeredCount: number
  displayStatus: ActivityDisplayStatus
  canRegister: boolean
  unavailableReason: UnavailableReason | null
  liveUrl: string | null
  coverUrl: string | null
  attachments: AttachmentSummary[]
  publishedAt: IsoDateTime | null
}

export const DISPLAY_STATUS_LABEL: Record<ActivityDisplayStatus, string> = {
  DRAFT: '未发布',
  REGISTERING: '报名中',
  CLOSED: '已截止',
  ENDED: '已结束',
}

export const UNAVAILABLE_REASON_TEXT: Record<UnavailableReason, string> = {
  NOT_PUBLISHED: '活动尚未发布',
  ACTIVITY_DELETED: '活动已失效',
  ACTIVITY_ENDED: '活动已结束',
  REGISTRATION_CLOSED: '报名已截止',
  CAPACITY_FULL: '名额已满',
}
