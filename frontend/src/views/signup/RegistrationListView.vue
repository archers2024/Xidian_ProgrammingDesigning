<template>
  <!-- UI-05 报名名单页（C岗）：签到列只读引用 D 岗数据，导出默认脱敏 -->
  <div>
    <div class="page-heading"><div><p class="eyebrow">REGISTRATION RECORDS</p><h2>报名名单</h2><p>查看参与者资料与签到状态，手机号默认脱敏。</p></div></div>
    <div class="toolbar">
      <el-checkbox v-model="maskPhone" :disabled="!canViewPhone" @change="load">手机号脱敏</el-checkbox>
      <el-button type="primary" @click="exportExcel">导出 Excel</el-button>
      <el-button @click="$router.back()">返回</el-button>
    </div>
    <p v-if="error" role="alert">{{error}} <el-button @click="load">重试</el-button></p><el-table :data="list" border v-loading="loading">
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
    </el-table><el-pagination layout="total,prev,pager,next" :total="total" :page-size="query.size" :current-page="query.current" @current-change="page"/>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import {usePagedResource} from '../../composables/usePagedResource'
import {download} from '../../utils/download'
import { useRoute } from 'vue-router'
import { signupApi, authApi } from '../../api'

const route = useRoute()
const {rows:list,total,loading,error,query,load,page}=usePagedResource(params=>signupApi.page(route.params.id,{...params,maskPhone:maskPhone.value}))
const maskPhone = ref(true)
const canViewPhone = ref(false)

const exportExcel=()=>download(signupApi.exportUrl(route.params.id, maskPhone.value),'活动名单.xlsx')
onMounted(async () => { canViewPhone.value = (await authApi.current()).roleCode === 'SUPER'; await load() })
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
</style>
