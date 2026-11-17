<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="logo"><span class="brand-mark">活</span><span>校园协同平台<span class="brand-caption">CAMPUS · WORKSPACE</span></span></div>
      <div class="right">
        <span class="avatar">{{ (profile.realName || profile.username || '管').slice(0, 1) }}</span>
        <span class="user">{{ profile.realName || profile.username }}<small>{{ profile.roleCode === 'SUPER' ? '超级管理员' : '活动管理员' }}</small></span>
        <el-button link type="primary" @click="changePassword">修改密码</el-button>
        <el-button link type="primary" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="224px" class="sidebar">
        <p class="nav-label">工作空间</p>
        <el-menu :default-active="activePath" router>
          <el-menu-item v-for="item in workspaceNav" :key="item.path" :index="item.path"><AppIcon :name="item.icon"/><span>{{item.label}}</span></el-menu-item>
        </el-menu>
        <template v-if="profile.roleCode === 'SUPER'">
          <p class="nav-label platform-label">平台管理</p>
          <el-menu :default-active="activePath" router>
            <el-menu-item v-for="item in platformNav" :key="item.path" :index="item.path"><AppIcon :name="item.icon"/><span>{{item.label}}</span></el-menu-item>
          </el-menu>
        </template>
        <div class="nav-foot"><span class="scope-dot"></span><div>{{profile.roleCode === 'SUPER' ? '平台管理空间' : '组织管理空间'}}<small>{{profile.roleCode === 'SUPER' ? '管理组织与协作流程' : '管理所属组织的活动'}}</small></div></div>
      </el-aside>
      <el-main class="workspace">
        <div class="breadcrumb">校园协同平台 <span>/</span> {{ route.path.startsWith('/dashboard') ? '工作台' : route.path.startsWith('/platform/organizations') ? '组织管理' : route.path.startsWith('/platform/reviews') ? '审批中心' : route.path.startsWith('/platform/audit') ? '操作审计' : route.path.startsWith('/statistics') ? '数据统计' : route.path.startsWith('/system') ? '账号管理' : '活动管理' }}</div>
        <div class="workspace-body"><router-view :key="route.fullPath" /></div>
      </el-main>
    </el-container>
  </el-container>
  <el-dialog v-model="passwordVisible" title="修改密码" width="440px" :close-on-click-modal="false" :show-close="!profile.mustChangePassword" :close-on-press-escape="!profile.mustChangePassword">
    <p class="password-tip">使用至少 8 位、包含字母与数字的新密码。修改后需重新登录。</p>
    <el-form label-position="top" @submit.prevent="savePassword">
      <el-form-item label="原密码"><el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
      <el-form-item label="新密码"><el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
      <el-form-item label="确认新密码"><el-input v-model="passwordForm.confirm" type="password" show-password autocomplete="new-password" @keyup.enter="savePassword" /></el-form-item>
    </el-form>
    <template #footer><el-button v-if="!profile.mustChangePassword" @click="passwordVisible = false">取消</el-button><el-button type="primary" :loading="passwordSaving" @click="savePassword">保存新密码</el-button></template>
  </el-dialog>
</template>

<script setup>
import AppIcon from '../components/AppIcon.vue'
import {computed} from 'vue'
const workspaceNav=[{path:'/dashboard',label:'工作台',icon:'workspace'},{path:'/activity',label:'活动管理',icon:'activity'},{path:'/platform/reviews',label:'审批中心',icon:'review'},{path:'/statistics',label:'数据统计',icon:'chart'}]
const platformNav=[{path:'/platform/organizations',label:'组织管理',icon:'organization'},{path:'/system/accounts',label:'账号管理',icon:'team'},{path:'/platform/audit',label:'操作审计',icon:'audit'}]
const activePath=computed(()=>route.path.startsWith('/activity')?'/activity':route.path)

import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '../api'
import {clearSession} from '../api/session'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const profile = ref({})
const passwordVisible = ref(false)
const passwordSaving = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const changePassword = () => { Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirm: '' }); passwordVisible.value = true }
const savePassword = async () => {
  if (passwordSaving.value) return
  if (!passwordForm.oldPassword || passwordForm.newPassword.length < 8 || !/[A-Za-z]/.test(passwordForm.newPassword) || !/\d/.test(passwordForm.newPassword)) { ElMessage.warning('请填写原密码，新密码至少 8 位并包含字母和数字'); return }
  if (passwordForm.newPassword !== passwordForm.confirm) { ElMessage.warning('两次输入的新密码不一致'); return }
  passwordSaving.value = true
  try {
    await authApi.changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    passwordVisible.value = false
    clearSession(); ElMessage.success('密码已修改，请重新登录'); router.push('/login')
  } catch { /* 保留输入，用户可检查后重试。 */ } finally { passwordSaving.value = false }
}

