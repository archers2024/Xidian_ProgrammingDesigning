<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus'

import AppIcon from '@/components/common/AppIcon.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { fetchDashboardOverview } from '@/api/admin/dashboard'
import { useAsyncData } from '@/composables/useAsyncData'
import { useAuthStore } from '@/stores/auth'
import { formatActivityTime } from '@/utils/datetime'

const router = useRouter()
const auth = useAuthStore()
const { data, error, loading, run } = useAsyncData((signal) => fetchDashboardOverview(signal))

const counts = computed(
  () =>
    data.value?.activityCounts ?? { total: 0, draft: 0, registering: 0, closed: 0, ended: 0 },
)
const recent = computed(() => data.value?.recentActivities ?? [])

const demoPromoRoute = { name: 'public-activity-promo', params: { id: 'act-ai-lecture' } }
const demoScreenRoute = { name: 'admin-checkin-screen', params: { id: 'act-ai-lecture' } }
/** 演示入口指向模拟数据中的活动，正式环境隐藏 */
const showDemoLinks = import.meta.env.VITE_USE_MOCK === 'true'
</script>

<template>
  <div class="dashboard">
    <PageHeader
      title="工作台"
      :eyebrow="`欢迎回来，${auth.displayName}`"
      description="活动、报名与签到的整体情况；统计数字来自后端概览接口，前端不做二次累计。"
    >
      <template #actions>
        <ElButton type="primary" @click="router.push({ name: 'admin-activity-create' })">
          <AppIcon name="plus" :size="16" />
          <span>创建活动</span>
        </ElButton>
      </template>
    </PageHeader>

    <ErrorState
      v-if="error"
      class="dashboard__error"
      title="概览数据加载失败"
      :message="error.message"
      :code="error.code"
      @retry="run"
    />

    <template v-else>
      <section class="dashboard__stats" aria-label="活动统计">
        <StatCard
          label="活动总数"
          :value="counts.total"
          icon="calendar"
          :hint="`草稿 ${counts.draft} · 已结束 ${counts.ended}`"
          :loading="loading"
        />
        <StatCard
          label="报名中"
          :value="counts.registering"
          icon="ticket"
          tone="accent"
          hint="已发布且未到报名截止"
          :loading="loading"
        />
        <StatCard
          label="累计报名"
          :value="data?.registrationCount ?? 0"
          icon="users"
          tone="success"
          hint="各活动报名人数合计"
          :loading="loading"
        />
        <StatCard
          label="已签到"
          :value="data?.checkinCount ?? 0"
          icon="check-circle"
          tone="muted"
          hint="按签到记录统计"
          :loading="loading"
        />
      </section>

      <div class="dashboard__columns">
        <section class="app-card app-card--padded dashboard__panel">
          <header class="dashboard__panel-head">
            <h2 class="section-title">近期活动</h2>
            <RouterLink class="dashboard__more" :to="{ name: 'admin-activity-list' }">
              全部活动
              <AppIcon name="chevron-right" :size="14" />
            </RouterLink>
          </header>

          <LoadingSkeleton v-if="loading" variant="list" :rows="4" />
          <ul v-else-if="recent.length > 0" class="recent">
            <li v-for="item in recent" :key="item.id" class="recent__row">
              <RouterLink
                class="recent__item"
                :to="{ name: 'admin-activity-detail', params: { id: item.id } }"
              >
                <span class="recent__main">
                  <span class="recent__name">{{ item.name }}</span>
                  <span class="recent__meta">
                    {{ item.typeName }} · {{ formatActivityTime(item.startAt, item.endAt) }}
                  </span>
                </span>
                <span class="recent__side">
                  <StatusTag :status="item.displayStatus" />
                  <span class="recent__count mono">
                    {{ item.registeredCount }}/{{ item.capacity }}
                  </span>
                </span>
              </RouterLink>
            </li>
          </ul>
          <EmptyState
            v-else
            title="还没有活动"
            description="创建第一个活动并发布后，手机端就能通过宣传页完成报名。"
            icon="calendar"
          >
            <template #actions>
              <ElButton type="primary" @click="router.push({ name: 'admin-activity-create' })">
                <AppIcon name="plus" :size="16" />
                <span>创建活动</span>
              </ElButton>
            </template>
          </EmptyState>
        </section>

        <aside class="dashboard__side">
          <section class="app-card app-card--padded dashboard__panel">
            <h2 class="section-title">快捷入口</h2>
            <div class="quick">
              <RouterLink class="quick__item" :to="{ name: 'admin-activity-list' }">
                <span class="quick__icon"><AppIcon name="calendar" :size="18" /></span>
                <span class="quick__text">
                  <strong>活动列表</strong>
                  <small>筛选、编辑与发布</small>
                </span>
                <AppIcon name="chevron-right" :size="16" />
              </RouterLink>

              <RouterLink v-if="showDemoLinks" class="quick__item" :to="demoPromoRoute" target="_blank">
                <span class="quick__icon quick__icon--accent"><AppIcon name="eye" :size="18" /></span>
                <span class="quick__text">
                  <strong>演示宣传页</strong>
                  <small>手机端展示效果</small>
                </span>
                <AppIcon name="external" :size="16" />
              </RouterLink>

              <RouterLink v-if="showDemoLinks" class="quick__item" :to="demoScreenRoute" target="_blank">
                <span class="quick__icon quick__icon--muted"><AppIcon name="screen" :size="18" /></span>
                <span class="quick__text">
                  <strong>签到大屏</strong>
                  <small>动态二维码与倒计时</small>
                </span>
                <AppIcon name="external" :size="16" />
              </RouterLink>
            </div>
          </section>

          <section class="app-card app-card--padded dashboard__panel dashboard__note">
            <h2 class="section-title">当前阶段</h2>
            <p>
              当前交付为前端框架：统一设计变量、路由与布局、接口封装、模拟接口与鉴权流程已经就绪。
              活动列表、创建与编辑、宣传页、报名页可直接操作；报名名单与签到管理将在后续批次接入。
            </p>
            <p class="dashboard__note-hint">
              模拟模式下数据保存在内存中，刷新页面会恢复为初始演示数据。
            </p>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.dashboard__error {
  margin-top: var(--space-2);
}

