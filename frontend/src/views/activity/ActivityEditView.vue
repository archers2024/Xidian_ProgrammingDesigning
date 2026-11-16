<template>
  <!-- UI-03 活动编辑页（B岗）：创建与编辑共用，含封面/附件/直播链接 -->
  <el-card>
    <h3>{{ isEdit ? '编辑活动' : '新建活动' }}</h3>
    <el-form :model="form" label-width="110px" class="form">
      <el-form-item label="活动名称" required><el-input v-model="form.title" maxlength="60" /></el-form-item>
      <el-form-item label="活动类型" required>
        <el-select v-model="form.typeCode">
          <el-option v-for="t in types" :key="t.typeCode" :label="t.typeName" :value="t.typeCode" />
        </el-select>
      </el-form-item>
      <el-form-item label="活动简介"><el-input v-model="form.introduction" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="活动地点" required><el-input v-model="form.location" /></el-form-item>
      <el-form-item label="地点经纬度">
        <el-input v-model="form.lngLat" placeholder="格式：108.9,34.2，准确签到校验用" />
      </el-form-item>
      <el-form-item label="活动时间" required>
        <el-date-picker v-model="timeRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
                        start-placeholder="开始时间" end-placeholder="结束时间" />
      </el-form-item>
      <el-form-item label="报名截止" required>
        <el-date-picker v-model="form.signupDeadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
                        placeholder="必须早于活动开始时间" />
      </el-form-item>
      <el-form-item label="人数上限" required>
        <el-input-number v-model="form.quota" :min="1" :max="5000" />
      </el-form-item>
      <el-form-item label="签到半径(米)"><el-input-number v-model="form.checkinRadius" :min="50" :max="5000" /></el-form-item>
      <el-form-item label="开启签到">
        <el-switch v-model="checkinEnabled" :disabled="!isEdit || !canToggleCheckin" @change="onToggleCheckin" />
        <span class="hint" v-if="!isEdit">创建并发布后可开启签到</span>
        <span class="hint" v-else-if="!canToggleCheckin">草稿或已取消的活动不能开启签到</span>
      </el-form-item>
      <el-form-item label="直播链接"><el-input v-model="form.liveUrl" placeholder="选填" /></el-form-item>
      <el-form-item label="封面图">
        <el-upload :http-request="uploadCover" :show-file-list="false" accept="image/*">
          <img v-if="form.coverUrl" :src="form.coverUrl" class="cover" />
          <el-button v-else>上传封面</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item label="活动附件">
        <el-upload :http-request="uploadAttachment" :show-file-list="true" :file-list="attachments"
                   :on-remove="removeAttachment">
          <el-button>选择文件</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">保存</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { activityApi, dictApi, fileApi } from '../../api'

const route = useRoute()
const router = useRouter()
const isEdit = !!route.params.id
const types = ref([])
const timeRange = ref(null)
const attachments = ref([])
// 签到开关与活动状态：草稿 / 已取消不允许开启
const checkinEnabled = ref(false)
const status = ref('')
const canToggleCheckin = computed(() => status.value !== 'DRAFT' && status.value !== 'CANCELED')
const form = reactive({
  title: '', typeCode: '', introduction: '', location: '', lngLat: '',
  signupDeadline: '', quota: 100, checkinRadius: 500, liveUrl: '', coverUrl: ''
})

const doUpload = async (options) => {
  const fd = new FormData()
  fd.append('file', options.file)
  return fileApi.upload(fd)
}
const uploadCover = async (options) => {
  const data = await doUpload(options)
  form.coverUrl = data.url
}
const uploadAttachment = async (options) => {
  const data = await doUpload(options)
  attachments.value.push({ name: data.fileName, fileName: data.fileName, url: data.url, fileSize: data.fileSize })
}
const removeAttachment = (file) => {
  attachments.value = attachments.value.filter(a => a.url !== file.url)
}

// 签到开关走独立接口，保存后立即生效并记录操作日志
const onToggleCheckin = async (val) => {
  try {
    await activityApi.toggleCheckin(route.params.id, val)
    ElMessage.success(val ? '已开启签到' : '已关闭签到')
  } catch (e) {
    checkinEnabled.value = !val
    ElMessage.error(e.message || '操作失败')
  }
}

const submit = async () => {
  if (!form.title || !form.typeCode || !form.location || !timeRange.value || !form.signupDeadline) {
    ElMessage.warning('请填写完整必填项')
    return
  }
  if (new Date(form.signupDeadline) >= new Date(timeRange.value[0])) {
    ElMessage.warning('报名截止时间必须早于活动开始时间')
    return
  }
  const payload = {
    ...form,
    startTime: timeRange.value[0],
    endTime: timeRange.value[1],
    attachments: attachments.value
  }
  if (isEdit) {
    await activityApi.update(route.params.id, payload)
    ElMessage.success('已保存，宣传页内容已同步更新')
  } else {
    await activityApi.create(payload)
    ElMessage.success('已创建，请在列表页点击发布')
  }
  router.push('/activity')
}

onMounted(async () => {
  types.value = await dictApi.activityTypes()
  if (isEdit) {
    const data = await activityApi.detail(route.params.id)
    const a = data.activity
    Object.assign(form, {
      title: a.title, typeCode: a.typeCode, introduction: a.introduction, location: a.location,
      lngLat: a.lngLat, signupDeadline: a.signupDeadline, quota: a.quota,
      checkinRadius: a.checkinRadius, liveUrl: a.liveUrl, coverUrl: a.coverUrl
    })
    timeRange.value = [a.startTime, a.endTime]
    status.value = a.status
    checkinEnabled.value = a.checkinEnabled === 1
    attachments.value = (data.attachments || []).map(x => ({ name: x.fileName, fileName: x.fileName, url: x.fileUrl, fileSize: x.fileSize }))
  }
})
</script>

<style scoped>
h3 { margin-bottom: 16px; }
.form { max-width: 720px; }
.cover { width: 160px; height: 100px; object-fit: cover; border-radius: 4px; }
.hint { margin-left: 10px; color: #909399; font-size: 12px; }
</style>