onMounted(async () => {
  profile.value = await authApi.current()
  if (profile.value.mustChangePassword) { ElMessage.warning('当前为临时密码，请先修改密码'); await changePassword() }
})

const logout = async () => {
  await authApi.logout()
  clearSession()
  router.push('/login')
}
</script>

<style scoped>
.layout{min-height:100dvh}.header{height:76px;padding:0 32px;display:flex;align-items:center;justify-content:space-between;background:#fff;border-bottom:1px solid var(--workspace-border)}
.logo{display:flex;align-items:center;gap:12px;font-size:18px;font-weight:650;color:#183b37;line-height:1.45;letter-spacing:-.03em}.brand-mark{display:grid;place-items:center;width:38px;height:38px;background:#176b59;color:white;border-radius:11px;font-size:21px}.brand-caption{display:block;font-size:9px;font-weight:500;letter-spacing:.14em;color:#87948f;margin-top:2px}
.right{display:flex;align-items:center;gap:14px}.avatar{width:34px;height:34px;background:#eef4ef;border:1px solid #e4ece6;border-radius:50%;display:grid;place-items:center;color:#176b59;font-weight:600}.user{font-size:13px;line-height:1.6;margin-right:12px}.user small{display:block;color:#7d8a84;font-size:11px}.right .el-button{font-size:12px;color:#63766b}
.sidebar{background:#fff;border-right:1px solid var(--workspace-border);padding:30px 16px 20px;display:flex;flex-direction:column}.nav-label{color:#8d9992;font-size:11px;padding:0 16px;margin:0 0 12px;letter-spacing:.08em}.platform-label{margin-top:32px}.sidebar :deep(.el-menu){border:0;background:transparent}.sidebar :deep(.el-menu-item){border-radius:8px;margin:4px 0;height:46px;color:#68776f;gap:12px;padding:0 16px!important;font-size:13px}.sidebar :deep(.el-menu-item:hover){background:#f5f7f4;color:#254b3e}.sidebar :deep(.el-menu-item.is-active){color:#176b59;background:#ecf4ee;font-weight:600}.sidebar svg{flex-shrink:0}.nav-foot{margin-top:auto;padding:36px 12px 4px;display:flex;align-items:flex-start;gap:10px;font-size:12px;color:#5c7062}.nav-foot small{display:block;font-size:10px;color:#939e96;margin-top:4px}.scope-dot{width:6px;height:6px;background:#609678;border-radius:50%;margin-top:7px;flex-shrink:0}
.workspace{min-width:0;padding:28px 42px 56px}.workspace-body,.breadcrumb{max-width:1400px;margin-left:auto;margin-right:auto}.breadcrumb{color:#8b9790;font-size:11px;margin-bottom:32px}.breadcrumb span{margin:0 12px;color:#c0cbc2}.password-tip{font-size:13px;color:#71838d;margin-bottom:22px}
@media(min-width:1800px){.workspace{padding-left:64px;padding-right:64px}}@media(max-width:1100px){.sidebar{width:190px;padding:26px 12px}.workspace{padding:24px}.header{padding:0 24px}}
@media(max-width:700px){.header{padding:16px;height:auto;gap:14px;flex-wrap:wrap}.logo{font-size:16px}.right{gap:10px}.avatar,.user small,.nav-label,.nav-foot{display:none}.layout>.el-container{flex-direction:column}.sidebar{width:100%;padding:6px 10px;border-right:0;border-bottom:1px solid var(--workspace-border);display:flex;flex-direction:row;overflow:auto}.sidebar :deep(.el-menu){display:flex;overflow:visible;flex-shrink:0}.sidebar :deep(.el-menu-item){padding:0 14px!important;gap:7px;height:42px;white-space:nowrap}.sidebar svg{width:16px}.workspace{padding:22px 16px 40px}.breadcrumb{margin-bottom:22px}.user{margin-right:0}}
</style>
