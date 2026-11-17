<template><el-dialog v-model="visible" title="提交时的活动内容" width="760px"><div v-if="loading" class="page-state">正在加载申请内容</div><p v-else-if="error" role="alert">{{error}}</p><template v-else-if="data"><el-descriptions :title="data.content.activity.title" :column="1" border><el-descriptions-item label="申请版本">{{data.version}}</el-descriptions-item><el-descriptions-item label="地点">{{data.content.activity.location}}</el-descriptions-item><el-descriptions-item label="时间">{{formatTime(data.content.activity.startTime)}} 至 {{formatTime(data.content.activity.endTime)}}</el-descriptions-item><el-descriptions-item label="报名截止">{{formatTime(data.content.activity.signupDeadline)}}</el-descriptions-item><el-descriptions-item label="名额">{{data.content.activity.quota}}</el-descriptions-item><el-descriptions-item label="活动简介">{{data.content.activity.introduction||'—'}}</el-descriptions-item><el-descriptions-item label="附件"><p v-for="item in data.content.attachments" :key="item.attId">{{item.fileName}}</p><span v-if="!data.content.attachments.length">无附件</span></el-descriptions-item></el-descriptions></template></el-dialog></template>
<script setup>
import {ref} from 'vue'
import {reviewApi} from '../api'
import {formatTime} from '../utils/display'
const visible=ref(false),loading=ref(false),error=ref(''),data=ref(null)
async function open(id){visible.value=true;loading.value=true;error.value='';data.value=null;try{data.value=await reviewApi.snapshot(id)}catch(e){error.value=e.message}finally{loading.value=false}}
defineExpose({open})
</script>
