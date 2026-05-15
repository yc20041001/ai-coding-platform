<script setup lang="ts">
defineProps<{
  status: string
  tone?: 'success' | 'warning' | 'danger' | 'primary' | 'muted'
}>()
</script>

<template>
  <span class="sp" :class="`sp--${tone || 'primary'}`">
    <span class="sp-dot" />
    <span class="sp-ring" />
    <span class="sp-label">{{ status }}</span>
  </span>
</template>

<style scoped>
.sp {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  position: relative;
  padding: 2px 14px 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.sp-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  position: relative;
  z-index: 1;
  animation: statusPulse 2s ease-in-out infinite;
}

.sp-ring {
  position: absolute;
  left: 9px;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 13px;
  height: 13px;
  border-radius: 50%;
  border: 1.5px solid currentColor;
  opacity: 0.25;
  animation: statusPulse 2s ease-in-out infinite;
  animation-delay: 0.3s;
}

.sp-label {
  text-transform: uppercase;
}

.sp--primary {
  background: var(--app-primary-soft);
  color: var(--app-primary);
  border: 1px solid rgba(56, 189, 248, 0.2);
}
.sp--primary .sp-dot {
  background: var(--app-primary);
  box-shadow: 0 0 8px var(--app-primary-glow);
}

.sp--success {
  background: var(--app-success-soft);
  color: var(--app-success);
  border: 1px solid rgba(34, 197, 94, 0.2);
}
.sp--success .sp-dot {
  background: var(--app-success);
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.4);
}

.sp--warning {
  background: var(--app-warning-soft);
  color: var(--app-warning);
  border: 1px solid rgba(245, 158, 11, 0.2);
}
.sp--warning .sp-dot {
  background: var(--app-warning);
  box-shadow: 0 0 8px rgba(245, 158, 11, 0.4);
}

.sp--danger {
  background: var(--app-danger-soft);
  color: var(--app-danger);
  border: 1px solid rgba(239, 68, 68, 0.2);
}
.sp--danger .sp-dot {
  background: var(--app-danger);
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.4);
}

.sp--muted {
  background: rgba(148, 163, 184, 0.08);
  color: var(--app-text-muted);
  border: 1px solid var(--app-border);
}
.sp--muted .sp-dot {
  background: var(--app-text-muted);
  box-shadow: none;
  animation: none;
}
.sp--muted .sp-ring {
  display: none;
}
</style>
