<template>
  <!-- UI-12 签到展示大屏（D岗）：动态二维码 30 秒刷新 + 倒计时 + 实时人数 -->
  <div class="board">
    <h1>{{ title || '活动签到' }}</h1>
    <p class="tip">请使用手机扫描下方二维码签到（二维码 {{ board.timeStep }} 秒刷新一次）</p>

    <div class="qr-wrap">
      <img v-if="qrUrl" :src="qrUrl" class="qr" />
      <div class="countdown">{{ board.remainSeconds }}s</div>
    </div>

    <div class="stats">
      <div><span class="num">{{ checkinCount }}</span>已签到</div>
      <div><span class="num">{{ board.remainSeconds }}s</span>刷新倒计时</div>
    </div>

    <p class="code">动态码：{{ board.code }}</p>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import QRCode from 'qrcode'
import { checkinApi } from '../api'

const route = useRoute()
const board = ref({ code: '', remainSeconds: 30, timeStep: 30 })
const qrUrl = ref('')
const title = ref('')
const checkinCount = ref(0)
let timer = null

const refresh = async () => {
  board.value = await checkinApi.board(route.params.activityId)
  const url = `${location.origin}/m/checkin/${route.params.activityId}?code=${board.value.code}`
  qrUrl.value = await QRCode.toDataURL(url, { width: 320 })
  const list = await checkinApi.list(route.params.activityId)
  checkinCount.value = list.length
}

const tick = async () => {
  if (board.value.remainSeconds > 1) {
    board.value.remainSeconds--
    return
  }
  await refresh()
}

onMounted(async () => {
  await refresh()
  timer = setInterval(tick, 1000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.board { min-height: 100vh; background: #0f1b33; color: #fff; text-align: center; padding: 40px 20px; }
h1 { font-size: 32px; margin-bottom: 8px; }
.tip { color: #9fb0cf; }
.qr-wrap { position: relative; display: inline-block; margin: 24px 0; padding: 16px; background: #fff; border-radius: 12px; }
.qr { width: 320px; display: block; }
.countdown { position: absolute; right: 8px; top: 8px; background: #f56c6c; color: #fff; border-radius: 12px; padding: 2px 10px; font-size: 14px; }
.stats { display: flex; justify-content: center; gap: 60px; margin-top: 20px; font-size: 16px; color: #9fb0cf; }
.num { display: block; font-size: 40px; color: #fff; font-weight: 600; }
.code { margin-top: 24px; color: #67c23a; letter-spacing: 2px; }
</style>
