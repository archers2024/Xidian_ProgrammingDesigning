<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElAlert,
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElOption,
  ElSelect,
  type FormInstance,
  type FormRules,
} from 'element-plus'

import AppIcon from '@/components/common/AppIcon.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { createRegistration } from '@/api/public/registration'
import { fetchPublicActivity } from '@/api/public/activity'
import { fetchColleges } from '@/api/public/catalog'
import { ApiError } from '@/api/http'
import { useAsyncData } from '@/composables/useAsyncData'
import type { RegistrationResult } from '@/types/registration'
import { UNAVAILABLE_REASON_TEXT } from '@/types/activity'
import { formatActivityTime, formatDateTime, remainingSeats } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()
const activityId = computed(() => String(route.params.id ?? ''))

const activity = useAsyncData((signal) => fetchPublicActivity(activityId.value, signal))
const colleges = useAsyncData((signal) => fetchColleges(signal))

const formRef = ref<FormInstance>()
const submitting = ref(false)
const submitError = ref<ApiError | null>(null)
const result = ref<RegistrationResult | null>(null)

const form = reactive({
  name: '',
  phone: '',
  collegeId: '',
})

const rules: FormRules<typeof form> = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    {
      pattern: /^[\u4e00-\u9fa5A-Za-z·\-\s]{2,30}$/,
      message: '姓名需为 2 至 30 个字符，可包含中文、英文、间隔号或连字符',
      trigger: 'blur',
    },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' },
  ],
  collegeId: [{ required: true, message: '请选择所属学院', trigger: 'change' }],
}

const BUSINESS_ERROR_TEXT: Record<string, string> = {
  REGISTRATION_DUPLICATE: '该手机号已报名本活动，同一活动只需报名一次。',
  REGISTRATION_FULL: '活动名额已满，很抱歉无法继续报名。',
  REGISTRATION_CLOSED: '报名时间已结束，无法提交报名。',
  ACTIVITY_UNAVAILABLE: '活动已结束或已失效，无法报名。',
  VALIDATION_ERROR: '填写的信息未通过校验，请检查后重试。',
  NETWORK_ERROR: '网络连接失败，请检查网络后重试。',
}

const currentActivity = computed(() => activity.data.value)
const remaining = computed(() =>
  currentActivity.value
    ? remainingSeats(currentActivity.value.capacity, currentActivity.value.registeredCount)
    : 0,
)
const closedText = computed(() =>
  currentActivity.value?.unavailableReason
    ? UNAVAILABLE_REASON_TEXT[currentActivity.value.unavailableReason]
    : '',
)
const notFound = computed(
  () => activity.error.value?.status === 404 || activity.error.value?.code === 'ACTIVITY_UNAVAILABLE',
)
const errorText = computed(() => {
  if (!submitError.value) return ''
  return BUSINESS_ERROR_TEXT[submitError.value.code] ?? submitError.value.message
})

