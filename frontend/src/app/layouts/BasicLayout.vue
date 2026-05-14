<script setup lang="ts">
import { computed } from 'vue'
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

.app-workspace {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 76px;
  position: relative;
  z-index: 1;
}
</style>
