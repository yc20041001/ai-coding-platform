<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/modules/auth/store'
import { getCaptcha, type CaptchaResponse } from '@/modules/auth/api'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  email: 'admin@example.com',
  password: 'Admin@123456',
  captchaId: '',
  captchaCode: '',
})

const loading = ref(false)
const errorMsg = ref('')
const captchaLoading = ref(false)
const captchaImage = ref('')
const captchaError = ref('')

async function loadCaptcha() {
  captchaLoading.value = true
  captchaError.value = ''
  try {
    const res = await getCaptcha()
    const data: CaptchaResponse = res.data.data
    form.captchaId = data.captchaId
    captchaImage.value = data.imageBase64
  } catch {
    captchaError.value = '验证码加载失败，请刷新'
  } finally {
    captchaLoading.value = false
  }
}

async function handleLogin() {
  errorMsg.value = ''
  // If we have captcha loaded (image or ID) and no error, proceed
  if (captchaImage.value || form.captchaId) {
    // captcha ready
  } else if (captchaError.value) {
    // captcha failed to load, show error and return
    return
  } else {
    // captcha not yet loaded (initial state), proceed anyway (captcha might be disabled)
  }
  loading.value = true
  const ok = await authStore.loginAction({
    email: form.email,
    password: form.password,
    captchaId: form.captchaId,
    captchaCode: form.captchaCode,
  })
  loading.value = false
  if (ok) {
    ElMessage.success('登录成功')
    router.replace('/')
  } else {
    errorMsg.value = '登录失败，请检查邮箱、密码和验证码。'
    form.captchaCode = ''
    loadCaptcha()
  }
}

onMounted(() => {
  loadCaptcha()
})
</script>

<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="login-brand-glow" />
      <div class="login-brand-content">
        <div class="login-logo">◈</div>
        <h1 class="login-title">AI Coding Platform</h1>
        <p class="login-subtitle">企业级 AI 智能研发工作台</p>
        <div class="login-features">
          <div class="login-feat">
            <span class="login-feat-dot" />
            <span>AI 智能体任务编排</span>
          </div>
          <div class="login-feat">
            <span class="login-feat-dot" />
            <span>RAG 知识库增强</span>
          </div>
          <div class="login-feat">
            <span class="login-feat-dot" />
            <span>实时 SSE 流式协作</span>
          </div>
        </div>
      </div>
    </div>
    <div class="login-form-side">
      <div class="login-card">
        <h2 class="login-card-title">控制台登录</h2>
        <el-form @submit.prevent="handleLogin" label-position="top" size="large">
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="admin@example.com" data-testid="login-email" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" placeholder="Admin@123456" show-password data-testid="login-password" />
          </el-form-item>
          <el-form-item label="验证码">
            <div class="captcha-row">
              <el-input
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                maxlength="4"
                style="flex:1"
                data-testid="login-captcha-code"
              />
              <div class="captcha-image-wrapper" :class="{ 'is-loading': captchaLoading }" @click="loadCaptcha" title="点击刷新验证码">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-image" />
                <span v-else-if="captchaError" class="captcha-error-text">{{ captchaError }}</span>
                <span v-else class="captcha-placeholder">加载中...</span>
              </div>
            </div>
            <div v-if="captchaError" class="captcha-hint-error">{{ captchaError }}</div>
          </el-form-item>
          <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" style="margin-bottom:16px" data-testid="login-error" />
          <el-button type="primary" native-type="submit" :loading="loading" style="width:100%" data-testid="login-submit">
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </el-button>
        </el-form>
        <div class="login-hint">
          <span>演示账号：admin@example.com / Admin@123456</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  background: #070b18;
  position: relative;
  overflow: hidden;
}

/* ---- Brand Side ---- */
.login-brand {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.login-brand-glow {
  position: absolute;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.12) 0%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}
.login-brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 40px;
}
.login-logo {
  font-size: 56px;
  color: var(--app-primary);
  filter: drop-shadow(0 0 20px rgba(56, 189, 248, 0.5));
  margin-bottom: 16px;
}
.login-title {
  font-size: 32px;
  font-weight: 800;
  color: var(--app-text);
  margin: 0;
  letter-spacing: 0.5px;
}
.login-subtitle {
  color: var(--app-text-muted);
  font-size: 16px;
  margin-top: 8px;
}
.login-features {
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}
.login-feat {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--app-text-soft);
  font-size: 14px;
}
.login-feat-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--app-primary);
  box-shadow: 0 0 6px rgba(56, 189, 248, 0.5);
}

/* ---- Form Side ---- */
.login-form-side {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: rgba(11, 17, 32, 0.6);
  border-left: 1px solid var(--app-border);
  backdrop-filter: blur(20px);
}
.login-card {
  width: 100%;
  max-width: 380px;
}
.login-card-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--app-text);
  margin-bottom: 28px;
}
.login-hint {
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: var(--app-text-muted);
}

/* ---- Captcha ---- */
.captcha-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.captcha-image-wrapper {
  width: 120px;
  height: 40px;
  border: 1px solid var(--app-border);
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  background: #f0f4f8;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
  flex-shrink: 0;
}
.captcha-image-wrapper:hover {
  border-color: var(--app-primary);
}
.captcha-image-wrapper.is-loading {
  cursor: default;
}
.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: fill;
}
.captcha-placeholder {
  font-size: 11px;
  color: #999;
}
.captcha-error-text {
  font-size: 10px;
  color: #ff4d4f;
  text-align: center;
  padding: 0 4px;
}
.captcha-hint-error {
  font-size: 11px;
  color: #ff4d4f;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }
  .login-brand {
    flex: none;
    padding: 40px 20px;
  }
  .login-form-side {
    width: 100%;
    flex: 1;
    border-left: none;
    border-top: 1px solid var(--app-border);
  }
}
</style>