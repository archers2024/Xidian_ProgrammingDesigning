<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElButton, ElInput } from 'element-plus'

import AppIcon from '@/components/common/AppIcon.vue'
import { ApiError } from '@/api/http'
import { fetchCheckinContext, submitCheckin } from '@/api/public/checkin'
import type { CheckinContext, CheckinResult } from '@/types/checkin'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()

const sessionId = computed(() => String(route.params.sessionId ?? ''))
const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''))
const context = ref<CheckinContext | null>(null)
const contextError = ref<ApiError | null>(null)
const loading = ref(true)
const phone = ref('')
const submitting = ref(false)
const submitError = ref('')
const result = ref<CheckinResult | null>(null)
const secondsLeft = ref(0)
let timer: number | undefined

async function loadContext(): Promise<void> {
  loading.value = true
  contextError.value = null
  try {
    context.value = await fetchCheckinContext({
      sessionId: sessionId.value,
      token: token.value || undefined,
    })
    secondsLeft.value = context.value.expiresInSeconds
  } catch (caught) {
    context.value = null
    contextError.value =
      caught instanceof ApiError ? caught : new ApiError('UNKNOWN_ERROR', '签到信息加载失败')
  } finally {
    loading.value = false
  }
}

async function submit(): Promise<void> {
  submitError.value = ''
  submitting.value = true
  try {
    result.value = await submitCheckin({
      sessionId: sessionId.value,
      phone: phone.value.trim(),
      token: token.value || undefined,
    })
  } catch (caught) {
    submitError.value = caught instanceof ApiError ? caught.message : '签到失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void loadContext()
  timer = window.setInterval(() => {
    if (secondsLeft.value > 0) secondsLeft.value -= 1
  }, 1000)
})

onUnmounted(() => {
  window.clearInterval(timer)
})
</script>

<template>
  <div class="checkin">
    <div class="checkin__container">
      <section class="app-card checkin__card">
        <span class="checkin__icon">
          <AppIcon name="qr" :size="30" />
        </span>
        <h1 class="checkin__title">活动签到</h1>
        <p class="checkin__lead">
          请输入报名时填写的手机号。动态二维码过期后需要重新扫描会场上的新码，本页不会自动领取新凭证。
        </p>

        <p v-if="loading" class="checkin__lead">正在核验签到码…</p>
        <p v-else-if="contextError" class="checkin__warn">{{ contextError.message }}</p>

        <dl v-else-if="context" class="checkin__facts">
          <div>
            <dt>活动</dt>
            <dd>{{ context.activityName }}</dd>
          </div>
          <div>
            <dt>签到方式</dt>
            <dd>{{ context.mode === 'DYNAMIC' ? '动态二维码' : '基本签到' }}</dd>
          </div>
          <div>
            <dt>有效至</dt>
            <dd>{{ formatDateTime(context.validUntil) }} · 剩余 {{ secondsLeft }} 秒</dd>
          </div>
        </dl>

        <form v-if="context && !result" class="checkin__form" @submit.prevent="submit">
          <label class="checkin__label" for="checkin-phone">报名手机号</label>
          <ElInput
            id="checkin-phone"
            v-model="phone"
            maxlength="11"
            placeholder="11 位手机号"
            size="large"
          />
          <p v-if="submitError" class="checkin__warn">{{ submitError }}</p>
          <div class="checkin__actions">
            <ElButton type="primary" size="large" native-type="submit" :loading="submitting">
              提交签到
            </ElButton>
          </div>
        </form>

        <div v-else-if="result" class="checkin__result">
          <p class="checkin__ok">
            {{ result.alreadyCheckedIn ? '你已经签到过了' : '签到成功' }}
          </p>
          <p>{{ result.activityName }}</p>
          <p class="checkin__lead">签到时间 {{ formatDateTime(result.checkedAt) }}</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.checkin {
  padding: var(--space-5) var(--space-4) var(--space-7);
}

.checkin__container {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
}

.checkin__card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-6) var(--space-5);
  text-align: center;
}

.checkin__icon {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: var(--brand-soft);
  color: var(--brand);
}

.checkin__title {
  font-size: var(--font-size-2xl);
}

.checkin__lead {
  max-width: 52ch;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.checkin__facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: var(--space-3);
  width: 100%;
  margin: var(--space-2) 0 0;
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--bg-subtle);
  text-align: left;
}

.checkin__facts dt {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.checkin__facts dd {
  margin: 2px 0 0;
  font-size: var(--font-size-sm);
  word-break: break-all;
}

.checkin__ok {
  color: var(--success);
}

.checkin__warn {
  color: var(--warning);
}

.checkin__panel {
  width: 100%;
  text-align: left;
}

.checkin__form,
.checkin__result {
  width: 100%;
  max-width: 420px;
  text-align: left;
}

.checkin__label {
  display: block;
  margin-bottom: var(--space-2);
  font-size: var(--font-size-sm);
}

.checkin__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
</style>
