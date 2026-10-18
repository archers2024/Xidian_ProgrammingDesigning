<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton } from 'element-plus'

import AppIcon from '@/components/common/AppIcon.vue'
import PlaceholderPanel from '@/components/common/PlaceholderPanel.vue'

const route = useRoute()
const router = useRouter()

const sessionId = computed(() => String(route.params.sessionId ?? ''))
const hasToken = computed(() => typeof route.query.token === 'string' && route.query.token.length > 0)

function backToAdmin(): void {
  void router.push({ name: 'admin-login' })
}
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
          你正在通过会场二维码进入签到页。签到需要提供报名时填写的手机号，并校验签到码是否仍在有效期内。
        </p>

        <dl class="checkin__facts">
          <div>
            <dt>签到会话</dt>
            <dd class="mono">{{ sessionId || '未提供' }}</dd>
          </div>
          <div>
            <dt>扫码凭证</dt>
            <dd>
              <span v-if="hasToken" class="checkin__ok">已携带动态凭证</span>
              <span v-else class="checkin__warn">缺少凭证，请重新扫描会场二维码</span>
            </dd>
          </div>
          <div>
            <dt>签到状态</dt>
            <dd>待接入</dd>
          </div>
        </dl>

        <PlaceholderPanel
          class="checkin__panel"
          title="手机签到流程"
          description="页面路由、布局与凭证参数解析已经就绪；签到接口与动态码校验在批次6接入，届时无需修改页面结构。"
          :pending="[
            '调用打卡上下文接口，校验动态凭证、会话版本与签到时段',
            '提交姓名手机号完成签到，重复签到返回原签到时间而非新增记录',
            '30 秒凭证过期后提示重新扫描现场新码，不在公开页面自动领取新凭证',
          ]"
        />

        <div class="checkin__actions">
          <ElButton type="primary" size="large" disabled>提交签到（批次6开放）</ElButton>
          <ElButton text @click="backToAdmin">管理员入口</ElButton>
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

.checkin__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  margin-top: var(--space-2);
}
</style>
