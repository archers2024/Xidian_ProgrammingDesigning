<template><div><div class="page-heading"><div><h2>操作审计</h2><p>追溯登录、活动变更、账号与组织管理操作。</p></div></div><div class="surface"><div class="toolbar"><el-input v-model="query.opType" placeholder="操作类型，例如 ACTIVITY_CREATE" clearable style="max-width:320px" @keyup.enter="search"/><el-button type="primary" @click="search">查询</el-button></div><p v-if="error" role="alert">{{error}} <el-button @click="load">重试</el-button></p><el-table :data="rows" v-loading="loading"><el-table-column prop="opType" label="操作类型" width="190"/><el-table-column prop="adminId" label="管理员ID" width="110"/><el-table-column prop="targetId" label="对象ID" width="100"/><el-table-column prop="opDetail" label="操作详情" min-width="240" show-overflow-tooltip/><el-table-column label="操作时间" width="180"><template #default="{row}">{{formatTime(row.opTime)}}</template></el-table-column></el-table><el-pagination style="margin-top:20px" layout="total,prev,pager,next" :total="total" :page-size="query.size" :current-page="query.current" @current-change="page=>{query.current=page;load()}"/></div></div></template>
<script setup>
import {onMounted} from 'vue'
import {usePagedResource} from '../../composables/usePagedResource'
import {organizationApi} from '../../api'
import {formatTime} from '../../utils/display'
const {rows,total,loading,error,query,load,search}=usePagedResource(organizationApi.audit,{opType:''})
onMounted(load)
</script>
