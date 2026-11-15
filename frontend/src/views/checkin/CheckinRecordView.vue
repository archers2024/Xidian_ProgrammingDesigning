<template>
  <!-- UI-06 签到记录页（D岗） -->
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="exportExcel">导出 Excel</el-button>
      <el-button @click="$router.back()">返回</el-button>
      <span class="count">共 {{ list.length }} 人签到</span>
    </div>
    <el-table :data="list" border>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column prop="collegeCode" label="学院" width="160" />
      <el-table-column prop="checkinTime" label="签到时间" width="180" />
      <el-table-column label="签到方式" width="140">
        <template #default="{ row }">
          <el-tag :type="row.method === 'ACCURATE' ? 'success' : 'info'">
            {{ row.method === 'ACCURATE' ? '准确签到' : '基本签到' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { checkinApi } from '../../api'

const route = useRoute()
const list = ref([])
const load = async () => { list.value = await checkinApi.list(route.params.id) }
const exportExcel = () => window.open(checkinApi.exportUrl(route.params.id))
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.count { color: #606266; font-size: 13px; }
</style>
