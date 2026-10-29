<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElStep,
  ElSteps,
  type FormInstance,
  type FormRules,
} from 'element-plus'

import AppIcon from '@/components/common/AppIcon.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ActivityFilesPanel from '@/components/activity/ActivityFilesPanel.vue'
import { createActivity, fetchActivityDetail, updateActivity } from '@/api/admin/activity'
import { fetchActivityTypes } from '@/api/public/catalog'
import { ApiError } from '@/api/http'
import { useAsyncData } from '@/composables/useAsyncData'
import type { ActivityFormPayload, AttachmentSummary } from '@/types/activity'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => route.name === 'admin-activity-edit')
const activityId = computed(() => (typeof route.params.id === 'string' ? route.params.id : ''))
const backTo = computed(() =>
  isEdit.value ? `/admin/activities/${activityId.value}` : '/admin/activities',
)

const formRef = ref<FormInstance>()
const step = ref(0)
const version = ref(1)
const saving = ref(false)
const loading = ref(false)
const loadError = ref<ApiError | null>(null)
const snapshot = ref('')
const allowLeave = ref(false)
const coverUrl = ref<string | null>(null)
const attachments = ref<AttachmentSummary[]>([])

const form = reactive<{
  name: string
  typeId: string
  description: string
  location: string
  startAt: string
  endAt: string
  registrationDeadline: string
  capacity: number
  liveUrl: string
}>({
  name: '',
  typeId: '',
  description: '',
  location: '',
  startAt: '',
  endAt: '',
  registrationDeadline: '',
  capacity: 50,
  liveUrl: '',
})

const steps = ['基本信息', '时间与名额', '宣传与附件']

const stepFields: string[][] = [
  ['name', 'typeId', 'location', 'description'],
  ['startAt', 'endAt', 'registrationDeadline', 'capacity'],
  ['liveUrl'],
]

const types = useAsyncData((signal) => fetchActivityTypes(signal))

type Validator = (error?: string | Error) => void

function validateEndAt(_rule: unknown, value: string, callback: Validator): void {
  if (!value) {
    callback(new Error('请选择活动结束时间'))
    return
  }
  if (form.startAt && Date.parse(value) <= Date.parse(form.startAt)) {
    callback(new Error('结束时间必须晚于开始时间'))
    return
  }
  callback()
}

function validateDeadline(_rule: unknown, value: string, callback: Validator): void {
  if (!value) {
    callback(new Error('请选择报名截止时间'))
    return
  }
  if (form.startAt && Date.parse(value) > Date.parse(form.startAt)) {
    callback(new Error('报名截止时间不能晚于活动开始时间'))
    return
  }
  callback()
}

function validateLiveUrl(_rule: unknown, value: string, callback: Validator): void {
  const url = (value ?? '').trim()
  if (!url) {
    callback()
    return
  }
  if (!/^https?:\/\/\S+$/i.test(url)) {
    callback(new Error('直播链接需以 http:// 或 https:// 开头'))
    return
  }
  callback()
}

const rules: FormRules = {
  name: [
    { required: true, message: '请输入活动名称', trigger: 'blur' },
    { min: 2, max: 80, message: '活动名称需为 2 至 80 个字符', trigger: 'blur' },
  ],
  typeId: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  location: [
    { required: true, message: '请输入活动地点', trigger: 'blur' },
    { max: 120, message: '活动地点不能超过 120 个字符', trigger: 'blur' },
  ],
  description: [{ max: 2000, message: '活动简介不能超过 2000 个字符', trigger: 'blur' }],
  startAt: [{ required: true, message: '请选择活动开始时间', trigger: 'change' }],
  endAt: [{ required: true, validator: validateEndAt, trigger: 'change' }],
  registrationDeadline: [
    { required: true, validator: validateDeadline, trigger: 'change' },
  ],
  capacity: [{ required: true, message: '请输入人数上限', trigger: 'change' }],
  liveUrl: [{ validator: validateLiveUrl, trigger: 'blur' }],
}

const isDirty = computed(
  () => snapshot.value.length > 0 && JSON.stringify(form) !== snapshot.value,
)

async function loadDetail(): Promise<void> {
  loading.value = true
  loadError.value = null
  try {
    const detail = await fetchActivityDetail(activityId.value)
    Object.assign(form, {
      name: detail.name,
      typeId: detail.typeId,
      description: detail.description,
      location: detail.location,
      startAt: detail.startAt,
      endAt: detail.endAt,
      registrationDeadline: detail.registrationDeadline,
      capacity: detail.capacity,
      liveUrl: detail.liveUrl ?? '',
    })
    version.value = detail.version
    coverUrl.value = detail.coverUrl
    attachments.value = detail.attachments
    step.value = 0
    snapshot.value = JSON.stringify(form)
  } catch (caught) {
    loadError.value =
      caught instanceof ApiError ? caught : new ApiError('UNKNOWN_ERROR', '活动加载失败')
  } finally {
    loading.value = false
  }
}

