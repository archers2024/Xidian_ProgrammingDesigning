import { apiPost } from '@/api/http'
import type { RegistrationCreatePayload, RegistrationResult } from '@/types/registration'

/**
 * 匿名报名。失败时按业务码区分：
 * ACTIVITY_UNAVAILABLE / REGISTRATION_CLOSED / REGISTRATION_FULL /
 * REGISTRATION_DUPLICATE / VALIDATION_ERROR。
 */
export function createRegistration(
  activityId: string,
  payload: RegistrationCreatePayload,
): Promise<RegistrationResult> {
  return apiPost<RegistrationResult>(`/public/activities/${activityId}/registrations`, payload)
}
