<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElButton,
  ElDatePicker,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElTooltip,
} from 'element-plus'

import ActivityTable from '@/components/activity/ActivityTable.vue'
import AppIcon from '@/components/common/AppIcon.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import {
  deleteActivity,
  fetchActivityPage,
  publishActivity,
} from '@/api/admin/activity'
import { fetchActivityTypes } from '@/api/public/catalog'
import { ApiError } from '@/api/http'
import { useAsyncData } from '@/composables/useAsyncData'
import { DISPLAY_STATUS_LABEL, type ActivityDisplayStatus, type ActivityListQuery, type ActivitySummary } from '@/types/activity'
import { toDayEnd, toDayStart } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const typeId = ref('')
const status = ref<'' | ActivityDisplayStatus>('')
const dateRange = ref<string[]>([])

const types = useAsyncData((signal) => fetchActivityTypes(signal))

const statusOptions = Object.entries(DISPLAY_STATUS_LABEL).map(([value, label]) => ({
  value: value as ActivityDisplayStatus,
  label,
}))

function parsePage(value: unknown, fallback: number, max: number): number {
  const parsed = Number(value)
  if (!Number.isInteger(parsed) || parsed < 1) return fallback
  return Math.min(parsed, max)
}

const page = computed(() => parsePage(route.query.page, 1, 1000))
const pageSize = computed(() => parsePage(route.query.pageSize, 10, 50))

const apiQuery = computed<ActivityListQuery>(() => {
  const query: ActivityListQuery = { page: page.value, pageSize: pageSize.value }
  const keywordValue = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  const typeValue = typeof route.query.typeId === 'string' ? route.query.typeId : ''
  const statusValue = typeof route.query.status === 'string' ? route.query.status : ''
  const from = typeof route.query.dateFrom === 'string' ? route.query.dateFrom : ''
  const to = typeof route.query.dateTo === 'string' ? route.query.dateTo : ''

  if (keywordValue) query.keyword = keywordValue
  if (typeValue) query.typeId = typeValue
  if (statusValue) query.status = statusValue as ActivityDisplayStatus
  if (from) query.dateFrom = from
  if (to) query.dateTo = to
  return query
})

const { data, error, loading, run } = useAsyncData(
  (signal) => fetchActivityPage(apiQuery.value, signal),
  { immediate: false },
)

const items = computed(() => data.value?.items ?? [])
const total = computed(() => data.value?.total ?? 0)

function syncFromQuery(): void {
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  typeId.value = typeof route.query.typeId === 'string' ? route.query.typeId : ''
  status.value =
    typeof route.query.status === 'string' ? (route.query.status as ActivityDisplayStatus) : ''
  const from = typeof route.query.dateFrom === 'string' ? route.query.dateFrom.slice(0, 10) : ''
  const to = typeof route.query.dateTo === 'string' ? route.query.dateTo.slice(0, 10) : ''
  dateRange.value = from && to ? [from, to] : []
}

watch(
  () => route.fullPath,
  () => {
    syncFromQuery()
    void run()
  },
  { immediate: true },
)

/** 筛选条件进入 URL，返回列表时恢复条件；分页参数单独维护 */
function applyFilters(patch: Record<string, string>, options: { resetPage?: boolean } = {}): void {
  const query: Record<string, string> = {}
  for (const [key, value] of Object.entries(route.query)) {
    if (typeof value === 'string' && value.length > 0) query[key] = value
  }
  Object.assign(query, patch)
  if (options.resetPage) query.page = '1'
  for (const key of Object.keys(query)) {
    if (!query[key]) delete query[key]
  }
  void router.replace({ query })
}

let keywordTimer: number | undefined

watch(keyword, (value) => {
  window.clearTimeout(keywordTimer)
  keywordTimer = window.setTimeout(() => {
    const current = typeof route.query.keyword === 'string' ? route.query.keyword : ''
    if (current !== value.trim()) {
      applyFilters({ keyword: value.trim() }, { resetPage: true })
    }
  }, 300)
})

onUnmounted(() => window.clearTimeout(keywordTimer))

function applySelects(): void {
  applyFilters({ typeId: typeId.value, status: status.value }, { resetPage: true })
}

function applyDateRange(value: unknown): void {
  const range = Array.isArray(value) ? value.map((item) => String(item)) : []
  if (range.length < 2) {
    applyFilters({ dateFrom: '', dateTo: '' }, { resetPage: true })
    return
  }
  applyFilters(
    { dateFrom: toDayStart(range[0]), dateTo: toDayEnd(range[1]) },
    { resetPage: true },
  )
}

function resetFilters(): void {
  window.clearTimeout(keywordTimer)
  keyword.value = ''
  typeId.value = ''
  status.value = ''
  dateRange.value = []
  void router.replace({ query: {} })
}

function handlePageChange(value: number): void {
  applyFilters({ page: String(value) })
}

function handleSizeChange(value: number): void {
  applyFilters({ pageSize: String(value) }, { resetPage: true })
}

