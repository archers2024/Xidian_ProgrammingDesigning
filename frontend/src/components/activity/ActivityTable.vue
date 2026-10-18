<script setup lang="ts">
import { ElButton, ElTable, ElTableColumn, ElTooltip } from 'element-plus'
import AppIcon from '@/components/common/AppIcon.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import type { ActivitySummary } from '@/types/activity'
import { formatShortDateTime, remainingSeats } from '@/utils/datetime'

defineProps<{
  items: ActivitySummary[]
}>()

const emit = defineEmits<{
  view: [id: string]
  edit: [id: string]
  publish: [activity: ActivitySummary]
  remove: [activity: ActivitySummary]
}>()

/** Element Plus 表格插槽使用宽松行类型，这里收窄回活动类型供事件使用 */
function asActivity(row: unknown): ActivitySummary {
  return row as ActivitySummary
}
</script>

<template>
  <ElTable :data="items" class="activity-table" row-key="id" stripe>
    <ElTableColumn label="活动" min-width="260">
      <template #default="{ row }">
        <div class="activity-table__main">
          <button class="activity-table__name" type="button" @click="emit('view', row.id)">
            {{ row.name }}
          </button>
          <span class="activity-table__meta">
            <AppIcon name="location" :size="13" />
            {{ row.location }}
          </span>
        </div>
      </template>
    </ElTableColumn>

    <ElTableColumn label="类型" width="112">
      <template #default="{ row }">
        <span class="text-sm">{{ row.typeName }}</span>
      </template>
    </ElTableColumn>

    <ElTableColumn label="活动时间" width="190">
      <template #default="{ row }">
        <div class="activity-table__time">
          <span>{{ formatShortDateTime(row.startAt) }}</span>
          <span class="activity-table__time-sub">
            报名截止 {{ formatShortDateTime(row.registrationDeadline) }}
          </span>
        </div>
      </template>
    </ElTableColumn>

    <ElTableColumn label="报名" width="150">
      <template #default="{ row }">
        <div class="activity-table__quota">
          <span class="mono">{{ row.registeredCount }} / {{ row.capacity }}</span>
          <span class="activity-table__quota-hint">
            {{
              remainingSeats(row.capacity, row.registeredCount) === 0
                ? '名额已满'
                : `余 ${remainingSeats(row.capacity, row.registeredCount)}`
            }}
          </span>
        </div>
      </template>
    </ElTableColumn>

    <ElTableColumn label="状态" width="108">
      <template #default="{ row }">
        <StatusTag :status="row.displayStatus" />
      </template>
    </ElTableColumn>

    <ElTableColumn label="操作" width="232" fixed="right">
      <template #default="{ row }">
        <div class="activity-table__actions">
          <ElButton link type="primary" @click="emit('view', row.id)">查看</ElButton>
          <ElButton link type="primary" @click="emit('edit', row.id)">编辑</ElButton>
          <ElTooltip
            v-if="row.publicationState === 'DRAFT'"
            content="发布后生成宣传页与报名二维码"
            placement="top"
          >
            <ElButton link type="primary" @click="emit('publish', asActivity(row))">发布</ElButton>
          </ElTooltip>
          <ElButton link type="danger" @click="emit('remove', asActivity(row))">删除</ElButton>
        </div>
      </template>
    </ElTableColumn>

    <template #empty>
      <EmptyState
        title="还没有符合条件的活动"
        description="调整筛选条件，或直接创建一个新活动；草稿状态的活动不会出现在公开页面。"
        icon="calendar"
      >
        <template #actions>
          <ElButton type="primary" @click="$router.push({ name: 'admin-activity-create' })">
            <AppIcon name="plus" :size="16" />
            <span>创建活动</span>
          </ElButton>
        </template>
      </EmptyState>
    </template>
  </ElTable>
</template>

<style scoped>
.activity-table {
  border-radius: var(--radius-card);
}

.activity-table__main {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.activity-table__name {
  padding: 0;
  border: 0;
  background: none;
  color: var(--text);
  font-size: var(--font-size-base);
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: color var(--dur-fast) var(--ease-standard);
}

.activity-table__name:hover {
  color: var(--brand);
}

.activity-table__meta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--text-muted);
  font-size: var(--font-size-xs);
}

.activity-table__time {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.activity-table__time-sub {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.activity-table__quota {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.activity-table__quota-hint {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.activity-table__actions {
  display: flex;
  flex-wrap: nowrap;
  gap: 2px;
}

/* Element Plus 给相邻按钮加了外边距，这里改用固定间距，避免操作列换行 */
.activity-table__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.activity-table__actions :deep(.el-button) {
  padding: 4px 6px;
}
</style>
