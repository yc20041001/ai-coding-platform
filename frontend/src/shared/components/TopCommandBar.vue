<script setup lang="ts">
defineProps<{
  username?: string
  provider?: string
}>()

defineEmits<{
  logout: []
}>()
</script>

<template>
  <header class="tcb">
    <div class="tcb-left">
      <div class="tcb-brand">
        <span class="tcb-logo">◈</span>
        <span class="tcb-name">AI Coding Platform</span>
      </div>
    </div>
    <div class="tcb-center">
      <div class="tcb-status">
        <span class="tcb-dot" />
        <span class="tcb-status-text">Online · {{ provider || 'Mock Gateway' }}</span>
      </div>
    </div>
    <div class="tcb-right">
      <span class="tcb-user">{{ username || 'admin' }}</span>
      <button class="tcb-logout" data-testid="btn-logout" @click="$emit('logout')">退出</button>
    </div>
  </header>
</template>

<style scoped>
.tcb {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 20px;
  background: var(--app-panel);
  border-bottom: 1px solid var(--app-border);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  position: relative;
  z-index: 100;
  flex-shrink: 0;
}
.tcb-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}
.tcb-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tcb-logo {
  font-size: 20px;
  color: var(--app-primary);
  filter: drop-shadow(0 0 8px rgba(56, 189, 248, 0.5));
}
.tcb-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--app-text);
  letter-spacing: 0.3px;
}
.tcb-center {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0;
}
.tcb-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 14px;
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid rgba(34, 197, 94, 0.18);
  border-radius: 20px;
}
.tcb-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--app-success);
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.5);
  animation: glowPulse 2s ease-in-out infinite;
}
.tcb-status-text {
  font-size: 12px;
  color: var(--app-success);
  font-weight: 500;
}
.tcb-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  justify-content: flex-end;
}
.tcb-user {
  font-size: 13px;
  color: var(--app-text-soft);
  font-weight: 500;
}
.tcb-logout {
  font-size: 12px;
  color: var(--app-text-muted);
  background: transparent;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  padding: 4px 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.tcb-logout:hover {
  color: var(--app-danger);
  border-color: rgba(239, 68, 68, 0.3);
  background: var(--app-danger-soft);
}

@keyframes glowPulse {
  0%, 100% { box-shadow: 0 0 8px rgba(34, 197, 94, 0.5); }
  50% { box-shadow: 0 0 16px rgba(34, 197, 94, 0.8); }
}
</style>
