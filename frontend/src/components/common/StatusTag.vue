<script setup lang="ts">
import { computed } from 'vue'
import { DISPLAY_STATUS_LABEL, type ActivityDisplayStatus } from '@/types/activity'

const props = defineProps<{
  status: ActivityDisplayStatus
}>()

type Tone = 'neutral' | 'active' | 'warning' | 'muted'

const tone = computed<Tone>(() => {
  switch (props.status) {
    case 'REGISTERING':
      return 'active'
    case 'CLOSED':
      return 'warning'
    case 'ENDED':
      return 'muted'
    default:
      return 'neutral'
  }
})

const label = computed(() => DISPLAY_STATUS_LABEL[props.status])
</script>

<template>
  <span class="status-tag" :class="`status-tag--${tone}`">
    <span class="status-tag__dot" aria-hidden="true" />
    {{ label }}
  </span>
</template>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-xs);
  font-weight: 600;
  white-space: nowrap;
}

.status-tag__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-tag--active {
  background: var(--accent-soft);
  color: var(--accent-strong);
}

.status-tag--warning {
  background: var(--warning-soft);
  color: var(--warning);
}

.status-tag--muted {
  background: var(--bg-subtle);
  color: var(--text-muted);
}

.status-tag--neutral {
  background: var(--brand-soft);
  color: var(--brand-600);
}
</style>
