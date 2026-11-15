<template>
  <!-- UI-07 数据统计页（D岗） -->
  <div>
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
import { onMounted, reactive, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { statisticsApi } from '../../api'

const days = ref(30)
const data = reactive({ activityCount: 0, signupCount: 0, checkinCount: 0, checkinRate: 0, typeStats: [], dailyStats: [] })
const barRef = ref(null)
const lineRef = ref(null)
let bar, line

const render = () => {
  bar = bar || echarts.init(barRef.value)
  bar.setOption({
    title: { text: '各类型报名与签到' },
    tooltip: {},
    legend: { data: ['报名人次', '签到人次'] },
    xAxis: { data: data.typeStats.map(x => x.typeName) },
    yAxis: {},
    series: [
      { name: '报名人次', type: 'bar', data: data.typeStats.map(x => x.signupCount), itemStyle: { color: '#409eff' } },
      { name: '签到人次', type: 'bar', data: data.typeStats.map(x => x.checkinCount), itemStyle: { color: '#67c23a' } }
    ]
  })
  line = line || echarts.init(lineRef.value)
  line.setOption({
    title: { text: '每日报名趋势' },
    tooltip: {},
    xAxis: { data: data.dailyStats.map(x => x.date) },
    yAxis: {},
    series: [{ name: '报名量', type: 'line', smooth: true, data: data.dailyStats.map(x => x.count), itemStyle: { color: '#e6a23c' } }]
  })
}

const load = async () => {
  const res = await statisticsApi.overview(days.value)
  Object.assign(data, res)
  await nextTick()
  render()
}

onMounted(() => { load() })
</script>

<style scoped>
.range { margin-bottom: 16px; }
.cards { margin-bottom: 16px; }
.num { display: block; font-size: 26px; font-weight: 600; margin-top: 6px; }
.chart { height: 320px; }
</style>