.dashboard__stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: var(--space-4);
}

.dashboard__columns {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
  gap: var(--space-4);
  align-items: start;
}

.dashboard__panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.dashboard__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.dashboard__more {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.dashboard__more:hover {
  color: var(--brand);
}

.recent {
  display: flex;
  flex-direction: column;
}

.recent__row + .recent__row {
  border-top: 1px solid var(--border);
}

.recent__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-2);
  border-radius: var(--radius-control);
  color: var(--text);
  transition: background-color var(--dur-fast) var(--ease-standard);
}

.recent__item:hover {
  background: var(--bg-subtle);
}

.recent__main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.recent__name {
  overflow: hidden;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent__meta {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.recent__side {
  display: flex;
  flex: none;
  align-items: center;
  gap: var(--space-3);
}

.recent__count {
  color: var(--text-muted);
}

.dashboard__side {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.quick {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.quick__item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
  color: var(--text);
  transition: border-color var(--dur-fast) var(--ease-standard),
    background-color var(--dur-fast) var(--ease-standard);
}

.quick__item:hover {
  border-color: var(--brand-200);
  background: var(--brand-50);
}

.quick__icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--brand-soft);
  color: var(--brand);
}

.quick__icon--accent {
  background: var(--accent-soft);
  color: var(--accent-strong);
}

.quick__icon--muted {
  background: var(--bg-subtle);
  color: var(--text-muted);
}

.quick__text {
  display: flex;
  flex: 1;
  flex-direction: column;
  line-height: 1.4;
}

.quick__text strong {
  font-size: var(--font-size-sm);
}

.quick__text small {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.dashboard__note p {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.dashboard__note-hint {
  margin-top: var(--space-2);
  color: var(--text-subtle) !important;
  font-size: var(--font-size-xs) !important;
}

@media (max-width: 1100px) {
  .dashboard__columns {
    grid-template-columns: 1fr;
  }
}
</style>
