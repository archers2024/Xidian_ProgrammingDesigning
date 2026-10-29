import { apiPost } from '@/api/http'
import type { CheckinContext, CheckinResult, CheckinSubmitPayload } from '@/types/checkin'

export function fetchCheckinContext(payload: {
  sessionId: string
  token?: string
}): Promise<CheckinContext> {
  return apiPost<CheckinContext>('/public/checkin-context', payload)
}

export function submitCheckin(payload: CheckinSubmitPayload): Promise<CheckinResult> {
  return apiPost<CheckinResult>('/public/checkins', payload)
}
