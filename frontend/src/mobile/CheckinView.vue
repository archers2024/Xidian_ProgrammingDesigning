<template>
  <!-- UI-11 签到页（D岗）：扫码进入，支持动态码 + 地理围栏 -->
  <div class="page mobile-shell">
    <p class="eyebrow">EVENT CHECK IN</p>
    <h2>{{ title || '活动签到' }}</h2>
    <section v-if="identity.needed.value"><el-button :disabled="!identity.enabled.value" @click="identity.start">校园身份验证</el-button><p>{{identity.hasProof.value?'已获取身份凭据，提交时将验证身份':identity.message.value}}</p></section>
    <p class="sub">{{ success ? '签到已完成，祝你活动愉快' : '请输入报名时使用的手机号' }}</p>

    <input v-if="!success" v-model="phone" type="tel" maxlength="11" placeholder="手机号" aria-label="报名手机号" autocomplete="tel" class="phone" @keyup.enter="submit" />
    <button v-if="!success" class="btn" :disabled="loading" @click="submit">{{ loading ? '签到中...' : '确认签到' }}</button>
    <p class="err" v-if="error" role="alert">{{ error }}</p>

    <div v-if="success" class="ok">
      <div class="icon">✓</div>
      <p>签到成功</p>
      <p class="row">{{ okInfo.name }} · {{ formatTime(okInfo.checkinTime) }}</p>
      <p class="row">方式：{{ okInfo.method === 'ACCURATE' ? '准确签到' : '基本签到' }}</p>
    </div>
  </div>
</template>

<script setup>
import {ref,onMounted} from 'vue'
import { useRoute } from 'vue-router'
import { checkinApi } from '../api'
import {useIdentity} from '../composables/useIdentity'
import {formatTime} from '../utils/display'

const route = useRoute()
const activityId = route.params.activityId
const phone = ref('')
const loading = ref(false)
const error = ref('')
const success = ref(false)
const okInfo = ref({})
const title = ref('')
const policy=ref('BASIC'),identity=useIdentity(()=>policy.value)
// 二维码载荷：动态码（30 秒刷新一次，容忍 1 个时间片偏差）
onMounted(async()=>{try{const info=await checkinApi.info(activityId);title.value=info.title;policy.value=info.checkinPolicy;if(['IDENTITY_REQUIRED','STRICT'].includes(info.checkinPolicy)&&!sessionStorage.getItem('campusIdentityProof'))error.value='该活动需要校园身份验证，请从校园认证入口进入'}catch(e){error.value=e.message}})
const code = new URLSearchParams(location.search).get('code') || ''

const submit = async () => {
  if (loading.value || success.value) return
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
      identityProof:sessionStorage.getItem('campusIdentityProof')||undefined,
      device: getDeviceId()
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

const getDeviceId = () => {
  let id = localStorage.getItem('checkinDevice')
  if (!id) { id = Array.from(crypto.getRandomValues(new Uint8Array(16)), n => n.toString(16).padStart(2, '0')).join(''); localStorage.setItem('checkinDevice', id) }
  return id
}
</script>

<style scoped>
.page { max-width: 480px; margin: 0 auto; padding: 54px 24px; text-align: center; }
h2 { font-size: 18px; }
.sub { color: #909399; font-size: 13px; margin: 8px 0 20px; }
.phone { width: 100%; height: 46px; padding: 0 14px; border: 1px solid #dcdfe6; border-radius: 8px; font-size: 16px; box-sizing: border-box; text-align: center; letter-spacing: 1px; }
.btn { width: 100%; height: 48px; margin-top: 16px; border: none; border-radius: 10px; background: #137c70; color: #fff; font-size: 16px; }
.btn:disabled { background: #a0cfff; }
.err { color: #f56c6c; font-size: 13px; margin-top: 12px; }
.ok { margin-top: 28px; }
.icon { width: 60px; height: 60px; line-height: 60px; border-radius: 50%; background: #67c23a; color: #fff; font-size: 32px; margin: 0 auto 12px; }
.row { color: #606266; font-size: 14px; margin: 4px 0; }
</style>
