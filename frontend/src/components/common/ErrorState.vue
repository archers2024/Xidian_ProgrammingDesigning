<script setup lang="ts">
import { ElButton } from 'element-plus'
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    title?: string
    message?: string
    /** 后端业务码，便于排查问题时不暴露堆栈 */
    code?: string | null
    retryable?: boolean
  }>(),
  {
    title: '加载失败',
    message: '暂时无法获取数据，请稍后重试。',
    code: null,
    retryable: true,
  },
)

const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <div class="error-state" role="alert">
    <span class="error-state__icon">
      <AppIcon name="alert" :size="24" />
    </span>
    <div class="error-state__body">
      <p class="error-state__title">{{ title }}</p>
      <p class="error-state__message">{{ message }}</p>
      <p v-if="code" class="error-state__code mono">业务码：{{ code }}</p>
    </div>
    <ElButton v-if="retryable" class="error-state__action" @click="emit('retry')">
      <AppIcon name="refresh" :size="16" />
      <span>重试</span>
    </ElButton>
  </div>
</template>

<style scoped>
.error-state {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  border: 1px solid #f4d4d4;
  border-radius: var(--radius-card);
  background: #fffafa;
}

.error-state__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--danger-soft);
  color: var(--danger);
}

.error-state__body {
  flex: 1;
  min-width: 0;
}

.error-state__title {
  font-weight: 600;
}

.error-state__message {
  margin-top: 2px;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.error-state__code {
  margin-top: var(--space-2);
  color: var(--text-subtle);
}

.error-state__action {
  flex: none;
}
</style>
