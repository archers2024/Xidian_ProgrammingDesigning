<template>
  <!-- UI-03 活动编辑页（B岗）：创建与编辑共用，含封面/附件/直播链接 -->
  <el-card>
    <div class="page-heading"><div><p class="eyebrow">ACTIVITY EDITOR</p><h2>{{ isEdit ? '编辑活动' : '新建活动' }}</h2><p>填写活动信息，保存后可在活动详情中发布。</p></div></div>
    <div v-if="!ready" class="page-state" role="status"><p>{{ loadError || '正在加载活动信息…' }}</p><el-button v-if="loadError" @click="load">重新加载</el-button></div>
    <el-form v-else ref="formRef" :rules="rules" :model="form" label-position="top" class="form">
      <el-form-item prop="orgId" label="归属组织" required><el-select v-model="form.orgId" filterable remote :remote-method="searchOrganizations" :disabled="isEdit"><el-option v-for="org in organizations" :key="org.orgId" :label="org.name" :value="org.orgId"/></el-select></el-form-item>
      <el-form-item prop="title" label="活动名称" required><el-input v-model="form.title" maxlength="200" /></el-form-item>
      <el-form-item prop="typeCode" label="活动类型" required>
        <el-select v-model="form.typeCode">
          <el-option v-for="t in types" :key="t.typeCode" :label="t.typeName" :value="t.typeCode" />
        </el-select>
      </el-form-item>
      <el-form-item label="活动简介" class="wide-field"><el-input v-model="form.introduction" type="textarea" :rows="3" /></el-form-item>
      <el-form-item prop="location" label="活动地点" required><el-input v-model="form.location" /></el-form-item>
      <el-form-item label="地点经纬度">
        <el-input v-model="form.lngLat" placeholder="格式：108.9,34.2，准确签到校验用" />
      </el-form-item>
      <p class="wide-field">以下时间均为北京时间（Asia/Shanghai）。</p>
      <el-form-item label="活动时间" class="wide-field" required>
        <el-date-picker v-model="timeRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss"
                        start-placeholder="开始时间" end-placeholder="结束时间" />
      </el-form-item>
      <el-form-item prop="signupDeadline" label="报名截止" required>
        <el-date-picker v-model="form.signupDeadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
                        placeholder="必须早于活动开始时间" />
      </el-form-item>
      <el-form-item label="人数上限" required>
        <el-input-number v-model="form.quota" :min="1" :max="5000" />
      </el-form-item>
      <el-form-item label="签到方式"><el-select v-model="form.checkinPolicy"><el-option label="基本签到" value="BASIC"/><el-option label="必须定位并扫描现场二维码" value="ACCURATE_REQUIRED"/><el-option label="必须验证报名身份" value="IDENTITY_REQUIRED"/><el-option label="身份验证与现场准确签到" value="STRICT"/></el-select><small>设备标识仅作为辅助风控，不能证明唯一物理设备。</small></el-form-item>
      <el-form-item label="签到半径(米)"><el-input-number v-model="form.checkinRadius" :min="50" :max="5000" /></el-form-item>
      <el-form-item label="直播链接"><el-input v-model="form.liveUrl" placeholder="选填" /></el-form-item>
      <el-form-item label="封面图">
        <el-upload :http-request="uploadCover" :show-file-list="false" accept="image/*">
          <img v-if="form.coverUrl" :src="form.coverUrl" class="cover" />
          <el-button v-else>上传封面</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item label="活动附件"><el-checkbox v-model="attachmentPublic">新上传附件公开给参与者下载</el-checkbox>
        <el-upload v-model:file-list="attachments" :http-request="uploadAttachment" :show-file-list="true" accept=".pdf,.doc,.docx,.ppt,.pptx,.jpg,.jpeg,.png,.gif">
          <el-button>选择文件</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" :disabled="uploading > 0" @click="submit">{{ uploading ? '文件上传中' : '保存活动' }}</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { onMounted,onUnmounted,computed,reactive,ref } from 'vue'
import { useRoute,useRouter,onBeforeRouteLeave } from 'vue-router'
import { ElMessage,ElMessageBox } from 'element-plus'
import { activityApi, dictApi, fileApi, organizationApi } from '../../api'

const route = useRoute()
const router = useRouter()
const isEdit = !!route.params.id
const types = ref([])
const organizations=ref([])
const timeRange = ref(null)
const attachments = ref([])
const saving = ref(false)
const ready=ref(false),loadError=ref('')
const uploading = ref(0)
const formRef=ref(null),initial=ref(''),attachmentPublic=ref(false)
const requestKey=ref(crypto.randomUUID())
const form = reactive({
  orgId:null,version:null,checkinPolicy:'BASIC',
  title: '', typeCode: '', introduction: '', location: '', lngLat: '',
  signupDeadline: '', quota: 100, checkinRadius: 500, liveUrl: '', coverUrl: ''
})

