import { apiGet, apiPost } from '@/api/http'
import type { CheckinChallenge, CheckinMode, CheckinSession } from '@/types/checkin'
import type { IsoDateTime } from '@/types/common'

export interface OpenCheckinPayload {
  mode: CheckinMode
  windowStart: IsoDateTime
  windowEnd: IsoDateTime
}

export function openCheckinSession(activityId: string, payload: OpenCheckinPayload): Promise<CheckinSession> {
  return apiPost<CheckinSession>(`/admin/activities/${activityId}/checkin-sessions`, payload)
}

export function closeCheckinSession(sessionId: string): Promise<CheckinSession> {
  return apiPost<CheckinSession>(`/admin/checkin-sessions/${sessionId}/close`)
}

export function fetchCheckinChallenge(sessionId: string, signal?: AbortSignal): Promise<CheckinChallenge> {
  return apiGet<CheckinChallenge>(`/admin/checkin-sessions/${sessionId}/challenge`, { signal })
}
