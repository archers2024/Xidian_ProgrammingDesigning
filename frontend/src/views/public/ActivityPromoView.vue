<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton } from 'element-plus'

import AppIcon from '@/components/common/AppIcon.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { fetchPublicActivity } from '@/api/public/activity'
import { useAsyncData } from '@/composables/useAsyncData'
import { UNAVAILABLE_REASON_TEXT } from '@/types/activity'
import { formatActivityTime, formatDateTime, remainingSeats } from '@/utils/datetime'
import { formatFileSize, progressPercent } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const activityId = computed(() => String(route.params.id ?? ''))

const { data, error, loading, run } = useAsyncData((signal) =>
  fetchPublicActivity(activityId.value, signal),
)

const activity = computed(() => data.value)
const remaining = computed(() =>
  activity.value ? remainingSeats(activity.value.capacity, activity.value.registeredCount) : 0,
)
const filledPercent = computed(() =>
  activity.value
    ? progressPercent(activity.value.registeredCount, activity.value.capacity)
    : 0,
)
const reasonText = computed(() =>
  activity.value?.unavailableReason
    ? UNAVAILABLE_REASON_TEXT[activity.value.unavailableReason]
    : '',
)
const notFound = computed(
  () => error.value?.code === 'ACTIVITY_UNAVAILABLE' || error.value?.status === 404,
)

function goRegister(): void {
  if (!activity.value?.canRegister) return
  void router.push({ name: 'public-registration', params: { id: activity.value.id } })
}
</script>

