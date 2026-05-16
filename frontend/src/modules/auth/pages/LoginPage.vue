<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/modules/auth/store'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  email: 'admin@example.com',
  password: 'Admin@123456',
})

const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  const ok = await authStore.loginAction({
    email: form.email,
    password: form.password,
  })
  loading.value = false
  if (ok) {
    ElMessage.success('Login successful')
    router.replace('/')
  } else {
    errorMsg.value = 'Login failed. Check email and password.'
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="login-brand-glow" />
      <div class="login-brand-content">
        <div class="login-logo">◈</div>
        <h1 class="login-title">AI Coding Platform</h1>
        <p class="login-subtitle">Enterprise AI-Powered Development Workspace</p>
        <div class="login-features">
          <div class="login-feat">
            <span class="login-feat-dot" />
            <span>AI Agent Task Orchestration</span>
          </div>
          <div class="login-feat">
            <span class="login-feat-dot" />
            <span>RAG Knowledge Base Augmentation</span>
          </div>
          <div class="login-feat">
            <span class="login-feat-dot" />
            <span>Real-time SSE Streaming Collaboration</span>
          </div>
        </div>
      </div>
    </div>
    <div class="login-form-side">
      <div class="login-card">
        <h2 class="login-card-title">Console Login</h2>
        <el-form @submit.prevent="handleLogin" label-position="top" size="large">
          <el-form-item label="Email">
            <el-input v-model="form.email" placeholder="admin@example.com" data-testid="login-email" />
          </el-form-item>
          <el-form-item label="Password">
            <el-input v-model="form.password" type="password" placeholder="Admin@123456" show-password data-testid="login-password" />
          </el-form-item>
          <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" style="margin-bottom:16px" data-testid="login-error" />
          <el-button type="primary" native-type="submit" :loading="loading" style="width:100%" data-testid="login-submit">
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </el-button>
        </el-form>
        <div class="login-hint">
          <span>Demo account: admin@example.com / Admin@123456</span>
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
