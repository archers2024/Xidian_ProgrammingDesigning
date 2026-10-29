<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElAlert,
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  type FormInstance,
  type FormRules,
} from 'element-plus'

import AppIcon from '@/components/common/AppIcon.vue'
import { ApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const mockMode = import.meta.env.VITE_USE_MOCK === 'true'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const errorMessage = ref('')

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules<typeof form> = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' },
    { min: 3, max: 40, message: '账号长度需为 3 至 40 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

async function submit(): Promise<void> {
  if (submitting.value || !formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  errorMessage.value = ''
  try {
    await auth.signIn({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    if (redirect.startsWith('/') && !redirect.startsWith('//')) {
      await router.replace(redirect)
    } else {
      await router.replace({ name: 'admin-dashboard' })
    }
  } catch (error) {
    errorMessage.value =
      error instanceof ApiError ? error.message : '登录失败，请检查网络后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login">
    <section class="login__brand">
      <div class="login__brand-top">
        <span class="login__logo">
          <AppIcon name="check" :size="20" :stroke-width="2.6" />
        </span>
        <div class="login__logo-text">
          <strong>校园活动</strong>
          <small>高校学生活动全流程管理系统</small>
        </div>
      </div>

      <div class="login__brand-body">
        <p class="login__eyebrow">PROGRAMMING PRACTICE · 第二题</p>
        <h1 class="login__headline">把一场活动，从海报做到签到。</h1>
        <p class="login__lead">
          活动创建与发布、手机扫码报名、动态二维码签到和名额统计集中在同一个系统里，
          现场演示时无需来回切换工具。
        </p>

        <ul class="login__features">
          <li>
            <AppIcon name="calendar" :size="18" />
            <span>组合筛选与四种活动状态，编辑不影响已有报名</span>
          </li>
          <li>
            <AppIcon name="qr" :size="18" />
            <span>发布即生成稳定的宣传链接与报名二维码</span>
          </li>
          <li>
            <AppIcon name="screen" :size="18" />
            <span>会场大屏动态签到码每 30 秒更新，旧码自动失效</span>
          </li>
        </ul>
      </div>

      <p class="login__brand-foot">程序设计实训大作业 · 高校学生活动全流程管理系统</p>
    </section>

    <section class="login__panel">
      <div class="login__card">
        <header class="login__card-head">
          <h2>管理员登录</h2>
          <p>使用学院分配的后台账号进入管理界面</p>
        </header>

        <ElAlert
          v-if="errorMessage"
          class="login__error"
          :title="errorMessage"
          type="error"
          :closable="false"
          show-icon
        />

        <ElForm
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="submit"
        >
          <ElFormItem label="管理员账号" prop="username">
            <ElInput
              v-model="form.username"
              autocomplete="username"
              placeholder="请输入账号"
              :disabled="submitting"
            >
              <template #prefix>
                <AppIcon name="users" :size="16" />
              </template>
            </ElInput>
          </ElFormItem>

          <ElFormItem label="登录密码" prop="password">
            <ElInput
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              show-password
              :disabled="submitting"
              @keyup.enter="submit"
            />
          </ElFormItem>

          <ElButton
            class="login__submit"
            type="primary"
            native-type="submit"
            size="large"
            :loading="submitting"
          >
            {{ submitting ? '正在登录…' : '登录' }}
          </ElButton>
        </ElForm>

        <p class="login__mock">
          演示账号 <strong>admin</strong> / <strong>admin123</strong>
          <template v-if="mockMode">。当前为模拟模式，刷新页面会恢复初始数据。</template>
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
  min-height: 100vh;
  background: var(--bg-surface);
}

.login__brand {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  overflow: hidden;
  padding: var(--space-7) var(--space-6);
  background: radial-gradient(circle at 18% 12%, rgba(14, 159, 159, 0.42), transparent 46%),
    radial-gradient(circle at 82% 88%, rgba(29, 78, 216, 0.55), transparent 52%),
    linear-gradient(150deg, #0c1630 0%, #142a63 58%, #1d4ed8 130%);
  color: #e8eefc;
}

.login__brand::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(circle at 30% 30%, black, transparent 72%);
  pointer-events: none;
}

.login__brand-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.login__logo {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.login__logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.login__logo-text strong {
  color: #fff;
  font-size: var(--font-size-md);
}

.login__logo-text small {
  color: rgba(232, 238, 252, 0.7);
  font-size: var(--font-size-xs);
}

.login__brand-body {
  position: relative;
  z-index: 1;
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-4);
  max-width: 520px;
}

.login__eyebrow {
  color: rgba(232, 238, 252, 0.62);
  font-size: var(--font-size-xs);
  font-weight: 600;
  letter-spacing: 0.16em;
}

.login__headline {
  color: #fff;
  font-size: clamp(30px, 3.4vw, 42px);
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.login__lead {
  color: rgba(232, 238, 252, 0.78);
  font-size: var(--font-size-base);
  line-height: 1.75;
}

.login__features {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.login__features li {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  color: rgba(232, 238, 252, 0.86);
  font-size: var(--font-size-sm);
}

.login__features svg {
  margin-top: 2px;
  color: #7fe3dc;
}

.login__brand-foot {
  position: relative;
  z-index: 1;
  color: rgba(232, 238, 252, 0.46);
  font-size: var(--font-size-xs);
}

.login__panel {
  display: grid;
  place-items: center;
  padding: var(--space-6) var(--space-5);
  background: var(--bg-page);
}

.login__card {
  width: 100%;
  max-width: 400px;
  padding: var(--space-6);
  border: 1px solid var(--border);
  border-radius: var(--radius-dialog);
  background: var(--bg-surface);
  box-shadow: var(--shadow-card);
}

.login__card-head {
  margin-bottom: var(--space-5);
}

.login__card-head h2 {
  font-size: var(--font-size-xl);
}

.login__card-head p {
  margin-top: var(--space-2);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.login__error {
  margin-bottom: var(--space-4);
}

.login__submit {
  width: 100%;
  margin-top: var(--space-2);
}

.login__mock {
  margin-top: var(--space-5);
  padding: var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--brand-soft);
  color: var(--brand-600);
  font-size: var(--font-size-xs);
  text-align: center;
}

@media (max-width: 900px) {
  .login {
    grid-template-columns: 1fr;
  }

  .login__brand {
    gap: var(--space-4);
    padding: var(--space-5) var(--space-4);
  }

  .login__brand-body {
    max-width: none;
  }

  .login__headline {
    font-size: 26px;
  }

  .login__lead,
  .login__features,
  .login__brand-foot {
    display: none;
  }

  .login__panel {
    align-items: start;
    padding: var(--space-5) var(--space-4) var(--space-7);
  }

  .login__card {
    padding: var(--space-5);
  }
}
</style>
