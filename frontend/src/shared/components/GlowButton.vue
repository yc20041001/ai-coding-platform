<script setup lang="ts">
defineProps<{
  accent?: 'primary' | 'success' | 'warning' | 'danger' | 'accent'
  loading?: boolean
  disabled?: boolean
  size?: 'small' | 'default' | 'large'
}>()

defineEmits<{
  click: []
}>()
</script>

<template>
  <button
    class="gbtn"
    :class="[`gbtn--${accent || 'primary'}`, `gbtn--${size || 'default'}`]"
    :disabled="disabled || loading"
    @click="$emit('click')"
  >
    <span v-if="loading" class="gbtn-spinner" />
    <slot />
  </button>
</template>

<style scoped>
.gbtn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid transparent;
  border-radius: var(--app-radius-sm);
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  letter-spacing: 0.3px;
  position: relative;
  overflow: hidden;
}
.gbtn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.gbtn--small { padding: 6px 14px; font-size: 12px; }
.gbtn--default { padding: 8px 20px; }
.gbtn--large { padding: 12px 28px; font-size: 15px; }

.gbtn--primary {
  background: rgba(56, 189, 248, 0.15);
  border-color: rgba(56, 189, 248, 0.3);
  color: var(--app-primary);
}
.gbtn--primary:hover:not(:disabled) {
  background: rgba(56, 189, 248, 0.25);
  border-color: var(--app-primary);
  box-shadow: 0 0 16px rgba(56, 189, 248, 0.2);
}
.gbtn--success {
  background: var(--app-success-soft);
  border-color: rgba(34, 197, 94, 0.3);
  color: var(--app-success);
}
.gbtn--success:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.2);
  border-color: var(--app-success);
  box-shadow: 0 0 16px rgba(34, 197, 94, 0.2);
}
.gbtn--warning {
  background: var(--app-warning-soft, rgba(245, 158, 11, 0.15));
  border-color: rgba(245, 158, 11, 0.3);
  color: var(--app-warning, #f59e0b);
}
.gbtn--warning:hover:not(:disabled) {
  background: rgba(245, 158, 11, 0.25);
  border-color: var(--app-warning, #f59e0b);
  box-shadow: 0 0 16px rgba(245, 158, 11, 0.2);
}
.gbtn--danger {
  background: var(--app-danger-soft);
  border-color: rgba(239, 68, 68, 0.3);
  color: var(--app-danger);
}
.gbtn--danger:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.2);
  border-color: var(--app-danger);
  box-shadow: 0 0 16px rgba(239, 68, 68, 0.2);
}
.gbtn--accent {
  background: var(--app-accent-soft);
  border-color: rgba(139, 92, 246, 0.3);
  color: var(--app-accent);
}
.gbtn--accent:hover:not(:disabled) {
  background: rgba(139, 92, 246, 0.25);
  border-color: var(--app-accent);
  box-shadow: 0 0 16px rgba(139, 92, 246, 0.2);
}
.gbtn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