async function refreshFiles(): Promise<void> {
  const detail = await fetchActivityDetail(activityId.value)
  coverUrl.value = detail.coverUrl
  attachments.value = detail.attachments
}

onMounted(() => {
  if (isEdit.value) {
    void loadDetail()
  } else {
    snapshot.value = JSON.stringify(form)
  }
})

async function goNext(): Promise<void> {
  const fields = stepFields[step.value] ?? []
  try {
    await formRef.value?.validateField(fields)
    step.value = Math.min(step.value + 1, steps.length - 1)
  } catch {
    // 校验失败由字段下方提示，保持当前步骤
  }
}

function goPrev(): void {
  step.value = Math.max(step.value - 1, 0)
}

async function save(): Promise<void> {
  if (!formRef.value || saving.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    const payload: ActivityFormPayload = {
      ...form,
      name: form.name.trim(),
      location: form.location.trim(),
      description: form.description.trim(),
      liveUrl: form.liveUrl.trim() ? form.liveUrl.trim() : null,
    }

    if (isEdit.value) {
      const updated = await updateActivity(activityId.value, {
        ...payload,
        version: version.value,
      })
      version.value = updated.version
      snapshot.value = JSON.stringify(form)
      allowLeave.value = true
      ElMessage.success('活动已更新，原有报名与宣传链接保持不变')
      await router.push({ name: 'admin-activity-detail', params: { id: activityId.value } })
    } else {
      const created = await createActivity(payload)
      snapshot.value = JSON.stringify(form)
      allowLeave.value = true
      ElMessage.success('活动已保存为草稿，可在详情页发布')
      await router.replace({ name: 'admin-activity-edit', params: { id: created.id } })
    }
  } catch (caught) {
    if (caught instanceof ApiError && caught.code === 'VERSION_CONFLICT') {
      ElMessage.error('活动已被其他管理员修改，请重新加载后再保存')
      await loadDetail()
      snapshot.value = JSON.stringify(form)
    } else {
      ElMessage.error(caught instanceof ApiError ? caught.message : '保存失败，请稍后重试')
    }
  } finally {
    saving.value = false
  }
}

function cancel(): void {
  void router.push(backTo.value)
}

onBeforeRouteLeave(async () => {
  if (allowLeave.value || saving.value || !isDirty.value) return true
  try {
    await ElMessageBox.confirm('离开后本次未保存的修改会丢失。', '尚未保存', {
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
      type: 'warning',
    })
    return true
  } catch {
    return false
  }
})
</script>

