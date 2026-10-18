<script setup lang="ts">
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
    /** 本模块后续批次要补齐的能力，逐条列出，避免“看起来能点但没有实现” */
    pending?: string[]
    badge?: string
  }>(),
  {
    description: '',
    pending: () => [],
    badge: '后续批次实现',
  },
)
</script>

<template>
  <div class="placeholder-panel">
    <div class="placeholder-panel__head">
      <span class="placeholder-panel__badge">{{ badge }}</span>
      <h2 class="placeholder-panel__title">{{ title }}</h2>
      <p v-if="description" class="placeholder-panel__description">{{ description }}</p>
    </div>

    <ul v-if="pending.length > 0" class="placeholder-panel__list">
      <li v-for="item in pending" :key="item" class="placeholder-panel__item">
        <AppIcon name="clock" :size="16" />
        <span>{{ item }}</span>
      </li>
    </ul>

    <slot />
  </div>
</template>

<style scoped>
.placeholder-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-5);
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-card);
  background: linear-gradient(180deg, #fbfcff 0%, var(--bg-surface) 100%);
}

.placeholder-panel__head {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  align-items: flex-start;
}

.placeholder-panel__badge {
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  background: var(--brand-soft);
  color: var(--brand-600);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.placeholder-panel__title {
  font-size: var(--font-size-md);
}

.placeholder-panel__description {
  max-width: 72ch;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.placeholder-panel__list {
  display: grid;
  gap: var(--space-2);
}

.placeholder-panel__item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.placeholder-panel__item svg {
  margin-top: 3px;
  color: var(--text-subtle);
}
</style>
