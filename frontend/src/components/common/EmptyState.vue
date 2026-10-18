<script setup lang="ts">
import AppIcon from './AppIcon.vue'
import type { IconName } from './icons'

withDefaults(
  defineProps<{
    title: string
    description?: string
    icon?: IconName
  }>(),
  {
    description: '',
    icon: 'inbox',
  },
)
</script>

<template>
  <div class="empty-state">
    <span class="empty-state__icon">
      <AppIcon :name="icon" :size="24" />
    </span>
    <p class="empty-state__title">{{ title }}</p>
    <p v-if="description" class="empty-state__description">{{ description }}</p>
    <div v-if="$slots.actions" class="empty-state__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-7) var(--space-4);
  text-align: center;
}

.empty-state__icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  margin-bottom: var(--space-1);
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand);
}

.empty-state__title {
  font-weight: 600;
}

.empty-state__description {
  max-width: 46ch;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.empty-state__actions {
  margin-top: var(--space-3);
}
</style>
