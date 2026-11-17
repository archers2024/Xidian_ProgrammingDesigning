<template>
  <!-- FR-12 管理员账号管理（A岗，仅超级管理员可见） -->
  <div>
    <div class="page-heading"><div><p class="eyebrow">ACCESS MANAGEMENT</p><h2>账号管理</h2><p>维护管理员账号与访问权限。</p></div></div>
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增管理员</el-button>
    </div>
    <div class="toolbar"><el-input v-model="query.keyword" placeholder="查找账号或姓名" @keyup.enter="search"/><el-button @click="search">查询</el-button></div><p v-if="error" role="alert">{{error}} <el-button @click="load">重试</el-button></p><el-table :data="list" border v-loading="loading">
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="dept" label="部门" />
      <el-table-column label="所属组织" min-width="160"><template #default="{row}">{{row.orgName || row.orgId}}</template></el-table-column>
      <el-table-column label="角色" min-width="130"><template #default="{row}">{{ row.roleCode === 'SUPER' ? '超级管理员' : '活动管理员' }}</template></el-table-column>
      <el-table-column label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          <el-button link type="warning" @click="resetPwd(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table><el-pagination layout="total,prev,pager,next" :total="total" :page-size="query.size" :current-page="query.current" @current-change="page"/>

    <el-dialog v-model="visible" title="新增管理员" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="所属组织"><el-select v-model="form.orgId" filterable remote :remote-method="loadOrganizations" placeholder="搜索组织名称"><el-option v-for="org in organizations.filter(x=>x.status===1)" :key="org.orgId" :value="org.orgId" :label="org.name"/></el-select></el-form-item>
        <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.dept" /></el-form-item>
        <el-form-item label="初始密码"><el-input v-model="form.passwordHash" type="password" show-password placeholder="不填则使用临时密码，首次登录需修改" autocomplete="new-password" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCode">
            <el-option label="活动管理员" value="ADMIN" />
            <el-option label="超级管理员" value="SUPER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">创建账号</el-button>
      </template>
    </el-dialog>
  </div>
<el-dialog v-model="passwordVisible" title="临时密码，仅本次显示" width="480px" @closed="temporaryPassword=''">
<p>请通过安全方式交给账号使用者，首次登录后必须修改。</p><el-input v-model="temporaryPassword" type="password" show-password readonly/><template #footer><el-button @click="copyPassword">复制密码</el-button><el-button type="primary" @click="passwordVisible=false">已保存</el-button></template></el-dialog>
</template>

<script setup>
import {onMounted,reactive,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {adminApi,organizationApi} from '../../api'
import {usePagedResource} from '../../composables/usePagedResource'
const {rows:list,total,loading,error,query,load,search,page}=usePagedResource(adminApi.page,{keyword:''})
const organizations=ref([]),visible=ref(false),saving=ref(false),passwordVisible=ref(false),temporaryPassword=ref('')
const form=reactive({username:'',realName:'',dept:'',passwordHash:'',roleCode:'ADMIN',orgId:1})
async function loadOrganizations(keyword=''){organizations.value=await organizationApi.available(keyword)}
onMounted(async()=>{await Promise.all([load(),loadOrganizations()])})
function openCreate(){Object.assign(form,{username:'',realName:'',dept:'',passwordHash:'',roleCode:'ADMIN',orgId:1});visible.value=true}
function showPassword(password){temporaryPassword.value=password;passwordVisible.value=true}
async function copyPassword(){try{await navigator.clipboard.writeText(temporaryPassword.value);ElMessage.success('密码已复制')}catch{ElMessage.warning('请手动复制并妥善保管密码')}}
async function submit(){if(saving.value)return;if(!/^[A-Za-z0-9_]{3,50}$/.test(form.username)||!form.realName.trim()){ElMessage.warning('请填写有效账号和姓名');return}saving.value=true;try{const result=await adminApi.create(form);visible.value=false;form.passwordHash='';showPassword(result.temporaryPassword);await load()}catch{}finally{saving.value=false}}
async function toggle(row){try{await adminApi.status(row.adminId,row.status===1?0:1);await load()}catch{}}
async function resetPwd(row){try{const result=await adminApi.resetPwd(row.adminId);showPassword(result.temporaryPassword)}catch{}}
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
