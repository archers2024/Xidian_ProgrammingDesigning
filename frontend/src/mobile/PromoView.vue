<template>
  <!-- UI-08 宣传海报页（C岗）：含活动信息与报名二维码 -->
  <div class="promo">
    <img v-if="activity.coverUrl" :src="activity.coverUrl" class="cover" />
    <div class="body">
      <div v-if="loadError" class="closed">{{ loadError }}</div>
      <template v-else>
      <h1>{{ activity.title }}</h1>
      <div class="meta">
        <span class="tag">{{ typeName }}</span>
        <span class="tag">{{ statusLabel }}</span>
      </div>
      <p class="row">时间：{{ activity.startTime }} ~ {{ activity.endTime }}</p>
      <p class="row">地点：{{ activity.location }}</p>
      <p class="row">报名截止：{{ activity.signupDeadline }}</p>
      <p class="row">名额：{{ activity.signedCount }} / {{ activity.quota }}</p>
      <p class="intro">{{ activity.introduction }}</p>

      <div v-if="signupOpen" class="qr-box">
        <img :src="qrUrl" class="qr" />
        <p class="hint">扫码或点击下方按钮报名</p>
      </div>
      <div v-else class="closed">{{ signupTip }}</div>

      <a v-if="activity.liveUrl" :href="activity.liveUrl" class="live">直播入口</a>
      <ul v-if="attachments.length" class="atts">
        <li v-for="a in attachments" :key="a.fileUrl">
          <a :href="a.fileUrl" target="_blank">{{ a.fileName }}</a>
        </li>
      </ul>

      <button class="btn" :disabled="!signupOpen" @click="goSignup">
        {{ signupOpen ? '立即报名' : '当前不可报名' }}
      </button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { dictApi, signupApi } from '../api'

const route = useRoute()
const router = useRouter()
const activity = ref({})
const attachments = ref([])
const signupOpen = ref(false)
const signupTip = ref('')
const qrUrl = ref('')
const loadError = ref('')

const types = ref([])
const statusMap = { DRAFT: '未发布', OPEN: '报名中', CLOSED: '已截止', ONGOING: '进行中', FINISHED: '已结束', CANCELED: '已取消' }
const statusLabel = computed(() => statusMap[activity.value.status] || '')
const typeName = computed(() => (types.value.find(t => t.typeCode === activity.value.typeCode) || {}).typeName || '')

const goSignup = () => router.push(`/m/signup/${route.params.guid}`)

onMounted(async () => {
  try {
    const [data, ts] = await Promise.all([signupApi.promo(route.params.guid), dictApi.activityTypes()])
    types.value = ts
    activity.value = data.activity
    attachments.value = data.attachments || []
    signupOpen.value = data.signupOpen
    signupTip.value = data.signupTip
    qrUrl.value = await QRCode.toDataURL(data.signupUrl, { width: 200 })
  } catch (e) {
    // 草稿与已取消活动按「链接已失效」返回，这里给出可读提示而不是白屏
    loadError.value = e.message || '活动不存在或链接已失效'
  }
})
</script>

<style scoped>
.promo { max-width: 430px; margin: 0 auto; background: #fff; min-height: 100vh; }
.cover { width: 100%; height: 200px; object-fit: cover; }
.body { padding: 16px; }
h1 { font-size: 20px; margin-bottom: 10px; }
.meta { margin-bottom: 10px; }
.tag { display: inline-block; background: #ecf5ff; color: #409eff; border-radius: 4px; padding: 2px 8px; font-size: 12px; margin-right: 8px; }
.row { color: #606266; font-size: 14px; margin: 4px 0; }
.intro { margin-top: 12px; color: #303133; line-height: 1.7; }
.qr-box { text-align: center; margin: 20px 0; }
.qr { width: 200px; }
.hint { color: #909399; font-size: 13px; }
.closed { margin: 20px 0; padding: 12px; background: #f4f4f5; color: #909399; text-align: center; border-radius: 6px; }
.live { color: #409eff; }
.atts { margin: 12px 0; font-size: 14px; }
.btn { width: 100%; height: 46px; border: none; border-radius: 23px; background: #409eff; color: #fff; font-size: 16px; }
.btn:disabled { background: #c0c4cc; }
</style>
