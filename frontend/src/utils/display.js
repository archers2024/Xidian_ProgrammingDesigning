export const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 19) : '—'

export function safeUrl(value) {
  if (!value) return ''
  try {
    const url = new URL(value, window.location.origin)
    return ['http:', 'https:'].includes(url.protocol) && !url.username && !url.password ? url.href : ''
  } catch { return '' }
}
