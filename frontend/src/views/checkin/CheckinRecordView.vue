<template>
  <!-- UI-06 签到记录页（D岗） -->
  <div>
    <div class="page-heading"><div><p class="eyebrow">ATTENDANCE RECORDS</p><h2>签到记录</h2><p>查看到场情况，导出活动签到名单。</p></div></div>
    <div class="toolbar">
      <el-button type="primary" @click="exportExcel">导出 Excel</el-button>
      <el-button @click="$router.back()">返回</el-button>
      <span class="count">共 {{ total }} 人签到</span>
    </div>
    <p v-if="error" role="alert">{{error}} <el-button @click="load">重试</el-button></p><el-table :data="list" border v-loading="loading">
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
    </el-table><el-pagination layout="total,prev,pager,next" :total="total" :page-size="query.size" :current-page="query.current" @current-change="page"/>
  </div>
</template>

<script setup>
import {onMounted} from 'vue'
import {useRoute} from 'vue-router'
import {checkinApi} from '../../api'
import {usePagedResource} from '../../composables/usePagedResource'
import {download} from '../../utils/download'
const route=useRoute()
const {rows:list,total,loading,error,query,load,page}=usePagedResource(params=>checkinApi.page(route.params.id,params))
const exportExcel=()=>download(checkinApi.exportUrl(route.params.id),'活动签到名单.xlsx')
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.count { color: #606266; font-size: 13px; }
</style>
