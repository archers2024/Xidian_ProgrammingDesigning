<template>
  <!-- UI-01 管理员登录页（A岗） -->
  <div class="login-page">
    <section class="login-story">
      <div class="story-brand"><span>活</span>校园协同平台</div>
      <div class="story-content"><h1>校园活动<br>有序发生。</h1><p>让活动组织更从容。</p></div>
      <p class="story-footer">校园组织与活动协同平台</p>
    </section>
    <el-card class="login-card">
      <p class="eyebrow">WELCOME BACK</p><h2>欢迎回来</h2><p class="login-sub">登录账号，开始管理校园活动。</p>
      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="submit">登录</el-button>
      </el-form>
      <p class="tip">请使用管理员分配的账号。忘记密码请联系超级管理员。</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {establish} from '../../api/session'
import { ElMessage } from 'element-plus'
import { authApi } from '../../api'

const router = useRouter()
const route = useRoute()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

const submit = async () => {
  if (loading.value) return
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const data = await authApi.login(form)
    establish(data.profile)
    const redirect=typeof route.query.redirect==='string'&&route.query.redirect.startsWith('/')&&!route.query.redirect.startsWith('//')?route.query.redirect:'/dashboard'
    router.push(redirect)
  } catch { /* 请求层统一显示错误，保留输入便于重试。 */ } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100dvh; display: grid; grid-template-columns: 1.1fr 1fr; background: #fff; }
.login-story { background: #214e3f; color: #fff; padding: 48px 12%; display: flex; flex-direction: column; min-height: 100dvh; }
.story-brand { display: flex; align-items: center; gap: 12px; font-size: 18px; font-weight: 600; }
.story-brand span { display: grid; place-items: center; width: 38px; height: 38px; border: 1px solid #628370; border-radius: 10px; }
.story-content { margin: auto 0; padding: 64px 0; }
.story-content .eyebrow { color: #9bc9bd; }
h1 { font-size: clamp(38px, 4vw, 62px); font-weight: 500; line-height: 1.3; letter-spacing: -.04em; margin: 22px 0; }
.story-content > p:not(.eyebrow) { color: #b8cdbd; line-height: 1.9; font-size: 14px; }
.story-steps { display: flex; gap: 28px; border-top: 1px solid #3d5c5e; padding-top: 24px; margin-top: 40px; color: #c5dcd6; font-size: 13px; }
.story-footer { color: #99b49f; font-size: 11px; margin: 0; }
.login-card { width: min(420px, calc(100% - 48px)); align-self: center; justify-self: center; border: 0; padding: 20px; }
h2 { font-size: 30px; margin: 0 0 8px; letter-spacing: -.04em; }
.login-sub { color: #819198; margin-bottom: 34px; font-size: 14px; }
.submit { width: 100%; height: 44px; margin-top: 12px; }
.tip { margin-top: 24px; font-size: 12px; color: #8b9a9f; line-height: 1.8; }
@media (max-width: 720px) { .login-page { display: block; background: #f3f6f6; padding-bottom: 32px; } .login-story { min-height: auto; padding: 26px; } .story-content { padding: 30px 0 0; } h1 { font-size: 34px; margin: 10px 0 20px; } .story-content > p:not(.eyebrow), .story-steps, .story-footer { display: none; } .login-card { position: relative; margin: -8px auto 0; padding: 12px; border-radius: 14px; background: #fff; } }
</style>
