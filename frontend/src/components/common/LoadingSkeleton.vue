<script setup lang="ts">
withDefaults(
  defineProps<{
    /** cards 用于统计卡，list 用于列表，table 用于表格，detail 用于详情 */
    variant?: 'cards' | 'list' | 'table' | 'detail'
    rows?: number
    label?: string
  }>(),
  {
    variant: 'list',
    rows: 4,
    label: '正在加载',
  },
)
</script>

<template>
  <div class="skeleton-block" role="status" :aria-label="label">
    <div v-if="variant === 'cards'" class="skeleton-block__cards">
      <div v-for="index in rows" :key="index" class="skeleton skeleton-block__card" />
    </div>

    <div v-else-if="variant === 'table'" class="skeleton-block__table">
      <div class="skeleton skeleton-block__row skeleton-block__row--head" />
      <div
        v-for="index in rows"
        :key="index"
        class="skeleton skeleton-block__row"
        :style="{ animationDelay: `${index * 60}ms` }"
      />
    </div>

    <div v-else-if="variant === 'detail'" class="skeleton-block__detail">
      <div class="skeleton skeleton-block__title" />
      <div class="skeleton skeleton-block__line" />
      <div class="skeleton skeleton-block__line skeleton-block__line--short" />
      <div class="skeleton skeleton-block__panel" />
    </div>

    <div v-else class="skeleton-block__list">
      <div v-for="index in rows" :key="index" class="skeleton-block__item">
        <div class="skeleton skeleton-block__avatar" />
        <div class="skeleton-block__lines">
          <div class="skeleton skeleton-block__line" />
          <div class="skeleton skeleton-block__line skeleton-block__line--short" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-block {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.skeleton-block__cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.skeleton-block__card {
  height: 108px;
  border-radius: var(--radius-card);
}

.skeleton-block__table {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.skeleton-block__row {
  height: 48px;
}

.skeleton-block__row--head {
  height: 40px;
  opacity: 0.7;
}

.skeleton-block__detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.skeleton-block__title {
  width: 40%;
  height: 28px;
}

.skeleton-block__line {
  height: 14px;
}

.skeleton-block__line--short {
  width: 62%;
}

.skeleton-block__panel {
  height: 180px;
  border-radius: var(--radius-card);
}

.skeleton-block__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.skeleton-block__item {
  display: flex;
  gap: var(--space-3);
  align-items: center;
}

.skeleton-block__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
}

.skeleton-block__lines {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: var(--space-2);
}
</style>
