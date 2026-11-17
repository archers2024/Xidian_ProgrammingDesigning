<template>
  <!-- UI-09 报名表单页 + UI-10 报名成功页（C岗）：提交后原地切成功态 -->
  <div class="page mobile-shell">
    <p class="eyebrow">EVENT REGISTRATION</p>
    <div v-if="initialLoading" class="page-state">正在加载报名信息…</div>
    <div v-else-if="initialError" class="page-state"><strong>无法打开报名页面</strong><p role="alert">{{ error }}</p><el-button @click="retry">重新加载</el-button></div>
    <div v-else-if="!success" class="form">
      <h2>{{ activity.title }}</h2>
      <p class="sub">{{ formatTime(activity.startTime) }} · {{ activity.location }}</p>
      <section v-if="identity.needed.value"><el-button :disabled="!identity.enabled.value" @click="identity.start">校园身份验证</el-button><p>{{identity.hasProof.value?'已获取身份凭据，提交时将验证身份':identity.message.value}}</p></section>
      <label>姓名<input v-model="form.name" maxlength="20" placeholder="请输入 2 至 20 字姓名" autocomplete="name" /></label>
      <label>手机号<input v-model="form.phone" type="tel" maxlength="11" placeholder="请输入手机号" /></label>
      <label>学院
        <select v-model="form.collegeCode">
          <option disabled value="">请选择所在学院</option>
          <option v-for="c in colleges" :key="c.collegeCode" :value="c.collegeCode">{{ c.collegeName }}</option>
        </select>
      </label>
      <button class="btn" :disabled="loading || !signupOpen" @click="submit">{{ loading ? '提交中...' : signupOpen ? '确认报名' : '当前不可报名' }}</button>
      <p class="err" v-if="error" role="alert">{{ error }}</p>
    </div>

    <div v-else class="success">
      <div class="icon">✓</div>
      <h2>报名成功</h2>
      <p class="row">姓名：{{ result.name }}</p>
      <p class="row">手机号：{{ result.phone }}</p>
      <p class="row">报名时间：{{ result.signupTime }}</p>
      <p class="tip">请准时到场，现场扫码签到即可</p>
      <img :src="qrUrl" class="qr" v-if="qrUrl" />
      <p class="hint">截图保存，签到时出示</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import {useIdentity} from '../composables/useIdentity'
import QRCode from 'qrcode'
import { dictApi, signupApi } from '../api'
import { formatTime } from '../utils/display'

const route = useRoute()
const activity = ref({})
const identity=useIdentity(()=>activity.value.checkinPolicy)
const colleges = ref([])
const form = reactive({ name: '', phone: '', collegeCode: '' })
const loading = ref(false)
const error = ref('')
const success = ref(false)
const result = ref({})
const qrUrl = ref('')
const signupOpen = ref(false)
const initialLoading = ref(true)
const initialError = ref(false)
const retry = () => window.location.reload()

const requestKey=crypto.randomUUID()
const submit = async () => {
  if (loading.value || !signupOpen.value || success.value) return
  error.value = ''
  if (form.name.trim().length < 2 || form.name.trim().length > 20) { error.value = '请输入 2-20 个字符的姓名'; return }
  if (!/^1[3-9]\d{9}$/.test(form.phone)) { error.value = '请输入正确的手机号'; return }
  if (!form.collegeCode) { error.value = '请选择学院'; return }
  loading.value = true
  try {
    result.value = await signupApi.submit({
      guid: route.params.guid,
      name: form.name,
      phone: form.phone,
      collegeCode: form.collegeCode,
      identityProof:sessionStorage.getItem('campusIdentityProof')||undefined,
      channel: 'SCAN'
    },requestKey)
    success.value = true
    qrUrl.value = await QRCode.toDataURL(
      `${location.origin}/m/checkin/${activity.value.activityId}`, { width: 180 })
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
  const [promo, cs] = await Promise.all([
    signupApi.promo(route.params.guid),
    dictApi.colleges()
  ])
  activity.value = promo.activity
  colleges.value = cs
  signupOpen.value = promo.signupOpen && promo.activity.signedCount < promo.activity.quota
  if (!signupOpen.value) { error.value = promo.signupTip || '活动名额已满，感谢关注' }
  } catch (e) { error.value = e.message; initialError.value = true } finally { initialLoading.value = false }
})
</script>

<style scoped>
.page { max-width: 480px; margin: 0 auto; padding: 32px 24px; min-height: 100dvh; background: #fff; }
h2 { font-size: 18px; margin-bottom: 6px; }
.sub { color: #909399; font-size: 13px; margin-bottom: 18px; }
label { display: block; margin-bottom: 14px; font-size: 14px; color: #606266; }
input, select { width: 100%; height: 44px; margin-top: 6px; padding: 0 12px; border: 1px solid #dcdfe6; border-radius: 6px; font-size: 15px; box-sizing: border-box; }
.btn { width: 100%; height: 48px; border: none; border-radius: 10px; background: #137c70; color: #fff; font-size: 16px; }
.btn:disabled { background: #a0cfff; }
.err { color: #f56c6c; font-size: 13px; margin-top: 10px; }
.success { text-align: center; padding-top: 30px; }
.icon { width: 64px; height: 64px; line-height: 64px; border-radius: 50%; background: #67c23a; color: #fff; font-size: 34px; margin: 0 auto 16px; }
.row { color: #606266; margin: 4px 0; }
.tip { margin-top: 16px; color: #909399; font-size: 13px; }
.qr { width: 180px; margin-top: 12px; }
.hint { color: #c0c4cc; font-size: 12px; }
</style>
