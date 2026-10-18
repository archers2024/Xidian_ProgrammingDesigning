import type {
  ActivityDetail,
  ActivityDisplayStatus,
  ActivitySummary,
  AttachmentSummary,
  PublicActivityView,
} from '@/types/activity'
import type { PublicationState, UnavailableReason } from '@/types/common'
import type { AdminUser } from '@/types/auth'
import type { ActivityTypeOption, CollegeOption } from '@/types/dictionary'

const minute = 60_000
const hour = 60 * minute
const day = 24 * hour

const startedAt = Date.now()
const at = (offsetMs: number): string => new Date(startedAt + offsetMs).toISOString()

export const activityTypes: ActivityTypeOption[] = [
  { id: 'type-lecture', name: '学术讲座' },
  { id: 'type-culture', name: '文体活动' },
  { id: 'type-contest', name: '学科竞赛' },
  { id: 'type-volunteer', name: '志愿服务' },
  { id: 'type-club', name: '社团活动' },
]

export const colleges: CollegeOption[] = [
  { id: 'col-cs', name: '计算机科学与技术学院' },
  { id: 'col-telecom', name: '通信工程学院' },
  { id: 'col-ee', name: '电子工程学院' },
  { id: 'col-me', name: '机电工程学院' },
  { id: 'col-physics', name: '物理与光电工程学院' },
  { id: 'col-econ', name: '经济与管理学院' },
  { id: 'col-foreign', name: '外国语学院' },
  { id: 'col-math', name: '数学与统计学院' },
]

export const demoAdmin: AdminUser = {
  id: 'admin-001',
  username: 'admin',
  displayName: '活动管理员',
}

export type MockAttachment = AttachmentSummary

export interface MockActivity {
  id: string
  name: string
  typeId: string
  description: string
  location: string
  startAt: string
  endAt: string
  registrationDeadline: string
  capacity: number
  registeredCount: number
  publicationState: PublicationState
  publishedAt: string | null
  liveUrl: string | null
  coverUrl: string | null
  version: number
  createdAt: string
  deletedAt: string | null
  attachments: MockAttachment[]
}

export interface MockRegistration {
  id: string
  activityId: string
  name: string
  phone: string
  collegeId: string
  createdAt: string
  checkedInAt: string | null
}

export interface MockCheckinSession {
  id: string
  activityId: string
  mode: 'BASIC' | 'DYNAMIC'
  windowStart: string
  windowEnd: string
  enabled: boolean
  createdAt: string
}

const handbookAttachment: MockAttachment = {
  id: 'att-handbook',
  originalName: '人工智能讲座活动手册.pdf',
  contentType: 'application/pdf',
  sizeBytes: 2_432_512,
  createdAt: at(-3 * day),
}

const scheduleAttachment: MockAttachment = {
  id: 'att-schedule',
  originalName: '讲座日程安排.docx',
  contentType:
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  sizeBytes: 328_704,
  createdAt: at(-3 * day),
}

