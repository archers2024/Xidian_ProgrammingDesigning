/**
 * 时间展示统一按 Asia/Shanghai 处理；接口传入带时区的 ISO 8601。
 * 时间边界判断一律使用后端返回值，前端只负责展示。
 */

const formatter = new Intl.DateTimeFormat('zh-CN', {
  timeZone: 'Asia/Shanghai',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

interface DateParts {
  date: string
  time: string
}

function partsOf(value: string | null | undefined): DateParts | null {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  const result = formatter.formatToParts(date)
  const pick = (type: Intl.DateTimeFormatPartTypes): string =>
    result.find((part) => part.type === type)?.value ?? ''
  return {
    date: `${pick('year')}-${pick('month')}-${pick('day')}`,
    time: `${pick('hour')}:${pick('minute')}`,
  }
}

/** 2026-10-20 19:00 */
export function formatDateTime(value: string | null | undefined): string {
  const parts = partsOf(value)
  return parts ? `${parts.date} ${parts.time}` : '—'
}

/** 10-20 19:00，用于列表等紧凑位置 */
export function formatShortDateTime(value: string | null | undefined): string {
  const parts = partsOf(value)
  return parts ? `${parts.date.slice(5)} ${parts.time}` : '—'
}

/** 2026-10-20 */
export function formatDate(value: string | null | undefined): string {
  return partsOf(value)?.date ?? '—'
}

/** 开始与结束时间：同一天只显示一次日期 */
export function formatActivityTime(
  startAt: string | null | undefined,
  endAt: string | null | undefined,
): string {
  const start = partsOf(startAt)
  const end = partsOf(endAt)
  if (!start) return '—'
  if (!end) return `${start.date} ${start.time}`
  if (start.date === end.date) return `${start.date} ${start.time} – ${end.time}`
  return `${start.date} ${start.time} – ${end.date} ${end.time}`
}

/** 把日期选择器的 YYYY-MM-DD 转成当天开始/结束的带时区时间 */
export function toDayStart(date: string): string {
  return `${date}T00:00:00+08:00`
}

export function toDayEnd(date: string): string {
  return `${date}T23:59:59+08:00`
}

/** 剩余名额的正向展示，不做负数 */
export function remainingSeats(capacity: number, registered: number): number {
  return Math.max(capacity - registered, 0)
}
