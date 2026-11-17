<template>
  <!-- UI-02 活动列表页（B岗）：四维组合筛选 + 发布/编辑/删除 -->
  <div>
    <div class="page-heading"><div><p class="eyebrow">ACTIVITY MANAGEMENT</p><h2>活动管理</h2><p>管理每一场活动，从发布到圆满结束。</p></div><el-button type="primary" @click="$router.push('/activity/create')">＋ 新建活动</el-button></div>
    <div class="surface">
    <el-form :inline="true" :model="query" class="filters" @submit.prevent="search" @keyup.enter="search">
      <el-form-item label="名称">
        <el-input v-model="query.keyword" placeholder="关键字" clearable />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.typeCode" clearable placeholder="全部">
          <el-option v-for="t in types" :key="t.typeCode" :label="t.typeName" :value="t.typeCode" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="活动时间">
        <el-date-picker v-model="range" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
                        start-placeholder="开始" end-placeholder="结束" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading">
      <el-table-column label="活动" min-width="230"><template #default="{row}"><el-button link type="primary" class="activity-title" @click="$router.push(`/activity/${row.activityId}`)">{{ row.title }}</el-button><div class="activity-location">{{ row.location }}</div></template></el-table-column>
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ typeName(row.typeCode) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="170"><template #default="{row}">{{ formatTime(row.startTime) }}</template></el-table-column>
      <el-table-column label="发布时间" width="170"><template #default="{row}">{{ row.publishTime ? formatTime(row.publishTime) : '未发布' }}</template></el-table-column>
      <el-table-column label="报名" width="110">
        <template #default="{ row }">{{ row.signedCount }} / {{ row.quota }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" :fixed="compact ? false : 'right'">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/activity/${row.activityId}`)">详情</el-button>
          <el-button link type="primary" @click="$router.push(`/activity/${row.activityId}/edit`)">编辑</el-button>
          <el-button v-if="row.status === 'DRAFT' && row.approvalStatus !== 'PENDING' && (!row.reviewRequired || row.approvalStatus === 'APPROVED')" link type="success" @click="publish(row)">发布</el-button>
          <el-button link type="danger" @click="remove(row)">删除活动</el-button>
        </template>
      </el-table-column>
    </el-table>
    <p class="list-note">共 {{ total }} 场活动 · 删除后停止报名与签到，历史报名和签到记录保留</p>

    <el-pagination class="pager" background layout="total, prev, pager, next"
                   :total="total" :current-page="query.current" :page-size="query.size"
                   @current-change="(p) => { query.current = p; load() }" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { formatTime } from '../../utils/display'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activityApi, dictApi } from '../../api'

const rows = ref([])
const types = ref([])
const total = ref(0)
const loading = ref(false)
const range = ref(null)
const query = reactive({ current: 1, size: 10, keyword: '', typeCode: '', status: '' })
const media = window.matchMedia('(max-width: 700px)')
const compact = ref(media.matches)
const resize = () => { compact.value = media.matches }
media.addEventListener('change', resize)
onUnmounted(() => { media.removeEventListener('change', resize); requestId++ })
let requestId = 0
const search = () => { query.current = 1; load() }

const statusOptions = [
  { value: 'DRAFT', label: '未发布' },
  { value: 'OPEN', label: '报名中' },
  { value: 'CLOSED', label: '已截止' },
  { value: 'ONGOING', label: '进行中' },
  { value: 'FINISHED', label: '已结束' },
  { value: 'CANCELED', label: '已取消' }
]
const statusLabel = (s) => (statusOptions.find(o => o.value === s) || {}).label || s
const statusTag = (s) => ({ DRAFT: 'info', OPEN: 'success', CLOSED: 'warning', ONGOING: 'primary', FINISHED: '', CANCELED: 'danger' }[s] || 'info')
const typeName = (code) => (types.value.find(t => t.typeCode === code) || {}).typeName || code

const load = async () => {
  const id = ++requestId
  loading.value = true
  try {
    const params = { ...query }
    if (range.value) { params.from = range.value[0]; params.to = range.value[1] }
    const page = await activityApi.page(params)
    if (id !== requestId) return
    rows.value = page.records
    total.value = page.total
  } catch { /* 请求层显示错误。 */ } finally { if (id === requestId) loading.value = false }
}
const reset = () => { Object.assign(query, { current: 1, keyword: '', typeCode: '', status: '' }); range.value = null; load() }
const publish = async (row) => { await activityApi.publish(row.activityId); ElMessage.success('已发布'); load() }
const remove = async (row) => {
  try {
  await ElMessageBox.confirm(`确认删除活动「${row.title}」？删除后停止报名与签到，历史报名和签到记录将保留。`, '删除活动', { type: 'warning' })
  await activityApi.remove(row.activityId)
  ElMessage.success('活动已删除')
  if (rows.value.length === 1 && query.current > 1) query.current--
  load()
  } catch { /* 取消对话框或请求失败均保留当前列表。 */ }
}

onMounted(async () => {
  types.value = await dictApi.activityTypes()
  load()
})
</script>

<style scoped>
.filters { margin-bottom: 16px; display: flex; flex-wrap: wrap; gap: 0 12px; }
.filters :deep(.el-form-item) { margin-right: 0; }
.filters :deep(.el-select) { width: 140px; }
.filters :deep(.el-input) { width: 180px; }
.list-note { color: #8a9ba2; font-size: 12px; margin: 18px 0 0; }
.activity-title { font-weight: 600; max-width: 100%; white-space: normal; text-align: left; height: auto; }
.activity-location { color: #8a9ba2; font-size: 12px; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pager { margin-top: 12px; justify-content: flex-end; }
@media (max-width: 700px) { .filters :deep(.el-form-item) { width: 100%; } .filters :deep(.el-form-item__content) { min-width: 0; } .filters :deep(.el-select), .filters :deep(.el-input), .filters :deep(.el-date-editor) { width: 100%; } .pager { justify-content: flex-start; } }
</style>
