import { apiGet } from '@/api/http'
import type { PublicActivityView } from '@/types/activity'

/** 公开宣传页数据；未发布、已删除或不存在时返回 404/410 业务码 */
export function fetchPublicActivity(id: string, signal?: AbortSignal): Promise<PublicActivityView> {
  return apiGet<PublicActivityView>(`/public/activities/${id}`, { signal })
}