<template>
  <div class="promo">
    <div v-if="loading && !activity" class="promo__container promo__skeleton">
      <LoadingSkeleton variant="detail" label="正在加载活动" />
    </div>

    <div v-else-if="error" class="promo__container promo__state">
      <EmptyState
        v-if="notFound"
        title="活动不存在或尚未发布"
        description="请确认链接是否完整，或联系活动组织者获取最新的宣传页地址。"
        icon="alert"
      />
      <ErrorState
        v-else
        title="活动加载失败"
        :message="error.message"
        :code="error.code"
        @retry="run"
      />
    </div>

    <div v-else-if="activity" class="promo__container promo__layout">
      <article class="promo__content">
        <figure class="promo__hero">
          <img
            v-if="activity.coverUrl"
            class="promo__hero-image"
            :src="activity.coverUrl"
            :alt="activity.name"
          />
          <div v-else class="promo__hero-fallback">
            <span class="promo__hero-type">{{ activity.typeName }}</span>
            <p class="promo__hero-text">校园活动</p>
            <span class="promo__hero-hint">封面图上传后在此展示</span>
          </div>
        </figure>

        <header class="promo__head">
          <div class="promo__head-meta">
            <span class="promo__type">{{ activity.typeName }}</span>
            <StatusTag :status="activity.displayStatus" />
          </div>
          <h1 class="promo__title">{{ activity.name }}</h1>
          <p class="promo__validity-inline">
            宣传有效期：{{ formatDateTime(activity.publishedAt) }} 起，至报名截止
            {{ formatDateTime(activity.registrationDeadline) }}
          </p>
        </header>

        <dl class="promo__facts">
          <div>
            <dt><AppIcon name="clock" :size="16" /> 活动时间</dt>
            <dd>{{ formatActivityTime(activity.startAt, activity.endAt) }}</dd>
          </div>
          <div>
            <dt><AppIcon name="location" :size="16" /> 活动地点</dt>
            <dd>{{ activity.location }}</dd>
          </div>
          <div>
            <dt><AppIcon name="ticket" :size="16" /> 报名名额</dt>
            <dd>{{ activity.registeredCount }} / {{ activity.capacity }} 人</dd>
          </div>
          <div>
            <dt><AppIcon name="info" :size="16" /> 报名截止</dt>
            <dd>{{ formatDateTime(activity.registrationDeadline) }}</dd>
          </div>
        </dl>

        <section class="promo__section">
          <h2 class="promo__section-title">活动介绍</h2>
          <p class="promo__description">{{ activity.description || '组织者暂未填写活动介绍。' }}</p>
        </section>

        <section v-if="activity.liveUrl" class="promo__section">
          <h2 class="promo__section-title">线上直播</h2>
          <a class="promo__live" :href="activity.liveUrl" target="_blank" rel="noreferrer">
            <AppIcon name="live" :size="18" />
            <span>进入直播间</span>
            <AppIcon name="external" :size="15" />
          </a>
        </section>

        <section v-if="activity.attachments.length > 0" class="promo__section">
          <h2 class="promo__section-title">活动材料</h2>
          <ul class="promo__attachments">
            <li
              v-for="item in activity.attachments"
              :key="item.id"
              class="promo__attachment"
            >
              <AppIcon name="inbox" :size="16" />
              <span class="promo__attachment-name">{{ item.originalName }}</span>
              <span class="promo__attachment-size mono">{{ formatFileSize(item.sizeBytes) }}</span>
            </li>
          </ul>
          <p class="promo__attachment-note">公开下载在批次4接入，当前只展示材料清单。</p>
        </section>
      </article>

      <aside class="promo__aside">
        <div class="promo__card">
          <p class="promo__card-title">报名参加</p>

          <div class="promo__quota">
            <p class="promo__quota-text">
              <strong>{{ activity.registeredCount }}</strong>
              <span>/ {{ activity.capacity }} 人已报名</span>
            </p>
            <div class="promo__progress" aria-hidden="true">
              <span class="promo__progress-bar" :style="{ width: `${filledPercent}%` }" />
            </div>
            <p class="promo__quota-hint">
              {{
                remaining > 0
                  ? `剩余 ${remaining} 个名额，额满即止`
                  : '名额已满，无法继续报名'
              }}
            </p>
          </div>

          <ElButton
            class="promo__cta"
            type="primary"
            size="large"
            :disabled="!activity.canRegister"
            @click="goRegister"
          >
            {{ activity.canRegister ? '立即报名' : reasonText }}
          </ElButton>

          <p v-if="!activity.canRegister" class="promo__closed">
            {{ reasonText }}，活动信息仍可查看。
          </p>

          <div class="promo__qr">
            <span class="promo__qr-box">
              <AppIcon name="qr" :size="34" />
            </span>
            <span class="promo__qr-text">
              <strong>手机扫码报名</strong>
              <small>二维码接入后可直接扫码进入报名页</small>
            </span>
          </div>

          <p class="promo__notice">
            无需注册账号，填写姓名、手机号与学院即可报名；同一活动同一手机号只能报名一次。
          </p>
        </div>
      </aside>
    </div>

    <div v-if="activity" class="promo__mobile-cta">
      <div class="promo__mobile-info">
        <strong>{{ remaining }}</strong>
        <span>个名额</span>
      </div>
      <ElButton
        class="promo__mobile-button"
        type="primary"
        size="large"
        :disabled="!activity.canRegister"
        @click="goRegister"
      >
        {{ activity.canRegister ? '立即报名' : reasonText }}
      </ElButton>
    </div>
  </div>
</template>

<style scoped>
.promo {
  padding: var(--space-5) var(--space-4) var(--space-7);
  background: var(--bg-page);
}

.promo__container {
  width: 100%;
  max-width: var(--public-max-width);
  margin: 0 auto;
}

.promo__skeleton {
  padding: var(--space-5);
  border-radius: var(--radius-card);
  background: var(--bg-surface);
}

.promo__layout {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(300px, 0.85fr);
  align-items: start;
  gap: var(--space-5);
}

