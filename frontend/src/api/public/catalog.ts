import { apiGet } from '@/api/http'
import type { ItemList } from '@/types/common'
import type { ActivityTypeOption, CollegeOption } from '@/types/dictionary'

/** 字典允许匿名访问，前端可缓存后复用 */
export function fetchActivityTypes(signal?: AbortSignal): Promise<ItemList<ActivityTypeOption>> {
  return apiGet<ItemList<ActivityTypeOption>>('/public/activity-types', { signal })
}

export function fetchColleges(signal?: AbortSignal): Promise<ItemList<CollegeOption>> {
  return apiGet<ItemList<CollegeOption>>('/public/colleges', { signal })
}
