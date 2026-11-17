<template>
  <!-- UI-04 活动详情页（B岗框架 + D岗统计卡片） -->
  <div v-if="activity"><p v-if="auxiliaryError" role="alert">{{auxiliaryError}} <el-button @click="load">重新加载</el-button></p>
    <el-descriptions :title="activity.title" :column="2" border>
      <el-descriptions-item label="活动编号">{{ activity.activityNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag>{{ statusLabel(activity.status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="活动时间">{{ formatTime(activity.startTime) }} 至 {{ formatTime(activity.endTime) }}</el-descriptions-item>
      <el-descriptions-item label="报名截止">{{ formatTime(activity.signupDeadline) }}</el-descriptions-item>
      <el-descriptions-item label="地点">{{ activity.location }}</el-descriptions-item>
      <el-descriptions-item label="报名情况">{{ activity.signedCount }} / {{ activity.quota }}</el-descriptions-item>
      <el-descriptions-item label="直播链接">{{ activity.liveUrl || '-' }}</el-descriptions-item>
      <el-descriptions-item label="发布时间">{{ activity.publishTime ? formatTime(activity.publishTime) : '未发布' }}</el-descriptions-item>
      <el-descriptions-item label="活动简介" :span="2">{{ activity.introduction || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-row :gutter="16" class="cards">
      <el-col :span="8"><el-card>已报名<span class="num">{{ activity.signedCount }}</span></el-card></el-col>
      <el-col :span="8"><el-card>已签到<span class="num">{{ checkinCount }}</span></el-card></el-col>
      <el-col :span="8"><el-card>签到率<span class="num">{{ checkinRate }}%</span></el-card></el-col>
    </el-row>

    <p>审批状态：{{ reviewLabels[activity.approvalStatus] || activity.approvalStatus }} · {{ activity.reviewRequired ? "审核通过后发布" : "允许直接发布" }}</p>
    <p class="list-note">所有状态均可编辑，报名人员和宣传链接保留；编辑会撤销待审申请，历史审批快照保留。</p>
    <div class="actions">
      <el-button @click="$router.push(`/activity/${activity.activityId}/edit`)">编辑活动</el-button>
      <el-button v-if="activity.status === 'DRAFT' && ['NONE','REJECTED'].includes(activity.approvalStatus)" type="primary" :loading="submitting" @click="submitReview">提交审核</el-button>
      <el-button v-if="activity.status === 'DRAFT' && activity.approvalStatus !== 'PENDING' && (!activity.reviewRequired || activity.approvalStatus === 'APPROVED')" type="primary" @click="publish">发布活动</el-button>
      <el-button :disabled="activity.status === 'DRAFT'" @click="showPromo = true">查看宣传页二维码</el-button>
      <el-button @click="$router.push(`/activity/${activity.activityId}/registrations`)">报名名单</el-button>
      <el-button @click="$router.push(`/activity/${activity.activityId}/checkins`)">签到记录</el-button>
      <el-button v-if="activity.status === 'ONGOING'" @click="toggleCheckin">{{ activity.checkinEnabled ? '关闭签到' : '开启签到' }}</el-button>
      <el-button :disabled="!activity.checkinEnabled || activity.status !== 'ONGOING'" @click="openBoard">打开签到大屏</el-button>
    </div>

    <el-card style="margin-top:24px"><template #header>审批历史</template><el-table :data="reviews"><el-table-column prop="submittedAt" label="提交时间"/><el-table-column label="结果"><template #default="{row}">{{reviewLabels[row.decision] || row.decision}}</template></el-table-column><el-table-column prop="comment" label="审批意见"/><el-table-column prop="reviewedAt" label="处理时间"/><el-table-column label="申请内容"><template #default="{row}"><el-button link @click="snapshotDialog.open(row.reviewId)">查看</el-button></template></el-table-column></el-table></el-card>
    <el-dialog v-model="showPromo" title="宣传海报页二维码" width="360px">
      <div class="qr">
        <img :src="qrDataUrl" v-if="qrDataUrl" />
        <p class="url">{{ promoUrl }}</p>
        <el-button type="primary" @click="copyUrl">复制链接</el-button>
      </div>
    </el-dialog>
  </div>
  <div v-else class="page-state"><strong>{{ loadError ? '暂时无法打开活动' : '正在加载活动' }}</strong><p>{{ loadError || '请稍候…' }}</p><el-button v-if="loadError" @click="load">重新加载</el-button></div>
<ReviewSnapshotDialog ref="snapshotDialog"/>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { activityApi, checkinApi, reviewApi } from '../../api'
import ReviewSnapshotDialog from '../../components/ReviewSnapshotDialog.vue'
import { formatTime } from '../../utils/display'

const snapshotDialog=ref(null)
const reviews=ref([]),submitting=ref(false)
const reviewLabels={NONE:'未提交',PENDING:'待审批',APPROVED:'已通过',REJECTED:'已驳回',CANCELED:'已撤销'}
async function submitReview(){if(submitting.value)return;submitting.value=true;try{await reviewApi.submit(route.params.id);ElMessage.success('已提交审核');await load()}catch{}finally{submitting.value=false}}
const route = useRoute()
const activity = ref(null)
const attendance=ref({checkinCount:0})
const auxiliaryError=ref('')
const showPromo = ref(false)
const qrDataUrl = ref('')
const loadError = ref('')
const statusMap = { DRAFT: '未发布', OPEN: '报名中', CLOSED: '已截止', ONGOING: '进行中', FINISHED: '已结束', CANCELED: '已取消' }
const statusLabel = (s) => statusMap[s] || s

const checkinCount = computed(() => attendance.value.checkinCount||0)
const checkinRate = computed(() => {
  const signed = activity.value?.signedCount || 0
  return signed === 0 ? 0 : Math.round((checkinCount.value * 100) / signed)
})
const promoUrl = computed(() => `${location.origin}/m/promo/${activity.value?.signupGuid}`)

const publish = async () => { await activityApi.publish(activity.value.activityId); ElMessage.success('已发布'); load() }
const openBoard = () => window.open(`/m/board/${activity.value.activityId}`, '_blank')
const toggleCheckin = async () => { await activityApi.checkinEnabled(activity.value.activityId, activity.value.checkinEnabled ? 0 : 1); await load() }

const load = async () => {
  loadError.value = ''
  try {
  const data = await activityApi.detail(route.params.id)
  activity.value = data.activity
  const sections=await Promise.allSettled([reviewApi.history(route.params.id),activityApi.summary(route.params.id),QRCode.toDataURL(promoUrl.value,{width:260})])
  if(sections[0].status==='fulfilled')reviews.value=sections[0].value
  if(sections[1].status==='fulfilled')attendance.value=sections[1].value
  if(sections[2].status==='fulfilled')qrDataUrl.value=sections[2].value
  auxiliaryError.value=sections.some(item=>item.status==='rejected')?'部分信息暂不可用，可重新加载。':''
  } catch (e) { activity.value = null; loadError.value = e.message }
}
const copyUrl = async () => {
  try { await navigator.clipboard.writeText(promoUrl.value); ElMessage.success('链接已复制') }
  catch { ElMessage.warning('无法访问剪贴板，请手动复制上方链接') }
}

onMounted(async () => {
  await load()
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
