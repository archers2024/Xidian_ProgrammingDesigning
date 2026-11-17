<template>
<div><div class="page-heading"><div><h2>审批中心</h2><p>审核活动内容，保留每一次提交与处理记录。提交者不能审批自己的申请。</p></div><el-tag>{{total}} 条申请</el-tag></div>
<div class="surface"><div class="toolbar"><el-select v-model="query.decision" placeholder="全部记录" style="width:180px" @change="search"><el-option label="全部记录" value=""/><el-option v-for="(name,key) in labels" :key="key" :label="name" :value="key"/></el-select><el-button @click="load">刷新</el-button></div>
<p v-if="error">{{error}}</p><el-table :data="rows" v-loading="loading"><el-table-column prop="activityTitle" label="活动" min-width="200"/><el-table-column prop="orgName" label="组织" min-width="150"/><el-table-column prop="submitterName" label="提交人" min-width="110"/><el-table-column label="提交时间" min-width="170"><template #default="{row}">{{formatTime(row.submittedAt)}}</template></el-table-column><el-table-column label="状态"><template #default="{row}"><el-tag :type="row.decision==='APPROVED'?'success':row.decision==='REJECTED'?'danger':'info'">{{labels[row.decision]}}</el-tag></template></el-table-column><el-table-column prop="comment" label="审批意见" min-width="180"/><el-table-column label="操作" min-width="150"><template #default="{row}"><el-button link @click="snapshotDialog.open(row.reviewId)">申请内容</el-button><el-button v-if="row.decision==='PENDING'" link @click="$router.push(`/activity/${row.activityId}`)">查看活动</el-button><el-button v-if="row.decision==='PENDING' && profile.roleCode==='SUPER' && row.submittedBy!==profile.adminId" link type="primary" @click="open(row)">审批</el-button></template></el-table-column></el-table>
<el-pagination style="margin-top:20px" layout="total,prev,pager,next" :total="total" :page-size="query.size" :current-page="query.current" @current-change="page=>{query.current=page;load()}"/></div>
<el-dialog v-model="visible" title="处理活动申请" width="480px"><p>{{selected?.activityTitle}}</p><el-form label-position="top"><el-form-item label="处理结果"><el-select v-model="form.decision"><el-option label="通过" value="APPROVED"/><el-option label="驳回" value="REJECTED"/></el-select></el-form-item><el-form-item label="审批意见（驳回必填）"><el-input v-model="form.comment" type="textarea" :rows="4" maxlength="500" show-word-limit/></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">确认处理</el-button></template></el-dialog></div>
<ReviewSnapshotDialog ref="snapshotDialog"/></template>
<script setup>
import {ref,reactive,onMounted} from 'vue'
import {usePagedResource} from '../../composables/usePagedResource'
import {reviewApi,authApi} from '../../api'
import ReviewSnapshotDialog from '../../components/ReviewSnapshotDialog.vue'
import {formatTime} from '../../utils/display'
import {ElMessage} from 'element-plus'
const snapshotDialog=ref(null)
const labels={PENDING:'待审批',APPROVED:'已通过',REJECTED:'已驳回',CANCELED:'已撤销'}
const {rows,total,loading,error,query,load,search}=usePagedResource(reviewApi.queue,{decision:'PENDING'})
const saving=ref(false),visible=ref(false),selected=ref(null),profile=ref({}),form=reactive({decision:'APPROVED',comment:''})
function open(row){selected.value=row;Object.assign(form,{decision:'APPROVED',comment:''});visible.value=true}
async function save(){if(saving.value)return;if(form.decision==='REJECTED'&&!form.comment.trim()){ElMessage.warning('请填写驳回原因');return}saving.value=true;try{await reviewApi.decide(selected.value.reviewId,form);visible.value=false;ElMessage.success('已处理，活动负责人可查看审批结果');await load()}catch{}finally{saving.value=false}}
onMounted(async()=>{profile.value=await authApi.current();await load()})
</script>

