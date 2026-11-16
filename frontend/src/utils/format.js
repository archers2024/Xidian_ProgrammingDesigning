/**
 * 时间展示格式化。
 *
 * 后端返回的是 ISO 字符串（2026-11-21T00:00:00 / 带 7 位小数秒），
 * 直接渲染会出现字母 T 和一长串小数，界面上必须统一。
 */

export function fmtTime(value, withSeconds = false) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const s = String(value).replace('T', ' ')
  const m = s.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})(?::(\d{2}))?/)
  if (!m) {
    return s.slice(0, withSeconds ? 19 : 16)
  }
  if (!withSeconds) {
    return `${m[1]} ${m[2]}`
  }
  return `${m[1]} ${m[2]}:${m[3] || '00'}`
}

/** 表格列 formatter 工厂：:formatter="timeCol()" */
export function timeCol(withSeconds = false) {
  return (row, column, cellValue) => fmtTime(cellValue, withSeconds)
}
