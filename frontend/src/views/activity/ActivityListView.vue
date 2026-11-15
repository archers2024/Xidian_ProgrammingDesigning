<template>
  <!-- UI-02 活动列表页（B岗）：四维组合筛选 + 发布/编辑/删除 -->
  <div>
    <el-form :inline="true" :model="query" class="filters">
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
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" @click="$router.push('/activity/create')">新建活动</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" border v-loading="loading">
      <el-table-column prop="title" label="活动名称" min-width="180" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ typeName(row.typeCode) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="170" />
      <el-table-column prop="location" label="地点" width="140" />
      <el-table-column label="报名" width="110">
        <template #default="{ row }">{{ row.signedCount }} / {{ row.quota }}</template>
      </el-table-column>
      <el-table-column prop="publishTime" label="发布时间" width="170" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/activity/${row.activityId}`)">详情</el-button>
          <el-button link type="primary" @click="$router.push(`/activity/${row.activityId}/edit`)">编辑</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="success" @click="publish(row)">发布</el-button>
          <el-button link type="danger" @click="remove(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next"
                   :total="total" :current-page="query.current" :page-size="query.size"
                   @current-change="(p) => { query.current = p; load() }" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activityApi, dictApi } from '../../api'

const rows = ref([])
const types = ref([])
const total = ref(0)
const loading = ref(false)
const range = ref(null)
const query = reactive({ current: 1, size: 10, keyword: '', typeCode: '', status: '' })

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
  loading.value = true
  try {
    const params = { ...query }
    if (range.value) { params.from = range.value[0]; params.to = range.value[1] }
    const page = await activityApi.page(params)
    rows.value = page.records
    total.value = page.total
  } finally { loading.value = false }
}
const reset = () => { Object.assign(query, { current: 1, keyword: '', typeCode: '', status: '' }); range.value = null; load() }
const publish = async (row) => { await activityApi.publish(row.activityId); ElMessage.success('已发布'); load() }
const remove = async (row) => {
  await ElMessageBox.confirm(`确认取消活动「${row.title}」？已报名人员记录将保留。`, '提示', { type: 'warning' })
  await activityApi.remove(row.activityId)
  ElMessage.success('已取消')
  load()
}

onMounted(async () => {
  types.value = await dictApi.activityTypes()
  load()
})
</script>

<style scoped>
.filters { margin-bottom: 8px; }
.pager { margin-top: 12px; justify-content: flex-end; }
</style>
