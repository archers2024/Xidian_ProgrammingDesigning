<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus'

import AppIcon from '@/components/common/AppIcon.vue'

const router = useRouter()

function goBack(): void {
  if (window.history.length > 1) {
    router.back()
    return
  }
  void router.push({ name: 'admin-dashboard' })
}
</script>

<template>
  <div class="not-found">
    <div class="not-found__card">
      <span class="not-found__code mono">404</span>
      <h1 class="not-found__title">没有找到这个页面</h1>
      <p class="not-found__description">
        链接可能已失效、活动已被删除，或者地址输入有误。可以返回上一页，或从管理后台重新进入。
      </p>

      <div class="not-found__actions">
        <ElButton @click="goBack">
          <AppIcon name="arrow-left" :size="16" />
          <span>返回上一页</span>
        </ElButton>
        <ElButton type="primary" @click="router.push({ name: 'admin-dashboard' })">
          进入管理后台
        </ElButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  display: grid;
  place-items: center;
  min-height: 62vh;
  padding: var(--space-6) var(--space-4);
}

.not-found__card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  max-width: 520px;
  padding: var(--space-6);
  text-align: center;
}

.not-found__code {
  color: var(--brand-200);
  font-size: 64px;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1;
}

.not-found__title {
  font-size: var(--font-size-xl);
}

.not-found__description {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  line-height: 1.8;
}

.not-found__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-3);
  margin-top: var(--space-2);
}
</style>
