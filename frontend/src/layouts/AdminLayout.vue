<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, ElDropdown, ElDropdownItem, ElDropdownMenu } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

import AppIcon from '@/components/common/AppIcon.vue'
import type { IconName } from '@/components/common/icons'
import { useAuthStore } from '@/stores/auth'

interface NavItem {
  name: string
  label: string
  icon: IconName
  description: string
}

const navItems: NavItem[] = [
  { name: 'admin-dashboard', label: '工作台', icon: 'dashboard', description: '概览与快捷入口' },
  {
    name: 'admin-activity-list',
    label: '活动管理',
    icon: 'calendar',
    description: '创建、编辑、发布与报名',
  },
]

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const drawerOpen = ref(false)

const transitionName = computed(() => route.meta.transition ?? 'page-fade')

const activeNav = computed(() => {
  const name = String(route.name ?? '')
  return name.startsWith('admin-activity') ? 'admin-activity-list' : name
})

const userInitial = computed(() => (auth.displayName || '管').slice(0, 1))

/** 演示链接指向模拟数据中的活动，正式环境不显示 */
const showDemoLinks = import.meta.env.VITE_USE_MOCK === 'true'

const demoPromoRoute = {
  name: 'public-activity-promo',
  params: { id: 'act-ai-lecture' },
} as const

watch(
  () => route.fullPath,
  async () => {
    drawerOpen.value = false
    window.scrollTo({ top: 0, behavior: 'instant' })
    await nextTick()
    const heading = document.querySelector<HTMLElement>('[data-page-heading]')
    heading?.focus({ preventScroll: true })
  },
)

onMounted(() => {
  if (!auth.initialized) void auth.ensureSession()
})

async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('退出后需要重新登录才能管理活动。', '确认退出登录？', {
      confirmButtonText: '退出登录',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await auth.signOut()
  ElMessage.success('已退出登录')
  await router.push({ name: 'admin-login' })
}

function handleCommand(command: string): void {
  if (command === 'logout') void handleLogout()
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar" :class="{ 'admin-sidebar--open': drawerOpen }">
      <div class="admin-brand">
        <span class="admin-brand__mark">
          <AppIcon name="check" :size="18" :stroke-width="2.6" />
        </span>
        <span class="admin-brand__text">
          <strong>校园活动</strong>
          <small>管理后台</small>
        </span>
      </div>

      <nav class="admin-nav" aria-label="后台主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="admin-nav__item"
          :class="{ 'is-active': activeNav === item.name }"
        >
          <AppIcon :name="item.icon" :size="19" />
          <span class="admin-nav__label">{{ item.label }}</span>
          <span class="admin-nav__hint">{{ item.description }}</span>
        </RouterLink>
      </nav>

      <div class="admin-sidebar__footer">
        <RouterLink
          v-if="showDemoLinks"
          :to="demoPromoRoute"
          target="_blank"
          class="admin-sidebar__link"
        >
          <AppIcon name="external" :size="15" />
          查看演示宣传页
        </RouterLink>
        <p class="admin-sidebar__version">前端框架 v0.1</p>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <button
          class="admin-topbar__menu"
          type="button"
          aria-label="打开导航菜单"
          @click="drawerOpen = true"
        >
          <AppIcon name="menu" :size="20" />
        </button>

        <nav class="admin-crumb" aria-label="页面位置">
          <span class="admin-crumb__root">管理后台</span>
          <AppIcon name="chevron-right" :size="14" class="admin-crumb__sep" />
          <span class="admin-crumb__current">{{ route.meta.title }}</span>
        </nav>

        <div class="admin-topbar__right">
          <ElDropdown trigger="click" placement="bottom-end" @command="handleCommand">
            <button class="admin-user" type="button" aria-label="账号菜单">
              <span class="admin-user__avatar">{{ userInitial }}</span>
              <span class="admin-user__name">{{ auth.displayName }}</span>
              <AppIcon name="chevron-down" :size="14" />
            </button>
            <template #dropdown>
              <ElDropdownMenu>
                <ElDropdownItem command="logout">
                  <AppIcon name="logout" :size="15" />
                  <span class="admin-user__menu-text">退出登录</span>
                </ElDropdownItem>
              </ElDropdownMenu>
            </template>
          </ElDropdown>
        </div>
      </header>

      <main class="admin-content">
        <RouterView v-slot="{ Component }">
          <Transition :name="transitionName" mode="out-in">
            <component :is="Component" :key="route.path" />
          </Transition>
        </RouterView>
      </main>
    </div>

    <button
      v-if="drawerOpen"
      class="admin-overlay"
      type="button"
      aria-label="关闭导航菜单"
      @click="drawerOpen = false"
    />
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background: var(--bg-page);
}

.admin-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex: none;
  flex-direction: column;
  gap: var(--space-5);
  width: var(--sidebar-width);
  height: 100vh;
  padding: var(--space-4) var(--space-3);
  border-right: 1px solid var(--border);
  background: var(--bg-surface);
  z-index: var(--z-sidebar);
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
}

