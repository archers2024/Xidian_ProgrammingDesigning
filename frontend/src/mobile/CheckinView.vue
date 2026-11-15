<template>
  <!-- UI-11 签到页（D岗）：扫码进入，支持动态码 + 地理围栏 -->
  <div class="page">
    <h2>{{ title || '活动签到' }}</h2>
    <p class="sub">请输入报名时使用的手机号</p>

    <input v-model="phone" type="tel" maxlength="11" placeholder="手机号" class="phone" />
    <button class="btn" :disabled="loading" @click="submit">{{ loading ? '签到中...' : '确认签到' }}</button>
    <p class="err" v-if="error">{{ error }}</p>

    <div v-if="success" class="ok">
      <div class="icon">✓</div>
      <p>签到成功</p>
      <p class="row">{{ okInfo.name }} · {{ okInfo.checkinTime }}</p>
      <p class="row">方式：{{ okInfo.method === 'ACCURATE' ? '准确签到' : '基本签到' }}</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { checkinApi } from '../api'

const route = useRoute()
const activityId = route.params.activityId
const phone = ref('')
const loading = ref(false)
const error = ref('')
const success = ref(false)
const okInfo = ref({})
const title = ref('')
// 二维码载荷：动态码（30 秒刷新一次，容忍 1 个时间片偏差）
const code = new URLSearchParams(location.search).get('code') || ''

const submit = async () => {
  error.value = ''
  if (!/^1[3-9]\d{9}$/.test(phone.value)) { error.value = '请输入正确的手机号'; return }
  loading.value = true
  try {
    let lngLat = ''
    if (code) {
      // 准确签到需要定位授权；用户拒绝时给出可执行提示
      const pos = await getLocation()
      if (!pos) { error.value = '需开启定位权限才能完成准确签到'; loading.value = false; return }
      lngLat = pos
    }
    okInfo.value = await checkinApi.submit({
      activityId,
      phone: phone.value,
      code,
      lngLat,
      device: navigator.userAgent
    })
    success.value = true
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

const getLocation = () => new Promise((resolve) => {
  if (!navigator.geolocation) { resolve(null); return }
  navigator.geolocation.getCurrentPosition(
    (p) => resolve(`${p.coords.longitude},${p.coords.latitude}`),
    () => resolve(null),
    { timeout: 8000 }
  )
})

onMounted(async () => {
  // 签到页无登录态：仅预热动态码接口，未开启签到时由提交接口给出明确提示
  try {
    await checkinApi.board(activityId)
  } catch (e) {
    // 忽略，提交阶段统一提示
  }
})
</script>

<style scoped>
.page { max-width: 430px; margin: 0 auto; padding: 28px 20px; text-align: center; }
h2 { font-size: 18px; }
.sub { color: #909399; font-size: 13px; margin: 8px 0 20px; }
.phone { width: 100%; height: 46px; padding: 0 14px; border: 1px solid #dcdfe6; border-radius: 8px; font-size: 16px; box-sizing: border-box; text-align: center; letter-spacing: 1px; }
.btn { width: 100%; height: 46px; margin-top: 16px; border: none; border-radius: 23px; background: #409eff; color: #fff; font-size: 16px; }
.btn:disabled { background: #a0cfff; }
.err { color: #f56c6c; font-size: 13px; margin-top: 12px; }
.ok { margin-top: 28px; }
.icon { width: 60px; height: 60px; line-height: 60px; border-radius: 50%; background: #67c23a; color: #fff; font-size: 32px; margin: 0 auto 12px; }
.row { color: #606266; font-size: 14px; margin: 4px 0; }
</style>
