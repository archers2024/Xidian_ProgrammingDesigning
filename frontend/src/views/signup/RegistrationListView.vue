<template>
  <!-- UI-05 报名名单页（C岗）：签到列只读引用 D 岗数据，导出默认脱敏 -->
  <div>
    <div class="toolbar">
      <el-checkbox v-model="maskPhone" @change="load">手机号脱敏</el-checkbox>
      <el-button type="primary" @click="exportExcel">导出 Excel</el-button>
      <el-button @click="$router.back()">返回</el-button>
    </div>
    <el-table :data="list" border v-loading="loading">
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column label="手机号" width="150">
        <template #default="{ row }">{{ row.phoneMasked }}</template>
      </el-table-column>
      <el-table-column prop="collegeName" label="学院" width="180" />
      <el-table-column prop="signupTime" label="报名时间" width="180" />
      <el-table-column label="签到状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.checkinTime ? 'success' : 'info'">{{ row.checkinTime ? '已签到' : '未签到' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="checkinTime" label="签到时间" width="180" />
      <el-table-column label="签到方式" width="120">
        <template #default="{ row }">
          {{ row.checkinMethod === 'ACCURATE' ? '准确签到' : (row.checkinMethod === 'BASIC' ? '基本签到' : '-') }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { signupApi } from '../../api'

const route = useRoute()
const list = ref([])
const loading = ref(false)
const maskPhone = ref(true)

const load = async () => {
  loading.value = true
  try {
    list.value = await signupApi.list(route.params.id, maskPhone.value)
  } finally { loading.value = false }
}
const exportExcel = () => {
  window.open(signupApi.exportUrl(route.params.id, maskPhone.value))
}
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
</style>
