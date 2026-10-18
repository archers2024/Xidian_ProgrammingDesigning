<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElButton,
  ElMessage,
  ElPagination,
  ElTabPane,
  ElTable,
  ElTableColumn,
  ElTabs,
  ElTag,
} from 'element-plus'

import AppIcon from '@/components/common/AppIcon.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import PlaceholderPanel from '@/components/common/PlaceholderPanel.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { fetchActivityDetail, fetchRegistrations } from '@/api/admin/activity'
import { useAsyncData } from '@/composables/useAsyncData'
import { UNAVAILABLE_REASON_TEXT } from '@/types/activity'
import { formatDateTime } from '@/utils/datetime'
import { formatFileSize } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const validTabs = ['basic', 'registrations', 'checkin'] as const
type TabKey = (typeof validTabs)[number]

const activityId = computed(() => String(route.params.id ?? ''))

const activeTab = computed<TabKey>({
  get: () => {
    const tab = typeof route.query.tab === 'string' ? route.query.tab : ''
    return (validTabs as readonly string[]).includes(tab) ? (tab as TabKey) : 'basic'
  },
  set: (value) => {
    void router.replace({ query: { ...route.query, tab: value } })
  },
})

const detail = useAsyncData((signal) => fetchActivityDetail(activityId.value, signal))

const regPage = ref(1)
const registrations = useAsyncData(
  (signal) =>
    fetchRegistrations(activityId.value, { page: regPage.value, pageSize: 10 }, signal),
  { immediate: false },
)

watch(
  [activeTab, regPage],
  () => {
    if (activeTab.value === 'registrations') void registrations.run()
  },
  { immediate: true },
)

const activity = computed(() => detail.data.value)
const published = computed(() => activity.value?.publicationState === 'PUBLISHED')
const unavailableText = computed(() => {
  const reason = activity.value?.unavailableReason
  return reason ? UNAVAILABLE_REASON_TEXT[reason] : ''
})

function openPromo(): void {
  if (!activity.value) return
  const target = router.resolve({
    name: 'public-activity-promo',
    params: { id: activity.value.id },
  })
  window.open(target.href, '_blank', 'noopener')
}

function openScreen(): void {
  void router.push({ name: 'admin-checkin-screen', params: { id: activityId.value } })
}

async function copyText(value: string | null, label: string): Promise<void> {
  if (!value) {
    ElMessage.warning('活动尚未发布，暂无公开链接')
    return
  }
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success(`${label}已复制`)
  } catch {
    ElMessage.error('浏览器未授权剪贴板，请手动复制链接')
  }
}
</script>

