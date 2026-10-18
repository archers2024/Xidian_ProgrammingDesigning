<script setup lang="ts">
import { useRouter } from 'vue-router'
import AppIcon from './AppIcon.vue'

const props = withDefaults(
  defineProps<{
    title: string
    description?: string
    eyebrow?: string
    /** 传入路由路径时显示返回按钮 */
    backTo?: string
  }>(),
  {
    description: '',
    eyebrow: '',
    backTo: '',
  },
)

const router = useRouter()

function goBack(): void {
  if (props.backTo) void router.push(props.backTo)
}
</script>

<template>
  <header class="page-header">
    <button v-if="backTo" class="page-header__back" type="button" @click="goBack">
      <AppIcon name="arrow-left" :size="16" />
      返回
    </button>

    <div class="page-header__body">
      <p v-if="eyebrow" class="page-header__eyebrow">{{ eyebrow }}</p>
      <h1 class="page-header__title" data-page-heading tabindex="-1">{{ title }}</h1>
      <p v-if="description" class="page-header__description">{{ description }}</p>
    </div>

    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3) var(--space-4);
}

.page-header__back {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  align-self: center;
  padding: 6px 10px 6px 6px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--bg-surface);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: color var(--dur-fast) var(--ease-standard),
    border-color var(--dur-fast) var(--ease-standard);
}

.page-header__back:hover {
  color: var(--brand);
  border-color: var(--brand-200);
}

.page-header__body {
  flex: 1 1 320px;
  min-width: 0;
}

.page-header__eyebrow {
  margin-bottom: var(--space-1);
  color: var(--brand);
  font-size: var(--font-size-xs);
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-header__title {
  font-size: var(--font-size-2xl);
  letter-spacing: -0.01em;
  outline: none;
}

.page-header__title:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 6px;
  border-radius: 6px;
}

.page-header__description {
  margin-top: var(--space-2);
  max-width: 68ch;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.page-header__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

@media (max-width: 767px) {
  .page-header__title {
    font-size: var(--font-size-xl);
  }
}
</style>