function notifyError(caught: unknown): void {
  ElMessage.error(caught instanceof ApiError ? caught.message : '操作失败，请稍后重试')
}

async function handlePublish(activity: ActivitySummary): Promise<void> {
  try {
    const result = await publishActivity(activity.id)
    ElMessage.success(`已发布「${activity.name}」，宣传页地址：${result.shareUrl}`)
    await run()
  } catch (caught) {
    notifyError(caught)
  }
}

async function handleDelete(activity: ActivitySummary): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `删除后公开页面会失效，也不能再报名或签到；已有报名记录仍会保留。`,
      `确认删除「${activity.name}」？`,
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteActivity(activity.id)
    ElMessage.success('活动已删除')
    await run()
  } catch (caught) {
    notifyError(caught)
  }
}

function goDetail(id: string): void {
  void router.push({ name: 'admin-activity-detail', params: { id } })
}

function goEdit(id: string): void {
  void router.push({ name: 'admin-activity-edit', params: { id } })
}
</script>

<template>
  <div class="activity-list">
    <PageHeader
      title="活动管理"
      description="按类型、状态与时间组合筛选；筛选条件和分页都保留在地址栏，进入详情再返回不会丢失。"
    >
      <template #actions>
        <ElButton type="primary" @click="router.push({ name: 'admin-activity-create' })">
          <AppIcon name="plus" :size="16" />
          <span>创建活动</span>
        </ElButton>
      </template>
    </PageHeader>

    <section class="app-card app-card--padded filters" aria-label="活动筛选">
      <div class="filters__grid">
        <div class="filters__field filters__field--grow">
          <span class="filters__label">关键词</span>
          <ElInput v-model="keyword" clearable placeholder="活动名称或地点">
            <template #prefix>
              <AppIcon name="search" :size="15" />
            </template>
          </ElInput>
        </div>

        <div class="filters__field">
          <span class="filters__label">活动类型</span>
          <ElSelect
            v-model="typeId"
            placeholder="全部类型"
            clearable
            :loading="types.loading.value"
            @change="applySelects"
          >
            <ElOption
              v-for="item in types.data.value?.items ?? []"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </ElSelect>
        </div>

        <div class="filters__field">
          <span class="filters__label">展示状态</span>
          <ElSelect v-model="status" placeholder="全部状态" clearable @change="applySelects">
            <ElOption
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
        </div>

        <div class="filters__field filters__field--wide">
          <span class="filters__label">活动时间</span>
          <ElDatePicker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            unlink-panels
            @change="applyDateRange"
          />
        </div>

        <div class="filters__actions">
          <ElButton @click="resetFilters">
            <AppIcon name="refresh" :size="15" />
            <span>重置</span>
          </ElButton>
        </div>
      </div>
    </section>

    <section class="app-card list-panel">
      <header class="list-panel__head">
        <p class="list-panel__count">
          共 <strong>{{ total }}</strong> 个活动
          <span class="list-panel__hint">状态由后端按服务器时间计算</span>
        </p>
        <ElTooltip content="重新读取当前筛选结果" placement="top">
          <ElButton text :loading="loading" @click="run">
            <AppIcon name="refresh" :size="15" />
            <span>刷新</span>
          </ElButton>
        </ElTooltip>
      </header>

      <div class="list-panel__body">
        <LoadingSkeleton v-if="loading && !data" variant="table" :rows="5" />
        <ErrorState
          v-else-if="error"
          :message="error.message"
          :code="error.code"
          @retry="run"
        />
        <template v-else>
          <ActivityTable
            :items="items"
            :class="{ 'list-panel__table--loading': loading }"
            @view="goDetail"
            @edit="goEdit"
            @publish="handlePublish"
            @remove="handleDelete"
          />
        </template>
      </div>

      <footer v-if="!error && total > 0" class="list-panel__footer">
        <ElPagination
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </footer>
    </section>
  </div>
</template>

<style scoped>
.activity-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.filters__grid {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: var(--space-3);
}

.filters__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 168px;
}

.filters__field--grow {
  flex: 1 1 240px;
  min-width: 200px;
}

.filters__field--wide {
  width: 268px;
}

.filters__label {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  font-weight: 550;
}

.filters__actions {
  margin-left: auto;
}

.list-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.list-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--border);
}

.list-panel__count {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.list-panel__count strong {
  color: var(--text);
  font-size: var(--font-size-md);
}

.list-panel__hint {
  margin-left: var(--space-2);
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.list-panel__body {
  padding: var(--space-2) var(--space-2) 0;
}

.list-panel__table--loading {
  opacity: 0.55;
  transition: opacity var(--dur-fast) var(--ease-standard);
}

.list-panel__footer {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-3) var(--space-4);
  border-top: 1px solid var(--border);
}

@media (max-width: 767px) {
  .filters__field,
  .filters__field--wide {
    width: 100%;
  }

  .filters__actions {
    width: 100%;
    margin-left: 0;
  }

  .filters__actions :deep(.el-button) {
    width: 100%;
  }

  .list-panel__hint {
    display: none;
  }
}
</style>