export const mockActivities: MockActivity[] = [
  {
    id: 'act-ai-lecture',
    name: '人工智能前沿技术讲座',
    typeId: 'type-lecture',
    description:
      '邀请人工智能领域专家介绍大模型、多模态与智能体方向的最新进展，并与同学交流科研与工程实践中的问题。活动含 40 分钟主题报告与 20 分钟自由问答，建议提前 15 分钟入场。',
    location: '南校区图书馆报告厅',
    startAt: at(2 * day + 2 * hour),
    endAt: at(2 * day + 4 * hour),
    registrationDeadline: at(2 * day - 4 * hour),
    capacity: 120,
    registeredCount: 86,
    publicationState: 'PUBLISHED',
    publishedAt: at(-3 * day),
    liveUrl: 'https://live.example.edu.cn/ai-lecture',
    coverUrl: null,
    version: 3,
    createdAt: at(-6 * day),
    deletedAt: null,
    attachments: [handbookAttachment, scheduleAttachment],
  },
  {
    id: 'act-basketball',
    name: '秋季校园篮球联赛开幕式',
    typeId: 'type-culture',
    description:
      '为期三周的校园篮球联赛正式开幕，开幕式包含球队入场、裁判宣誓与揭幕战。观众无需门票，凭报名记录在入口处签到入场。',
    location: '北校区体育馆',
    startAt: at(4 * day),
    endAt: at(4 * day + 2 * hour),
    registrationDeadline: at(3 * day),
    capacity: 300,
    registeredCount: 300,
    publicationState: 'PUBLISHED',
    publishedAt: at(-2 * day),
    liveUrl: null,
    coverUrl: null,
    version: 1,
    createdAt: at(-5 * day),
    deletedAt: null,
    attachments: [],
  },
  {
    id: 'act-edc-select',
    name: '电子设计竞赛校内选拔',
    typeId: 'type-contest',
    description:
      '面向全校本科生的电子设计竞赛校内选拔，采用三人组队形式，题目现场公布。选拔成绩将作为推荐参加省级竞赛的依据。',
    location: '实验楼 A 座 305',
    startAt: at(1 * day),
    endAt: at(1 * day + 6 * hour),
    registrationDeadline: at(-2 * hour),
    capacity: 60,
    registeredCount: 60,
    publicationState: 'PUBLISHED',
    publishedAt: at(-9 * day),
    liveUrl: null,
    coverUrl: null,
    version: 4,
    createdAt: at(-12 * day),
    deletedAt: null,
    attachments: [],
  },
  {
    id: 'act-volunteer',
    name: '敬老院志愿服务活动',
    typeId: 'type-volunteer',
    description:
      '走进社区敬老院开展陪伴与义务维修服务，志愿者需自行前往集合点。活动已结束，感谢各位同学的参与。',
    location: '长安区幸福敬老院',
    startAt: at(-2 * day),
    endAt: at(-2 * day + 3 * hour),
    registrationDeadline: at(-3 * day),
    capacity: 40,
    registeredCount: 38,
    publicationState: 'PUBLISHED',
    publishedAt: at(-10 * day),
    liveUrl: null,
    coverUrl: null,
    version: 2,
    createdAt: at(-14 * day),
    deletedAt: null,
    attachments: [],
  },
  {
    id: 'act-singer-audition',
    name: '校园歌手大赛海选',
    typeId: 'type-culture',
    description: '海选报名与赛制尚未最终确定，活动仍处于草稿状态，仅在管理后台可见。',
    location: '大学生活动中心',
    startAt: at(10 * day),
    endAt: at(10 * day + 3 * hour),
    registrationDeadline: at(8 * day),
    capacity: 200,
    registeredCount: 0,
    publicationState: 'DRAFT',
    publishedAt: null,
    liveUrl: null,
    coverUrl: null,
    version: 1,
    createdAt: at(-1 * day),
    deletedAt: null,
    attachments: [],
  },
  {
    id: 'act-newcomer-forum',
    name: '新生学业发展论坛',
    typeId: 'type-lecture',
    description:
      '学院教学负责人与优秀学长学姐围绕课程安排、竞赛与科研入门进行分享，现场答疑。报名已截止，欢迎到场旁听。',
    location: '教三楼 101',
    startAt: at(3 * hour),
    endAt: at(5 * hour),
    registrationDeadline: at(-30 * minute),
    capacity: 150,
    registeredCount: 143,
    publicationState: 'PUBLISHED',
    publishedAt: at(-7 * day),
    liveUrl: null,
    coverUrl: null,
    version: 2,
    createdAt: at(-9 * day),
    deletedAt: null,
    attachments: [],
  },
  {
    id: 'act-robot-workshop',
    name: '机器人创新工作坊',
    typeId: 'type-club',
    description:
      '面向零基础同学的机器人入门工作坊，内容包含结构搭建、电机控制与简单巡线，材料由社团提供。名额有限，报名后请按时到场。',
    location: '工程训练中心 B201',
    startAt: at(6 * day),
    endAt: at(6 * day + 3 * hour),
    registrationDeadline: at(5 * day),
    capacity: 30,
    registeredCount: 12,
    publicationState: 'PUBLISHED',
    publishedAt: at(-1 * day),
    liveUrl: null,
    coverUrl: null,
    version: 1,
    createdAt: at(-2 * day),
    deletedAt: null,
    attachments: [],
  },
  {
    id: 'act-lawn-concert',
    name: '国庆草坪音乐会',
    typeId: 'type-culture',
    description:
      '校园乐队与社团联合演出，活动已于国庆假期结束。报名与签到通道均已关闭。',
    location: '南校区中央草坪',
    startAt: at(-13 * day),
    endAt: at(-13 * day + 4 * hour),
    registrationDeadline: at(-14 * day),
    capacity: 500,
    registeredCount: 462,
    publicationState: 'PUBLISHED',
    publishedAt: at(-20 * day),
    liveUrl: null,
    coverUrl: null,
    version: 3,
    createdAt: at(-25 * day),
    deletedAt: null,
    attachments: [],
  },
]

