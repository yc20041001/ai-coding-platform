<script setup lang="ts">
withDefaults(defineProps<{
  mode?: 'demo' | 'mock' | 'live'
  label?: string
}>(), {
  mode: 'demo',
})
</script>

<template>
  <span
    class="demo-badge"
    :class="`demo-badge--${mode}`"
  >
    <span class="demo-badge-dot" />
    <span class="demo-badge-label">{{ label || (mode === 'mock' ? 'MOCK' : mode === 'live' ? 'LIVE' : 'DEMO') }}</span>
  </span>
</template>

<style scoped>
.demo-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 9px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  font-family: monospace;
}

.demo-badge--demo {
  background: rgba(64, 158, 255, 0.12);
  color: var(--app-cyan, #409EFF);
  border: 1px solid rgba(64, 158, 255, 0.25);
}

.demo-badge--mock {
  background: rgba(230, 162, 60, 0.12);
  color: var(--app-amber, #E6A23C);
  border: 1px solid rgba(230, 162, 60, 0.25);
}

.demo-badge--live {
  background: rgba(103, 194, 58, 0.12);
  color: var(--app-green, #67C23A);
  border: 1px solid rgba(103, 194, 58, 0.25);
}

.demo-badge-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}
</style>
