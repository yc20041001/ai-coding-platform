<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

defineProps<{
  showObservability?: boolean
}>()

const route = useRoute()

interface DockItem {
  path: string
  label: string
  icon: string
}

const items: DockItem[] = [
  { path: '/dashboard', label: 'Dashboard', icon: '◆' },
  { path: '/projects', label: 'Projects', icon: '◇' },
  { path: '/agents', label: 'Agents', icon: '◈' },
]

const isActive = (path: string) => {
  if (path === '/dashboard') return route.path === '/dashboard'
  return route.path.startsWith(path)
}
</script>

<template>
  <nav class="fdock">
    <div class="fdock-inner">
      <router-link
        v-for="item in items"
        :key="item.path"
        :to="item.path"
        class="fdock-item"
        :class="{ active: isActive(item.path) }"
      >
        <span class="fdock-icon">{{ item.icon }}</span>
        <span class="fdock-label">{{ item.label }}</span>
      </router-link>
      <router-link
        v-if="showObservability"
        to="/observability"
        class="fdock-item"
        :class="{ active: route.path.startsWith('/observability') }"
      >
        <span class="fdock-icon">◎</span>
        <span class="fdock-label">Observe</span>
      </router-link>
    </div>
  </nav>
</template>

<style scoped>
.fdock {
  position: fixed;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 200;
  display: flex;
}
.fdock-inner {
  display: flex;
  gap: 2px;
  padding: 4px;
  background: var(--app-panel);
  border: 1px solid var(--app-border-strong);
  border-radius: 14px;
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  box-shadow: var(--app-glow-strong), var(--app-shadow);
}
.fdock-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 11px;
  color: var(--app-text-muted);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
  position: relative;
}
.fdock-item:hover {
  color: var(--app-text-soft);
  background: var(--app-panel-hover);
  transform: translateY(-1px);
}
.fdock-item.active {
  color: var(--app-primary);
  background: var(--app-primary-soft);
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.15);
}
.fdock-icon {
  font-size: 14px;
}
.fdock-label {
  letter-spacing: 0.3px;
}
</style>
