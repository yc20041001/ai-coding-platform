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
  { path: '/github', label: 'GitHub', icon: '⬡' },
]

const isActive = (path: string) => {
  if (path === '/dashboard') return route.path === '/dashboard'
  return route.path.startsWith(path)
}
</script>

<template>
  <nav class="fdock">
    <div class="fdock-glow" />
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
        <span v-if="isActive(item.path)" class="fdock-bar" />
      </router-link>

      <template v-if="showObservability">
        <div class="fdock-sep" />
        <router-link
          to="/observability"
          class="fdock-item"
          :class="{ active: route.path.startsWith('/observability') }"
        >
          <span class="fdock-icon">◎</span>
          <span class="fdock-label">Observe</span>
          <span v-if="route.path.startsWith('/observability')" class="fdock-bar" />
        </router-link>
        <router-link
          to="/model-gateway"
          class="fdock-item"
          :class="{ active: route.path.startsWith('/model-gateway') }"
        >
          <span class="fdock-icon">◉</span>
          <span class="fdock-label">Models</span>
          <span v-if="route.path.startsWith('/model-gateway')" class="fdock-bar" />
        </router-link>
      </template>
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
  transition: bottom 0.22s var(--app-ease-out), opacity 0.22s var(--app-ease-out);
}

.fdock-glow {
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 80%;
  height: 20px;
  background: radial-gradient(ellipse 100% 100% at 50% 0%, rgba(56, 189, 248, 0.1) 0%, transparent 70%);
  filter: blur(12px);
  pointer-events: none;
}

.fdock-inner {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 5px 6px;
  background: var(--app-panel);
  border: 1px solid var(--app-border-strong);
  border-radius: 16px;
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  box-shadow: var(--app-glow-strong), var(--app-shadow-lg);
  position: relative;
}

.fdock-sep {
  width: 1px;
  height: 22px;
  background: var(--app-border-strong);
  margin: 0 4px;
}

.fdock-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 12px;
  color: var(--app-text-muted);
  text-decoration: none;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s var(--app-ease-out);
  position: relative;
  overflow: hidden;
}

.fdock-item::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, transparent 40%, rgba(56, 189, 248, 0.04) 50%, transparent 60%);
  background-size: 200% 200%;
  opacity: 0;
  transition: opacity 0.3s;
}

.fdock-item:hover {
  color: var(--app-text-soft);
  background: var(--app-panel-hover);
  transform: translateY(-2px);
}

.fdock-item:hover::before {
  opacity: 1;
  animation: shimmer 2s ease-in-out infinite;
}

.fdock-item.active {
  color: var(--app-primary);
  background: var(--app-primary-soft);
  box-shadow:
    0 0 16px var(--app-primary-glow),
    inset 0 0 8px rgba(56, 189, 248, 0.04);
}

.fdock-icon {
  font-size: 14px;
  transition: transform 0.2s var(--app-ease-spring);
}

.fdock-item:hover .fdock-icon {
  transform: scale(1.15);
}

.fdock-item.active .fdock-icon {
  filter: drop-shadow(0 0 6px rgba(56, 189, 248, 0.5));
}

.fdock-label {
  letter-spacing: 0.4px;
  font-weight: 500;
}

.fdock-bar {
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 2px;
  background: var(--app-primary);
  border-radius: 2px;
  box-shadow: 0 0 10px var(--app-primary-glow);
  animation: barReveal 0.3s var(--app-ease-spring);
}

@keyframes barReveal {
  from { width: 0; opacity: 0; }
  to { width: 20px; opacity: 1; }
}

@keyframes shimmer {
  0% { background-position: -100% 0; }
  100% { background-position: 200% 0; }
}

@media (max-height: 820px) {
  .fdock {
    bottom: -18px;
    opacity: 0.82;
  }

  .fdock:hover,
  .fdock:focus-within {
    bottom: 12px;
    opacity: 1;
  }
}
</style>
