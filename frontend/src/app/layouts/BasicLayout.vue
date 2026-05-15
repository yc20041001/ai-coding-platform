<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/modules/auth/store'
import TopCommandBar from '@/shared/components/TopCommandBar.vue'
import FloatingDock from '@/shared/components/FloatingDock.vue'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <div class="app-bg-mesh" />
    <TopCommandBar
      :username="authStore.username"
      provider="Mock Gateway"
      @logout="handleLogout"
    />
    <main class="app-workspace">
      <router-view />
    </main>
    <FloatingDock :show-observability="authStore.isAdmin" />
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  position: relative;
}

.app-bg-mesh {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(ellipse 90% 70% at 20% 10%, rgba(56, 189, 248, 0.015) 0%, transparent 50%),
    radial-gradient(ellipse 70% 80% at 80% 80%, rgba(139, 92, 246, 0.015) 0%, transparent 50%),
    radial-gradient(ellipse 50% 50% at 50% 50%, rgba(34, 197, 94, 0.01) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.app-workspace {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 128px;
  position: relative;
  z-index: 1;
}
</style>
