import { apiDelete, apiPost } from '@/api/http'
import type { ActivityDetail, AttachmentSummary } from '@/types/activity'

function formFile(file: File): FormData {
  const form = new FormData()
  form.append('file', file)
  return form
}

export function uploadCover(activityId: string, file: File): Promise<ActivityDetail> {
  return apiPost<ActivityDetail>(`/admin/activities/${activityId}/cover`, formFile(file))
}

export function uploadAttachment(activityId: string, file: File): Promise<AttachmentSummary> {
  return apiPost<AttachmentSummary>(`/admin/activities/${activityId}/attachments`, formFile(file))
}

export function deleteAttachment(attachmentId: string): Promise<void> {
  return apiDelete<void>(`/admin/attachments/${attachmentId}`)
}

export function adminAttachmentUrl(attachmentId: string): string {
  return `/api/v1/admin/attachments/${attachmentId}/download`
}

export function publicAttachmentUrl(attachmentId: string): string {
  return `/api/v1/public/attachments/${attachmentId}/download`
}
