<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="logo">高校学生活动全流程管理系统</div>
      <div class="right">
        <span class="user">{{ profile.realName || profile.username }}</span>
        <el-button link type="primary" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px">
        <el-menu :default-active="route.path" router>
          <el-menu-item index="/activity">活动管理</el-menu-item>
          <el-menu-item index="/statistics">数据统计</el-menu-item>
          <el-menu-item v-if="profile.roleCode === 'SUPER'" index="/system/accounts">账号管理</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '../api'

const route = useRoute()
const router = useRouter()
const profile = ref({})

onMounted(async () => {
  profile.value = await authApi.current()
})

const logout = async () => {
  await authApi.logout()
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }
.header { display: flex; align-items: center; justify-content: space-between; background: #1f6feb; color: #fff; }
.logo { font-size: 18px; font-weight: 600; }
.right { display: flex; align-items: center; gap: 12px; }
.user { font-size: 14px; }
</style>
