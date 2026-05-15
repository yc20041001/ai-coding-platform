<script setup lang="ts">
defineProps<{
  tone?: 'primary' | 'success' | 'warning' | 'danger' | 'accent' | 'muted'
  active?: boolean
}>()
</script>

<template>
  <div class="ss" :class="[`ss--${tone || 'primary'}`, { 'ss--active': active }]">
    <div class="ss-track">
      <div class="ss-flow" />
      <div class="ss-flow ss-flow--slow" />
    </div>
  </div>
</template>

<style scoped>
.ss {
  height: 3px;
  border-radius: 10px;
  overflow: hidden;
  position: relative;
}

.ss-track {
  position: absolute;
  inset: 0;
  background: rgba(148, 163, 184, 0.08);
  border-radius: 10px;
  overflow: hidden;
}

.ss-flow {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    transparent 0%,
    var(--flow-color) 20%,
    var(--flow-color) 40%,
    transparent 60%
  );
  background-size: 200% 100%;
  animation: signalFlow 1.5s linear infinite;
  border-radius: 10px;
}

.ss-flow--slow {
  animation-duration: 3s;
  animation-delay: 0.5s;
  opacity: 0.5;
}

.ss--primary {
  --flow-color: var(--app-primary);
}
.ss--success {
  --flow-color: var(--app-success);
}
.ss--warning {
  --flow-color: var(--app-warning);
}
.ss--danger {
  --flow-color: var(--app-danger);
}
.ss--accent {
  --flow-color: var(--app-accent);
}
.ss--muted {
  --flow-color: var(--app-text-muted);
}

.ss--active .ss-track {
  background: rgba(56, 189, 248, 0.1);
  box-shadow: 0 0 10px rgba(56, 189, 248, 0.15);
}
</style>
