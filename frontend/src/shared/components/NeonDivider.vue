<script setup lang="ts">
defineProps<{
  tone?: 'primary' | 'accent' | 'muted'
}>()
</script>

<template>
  <div class="nd" :class="`nd--${tone || 'primary'}`">
    <div class="nd-line" />
    <div class="nd-glow" />
  </div>
</template>

<style scoped>
.nd {
  position: relative;
  height: 2px;
  width: 100%;
  margin: 4px 0;
}

.nd-line {
  position: absolute;
  inset: 0;
  border-radius: 1px;
}

.nd-glow {
  position: absolute;
  inset: -3px 0;
  opacity: 0.15;
  filter: blur(6px);
  border-radius: 1px;
}

.nd--primary .nd-line {
  background: linear-gradient(90deg, transparent 0%, var(--app-primary) 20%, var(--app-primary) 80%, transparent 100%);
}
.nd--primary .nd-glow {
  background: linear-gradient(90deg, transparent 0%, var(--app-primary) 20%, var(--app-primary) 80%, transparent 100%);
}

.nd--accent .nd-line {
  background: linear-gradient(90deg, transparent 0%, var(--app-accent) 20%, var(--app-accent) 80%, transparent 100%);
}
.nd--accent .nd-glow {
  background: linear-gradient(90deg, transparent 0%, var(--app-accent) 20%, var(--app-accent) 80%, transparent 100%);
}

.nd--muted .nd-line {
  background: linear-gradient(90deg, transparent 0%, var(--app-text-muted) 20%, var(--app-text-muted) 80%, transparent 100%);
}
.nd--muted .nd-glow {
  background: linear-gradient(90deg, transparent 0%, var(--app-text-muted) 20%, var(--app-text-muted) 80%, transparent 100%);
}
</style>
