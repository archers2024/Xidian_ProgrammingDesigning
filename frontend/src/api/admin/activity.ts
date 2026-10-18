import { apiDelete, apiGet, apiPost, apiPut } from '@/api/http'
import { compactParams } from '@/api/params'
import type { PageResult } from '@/types/common'
import type {
  ActivityCreatePayload,
  ActivityDetail,
  ActivityListQuery,
  ActivitySummary,
  ActivityUpdatePayload,
  PublishResult,
} from '@/types/activity'
import type { RegistrationSummary } from '@/types/registration'
import type { CheckinSummary } from '@/types/checkin'

/** 组合筛选与分页在数据库内完成，状态筛选按服务器时间计算 */
export function fetchActivityPage(
  query: ActivityListQuery,
  signal?: AbortSignal,
): Promise<PageResult<ActivitySummary>> {
  return apiGet<PageResult<ActivitySummary>>('/admin/activities', {
    params: compactParams(query as Record<string, unknown>),
    signal,
  })
}

export function fetchActivityDetail(id: string, signal?: AbortSignal): Promise<ActivityDetail> {
  return apiGet<ActivityDetail>(`/admin/activities/${id}`, { signal })
}

/** 创建成功后返回新活动 ID，前端再上传封面与附件 */
export function createActivity(payload: ActivityCreatePayload): Promise<{ id: string }> {
  return apiPost<{ id: string }>('/admin/activities', payload)
}

/** 编辑更新原活动，携带 version 避免并发覆盖 */
export function updateActivity(id: string, payload: ActivityUpdatePayload): Promise<ActivityDetail> {
  return apiPut<ActivityDetail>(`/admin/activities/${id}`, payload)
}

/** 逻辑删除；各状态都允许操作 */
export function deleteActivity(id: string): Promise<void> {
  return apiDelete<void>(`/admin/activities/${id}`)
}

/** 发布幂等：重复调用不重复建活动，返回稳定的宣传与报名地址 */
export function publishActivity(id: string): Promise<PublishResult> {
  return apiPost<PublishResult>(`/admin/activities/${id}/publish`)
}

export function fetchRegistrations(
  activityId: string,
  query: { page?: number; pageSize?: number } = {},
  signal?: AbortSignal,
): Promise<PageResult<RegistrationSummary>> {
  return apiGet<PageResult<RegistrationSummary>>(`/admin/activities/${activityId}/registrations`, {
    params: compactParams(query),
    signal,
  })
}

/** 大屏汇总：只返回人数，不含手机号 */
export function fetchCheckinSummary(
  activityId: string,
  signal?: AbortSignal,
): Promise<CheckinSummary> {
  return apiGet<CheckinSummary>(`/admin/activities/${activityId}/checkin-summary`, { signal })
}
