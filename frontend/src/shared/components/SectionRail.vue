<script setup lang="ts">
import type { Component } from 'vue'

export interface RailItem {
  key: string
  label: string
  icon?: string
  count?: number
  tone?: 'primary' | 'success' | 'warning' | 'danger' | 'muted'
}

defineProps<{
  items: RailItem[]
  activeKey: string
}>()

const emit = defineEmits<{
  select: [key: string]
}>()
</script>

<template>
  <nav class="sr">
    <button
      v-for="item in items"
      :key="item.key"
      class="sr-item"
      :class="{ 'sr-item--active': item.key === activeKey }"
      @click="emit('select', item.key)"
    >
      <span v-if="item.icon" class="sr-icon">{{ item.icon }}</span>
      <span class="sr-label">{{ item.label }}</span>
      <span v-if="item.count !== undefined" class="sr-count">{{ item.count }}</span>
      <span v-if="item.key === activeKey" class="sr-indicator" />
    </button>
  </nav>
</template>

<style scoped>
.sr {
  display: flex;
  gap: 2px;
  padding: 4px;
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  width: fit-content;
}

.sr-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--app-radius-sm);
  border: none;
  background: transparent;
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s var(--app-ease-out);
  position: relative;
  font-family: inherit;
}

.sr-item:hover {
  color: var(--app-text-soft);
  background: var(--app-panel-hover);
}

.sr-item--active {
  color: var(--app-primary);
  background: var(--app-primary-soft);
}

.sr-icon {
  font-size: 14px;
}

.sr-label {
  letter-spacing: 0.3px;
}

.sr-count {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 10px;
  background: rgba(148, 163, 184, 0.12);
  color: var(--app-text-muted);
}

.sr-item--active .sr-count {
  background: var(--app-primary-soft);
  color: var(--app-primary);
}

.sr-indicator {
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 2px;
  background: var(--app-primary);
  border-radius: 2px;
  box-shadow: 0 0 8px var(--app-primary-glow);
}
</style>
