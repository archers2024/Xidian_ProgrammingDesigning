<template>
  <!-- UI-04 活动详情页（B岗框架 + D岗统计卡片） -->
  <div v-if="activity">
    <el-descriptions :title="activity.title" :column="2" border>
      <el-descriptions-item label="活动编号">{{ activity.activityNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag>{{ statusLabel(activity.status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="活动时间">{{ activity.startTime }} ~ {{ activity.endTime }}</el-descriptions-item>
      <el-descriptions-item label="报名截止">{{ activity.signupDeadline }}</el-descriptions-item>
      <el-descriptions-item label="地点">{{ activity.location }}</el-descriptions-item>
      <el-descriptions-item label="报名情况">{{ activity.signedCount }} / {{ activity.quota }}</el-descriptions-item>
      <el-descriptions-item label="直播链接">{{ activity.liveUrl || '-' }}</el-descriptions-item>
      <el-descriptions-item label="发布时间">{{ activity.publishTime || '未发布' }}</el-descriptions-item>
      <el-descriptions-item label="活动简介" :span="2">{{ activity.introduction || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-row :gutter="16" class="cards">
      <el-col :span="8"><el-card>已报名<span class="num">{{ activity.signedCount }}</span></el-card></el-col>
      <el-col :span="8"><el-card>已签到<span class="num">{{ checkinCount }}</span></el-card></el-col>
      <el-col :span="8"><el-card>签到率<span class="num">{{ checkinRate }}%</span></el-card></el-col>
    </el-row>

    <div class="actions">
      <el-button v-if="activity.status === 'DRAFT'" type="primary" @click="publish">发布活动</el-button>
      <el-button @click="showPromo = true">查看宣传页二维码</el-button>
      <el-button @click="$router.push(`/activity/${activity.activityId}/registrations`)">报名名单</el-button>
      <el-button @click="$router.push(`/activity/${activity.activityId}/checkins`)">签到记录</el-button>
      <el-button @click="openBoard">打开签到大屏</el-button>
    </div>

    <el-dialog v-model="showPromo" title="宣传海报页二维码" width="360px">
      <div class="qr">
        <img :src="qrDataUrl" v-if="qrDataUrl" />
        <p class="url">{{ promoUrl }}</p>
        <el-button type="primary" @click="copyUrl">复制链接</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { activityApi, checkinApi } from '../../api'

const route = useRoute()
const activity = ref(null)
const checkins = ref([])
const showPromo = ref(false)
const qrDataUrl = ref('')
const statusMap = { DRAFT: '未发布', OPEN: '报名中', CLOSED: '已截止', ONGOING: '进行中', FINISHED: '已结束', CANCELED: '已取消' }
const statusLabel = (s) => statusMap[s] || s

const checkinCount = computed(() => checkins.value.length)
const checkinRate = computed(() => {
  const signed = activity.value?.signedCount || 0
  return signed === 0 ? 0 : Math.round((checkinCount.value * 100) / signed)
})
const promoUrl = computed(() => `${location.origin}/m/promo/${activity.value?.signupGuid}`)

const publish = async () => { await activityApi.publish(activity.value.activityId); ElMessage.success('已发布'); load() }
const openBoard = () => window.open(`/m/board/${activity.value.activityId}`, '_blank')

const load = async () => {
  const data = await activityApi.detail(route.params.id)
  activity.value = data.activity
  checkins.value = await checkinApi.list(activity.value.activityId)
}
const copyUrl = () => {
  navigator.clipboard.writeText(promoUrl.value)
  ElMessage.success('链接已复制')
}

onMounted(async () => {
  await load()
  qrDataUrl.value = await QRCode.toDataURL(promoUrl.value, { width: 260 })
})
</script>

<style scoped>
.cards { margin: 16px 0; }
.num { display: block; font-size: 26px; font-weight: 600; margin-top: 6px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.qr { text-align: center; }
.qr img { width: 260px; }
.url { font-size: 12px; color: #606266; word-break: break-all; }
</style>
