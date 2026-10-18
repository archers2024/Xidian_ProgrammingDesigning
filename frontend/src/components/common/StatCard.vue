<script setup lang="ts">
import AppIcon from './AppIcon.vue'
import type { IconName } from './icons'

withDefaults(
  defineProps<{
    label: string
    value: number | string
    hint?: string
    icon?: IconName
    tone?: 'brand' | 'accent' | 'success' | 'muted'
    loading?: boolean
  }>(),
  {
    hint: '',
    icon: 'dashboard',
    tone: 'brand',
    loading: false,
  },
)
</script>

<template>
  <article class="stat-card" :class="`stat-card--${tone}`">
    <header class="stat-card__head">
      <span class="stat-card__label">{{ label }}</span>
      <span class="stat-card__icon">
        <AppIcon :name="icon" :size="18" />
      </span>
    </header>

    <p v-if="loading" class="skeleton stat-card__skeleton" />
    <p v-else class="stat-card__value">{{ value }}</p>

    <p v-if="hint" class="stat-card__hint">{{ hint }}</p>
  </article>
</template>

<style scoped>
.stat-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-5);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--bg-surface);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--dur-fast) var(--ease-standard),
    transform var(--dur-fast) var(--ease-standard);
}

.stat-card:hover {
  box-shadow: var(--shadow-card);
  transform: translateY(-1px);
}

.stat-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.stat-card__label {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.stat-card__icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: var(--brand-soft);
  color: var(--brand);
}

.stat-card__value {
  font-size: var(--font-size-2xl);
  font-weight: 650;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}

.stat-card__skeleton {
  width: 56%;
  height: 34px;
}

.stat-card__hint {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.stat-card--accent .stat-card__icon {
  background: var(--accent-soft);
  color: var(--accent-strong);
}

.stat-card--success .stat-card__icon {
  background: var(--success-soft);
  color: var(--success);
}

.stat-card--muted .stat-card__icon {
  background: var(--bg-subtle);
  color: var(--text-muted);
}
</style>
