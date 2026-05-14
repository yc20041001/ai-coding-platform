<script setup lang="ts">
defineProps<{
  status: 'online' | 'offline' | 'streaming' | 'idle'
  label?: string
}>()
</script>

<template>
  <span class="rb" :class="`rb--${status}`">
    <span class="rb-dot" />
    <span class="rb-label">{{ label || status }}</span>
  </span>
</template>

<style scoped>
.rb {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}
.rb-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.rb--online {
  background: var(--app-success-soft);
  color: var(--app-success);
  border: 1px solid rgba(34, 197, 94, 0.2);
}
.rb--online .rb-dot {
  background: var(--app-success);
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.5);
}
.rb--streaming {
  background: var(--app-primary-soft);
  color: var(--app-primary);
  border: 1px solid rgba(56, 189, 248, 0.2);
}
.rb--streaming .rb-dot {
  background: var(--app-primary);
  box-shadow: 0 0 6px rgba(56, 189, 248, 0.5);
  animation: glowPulse 1.5s ease-in-out infinite;
}
.rb--idle {
  background: rgba(148, 163, 184, 0.08);
  color: var(--app-text-muted);
  border: 1px solid var(--app-border);
}
.rb--idle .rb-dot {
  background: var(--app-text-muted);
}
.rb--offline {
  background: var(--app-danger-soft);
  color: var(--app-danger);
  border: 1px solid rgba(239, 68, 68, 0.2);
}
.rb--offline .rb-dot {
  background: var(--app-danger);
}
@keyframes glowPulse {
  0%, 100% { box-shadow: 0 0 6px rgba(56, 189, 248, 0.5); }
  50% { box-shadow: 0 0 12px rgba(56, 189, 248, 0.8); }
}
</style>
