<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import AppIcon from '@/components/common/AppIcon.vue'

const route = useRoute()

/** 前进/后退方向依据路由声明的 depth；只影响手机端滑动方向 */
const direction = ref<'forward' | 'back' | 'none'>('none')
let previousDepth = Number(route.meta.depth ?? 0)

watch(
  () => route.fullPath,
  () => {
    const depth = Number(route.meta.depth ?? 0)
    direction.value = depth > previousDepth ? 'forward' : depth < previousDepth ? 'back' : 'none'
    previousDepth = depth
  },
)

const transitionName = computed(() => {
  if (route.meta.transition === 'none') return 'none'
  if (direction.value === 'forward') return 'slide-forward'
  if (direction.value === 'back') return 'slide-back'
  return 'page-fade'
})
</script>

<template>
  <div class="public-shell">
    <header class="public-header">
      <div class="public-header__inner">
        <div class="public-brand">
          <span class="public-brand__mark">
            <AppIcon name="check" :size="16" :stroke-width="2.6" />
          </span>
          <span class="public-brand__text">
            <strong>校园活动</strong>
            <small>{{ route.meta.title }}</small>
          </span>
        </div>

        <RouterLink class="public-header__admin" :to="{ name: 'admin-login' }">
          管理员入口
          <AppIcon name="chevron-right" :size="14" />
        </RouterLink>
      </div>
    </header>

    <main class="public-main">
      <RouterView v-slot="{ Component }">
        <Transition :name="transitionName" mode="out-in">
          <component :is="Component" :key="route.path" />
        </Transition>
      </RouterView>
    </main>

    <footer class="public-footer">
      <div class="public-footer__inner">
        <span>高校学生活动全流程管理系统</span>
        <RouterLink :to="{ name: 'admin-login' }">管理后台</RouterLink>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.public-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.public-header {
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
}

.public-header__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  width: 100%;
  max-width: var(--public-max-width);
  margin: 0 auto;
  padding: 10px var(--space-5);
}

.public-brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.public-brand__mark {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--brand) 0%, var(--accent) 100%);
  color: #fff;
}

.public-brand__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.25;
}

.public-brand__text strong {
  font-size: var(--font-size-base);
}

.public-brand__text small {
  overflow: hidden;
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.public-header__admin {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 6px 10px;
  border-radius: var(--radius-pill);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  transition: background-color var(--dur-fast) var(--ease-standard),
    color var(--dur-fast) var(--ease-standard);
}

.public-header__admin:hover {
  background: var(--bg-subtle);
  color: var(--brand);
}

.public-main {
  flex: 1;
  width: 100%;
}

.public-footer {
  border-top: 1px solid var(--border);
  background: var(--bg-surface);
}

.public-footer__inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  width: 100%;
  max-width: var(--public-max-width);
  margin: 0 auto;
  padding: var(--space-4) var(--space-5) calc(var(--space-4) + env(safe-area-inset-bottom));
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

@media (max-width: 767px) {
  .public-header__inner {
    padding: 8px var(--space-4);
  }

  .public-footer__inner {
    padding: var(--space-4);
  }
}
</style>
