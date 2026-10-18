/** 去掉空查询参数，避免把 keyword='' 之类发到后端影响筛选与缓存键 */
export function compactParams(params: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== undefined && value !== null && value !== '',
    ),
  )
}
