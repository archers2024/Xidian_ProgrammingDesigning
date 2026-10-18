import type { ActivitySummary } from './activity'

export interface ActivityStatusCounts {
  total: number
  draft: number
  registering: number
  closed: number
  ended: number
}

/** 工作台概览：统计必须来自真实接口，前端不自行累计出“好看”的数字 */
export interface DashboardOverview {
  activityCounts: ActivityStatusCounts
  registrationCount: number
  checkinCount: number
  recentActivities: ActivitySummary[]
}
