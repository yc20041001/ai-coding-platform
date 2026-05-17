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
      <div class="tcb-divider" />
      <div class="tcb-env">
        <span class="tcb-env-dot" />
        <span class="tcb-env-text">{{ provider || '模拟网关' }}</span>
      </div>
    </div>

    <div class="tcb-center">
      <div class="tcb-cmd">
        <span class="tcb-cmd-icon">›</span>
        <span class="tcb-cmd-placeholder">指挥中心</span>
        <span class="tcb-cmd-hint">⌘K</span>
      </div>
    </div>

    <div class="tcb-right">
      <div class="tcb-status">
        <span class="tcb-dot" />
        <span class="tcb-status-text">系统在线</span>
      </div>
      <div class="tcb-divider" />
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
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  position: relative;
  z-index: 100;
  flex-shrink: 0;
}

.tcb::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    var(--app-primary) 20%,
    transparent 50%,
    var(--app-accent) 80%,
    transparent 100%
  );
  opacity: 0.3;
  pointer-events: none;
}

.tcb-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.tcb-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tcb-logo {
  font-size: 18px;
  color: var(--app-primary);
  filter: drop-shadow(0 0 8px rgba(56, 189, 248, 0.5));
}

.tcb-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--app-text);
  letter-spacing: 0.3px;
}

.tcb-divider {
  width: 1px;
  height: 20px;
  background: var(--app-border-strong);
}

.tcb-env {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tcb-env-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--app-text-muted);
}

.tcb-env-text {
  font-size: 11px;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  font-weight: 500;
}

.tcb-center {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0;
}

.tcb-cmd {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 16px;
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: 8px;
  transition: border-color 0.2s;
  cursor: default;
}

.tcb-cmd:hover {
  border-color: var(--app-border-strong);
}

.tcb-cmd-icon {
  color: var(--app-primary);
  font-size: 16px;
  font-weight: 700;
}

.tcb-cmd-placeholder {
  font-size: 12px;
  color: var(--app-text-muted);
  letter-spacing: 0.3px;
}

.tcb-cmd-hint {
  font-size: 10px;
  color: var(--app-text-muted);
  padding: 1px 5px;
  border: 1px solid var(--app-border);
  border-radius: 4px;
  font-family: monospace;
}

.tcb-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  justify-content: flex-end;
}

.tcb-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: var(--app-success-soft);
  border: 1px solid rgba(34, 197, 94, 0.18);
  border-radius: 20px;
}

.tcb-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--app-success);
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.5);
  animation: statusPulse 2s ease-in-out infinite;
}

.tcb-status-text {
  font-size: 11px;
  color: var(--app-success);
  font-weight: 600;
  letter-spacing: 0.3px;
}

.tcb-user {
  font-size: 12px;
  color: var(--app-text-soft);
  font-weight: 500;
}

.tcb-logout {
  font-size: 11px;
  color: var(--app-text-muted);
  background: transparent;
  border: 1px solid var(--app-border);
  border-radius: 6px;
  padding: 4px 12px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  letter-spacing: 0.3px;
}

.tcb-logout:hover {
  color: var(--app-danger);
  border-color: rgba(239, 68, 68, 0.3);
  background: var(--app-danger-soft);
}

@keyframes statusPulse {
  0%, 100% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.3); opacity: 1; }
}
</style>
