<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="brand">
        <div class="logo">校</div>
        <div>
          <h1>{{ t('login.title') }}</h1>
          <p>{{ t('login.subtitle') }}</p>
        </div>
      </div>

      <div class="tips">
        <div class="tip-item"><el-icon><Postcard /></el-icon><span>访客预约 · 通行码 · 黑名单拦截</span></div>
        <div class="tip-item"><el-icon><AlarmClock /></el-icon><span>消防一物一码 · 隐患整改闭环</span></div>
        <div class="tip-item"><el-icon><Compass /></el-icon><span>巡逻打卡 · 事件工单 · 应急联动</span></div>
        <div class="tip-item"><el-icon><Van /></el-icon><span>车牌识别对接 · 固定/临时车牌下发</span></div>
      </div>
      <div class="domain">微信合法域名：wx-bwc-api.greathiit.com</div>
    </div>

    <div class="login-form-wrap">
      <el-card class="login-card" shadow="never">
        <div class="card-head">
          <h2>账号登录</h2>
          <span class="muted">多租户 SaaS 平台</span>
        </div>
        <el-tabs v-model="loginType" stretch>
          <el-tab-pane :label="t('login.tenant')" name="tenant" />
          <el-tab-pane :label="t('login.platform')" name="platform" />
        </el-tabs>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
          <el-form-item :label="t('login.username')" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item :label="t('login.password')" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item v-if="loginType === 'tenant'" :label="t('login.tenantCode')">
            <el-input v-model="form.tenantCode" size="large" placeholder="DEMO / UNIV，留空自动匹配" />
          </el-form-item>
          <el-button class="full" type="primary" size="large" :loading="loading" @click="submit">
            {{ t('login.submit') }}
          </el-button>
        </el-form>
        <div class="login-foot">
          <span>{{ t('login.tip') }}</span>
          <span class="muted">v1.0.0</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard, AlarmClock, Compass, Van } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/store/auth'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)
const loginType = ref<'tenant' | 'platform'>('tenant')
const form = reactive({ username: '', password: '', tenantCode: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await auth.login({
      username: form.username,
      password: form.password,
      tenantCode: form.tenantCode || undefined,
      loginType: loginType.value
    })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch {
    // 失败提示统一由 http 响应拦截器展示，避免此处二次吞掉错误
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  background: radial-gradient(circle at 20% 20%, #1e3a8a 0%, #0f172a 45%, #0b1220 100%);
}
.login-panel {
  padding: 64px 72px;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 38px;
}
.brand { display: flex; gap: 18px; align-items: center; }
.logo {
  width: 64px; height: 64px; border-radius: 16px; display: grid; place-items: center;
  background: linear-gradient(135deg, #60a5fa, #2563eb); font-size: 30px; font-weight: 800;
  box-shadow: 0 10px 30px rgba(37, 99, 235, 0.4);
}
.brand h1 { margin: 0; font-size: 30px; letter-spacing: 1px; }
.brand p { margin: 8px 0 0; color: #94a3b8; }
.tips { display: grid; gap: 16px; max-width: 520px; }
.tip-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-radius: 12px;
  background: rgba(148, 163, 184, 0.08); border: 1px solid rgba(148, 163, 184, 0.15);
  color: #cbd5e1;
}
.domain { color: #64748b; font-size: 12px; letter-spacing: 0.5px; }
.login-form-wrap { display: grid; place-items: center; background: #f8fafc; padding: 32px; }
.login-card { width: 400px; border-radius: 18px; padding: 10px 8px 4px; box-shadow: 0 24px 60px rgba(15, 23, 42, 0.15); }
.card-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 8px; }
.card-head h2 { margin: 0; font-size: 22px; }
.login-foot { display: flex; justify-content: space-between; margin-top: 18px; font-size: 12px; color: #94a3b8; }
.full { width: 100%; margin-top: 6px; }
@media (max-width: 900px) {
  .login-page { grid-template-columns: 1fr; }
  .login-panel { display: none; }
  .login-form-wrap { min-height: 100vh; }
}
</style>
