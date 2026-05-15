<script setup lang="ts">
defineProps<{
  tone?: 'primary' | 'accent' | 'success'
}>()
</script>

<template>
  <div class="do" :class="`do--${tone || 'primary'}`">
    <svg class="do-svg" viewBox="0 0 120 120" fill="none">
      <circle cx="60" cy="60" r="52" class="do-track" />
      <circle cx="60" cy="60" r="52" class="do-ring" />
      <circle cx="60" cy="60" r="16" class="do-center" />
      <circle cx="60" cy="8" r="4" class="do-node" />
      <circle cx="112" cy="60" r="3" class="do-node do-node--slow" />
      <circle cx="60" cy="112" r="3.5" class="do-node do-node--slow" />
    </svg>
  </div>
</template>

<style scoped>
.do {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.do-svg {
  width: 100px;
  height: 100px;
}

.do-track {
  fill: none;
  stroke: rgba(148, 163, 184, 0.06);
  stroke-width: 8;
}

.do-ring {
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-dasharray: 4 8;
  opacity: 0.2;
  animation: dataFlow 3s linear infinite;
}

.do-center {
  fill: currentColor;
  opacity: 0.15;
}

.do-node {
  fill: currentColor;
  opacity: 0.7;
  animation: statusPulse 2s ease-in-out infinite;
}

.do-node--slow {
  animation-duration: 3s;
  animation-delay: 1s;
}

.do--primary {
  color: var(--app-primary);
}
.do--accent {
  color: var(--app-accent);
}
.do--success {
  color: var(--app-success);
}
</style>
