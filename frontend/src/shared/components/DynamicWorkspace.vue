<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
  eyebrow?: string
  status?: string
}>()
</script>

<template>
  <div class="dw">
    <div class="dw-bg" />
    <div class="dw-scanline" />
    <header class="dw-header">
      <div class="dw-header-left">
        <span v-if="eyebrow" class="dw-eyebrow">{{ eyebrow }}</span>
        <h1 class="dw-title">{{ title }}</h1>
        <p v-if="subtitle" class="dw-subtitle">{{ subtitle }}</p>
      </div>
      <div class="dw-header-right">
        <span v-if="status" class="dw-status">
          <span class="dw-status-dot" />
          {{ status }}
        </span>
        <slot name="actions" />
      </div>
    </header>
    <div v-if="$slots.metrics" class="dw-metrics">
      <slot name="metrics" />
    </div>
    <div class="dw-body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.dw {
  position: relative;
  border-radius: var(--app-radius-lg);
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  overflow: hidden;
  animation: pageFadeIn 0.3s var(--app-ease-out);
}

.dw-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 60% 40% at 20% 0%, rgba(56, 189, 248, 0.03) 0%, transparent 70%),
    radial-gradient(ellipse 50% 30% at 80% 100%, rgba(139, 92, 246, 0.03) 0%, transparent 70%);
  pointer-events: none;
}

.dw-scanline {
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(148, 163, 184, 0.008) 2px,
    rgba(148, 163, 184, 0.008) 4px
  );
  pointer-events: none;
  animation: holographicShift 6s ease-in-out infinite;
}

.dw-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px 28px 0;
  position: relative;
  z-index: 1;
}

.dw-header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dw-eyebrow {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--app-primary);
}

.dw-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--app-text);
  margin: 0;
  line-height: 1.3;
}

.dw-subtitle {
  font-size: 13px;
  color: var(--app-text-muted);
  margin: 0;
}

.dw-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dw-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--app-success);
  padding: 4px 12px;
  background: var(--app-success-soft);
  border: 1px solid rgba(34, 197, 94, 0.18);
  border-radius: 20px;
}

.dw-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--app-success);
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.5);
  animation: statusPulse 2s ease-in-out infinite;
}

.dw-metrics {
  display: flex;
  gap: 16px;
  padding: 20px 28px 0;
  flex-wrap: wrap;
  position: relative;
  z-index: 1;
}

.dw-body {
  padding: 20px 28px 112px;
  position: relative;
  z-index: 1;
}
</style>
