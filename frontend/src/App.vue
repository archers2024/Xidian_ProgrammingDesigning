<template>
  <el-config-provider :locale="zhCn"><router-view :key="$route.path.startsWith('/m/') ? $route.fullPath : 'admin'" /></el-config-provider>
</template>

<script setup>
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>