async function submit(): Promise<void> {
  if (submitting.value || !currentActivity.value?.canRegister) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  submitError.value = null
  try {
    result.value = await createRegistration(activityId.value, {
      name: form.name.trim(),
      phone: form.phone.trim(),
      collegeId: form.collegeId,
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (caught) {
    submitError.value =
      caught instanceof ApiError
        ? caught
        : new ApiError('UNKNOWN_ERROR', '报名提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function backToActivity(): void {
  void router.push({ name: 'public-activity-promo', params: { id: activityId.value } })
}
</script>

<template>
  <div class="register">
    <div class="register__container">
      <div v-if="activity.loading.value && !currentActivity" class="register__loading app-card">
        <LoadingSkeleton variant="detail" label="正在加载活动信息" />
      </div>

      <EmptyState
        v-else-if="notFound"
        class="register__state app-card"
        title="活动不存在或尚未发布"
        description="报名页仅在活动发布后可用，请确认链接是否来自有效的宣传页。"
        icon="alert"
      >
        <template #actions>
          <ElButton @click="router.push('/admin/login')">前往管理后台</ElButton>
        </template>
      </EmptyState>

      <ErrorState
        v-else-if="activity.error.value"
        title="活动加载失败"
        :message="activity.error.value.message"
        :code="activity.error.value.code"
        @retry="activity.run"
      />

      <template v-else-if="currentActivity">
        <article v-if="result" class="register__success app-card">
          <span class="register__success-icon">
            <AppIcon name="check-circle" :size="34" />
          </span>
          <h1 class="register__success-title">报名成功</h1>
          <p class="register__success-sub">
            已为 {{ result.activityName }} 记录你的报名，请按活动时间地点准时参加。
          </p>

          <dl class="register__result-facts">
            <div>
              <dt>活动时间</dt>
              <dd>{{ formatActivityTime(result.startAt, currentActivity.endAt) }}</dd>
            </div>
            <div>
              <dt>活动地点</dt>
              <dd>{{ result.location }}</dd>
            </div>
            <div>
              <dt>报名编号</dt>
              <dd class="mono">{{ result.registrationId }}</dd>
            </div>
            <div>
              <dt>报名时间</dt>
              <dd>{{ formatDateTime(result.registeredAt) }}</dd>
            </div>
          </dl>

          <p class="register__success-note">
            现场签到只需提供报名手机号；请勿在同一活动重复提交报名。
          </p>

          <div class="register__success-actions">
            <ElButton type="primary" @click="backToActivity">返回活动详情</ElButton>
          </div>
        </article>

        <div v-else class="register__layout">
          <section class="app-card register__form-card">
            <header class="register__head">
              <p class="register__eyebrow">活动报名</p>
              <h1 class="register__title">{{ currentActivity.name }}</h1>
              <p class="register__meta">
                {{ formatActivityTime(currentActivity.startAt, currentActivity.endAt) }} ·
                {{ currentActivity.location }}
              </p>
            </header>

            <ElAlert
              v-if="closedText"
              class="register__closed"
              type="warning"
              show-icon
              :closable="false"
              :title="`当前不可报名：${closedText}`"
            />

            <ElAlert
              v-if="errorText"
              class="register__error"
              type="error"
              show-icon
              :closable="false"
              :title="errorText"
            >
              <template #default>
                <span class="register__error-code mono">业务码：{{ submitError?.code }}</span>
              </template>
            </ElAlert>

            <ElForm
              ref="formRef"
              :model="form"
              :rules="rules"
              label-position="top"
              size="large"
              class="register__form"
              @submit.prevent="submit"
            >
              <ElFormItem label="姓名" prop="name">
                <ElInput
                  v-model="form.name"
                  placeholder="请输入真实姓名"
                  maxlength="30"
                  :disabled="!currentActivity.canRegister"
                />
              </ElFormItem>

              <ElFormItem label="手机号" prop="phone">
                <ElInput
                  v-model="form.phone"
                  placeholder="用于报名确认与现场签到"
                  maxlength="11"
                  inputmode="numeric"
                  :disabled="!currentActivity.canRegister"
                />
              </ElFormItem>

              <ElFormItem label="所属学院" prop="collegeId">
                <ElSelect
                  v-model="form.collegeId"
                  placeholder="请选择学院"
                  filterable
                  :loading="colleges.loading.value"
                  :disabled="!currentActivity.canRegister"
                >
                  <ElOption
                    v-for="item in colleges.data.value?.items ?? []"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </ElSelect>
              </ElFormItem>

              <ElButton
                class="register__submit"
                type="primary"
                size="large"
                native-type="submit"
                :loading="submitting"
                :disabled="!currentActivity.canRegister"
              >
                {{ submitting ? '正在提交…' : currentActivity.canRegister ? '提交报名' : closedText }}
              </ElButton>

              <p class="register__privacy">
                报名信息仅用于本次活动的名单与签到核验，不会公开展示。
              </p>
            </ElForm>
          </section>

          <aside class="register__aside">
            <div class="app-card register__summary">
              <div class="register__summary-head">
                <span class="register__type">{{ currentActivity.typeName }}</span>
                <StatusTag :status="currentActivity.displayStatus" />
              </div>

              <dl class="register__summary-facts">
                <div>
                  <dt>活动时间</dt>
                  <dd>{{ formatActivityTime(currentActivity.startAt, currentActivity.endAt) }}</dd>
                </div>
                <div>
                  <dt>活动地点</dt>
                  <dd>{{ currentActivity.location }}</dd>
                </div>
                <div>
                  <dt>报名截止</dt>
                  <dd>{{ formatDateTime(currentActivity.registrationDeadline) }}</dd>
                </div>
                <div>
                  <dt>剩余名额</dt>
                  <dd>{{ remaining }} / {{ currentActivity.capacity }}</dd>
                </div>
              </dl>

              <ElButton text class="register__back" @click="backToActivity">
                <AppIcon name="arrow-left" :size="15" />
                <span>返回活动详情</span>
              </ElButton>
            </div>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.register {
  padding: var(--space-5) var(--space-4) var(--space-7);
}

.register__container {
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
}

.register__loading {
  padding: var(--space-5);
}

.register__state {
  padding: var(--space-4);
}

.register__layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(260px, 0.75fr);
  align-items: start;
  gap: var(--space-4);
}

.register__form-card {
  padding: var(--space-5);
}

.register__head {
  margin-bottom: var(--space-4);
}

.register__eyebrow {
  color: var(--brand);
  font-size: var(--font-size-xs);
  font-weight: 600;
  letter-spacing: 0.12em;
}

.register__title {
  margin-top: var(--space-1);
  font-size: var(--font-size-xl);
}

.register__meta {
  margin-top: var(--space-2);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.register__closed,
.register__error {
  margin-bottom: var(--space-4);
  border-radius: var(--radius-control);
}

.register__error-code {
  color: var(--text-subtle);
}

.register__form {
  display: flex;
  flex-direction: column;
}

.register__submit {
  width: 100%;
  margin-top: var(--space-2);
}

.register__privacy {
  margin-top: var(--space-3);
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
  line-height: 1.7;
}

.register__aside {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.register__summary {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
}

.register__summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.register__type {
  padding: 3px 9px;
  border-radius: var(--radius-pill);
  background: var(--brand-soft);
  color: var(--brand-600);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.register__summary-facts {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: 0;
}

.register__summary-facts div {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.register__summary-facts dt {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.register__summary-facts dd {
  margin: 0;
  font-size: var(--font-size-sm);
}

.register__back {
  align-self: flex-start;
  color: var(--text-muted);
}

.register__success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-7) var(--space-5);
  text-align: center;
}

.register__success-icon {
  display: grid;
  place-items: center;
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: var(--success-soft);
  color: var(--success);
  animation: pop-in 240ms var(--ease-out);
}

@keyframes pop-in {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.register__success-title {
  font-size: var(--font-size-2xl);
}

.register__success-sub {
  max-width: 46ch;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.register__result-facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--space-4);
  width: 100%;
  margin: var(--space-3) 0 0;
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--bg-subtle);
  text-align: left;
}

.register__result-facts dt {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.register__result-facts dd {
  margin: 2px 0 0;
  font-size: var(--font-size-sm);
  word-break: break-all;
}

.register__success-note {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.register__success-actions {
  margin-top: var(--space-2);
}

@media (max-width: 880px) {
  .register__layout {
    grid-template-columns: 1fr;
  }

  .register__aside {
    order: 2;
  }

  .register__form-card {
    padding: var(--space-4);
  }
}
</style>
