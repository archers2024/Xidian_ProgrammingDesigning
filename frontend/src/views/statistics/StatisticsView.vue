<template>
  <!-- UI-07 数据统计页（D岗） -->
  <div v-loading="loading">
    <div class="page-heading"><div><p class="eyebrow">ACTIVITY INSIGHTS</p><h2>数据统计</h2><p>了解活动参与情况，让每次组织更有依据。</p></div></div>
    <p class="scope-note">统计所选期间内创建且未取消的活动，以及这些活动的累计有效报名与签到。</p>
    <p v-if="error" role="alert">{{ error }} <el-button link type="primary" @click="load">重新加载</el-button></p>
    <el-radio-group v-model="days" @change="load" class="range">
      <el-radio-button :value="7">近 7 天</el-radio-button>
      <el-radio-button :value="30">近 30 天</el-radio-button>
      <el-radio-button :value="90">近 90 天</el-radio-button>
    </el-radio-group>

    <el-row :gutter="16" class="cards">
      <el-col :span="6"><el-card>活动总数<span class="num">{{ data.activityCount }}</span></el-card></el-col>
      <el-col :span="6"><el-card>报名人次<span class="num">{{ data.signupCount }}</span></el-card></el-col>
      <el-col :span="6"><el-card>签到人次<span class="num">{{ data.checkinCount }}</span></el-card></el-col>
      <el-col :span="6"><el-card>平均签到率<span class="num">{{ data.checkinRate }}%</span></el-card></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12"><el-card><div ref="barRef" class="chart" /></el-card></el-col>
      <el-col :span="12"><el-card><div ref="lineRef" class="chart" /></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TitleComponent, TooltipComponent, LegendComponent, GraphicComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([BarChart, LineChart, GridComponent, TitleComponent, TooltipComponent, LegendComponent, GraphicComponent, CanvasRenderer])
import { statisticsApi } from '../../api'

const days = ref(30)
const data = reactive({ activityCount: 0, signupCount: 0, checkinCount: 0, checkinRate: 0, typeStats: [], dailyStats: [] })
const barRef = ref(null)
const lineRef = ref(null)
const loading = ref(false)
const error = ref('')
let requestId = 0
let disposed = false
let observer
let bar, line

const render = () => {
  bar = bar || echarts.init(barRef.value)
  bar.setOption({
    title: { text: '各类型报名与签到', textStyle: { fontSize: 16, color: '#263e46' } },
    tooltip: {},
    legend: { top: 34, left: 0, data: ['报名人次', '签到人次'] },
    grid: { top: 90, left: 10, right: 15, bottom: 10, containLabel: true },
    xAxis: { data: data.typeStats.map(x => x.typeName) },
    yAxis: { minInterval: 1 },
    series: [
      { name: '报名人次', type: 'bar', data: data.typeStats.map(x => x.signupCount), itemStyle: { color: '#137c70', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 32 },
      { name: '签到人次', type: 'bar', data: data.typeStats.map(x => x.checkinCount), itemStyle: { color: '#91b8af', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 32 }
    ]
  })
  line = line || echarts.init(lineRef.value)
  line.setOption({
    title: { text: '每日报名趋势', textStyle: { fontSize: 16, color: '#263e46' } },
    graphic: { type: 'text', left: 'center', top: 'middle', style: { text: data.dailyStats.length ? '' : '所选活动暂无报名记录', fill: '#92a3aa', fontSize: 13 } },
    grid: { top: 70, left: 10, right: 15, bottom: 10, containLabel: true },
    tooltip: {},
    xAxis: { data: data.dailyStats.map(x => x.date) },
    yAxis: { minInterval: 1 },
    series: [{ name: '报名量', type: 'line', smooth: true, data: data.dailyStats.map(x => x.count), itemStyle: { color: '#e6a23c' } }]
  })
}

const load = async () => {
  const id = ++requestId
  loading.value = true; error.value = ''
  try {
  const res = await statisticsApi.overview(days.value)
  if (id !== requestId || disposed) return
  Object.assign(data, res)
  await nextTick()
  if (!disposed) render()
  } catch (e) { if (id === requestId) error.value = e.message } finally { if (id === requestId) loading.value = false }
}

onMounted(() => { load(); observer = new ResizeObserver(() => { bar?.resize(); line?.resize() }); observer.observe(barRef.value); observer.observe(lineRef.value) })
onUnmounted(() => { disposed = true; requestId++; observer?.disconnect(); bar?.dispose(); line?.dispose() })
</script>

<style scoped>
.range { margin-bottom: 16px; }
.cards { margin-bottom: 16px; }
.num { display: block; font-size: 26px; font-weight: 600; margin-top: 6px; }
.chart { height: 320px; }
.scope-note { color: #86969d; font-size: 12px; margin-bottom: 20px; }
@media (max-width: 900px) { .cards > .el-col { max-width: 50%; flex-basis: 50%; } .el-row:not(.cards) { gap: 16px; } .el-row:not(.cards) > .el-col { max-width: 100%; flex-basis: 100%; } }
</style>
