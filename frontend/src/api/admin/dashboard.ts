import { apiGet } from '@/api/http'
import type { DashboardOverview } from '@/types/dashboard'

export function fetchDashboardOverview(signal?: AbortSignal): Promise<DashboardOverview> {
  return apiGet<DashboardOverview>('/admin/dashboard', { signal })
}
