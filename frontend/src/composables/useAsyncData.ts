import { onScopeDispose, ref, type Ref } from 'vue'

import { ApiError } from '@/api/http'

export interface AsyncDataResult<T> {
  data: Ref<T | null>
  error: Ref<ApiError | null>
  loading: Ref<boolean>
  run: () => Promise<void>
  cancel: () => void
}

/**
 * 统一的异步数据加载：
 * - 每次请求自动取消上一次，避免旧响应覆盖新结果；
 * - 组件卸载时取消在途请求；
 * - 错误统一转为 ApiError，页面按业务码分支。
 */
export function useAsyncData<T>(
  loader: (signal: AbortSignal) => Promise<T>,
  options: { immediate?: boolean } = {},
): AsyncDataResult<T> {
  const data = ref(null) as Ref<T | null>
  const error = ref<ApiError | null>(null)
  const loading = ref(false)

  let controller: AbortController | null = null
  let sequence = 0

  async function run(): Promise<void> {
    const current = ++sequence
    controller?.abort()
    const local = new AbortController()
    controller = local

    loading.value = true
    error.value = null

    try {
      const result = await loader(local.signal)
      if (current === sequence) data.value = result
    } catch (caught) {
      if (current !== sequence || local.signal.aborted) return
      error.value =
        caught instanceof ApiError
          ? caught
          : new ApiError('UNKNOWN_ERROR', '数据加载失败，请稍后重试')
    } finally {
      if (current === sequence) loading.value = false
    }
  }

  function cancel(): void {
    controller?.abort()
  }

  onScopeDispose(cancel)

  if (options.immediate !== false) void run()

  return { data, error, loading, run, cancel }
}
