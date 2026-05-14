<script setup lang="ts">
defineProps<{
  title?: string
  glow?: boolean
}>()
</script>

<template>
  <div class="tech-panel" :class="{ 'tech-panel--glow': glow }">
    <div v-if="title" class="tech-panel__header">{{ title }}</div>
    <div class="tech-panel__body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.tech-panel {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.tech-panel--glow {
  box-shadow: var(--app-glow);
}
.tech-panel--glow:hover {
  border-color: var(--app-border-strong);
  box-shadow: var(--app-glow-strong);
}
.tech-panel__header {
  padding: 14px 20px;
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
  border-bottom: 1px solid var(--app-border);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.tech-panel__body {
  padding: 20px;
}
</style>