<template>
  <div class="detail">
    <PageHeader
      :title="activity?.name ?? '活动详情'"
      :eyebrow="activity?.typeName"
      :description="
        activity
          ? `${formatDateTime(activity.startAt)} 开始 · ${activity.location}`
          : '正在读取活动信息…'
      "
      back-to="/admin/activities"
    >
      <template #actions>
        <ElButton v-if="published" @click="openPromo">
          <AppIcon name="external" :size="15" />
          <span>宣传页</span>
        </ElButton>
        <ElButton v-if="published" @click="openScreen">
          <AppIcon name="screen" :size="15" />
          <span>签到大屏</span>
        </ElButton>
        <ElButton
          type="primary"
          :disabled="!activity"
          @click="router.push({ name: 'admin-activity-edit', params: { id: activityId } })"
        >
          <AppIcon name="edit" :size="15" />
          <span>编辑活动</span>
        </ElButton>
      </template>
    </PageHeader>

    <LoadingSkeleton v-if="detail.loading.value && !activity" variant="detail" />
    <ErrorState
      v-else-if="detail.error.value"
      title="活动加载失败"
      :message="detail.error.value.message"
      :code="detail.error.value.code"
      @retry="detail.run"
    />

    <template v-else-if="activity">
      <section class="app-card detail__summary">
        <div class="detail__summary-main">
          <StatusTag :status="activity.displayStatus" />
          <span v-if="unavailableText" class="detail__reason">{{ unavailableText }}</span>
          <span v-else class="detail__reason detail__reason--ok">当前可报名</span>
          <span class="detail__version mono">版本 v{{ activity.version }}</span>
        </div>
        <div class="detail__summary-stats">
          <div>
            <span class="detail__stat-label">已报名</span>
            <strong class="detail__stat-value">{{ activity.registeredCount }}</strong>
          </div>
          <div>
            <span class="detail__stat-label">人数上限</span>
            <strong class="detail__stat-value">{{ activity.capacity }}</strong>
          </div>
          <div>
            <span class="detail__stat-label">报名截止</span>
            <strong class="detail__stat-value detail__stat-value--time">
              {{ formatDateTime(activity.registrationDeadline) }}
            </strong>
          </div>
        </div>
      </section>

      <section class="app-card detail__tabs">
        <ElTabs v-model="activeTab">
          <ElTabPane label="基本信息" name="basic">
            <dl class="facts">
              <div class="facts__item">
                <dt>活动类型</dt>
                <dd>{{ activity.typeName }}</dd>
              </div>
              <div class="facts__item">
                <dt>活动时间</dt>
                <dd>
                  {{ formatDateTime(activity.startAt) }} – {{ formatDateTime(activity.endAt) }}
                </dd>
              </div>
              <div class="facts__item">
                <dt>报名截止</dt>
                <dd>{{ formatDateTime(activity.registrationDeadline) }}</dd>
              </div>
              <div class="facts__item">
                <dt>活动地点</dt>
                <dd>{{ activity.location }}</dd>
              </div>
              <div class="facts__item">
                <dt>发布时间</dt>
                <dd>{{ activity.publishedAt ? formatDateTime(activity.publishedAt) : '尚未发布' }}</dd>
              </div>
              <div class="facts__item">
                <dt>直播链接</dt>
                <dd>
                  <a
                    v-if="activity.liveUrl"
                    :href="activity.liveUrl"
                    target="_blank"
                    rel="noreferrer"
                  >
                    {{ activity.liveUrl }}
                  </a>
                  <span v-else class="text-subtle">未设置</span>
                </dd>
              </div>
              <div class="facts__item facts__item--wide">
                <dt>活动简介</dt>
                <dd class="facts__description">
                  {{ activity.description || '暂无简介' }}
                </dd>
              </div>
              <div class="facts__item facts__item--wide">
                <dt>公开链接</dt>
                <dd class="facts__links">
                  <span v-if="!published" class="text-subtle">发布后生成宣传页与报名链接</span>
                  <template v-else>
                    <button
                      class="facts__link"
                      type="button"
                      @click="copyText(activity.shareUrl, '宣传页链接')"
                    >
                      <AppIcon name="link" :size="14" />
                      复制宣传页链接
                    </button>
                    <button
                      class="facts__link"
                      type="button"
                      @click="copyText(activity.registrationUrl, '报名链接')"
                    >
                      <AppIcon name="qr" :size="14" />
                      复制报名链接
                    </button>
                  </template>
                </dd>
              </div>
              <div class="facts__item facts__item--wide">
                <dt>活动附件</dt>
                <dd>
                  <ul v-if="activity.attachments.length > 0" class="attachments">
                    <li v-for="item in activity.attachments" :key="item.id" class="attachments__item">
                      <AppIcon name="inbox" :size="16" />
                      <span class="attachments__name">{{ item.originalName }}</span>
                      <span class="attachments__size mono">{{ formatFileSize(item.sizeBytes) }}</span>
                      <ElTag size="small" type="info">下载待接入</ElTag>
                    </li>
                  </ul>
                  <span v-else class="text-subtle">暂无附件（上传能力在批次4接入）</span>
                </dd>
              </div>
            </dl>
          </ElTabPane>

          <ElTabPane label="报名名单" name="registrations">
            <LoadingSkeleton v-if="registrations.loading.value && !registrations.data.value" variant="table" :rows="4" />
            <ErrorState
              v-else-if="registrations.error.value"
              :message="registrations.error.value.message"
              :code="registrations.error.value.code"
              @retry="registrations.run"
            />
            <template v-else>
              <ElTable :data="registrations.data.value?.items ?? []" row-key="id">
                <ElTableColumn prop="name" label="姓名" min-width="110" />
                <ElTableColumn prop="phone" label="手机号" min-width="130">
                  <template #default="{ row }">
                    <span class="mono">{{ row.phone }}</span>
                  </template>
                </ElTableColumn>
                <ElTableColumn prop="collegeName" label="学院" min-width="180" />
                <ElTableColumn label="报名时间" min-width="150">
                  <template #default="{ row }">{{ formatDateTime(row.registeredAt) }}</template>
                </ElTableColumn>
                <ElTableColumn label="签到" width="110">
                  <template #default="{ row }">
                    <ElTag :type="row.checkedIn ? 'success' : 'info'" size="small">
                      {{ row.checkedIn ? '已签到' : '未签到' }}
                    </ElTag>
                  </template>
                </ElTableColumn>
              </ElTable>
              <div class="detail__table-footer">
                <span class="text-muted text-sm">
                  共 {{ registrations.data.value?.total ?? 0 }} 条报名记录
                </span>
                <ElPagination
                  v-model:current-page="regPage"
                  :page-size="10"
                  :total="registrations.data.value?.total ?? 0"
                  layout="prev, pager, next"
                  small
                />
              </div>
            </template>
          </ElTabPane>

          <ElTabPane label="签到管理" name="checkin">
            <PlaceholderPanel
              title="签到会话与动态二维码"
              description="签到会话、静态/动态模式切换、30 秒动态码签发与撤销在批次6接入；现在可以先查看大屏框架与已签到人数。"
              :pending="[
                '创建签到会话并设置签到起止时间（默认活动开始前 30 分钟至结束后 15 分钟）',
                '动态模式每 30 秒刷新二维码，关闭会话或修改版本后旧码立即失效',
                '报名名单实时显示签到状态，并支持导出',
              ]"
            >
              <div class="detail__placeholder-actions">
                <ElButton v-if="published" type="primary" plain @click="openScreen">
                  <AppIcon name="screen" :size="15" />
                  <span>打开签到大屏</span>
                </ElButton>
              </div>
            </PlaceholderPanel>
          </ElTabPane>
        </ElTabs>
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.detail__summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
}

