/** 文件大小等展示格式 */
export function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  const value = bytes / 1024 ** index
  return `${index === 0 || value >= 10 ? Math.round(value) : value.toFixed(1)} ${units[index]}`
}

/** 报名进度百分比，限制在 0–100 */
export function progressPercent(registered: number, capacity: number): number {
  if (!capacity || capacity <= 0) return 0
  return Math.min(Math.round((registered / capacity) * 100), 100)
}