.promo__content {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.promo__hero {
  margin: 0;
  overflow: hidden;
  border-radius: var(--radius-card);
  background: var(--bg-surface);
  box-shadow: var(--shadow-card);
}

.promo__hero-image {
  width: 100%;
  aspect-ratio: 16 / 7;
  object-fit: cover;
}

.promo__hero-fallback {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: var(--space-2);
  min-height: 220px;
  padding: var(--space-5);
  background: radial-gradient(circle at 12% 18%, rgba(14, 159, 159, 0.55), transparent 48%),
    linear-gradient(135deg, #142a63 0%, #1d4ed8 62%, #3f6ff0 120%);
  color: #fff;
}

.promo__hero-type {
  align-self: flex-start;
  padding: 4px 10px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.18);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.promo__hero-text {
  font-size: clamp(24px, 3vw, 34px);
  font-weight: 650;
  letter-spacing: 0.02em;
}

.promo__hero-hint {
  color: rgba(255, 255, 255, 0.66);
  font-size: var(--font-size-xs);
}

.promo__head {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.promo__head-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.promo__type {
  padding: 3px 9px;
  border-radius: var(--radius-pill);
  background: var(--brand-soft);
  color: var(--brand-600);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.promo__title {
  font-size: clamp(26px, 3.4vw, 38px);
  line-height: 1.22;
  letter-spacing: -0.02em;
}

.promo__validity-inline {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.promo__facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-3);
  margin: 0;
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--bg-surface);
}

.promo__facts div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.promo__facts dt {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.promo__facts dd {
  margin: 0;
  font-size: var(--font-size-sm);
  font-weight: 550;
}

.promo__section {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-5);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--bg-surface);
}

.promo__section-title {
  font-size: var(--font-size-md);
}

.promo__description {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.85;
  white-space: pre-line;
}

.promo__live {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  align-self: flex-start;
  padding: 10px 14px;
  border: 1px solid var(--brand-200);
  border-radius: var(--radius-control);
  background: var(--brand-50);
  color: var(--brand-600);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.promo__attachments {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.promo__attachment {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.promo__attachment-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.promo__attachment-size {
  color: var(--text-subtle);
}

.promo__attachment-note {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.promo__aside {
  position: sticky;
  top: calc(var(--space-5) + 64px);
}

.promo__card {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-5);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--bg-surface);
  box-shadow: var(--shadow-card);
}

.promo__card-title {
  font-size: var(--font-size-md);
  font-weight: 650;
}

.promo__quota {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.promo__quota-text {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.promo__quota-text strong {
  color: var(--text);
  font-size: var(--font-size-xl);
  font-variant-numeric: tabular-nums;
}

.promo__progress {
  height: 6px;
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: var(--bg-subtle);
}

.promo__progress-bar {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--brand) 0%, var(--accent) 100%);
  transition: width var(--dur-page) var(--ease-out);
}

.promo__quota-hint {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.promo__cta {
  width: 100%;
}

.promo__closed {
  color: var(--warning);
  font-size: var(--font-size-xs);
  text-align: center;
}

.promo__qr {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-control);
  background: var(--bg-subtle);
}

.promo__qr-box {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 10px;
  background: var(--bg-surface);
  color: var(--text-muted);
  box-shadow: var(--shadow-sm);
}

.promo__qr-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: var(--font-size-sm);
}

.promo__qr-text small {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.promo__notice {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
  line-height: 1.7;
}

.promo__mobile-cta {
  display: none;
}

@media (max-width: 960px) {
  .promo {
    padding: var(--space-4) var(--space-4) calc(96px + env(safe-area-inset-bottom));
  }

  .promo__layout {
    grid-template-columns: 1fr;
    gap: var(--space-4);
  }

  .promo__aside {
    position: static;
    order: 2;
  }

  .promo__cta,
  .promo__closed {
    display: none;
  }

  .promo__hero-fallback {
    min-height: 168px;
  }

  .promo__mobile-cta {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 30;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4) calc(var(--space-3) + env(safe-area-inset-bottom));
    border-top: 1px solid var(--border);
    background: rgba(255, 255, 255, 0.94);
    backdrop-filter: blur(12px);
  }

  .promo__mobile-info {
    display: flex;
    align-items: baseline;
    gap: 4px;
    color: var(--text-muted);
    font-size: var(--font-size-xs);
  }

  .promo__mobile-info strong {
    color: var(--text);
    font-size: var(--font-size-xl);
  }

  .promo__mobile-button {
    min-width: 168px;
  }
}
</style>