.detail__summary-main {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.detail__reason {
  color: var(--warning);
  font-size: var(--font-size-sm);
}

.detail__reason--ok {
  color: var(--success);
}

.detail__version {
  color: var(--text-subtle);
}

.detail__summary-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-5);
}

.detail__stat-label {
  display: block;
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.detail__stat-value {
  font-size: var(--font-size-md);
  font-variant-numeric: tabular-nums;
}

.detail__stat-value--time {
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.detail__tabs {
  padding: var(--space-2) var(--space-5) var(--space-5);
}

.facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--space-4);
}

.facts__item dt {
  margin-bottom: 2px;
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.facts__item dd {
  margin: 0;
  font-size: var(--font-size-sm);
  word-break: break-word;
}

.facts__item--wide {
  grid-column: 1 / -1;
}

.facts__description {
  max-width: 80ch;
  color: var(--text-secondary);
  line-height: 1.7;
  white-space: pre-line;
}

.facts__links {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.facts__link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--bg-surface);
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  cursor: pointer;
  transition: border-color var(--dur-fast) var(--ease-standard),
    color var(--dur-fast) var(--ease-standard);
}

.facts__link:hover {
  border-color: var(--brand-200);
  color: var(--brand);
}

.attachments {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.attachments__item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.attachments__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachments__size {
  color: var(--text-subtle);
}

.detail__table-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.detail__placeholder-actions {
  margin-top: var(--space-2);
}

@media (max-width: 767px) {
  .detail__tabs {
    padding: var(--space-2) var(--space-3) var(--space-4);
  }

  .detail__summary-stats {
    gap: var(--space-4);
  }
}
</style>