export const mockRegistrations: MockRegistration[] = [
  {
    id: 'reg-001',
    activityId: 'act-ai-lecture',
    name: '张伟',
    phone: '13800001111',
    collegeId: 'col-cs',
    createdAt: at(-2 * day),
    checkedInAt: at(-2 * hour),
  },
  {
    id: 'reg-002',
    activityId: 'act-ai-lecture',
    name: '李娜',
    phone: '13900002222',
    collegeId: 'col-telecom',
    createdAt: at(-2 * day + 20 * minute),
    checkedInAt: at(-1 * hour),
  },
  {
    id: 'reg-003',
    activityId: 'act-ai-lecture',
    name: '王强',
    phone: '13700003333',
    collegeId: 'col-ee',
    createdAt: at(-1 * day),
    checkedInAt: null,
  },
  {
    id: 'reg-004',
    activityId: 'act-ai-lecture',
    name: '刘洋',
    phone: '13600004444',
    collegeId: 'col-math',
    createdAt: at(-1 * day + 3 * hour),
    checkedInAt: null,
  },
  {
    id: 'reg-005',
    activityId: 'act-ai-lecture',
    name: '陈静',
    phone: '13500005555',
    collegeId: 'col-foreign',
    createdAt: at(-10 * hour),
    checkedInAt: null,
  },
]

export const mockSessions: MockCheckinSession[] = [
  {
    id: 'session-ai-lecture',
    activityId: 'act-ai-lecture',
    mode: 'DYNAMIC',
    windowStart: at(2 * day + 1 * hour),
    windowEnd: at(2 * day + 4 * hour + 15 * minute),
    enabled: false,
    createdAt: at(-1 * day),
  },
]

/** 状态由“当前时间 + 发布状态 + 截止时间”计算，和真实后端规则一致 */
export function resolveDisplayStatus(activity: MockActivity, now = Date.now()): ActivityDisplayStatus {
  if (activity.publicationState === 'DRAFT') return 'DRAFT'
  if (now >= Date.parse(activity.endAt)) return 'ENDED'
  if (now >= Date.parse(activity.registrationDeadline)) return 'CLOSED'
  return 'REGISTERING'
}

export function resolveUnavailableReason(
  activity: MockActivity,
  now = Date.now(),
): UnavailableReason | null {
  if (activity.deletedAt) return 'ACTIVITY_DELETED'
  if (activity.publicationState === 'DRAFT') return 'NOT_PUBLISHED'
  if (now >= Date.parse(activity.endAt)) return 'ACTIVITY_ENDED'
  if (now >= Date.parse(activity.registrationDeadline)) return 'REGISTRATION_CLOSED'
  if (activity.registeredCount >= activity.capacity) return 'CAPACITY_FULL'
  return null
}

export function typeNameOf(typeId: string): string {
  return activityTypes.find((item) => item.id === typeId)?.name ?? '未分类'
}

export function siteBase(): string {
  return typeof window === 'undefined' ? 'http://localhost:5173' : window.location.origin
}

export function toActivitySummary(activity: MockActivity, now = Date.now()): ActivitySummary {
  const reason = resolveUnavailableReason(activity, now)
  return {
    id: activity.id,
    name: activity.name,
    typeId: activity.typeId,
    typeName: typeNameOf(activity.typeId),
    location: activity.location,
    startAt: activity.startAt,
    endAt: activity.endAt,
    registrationDeadline: activity.registrationDeadline,
    capacity: activity.capacity,
    registeredCount: activity.registeredCount,
    publicationState: activity.publicationState,
    publishedAt: activity.publishedAt,
    displayStatus: resolveDisplayStatus(activity, now),
    canRegister: reason === null,
    unavailableReason: reason,
    version: activity.version,
    createdAt: activity.createdAt,
    updatedAt: null,
  }
}

export function toActivityDetail(activity: MockActivity, now = Date.now()): ActivityDetail {
  const summary = toActivitySummary(activity, now)
  const published = activity.publicationState === 'PUBLISHED' && activity.deletedAt === null
  return {
    ...summary,
    description: activity.description,
    liveUrl: activity.liveUrl,
    coverUrl: activity.coverUrl,
    attachments: activity.attachments,
    shareUrl: published ? `${siteBase()}/events/${activity.id}` : null,
    registrationUrl: published ? `${siteBase()}/events/${activity.id}/register` : null,
  }
}

export function toPublicActivity(activity: MockActivity, now = Date.now()): PublicActivityView {
  const summary = toActivitySummary(activity, now)
  return {
    id: summary.id,
    name: summary.name,
    typeName: summary.typeName,
    description: activity.description,
    location: summary.location,
    startAt: summary.startAt,
    endAt: summary.endAt,
    registrationDeadline: summary.registrationDeadline,
    capacity: summary.capacity,
    registeredCount: summary.registeredCount,
    displayStatus: summary.displayStatus,
    canRegister: summary.canRegister,
    unavailableReason: summary.unavailableReason,
    liveUrl: activity.liveUrl,
    coverUrl: activity.coverUrl,
    attachments: activity.attachments,
    publishedAt: activity.publishedAt,
  }
}