.admin-brand__mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  background: linear-gradient(135deg, var(--brand) 0%, var(--accent) 100%);
  color: #fff;
  box-shadow: var(--shadow-brand);
}

.admin-brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.admin-brand__text strong {
  font-size: var(--font-size-md);
  letter-spacing: 0.01em;
}

.admin-brand__text small {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.admin-nav__item {
  position: relative;
  display: grid;
  grid-template-columns: 20px 1fr;
  grid-template-rows: auto auto;
  align-items: center;
  column-gap: var(--space-3);
  row-gap: 1px;
  padding: 9px var(--space-3);
  border-radius: var(--radius-control);
  color: var(--text-secondary);
  transition: background-color var(--dur-fast) var(--ease-standard),
    color var(--dur-fast) var(--ease-standard);
}

.admin-nav__item svg {
  grid-row: 1 / span 2;
  color: var(--text-muted);
  transition: color var(--dur-fast) var(--ease-standard);
}

.admin-nav__item:hover {
  background: var(--bg-subtle);
  color: var(--text);
}

.admin-nav__label {
  font-size: var(--font-size-base);
  font-weight: 550;
}

.admin-nav__hint {
  color: var(--text-subtle);
  font-size: 11px;
}

.admin-nav__item.is-active {
  background: var(--brand-soft);
  color: var(--brand-600);
}

.admin-nav__item.is-active svg {
  color: var(--brand);
}

.admin-sidebar__footer {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: auto;
  padding: var(--space-3) var(--space-3) var(--space-2);
  border-top: 1px solid var(--border);
}

.admin-sidebar__link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.admin-sidebar__link:hover {
  color: var(--brand);
}

.admin-sidebar__version {
  color: var(--text-subtle);
  font-size: 11px;
}

.admin-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.admin-topbar {
  position: sticky;
  top: 0;
  z-index: var(--z-topbar);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  height: var(--topbar-height);
  padding: 0 var(--space-5);
  border-bottom: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(12px);
}

.admin-topbar__menu {
  display: none;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-surface);
  cursor: pointer;
}

.admin-crumb {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.admin-crumb__sep {
  color: var(--text-subtle);
}

.admin-crumb__current {
  overflow: hidden;
  color: var(--text);
  font-weight: 550;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-topbar__right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-left: auto;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 4px 8px 4px 4px;
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: border-color var(--dur-fast) var(--ease-standard),
    background-color var(--dur-fast) var(--ease-standard);
}

.admin-user:hover {
  border-color: var(--border);
  background: var(--bg-surface);
}

.admin-user__avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--brand) 0%, var(--accent) 100%);
  color: #fff;
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.admin-user__name {
  max-width: 8em;
  overflow: hidden;
  font-size: var(--font-size-sm);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-user__menu-text {
  margin-left: 8px;
}

.admin-content {
  width: 100%;
  max-width: var(--content-max-width);
  margin: 0 auto;
  padding: var(--space-5) var(--space-6) var(--space-7);
}

.admin-overlay {
  position: fixed;
  inset: 0;
  z-index: var(--z-overlay);
  border: 0;
  background: rgba(16, 26, 46, 0.38);
  cursor: pointer;
}

@media (max-width: 1023px) {
  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    transform: translateX(-100%);
    transition: transform var(--dur-page) var(--ease-out);
    box-shadow: var(--shadow-float);
  }

  .admin-sidebar--open {
    transform: translateX(0);
    z-index: calc(var(--z-overlay) + 1);
  }

  .admin-topbar__menu {
    display: grid;
    place-items: center;
  }

  .admin-topbar {
    padding: 0 var(--space-4);
  }

  .admin-content {
    padding: var(--space-4) var(--space-4) var(--space-6);
  }
}
</style>
