<template>
  <!-- UI-01 管理员登录页（A岗） -->
  <div class="login-page">
    <el-card class="login-card">
      <h2>管理员登录</h2>
      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
      <p class="tip">默认账号：admin / admin123（首次登录后请修改密码）</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../../api'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

const submit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const data = await authApi.login(form)
    localStorage.setItem('token', data.token)
    router.push('/activity')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f2f5fa; }
.login-card { width: 380px; padding: 12px; }
h2 { text-align: center; margin-bottom: 20px; }
.submit { width: 100%; }
.tip { margin-top: 14px; font-size: 12px; color: #909399; text-align: center; }
</style>
