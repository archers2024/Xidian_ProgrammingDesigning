<template>
  <!-- UI-09 报名表单页 + UI-10 报名成功页（C岗）：提交后原地切成功态 -->
  <div class="page">
    <div v-if="!success" class="form">
      <h2>{{ activity.title }}</h2>
      <p class="sub">{{ activity.startTime }} · {{ activity.location }}</p>
      <label>姓名<input v-model="form.name" placeholder="请输入姓名" /></label>
      <label>手机号<input v-model="form.phone" type="tel" maxlength="11" placeholder="请输入手机号" /></label>
      <label>学院
        <select v-model="form.collegeCode">
          <option v-for="c in colleges" :key="c.collegeCode" :value="c.collegeCode">{{ c.collegeName }}</option>
        </select>
      </label>
      <button class="btn" :disabled="loading" @click="submit">{{ loading ? '提交中...' : '提交报名' }}</button>
      <p class="err" v-if="error">{{ error }}</p>
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
import QRCode from 'qrcode'
import { dictApi, signupApi } from '../api'

const route = useRoute()
const activity = ref({})
const colleges = ref([])
const form = reactive({ name: '', phone: '', collegeCode: '' })
const loading = ref(false)
const error = ref('')
const success = ref(false)
const result = ref({})
const qrUrl = ref('')

const submit = async () => {
  error.value = ''
  if (form.name.trim().length < 2) { error.value = '请输入 2-20 个字符的姓名'; return }
  if (!/^1[3-9]\d{9}$/.test(form.phone)) { error.value = '请输入正确的手机号'; return }
  if (!form.collegeCode) { error.value = '请选择学院'; return }
  loading.value = true
  try {
    result.value = await signupApi.submit({
      guid: route.params.guid,
      name: form.name,
      phone: form.phone,
      collegeCode: form.collegeCode,
      channel: 'SCAN'
    })
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
  const [promo, cs] = await Promise.all([
    signupApi.promo(route.params.guid),
    dictApi.colleges()
  ])
  activity.value = promo.activity
  colleges.value = cs
  if (!promo.signupOpen) { error.value = promo.signupTip }
})
</script>

<style scoped>
.page { max-width: 430px; margin: 0 auto; padding: 20px; min-height: 100vh; background: #f7f8fa; }
h2 { font-size: 18px; margin-bottom: 6px; }
.sub { color: #909399; font-size: 13px; margin-bottom: 18px; }
label { display: block; margin-bottom: 14px; font-size: 14px; color: #606266; }
input, select { width: 100%; height: 44px; margin-top: 6px; padding: 0 12px; border: 1px solid #dcdfe6; border-radius: 6px; font-size: 15px; box-sizing: border-box; }
.btn { width: 100%; height: 46px; border: none; border-radius: 23px; background: #409eff; color: #fff; font-size: 16px; }
.btn:disabled { background: #a0cfff; }
.err { color: #f56c6c; font-size: 13px; margin-top: 10px; }
.success { text-align: center; padding-top: 30px; }
.icon { width: 64px; height: 64px; line-height: 64px; border-radius: 50%; background: #67c23a; color: #fff; font-size: 34px; margin: 0 auto 16px; }
.row { color: #606266; margin: 4px 0; }
.tip { margin-top: 16px; color: #909399; font-size: 13px; }
.qr { width: 180px; margin-top: 12px; }
.hint { color: #c0c4cc; font-size: 12px; }
</style>
