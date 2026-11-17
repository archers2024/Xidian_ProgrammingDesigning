<template>
  <!-- UI-08 宣传海报页（C岗）：含活动信息与报名二维码 -->
  <div v-if="loading || error" class="mobile-shell page-state"><strong>{{ loading ? '正在加载活动' : '活动暂不可用' }}</strong><p>{{ error || '精彩活动，即将呈现' }}</p><el-button v-if="error" @click="load">重试</el-button></div>
  <div v-else class="promo">
    <img v-if="activity.coverUrl" :src="safeUrl(activity.coverUrl)" :alt="activity.title" class="cover" />
    <div v-else class="cover-placeholder"><span>CAMPUS EVENTS</span><strong>校园生活<br>不止课堂</strong></div>
    <div class="body">
      <h1>{{ activity.title }}</h1>
      <div class="meta">
        <span class="tag">{{ typeName }}</span>
        <span class="tag">{{ statusLabel }}</span>
      </div>
      <p class="row">时间：{{ formatTime(activity.startTime) }} 至 {{ formatTime(activity.endTime) }}</p>
      <p class="row">地点：{{ activity.location }}</p>
      <p class="row">报名截止：{{ formatTime(activity.signupDeadline) }}</p>
      <p class="row">名额：{{ activity.signedCount }} / {{ activity.quota }}</p>
      <p class="intro">{{ activity.introduction }}</p>

      <div v-if="signupOpen" class="qr-box">
        <img :src="qrUrl" class="qr" alt="活动报名二维码" />
        <p class="hint">扫码或点击下方按钮报名</p>
      </div>
      <div v-else class="closed">{{ signupTip }}</div>

      <a v-if="safeUrl(activity.liveUrl)" :href="safeUrl(activity.liveUrl)" class="live" target="_blank" rel="noopener noreferrer">进入活动直播 ↗</a>
      <ul v-if="attachments.length" class="atts">
        <li v-for="a in attachments" :key="a.fileUrl">
          <a v-if="safeUrl(a.fileUrl)" :href="safeUrl(a.fileUrl)" target="_blank" rel="noopener noreferrer">{{ a.fileName }}</a>
        </li>
      </ul>

      <button class="btn" :disabled="!signupOpen" @click="goSignup">
        {{ signupOpen ? '立即报名' : '当前不可报名' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { dictApi, signupApi } from '../api'
import { formatTime, safeUrl } from '../utils/display'

const route = useRoute()
const router = useRouter()
const activity = ref({})
const attachments = ref([])
const signupOpen = ref(false)
const signupTip = ref('')
const qrUrl = ref('')
const loading = ref(true)
const error = ref('')

const types = ref([])
const statusMap = { DRAFT: '未发布', OPEN: '报名中', CLOSED: '已截止', ONGOING: '进行中', FINISHED: '已结束', CANCELED: '已取消' }
const statusLabel = computed(() => statusMap[activity.value.status] || '')
const typeName = computed(() => (types.value.find(t => t.typeCode === activity.value.typeCode) || {}).typeName || '')

const goSignup = () => router.push(`/m/signup/${route.params.guid}`)

const load = async () => {
  loading.value = true; error.value = ''
  try {
  const [data, ts] = await Promise.all([signupApi.promo(route.params.guid), dictApi.activityTypes()])
  types.value = ts
  activity.value = data.activity
  attachments.value = data.attachments || []
  signupOpen.value = data.signupOpen && activity.value.signedCount < activity.value.quota
  signupTip.value = data.signupOpen && !signupOpen.value ? '活动名额已满，感谢关注' : data.signupTip
  qrUrl.value = await QRCode.toDataURL(`${location.origin}/m/signup/${route.params.guid}`, { width: 200 })
  } catch (e) { error.value = e.message } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.promo { max-width: 480px; margin: 0 auto; background: #fff; min-height: 100dvh; }
.cover-placeholder { padding: 32px; background: #173c40; color: #fff; }
.cover-placeholder span { color: #a4c8bd; font-size: 11px; letter-spacing: .16em; }
.cover-placeholder strong { display: block; font-size: 31px; line-height: 1.4; margin-top: 20px; font-weight: 500; }
.cover { width: 100%; height: 200px; object-fit: cover; }
.body { padding: 26px 24px; }
h1 { font-size: 20px; margin-bottom: 10px; }
.meta { margin-bottom: 10px; }
.tag { display: inline-block; background: #ecf5ff; color: #409eff; border-radius: 4px; padding: 2px 8px; font-size: 12px; margin-right: 8px; }
.row { color: #606266; font-size: 14px; margin: 4px 0; }
.intro { margin-top: 18px; color: #303133; line-height: 1.8; white-space: pre-wrap; overflow-wrap: anywhere; }
.qr-box { text-align: center; margin: 20px 0; }
.qr { width: 200px; }
.hint { color: #909399; font-size: 13px; }
.closed { margin: 20px 0; padding: 12px; background: #f4f4f5; color: #909399; text-align: center; border-radius: 6px; }
.live { color: #409eff; }
.atts { margin: 12px 0; font-size: 14px; }
.btn { width: 100%; height: 48px; border: none; border-radius: 10px; background: #137c70; color: #fff; font-size: 16px; margin-top: 18px; }
.btn:disabled { background: #c0c4cc; }
</style>
