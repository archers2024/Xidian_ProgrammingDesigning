<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton } from 'element-plus'

import AppIcon from '@/components/common/AppIcon.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import { fetchActivityDetail, fetchCheckinSummary } from '@/api/admin/activity'
import { useAsyncData } from '@/composables/useAsyncData'
import { formatActivityTime } from '@/utils/datetime'
import { progressPercent } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const activityId = computed(() => String(route.params.id ?? ''))

const { data, error, loading, run } = useAsyncData(async (signal) => {
  const [activity, summary] = await Promise.all([
    fetchActivityDetail(activityId.value, signal),
    fetchCheckinSummary(activityId.value, signal),
  ])
  return { activity, summary }
})

/** 动态码每 30 秒一段；凭证接口接入前先展示倒计时框架 */
const secondsLeft = ref(30)
let timer: number | undefined

onMounted(() => {
  timer = window.setInterval(() => {
    secondsLeft.value = secondsLeft.value <= 1 ? 30 : secondsLeft.value - 1
  }, 1000)
})

onUnmounted(() => {
  window.clearInterval(timer)
})

const summary = computed(() => data.value?.summary)
const activity = computed(() => data.value?.activity)

const checkinRate = computed(() =>
  summary.value ? progressPercent(summary.value.checkedInCount, summary.value.registeredCount) : 0,
)

const dipPercent = computed(() => Math.round(((30 - secondsLeft.value) / 30) * 100))

const sessionEnabled = computed(() => summary.value?.sessionEnabled === true)

function backToDetail(): void {
  void router.push({ name: 'admin-activity-detail', params: { id: activityId.value } })
}
</script>

<template>
  <div class="screen">
    <header class="screen__bar">
      <div class="screen__brand">
        <span class="screen__mark">
          <AppIcon name="check" :size="18" :stroke-width="2.6" />
        </span>
        <div class="screen__brand-text">
          <strong>校园活动 · 签到大屏</strong>
          <small>动态二维码每 30 秒更新</small>
        </div>
      </div>

      <ElButton class="screen__back" @click="backToDetail">
        <AppIcon name="arrow-left" :size="16" />
        <span>返回活动</span>
      </ElButton>
    </header>

    <main class="screen__main">
      <LoadingSkeleton v-if="loading && !data" label="正在加载签到信息" variant="cards" :rows="2" />
      <ErrorState
        v-else-if="error"
        title="签到信息加载失败"
        :message="error.message"
        :code="error.code"
        @retry="run"
      />

      <template v-else-if="activity && summary">
        <section class="screen__info">
          <p class="screen__eyebrow">{{ activity.typeName }}</p>
          <h1 class="screen__title">{{ activity.name }}</h1>
          <p class="screen__time">
            <AppIcon name="clock" :size="18" />
            {{ formatActivityTime(activity.startAt, activity.endAt) }}
          </p>
          <p class="screen__time">
            <AppIcon name="location" :size="18" />
            {{ activity.location }}
          </p>

          <div class="screen__stats">
            <div class="screen__stat">
              <span class="screen__stat-label">已签到</span>
              <strong class="screen__stat-value">{{ summary.checkedInCount }}</strong>
            </div>
            <div class="screen__stat">
              <span class="screen__stat-label">报名人数</span>
              <strong class="screen__stat-value">{{ summary.registeredCount }}</strong>
            </div>
            <div class="screen__stat">
              <span class="screen__stat-label">签到率</span>
              <strong class="screen__stat-value">{{ checkinRate }}%</strong>
            </div>
          </div>

          <div class="screen__progress" aria-hidden="true">
            <span class="screen__progress-bar" :style="{ width: `${checkinRate}%` }" />
          </div>

          <p class="screen__status">
            <span class="screen__dot" :class="{ 'screen__dot--live': sessionEnabled }" />
            {{
              sessionEnabled
                ? summary.sessionMode === 'DYNAMIC'
                  ? '动态签到已开启'
                  : '基本签到已开启'
                : '签到会话尚未开启'
            }}
            <span class="screen__status-note">签到会话的创建与关闭在批次6接入</span>
          </p>
        </section>

        <section class="screen__qr" aria-label="签到二维码区域">
          <div class="screen__qr-frame">
            <div class="screen__qr-placeholder">
              <AppIcon name="qr" :size="64" :stroke-width="1.2" />
              <p>动态二维码</p>
              <span>凭证接口接入后在此展示</span>
            </div>
            <div class="screen__qr-ring" :style="{ '--ring': `${dipPercent}%` }">
              <span>{{ secondsLeft }}</span>
              <small>秒</small>
            </div>
          </div>
          <p class="screen__qr-hint">
            学生使用手机扫描大屏二维码，在签到页输入报名手机号完成签到。
          </p>
          <p class="screen__qr-note">
            当前为框架阶段：二维码编码库与动态凭证将在批次6接入，倒计时按 30 秒周期演示。
          </p>
        </section>
      </template>
    </main>

    <footer class="screen__footer">
      <span>高校学生活动全流程管理系统 · 签到大屏</span>
      <span class="mono">session {{ activityId }} · 前端框架 v0.1</span>
    </footer>
  </div>