<template>
  <div class="editor">
    <PageHeader
      :title="isEdit ? '编辑活动' : '创建活动'"
      :back-to="backTo"
      :description="
        isEdit
          ? '更新原活动记录，活动 ID、宣传链接与已有报名全部保留；保存使用版本号避免并发覆盖。'
          : '先保存为草稿，再在详情页上传材料并发布；发布后手机端即可通过宣传页报名。'
      "
    >
      <template #actions>
        <ElButton :disabled="saving" @click="cancel">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="save">
          {{ isEdit ? '保存修改' : '保存草稿' }}
        </ElButton>
      </template>
    </PageHeader>

    <LoadingSkeleton v-if="loading" variant="detail" />
    <ErrorState
      v-else-if="loadError"
      title="活动加载失败"
      :message="loadError.message"
      :code="loadError.code"
      @retry="loadDetail"
    />

    <section v-else class="app-card app-card--padded editor__card">
      <ElSteps class="editor__steps" :active="step" align-center finish-status="success">
        <ElStep
          v-for="(item, index) in steps"
          :key="item"
          :title="item"
          :status="index < step ? 'success' : undefined"
        />
      </ElSteps>

      <ElAlert
        v-if="isEdit"
        class="editor__notice"
        type="info"
        show-icon
        :closable="false"
        title="编辑不会重建活动：修改后宣传页展示新信息，原有报名记录与报名人数不变。"
      />

      <ElForm
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="editor__form"
        @submit.prevent
      >
        <div v-show="step === 0" class="editor__section">
          <h2 class="section-title">基本信息</h2>

          <ElFormItem label="活动名称" prop="name">
            <ElInput
              v-model="form.name"
              maxlength="80"
              show-word-limit
              placeholder="例如：人工智能前沿技术讲座"
            />
          </ElFormItem>

          <div class="editor__grid">
            <ElFormItem label="活动类型" prop="typeId">
              <ElSelect
                v-model="form.typeId"
                placeholder="请选择活动类型"
                :loading="types.loading.value"
              >
                <ElOption
                  v-for="item in types.data.value?.items ?? []"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </ElSelect>
            </ElFormItem>

            <ElFormItem label="活动地点" prop="location">
              <ElInput v-model="form.location" placeholder="例如：南校区图书馆报告厅" />
            </ElFormItem>
          </div>

          <ElFormItem label="活动简介" prop="description">
            <ElInput
              v-model="form.description"
              type="textarea"
              :rows="5"
              maxlength="2000"
              show-word-limit
              placeholder="介绍活动内容、参与对象与注意事项；公开页面会展示这段文字。"
            />
          </ElFormItem>
        </div>

        <div v-show="step === 1" class="editor__section">
          <h2 class="section-title">时间与名额</h2>

          <div class="editor__grid">
            <ElFormItem label="活动开始时间" prop="startAt">
              <ElDatePicker
                v-model="form.startAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ssZ"
                format="YYYY-MM-DD HH:mm"
                placeholder="选择开始时间"
                style="width: 100%"
              />
            </ElFormItem>

            <ElFormItem label="活动结束时间" prop="endAt">
              <ElDatePicker
                v-model="form.endAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ssZ"
                format="YYYY-MM-DD HH:mm"
                placeholder="选择结束时间"
                style="width: 100%"
              />
            </ElFormItem>
          </div>

          <div class="editor__grid">
            <ElFormItem label="报名截止时间" prop="registrationDeadline">
              <ElDatePicker
                v-model="form.registrationDeadline"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ssZ"
                format="YYYY-MM-DD HH:mm"
                placeholder="选择报名截止时间"
                style="width: 100%"
              />
            </ElFormItem>

            <ElFormItem label="人数上限" prop="capacity">
              <ElInputNumber
                v-model="form.capacity"
                :min="1"
                :max="100000"
                :step="1"
                style="width: 100%"
              />
            </ElFormItem>
          </div>

          <p class="editor__hint">
            活动开始时间需早于结束时间，报名截止时间不能晚于开始时间。保存草稿允许过去日期，
            是否可报名由后端按服务器时间判断。
          </p>
        </div>

        <div v-show="step === 2" class="editor__section">
          <h2 class="section-title">宣传与附件</h2>

          <ElFormItem label="直播链接（可选）" prop="liveUrl">
            <ElInput
              v-model="form.liveUrl"
              placeholder="https://live.example.edu.cn/xxx"
              clearable
            />
          </ElFormItem>

          <ActivityFilesPanel
            v-if="isEdit && activityId"
            :activity-id="activityId"
            :cover-url="coverUrl"
            :attachments="attachments"
            @changed="refreshFiles"
          />
          <p v-else class="editor__hint">
            请先保存草稿。保存后可在此上传封面和附件；某个文件失败时，活动草稿会保留。
          </p>

          <div class="editor__summary">
            <h3 class="editor__summary-title">保存前检查</h3>
            <dl class="editor__summary-list">
              <div>
                <dt>活动名称</dt>
                <dd>{{ form.name || '未填写' }}</dd>
              </div>
              <div>
                <dt>活动类型</dt>
                <dd>
                  {{
                    types.data.value?.items.find((item) => item.id === form.typeId)?.name ??
                    '未选择'
                  }}
                </dd>
              </div>
              <div>
                <dt>活动地点</dt>
                <dd>{{ form.location || '未填写' }}</dd>
              </div>
              <div>
                <dt>人数上限</dt>
                <dd>{{ form.capacity }} 人</dd>
              </div>
            </dl>
          </div>
        </div>
      </ElForm>

      <footer class="editor__footer">
        <ElButton v-if="step > 0" @click="goPrev">
          <AppIcon name="chevron-left" :size="16" />
          <span>上一步</span>
        </ElButton>
        <div class="editor__footer-right">
          <ElButton v-if="step < steps.length - 1" type="primary" plain @click="goNext">
            <span>下一步</span>
            <AppIcon name="chevron-right" :size="16" />
          </ElButton>
          <ElButton v-if="step === steps.length - 1" type="primary" :loading="saving" @click="save">
            {{ isEdit ? '保存修改' : '保存草稿' }}
          </ElButton>
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.editor {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.editor__card {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
}

.editor__steps {
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
}

/* Element Plus 的步骤项默认按内容宽度收缩，这里让其均分容器宽度并保持标题单行 */
.editor__steps :deep(.el-step) {
  flex: 1 1 0;
  min-width: 0;
}

.editor__steps :deep(.el-step__title) {
  font-size: var(--font-size-sm);
  font-weight: 600;
  white-space: nowrap;
}

.editor__notice {
  border-radius: var(--radius-control);
}

.editor__form {
  width: 100%;
}

.editor__section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.editor__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 0 var(--space-4);
}

.editor__hint {
  padding: var(--space-3);
  border-radius: var(--radius-control);
  background: var(--bg-subtle);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.editor__summary {
  margin-top: var(--space-3);
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--bg-subtle);
}

.editor__summary-title {
  margin-bottom: var(--space-3);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.editor__summary-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--space-3);
}

.editor__summary-list dt {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.editor__summary-list dd {
  margin: 2px 0 0;
  font-size: var(--font-size-sm);
  font-weight: 550;
  word-break: break-all;
}

.editor__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--border);
}

.editor__footer-right {
  display: flex;
  gap: var(--space-2);
  margin-left: auto;
}

@media (max-width: 767px) {
  .editor__steps :deep(.el-step__description) {
    display: none;
  }

  .editor__footer {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .editor__footer :deep(.el-button) {
    width: 100%;
  }

  .editor__footer-right {
    flex-direction: column;
    margin-left: 0;
  }
}
</style>