const rules={orgId:[{required:true,message:'请选择组织',trigger:'change'}],title:[{required:true,message:'请输入活动名称',trigger:'blur'},{max:200,message:'最多200字',trigger:'blur'}],typeCode:[{required:true,message:'请选择类型',trigger:'change'}],location:[{required:true,message:'请输入地点',trigger:'blur'}],signupDeadline:[{required:true,message:'请选择截止时间',trigger:'change'}]}
const serialize=()=>JSON.stringify({form,time:timeRange.value,attachments:attachments.value.map(({url,fileName})=>({url,fileName}))})
const dirty=computed(()=>initial.value!==''&&serialize()!==initial.value)
onBeforeRouteLeave(async()=>{if(!dirty.value||saving.value)return true;try{await ElMessageBox.confirm('还有未保存的内容，确定离开？','离开页面');return true}catch{return false}})
const beforeUnload=event=>{if(dirty.value){event.preventDefault();event.returnValue=''}}
window.addEventListener('beforeunload',beforeUnload)
onUnmounted(()=>window.removeEventListener('beforeunload',beforeUnload))
async function searchOrganizations(keyword){organizations.value=await organizationApi.available(keyword)}
const doUpload = async (options) => {
  const fd = new FormData()
  fd.append('file', options.file)
  fd.append('visibility',options.visibility||'PRIVATE')
  if(form.orgId)fd.append('orgId',form.orgId)
  uploading.value++
  try { return await fileApi.upload(fd) } finally { uploading.value-- }
}
const uploadCover = async (options) => {
  const data = await doUpload({...options,visibility:'PUBLIC'})
  form.coverUrl = data.url
}
const uploadAttachment = async (options) => {
  const data = await doUpload({...options,visibility:attachmentPublic.value?'PUBLIC':'PRIVATE'})
  const file = attachments.value.find(x => x.uid === options.file.uid)
  if (file) Object.assign(file, { name: data.fileName, fileName: data.fileName, url: data.url, fileSize: data.fileSize })
  return data
}

const submit = async () => {
  if (saving.value || uploading.value) return
  if(!await formRef.value?.validate().catch(()=>false)){document.querySelector('.el-form-item.is-error input')?.focus();return}
  if (!form.title || !form.typeCode || !form.location || !timeRange.value || !form.signupDeadline) {
    ElMessage.warning('请填写完整必填项')
    return
  }
  if (new Date(form.signupDeadline) >= new Date(timeRange.value[0])) {
    ElMessage.warning('报名截止时间必须早于活动开始时间')
    return
  }
  if (new Date(timeRange.value[0]) >= new Date(timeRange.value[1])) { ElMessage.warning('活动结束时间必须晚于开始时间'); return }
  if (attachments.value.some(x => !x.url)) { ElMessage.warning('请移除上传失败的附件，或重新上传'); return }
  const payload = {
    ...form,
    startTime: timeRange.value[0],
    endTime: timeRange.value[1],
    attachments: attachments.value
  }
  saving.value = true
  try { if (isEdit) {
    await activityApi.update(route.params.id, payload)
    ElMessage.success('修改已保存，报名人员和宣传链接已保留')
  } else {
    await activityApi.create(payload,requestKey.value)
    ElMessage.success('已保存草稿，请在活动详情查看审核和发布要求')
  }
  initial.value=serialize()
  requestKey.value=crypto.randomUUID()
  router.push('/activity')
  } catch { /* 保留表单供重试。 */ } finally { saving.value = false }
}

async function load() {
  ready.value=false;loadError.value=''
  try {
  organizations.value=await organizationApi.available();form.orgId=organizations.value[0]?.orgId;
  types.value = await dictApi.activityTypes()
  if (isEdit) {
    const data = await activityApi.detail(route.params.id)
    const a = data.activity
    Object.assign(form, {
      orgId:a.orgId,version:a.version,checkinPolicy:a.checkinPolicy,
      title: a.title, typeCode: a.typeCode, introduction: a.introduction, location: a.location,
      lngLat: a.lngLat, signupDeadline: a.signupDeadline, quota: a.quota,
      checkinRadius: a.checkinRadius, liveUrl: a.liveUrl, coverUrl: a.coverUrl
    })
    timeRange.value = [a.startTime, a.endTime]
    attachments.value = (data.attachments || []).map(x => ({ name: x.fileName, fileName: x.fileName, url: x.fileUrl, fileSize: x.fileSize }))
  }
  initial.value=serialize()
  ready.value=true
  } catch(e) { loadError.value=e.message||'活动信息加载失败，请重试' }
}
onMounted(load)
</script>

<style scoped>
h3 { margin-bottom: 16px; }
.form { max-width: 860px; display: grid; grid-template-columns: 1fr 1fr; column-gap: 26px; }
.form :deep(.wide-field), .form :deep(.el-form-item:last-child) { grid-column: 1 / -1; }
.form :deep(.el-select), .form :deep(.el-date-editor) { width: 100%; }
.form :deep(.el-form-item__content) { min-width: 0; }
.cover { width: 160px; height: 100px; object-fit: cover; border-radius: 4px; }
@media (max-width: 800px) { .form { grid-template-columns: 1fr; } }
</style>