</template>

<style scoped>
.screen {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: var(--space-5) var(--space-6);
  background: radial-gradient(circle at 15% 10%, rgba(14, 159, 159, 0.28), transparent 42%),
    radial-gradient(circle at 85% 85%, rgba(29, 78, 216, 0.42), transparent 50%),
    linear-gradient(145deg, #0b1226 0%, #101c3a 55%, #16234a 100%);
  color: #eef2fb;
}

.screen__bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.screen__brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.screen__mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
}

.screen__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.screen__brand-text strong {
  font-size: var(--font-size-md);
}

.screen__brand-text small {
  color: rgba(238, 242, 251, 0.62);
  font-size: var(--font-size-xs);
}

.screen__back {
  border-color: rgba(255, 255, 255, 0.24);
  background: rgba(255, 255, 255, 0.08);
  color: #eef2fb;
}

.screen__back:hover {
  border-color: rgba(255, 255, 255, 0.42);
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.screen__main {
  display: grid;
  flex: 1;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  align-items: center;
  gap: var(--space-7);
  padding: var(--space-6) 0;
}

.screen__eyebrow {
  color: #7fe3dc;
  font-size: var(--font-size-sm);
  font-weight: 600;
  letter-spacing: 0.14em;
}

.screen__title {
  margin-top: var(--space-2);
  color: #fff;
  font-size: clamp(30px, 4vw, 52px);
  line-height: 1.16;
  letter-spacing: -0.02em;
}

.screen__time {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-3);
  color: rgba(238, 242, 251, 0.76);
  font-size: var(--font-size-lg);
}

.screen__stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-7);
  margin-top: var(--space-6);
}

.screen__stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.screen__stat-label {
  color: rgba(238, 242, 251, 0.6);
  font-size: var(--font-size-sm);
}

.screen__stat-value {
  color: #fff;
  font-size: clamp(32px, 3.4vw, 46px);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}

.screen__progress {
  height: 8px;
  margin-top: var(--space-5);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.14);
  overflow: hidden;
}

.screen__progress-bar {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #35d3c6, #7fe3dc);
  transition: width var(--dur-page) var(--ease-out);
}

.screen__status {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-5);
  color: rgba(238, 242, 251, 0.82);
  font-size: var(--font-size-sm);
}

.screen__status-note {
  color: rgba(238, 242, 251, 0.44);
  font-size: var(--font-size-xs);
}

.screen__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(238, 242, 251, 0.4);
}

.screen__dot--live {
  background: #35d3c6;
  box-shadow: 0 0 0 4px rgba(53, 211, 198, 0.18);
}

.screen__qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
}

.screen__qr-frame {
  position: relative;
  display: grid;
  place-items: center;
  width: min(340px, 68vw);
  aspect-ratio: 1;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: 0 30px 70px rgba(3, 8, 20, 0.45);
}

.screen__qr-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  color: rgba(238, 242, 251, 0.72);
  text-align: center;
}

.screen__qr-placeholder p {
  font-size: var(--font-size-md);
  font-weight: 600;
}

.screen__qr-placeholder span {
  color: rgba(238, 242, 251, 0.46);
  font-size: var(--font-size-xs);
}

.screen__qr-ring {
  position: absolute;
  right: -14px;
  bottom: -14px;
  display: grid;
  place-items: center;
  width: 84px;
  height: 84px;
  border-radius: 50%;
  background: conic-gradient(#35d3c6 var(--ring), rgba(255, 255, 255, 0.16) 0);
  color: #fff;
}

.screen__qr-ring::before {
  content: '';
  position: absolute;
  inset: 6px;
  border-radius: 50%;
  background: #101c3a;
}

.screen__qr-ring span,
.screen__qr-ring small {
  position: relative;
  z-index: 1;
}

.screen__qr-ring span {
  font-size: var(--font-size-xl);
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

.screen__qr-ring small {
  color: rgba(238, 242, 251, 0.6);
  font-size: 10px;
}

.screen__qr-hint {
  max-width: 34ch;
  color: rgba(238, 242, 251, 0.8);
  font-size: var(--font-size-sm);
  text-align: center;
}

.screen__qr-note {
  max-width: 40ch;
  color: rgba(238, 242, 251, 0.42);
  font-size: var(--font-size-xs);
  text-align: center;
}

.screen__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding-top: var(--space-4);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(238, 242, 251, 0.42);
  font-size: var(--font-size-xs);
}

@media (max-width: 900px) {
  .screen {
    padding: var(--space-4);
  }

  .screen__main {
    grid-template-columns: 1fr;
    gap: var(--space-5);
    padding: var(--space-4) 0;
  }

  .screen__stats {
    gap: var(--space-4);
    margin-top: var(--space-4);
  }

  .screen__title {
    font-size: 30px;
  }

  .screen__qr-frame {
    width: min(300px, 74vw);
  }
}
</style>
