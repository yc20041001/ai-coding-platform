<script setup lang="ts">
defineProps<{
  value: string | number
  label: string
  accent?: 'primary' | 'success' | 'warning' | 'danger' | 'accent'
  trend?: 'up' | 'down'
}>()
</script>

<template>
  <div class="mtile" :class="`mtile--${accent || 'primary'}`">
    <div class="mtile-value">
      {{ value }}
      <span v-if="trend" class="mtile-trend" :class="`mtile-trend--${trend}`">
        {{ trend === 'up' ? '↗' : '↘' }}
      </span>
    </div>
    <div class="mtile-label">{{ label }}</div>
  </div>
</template>

<style scoped>
.mtile {
  padding: 20px;
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  text-align: center;
  transition: all 0.2s;
}
.mtile:hover {
  border-color: var(--app-border-strong);
  transform: translateY(-2px);
  box-shadow: var(--app-glow);
}
.mtile-value {
  font-size: 30px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.mtile--primary .mtile-value { color: var(--app-primary); }
.mtile--success .mtile-value { color: var(--app-success); }
.mtile--warning .mtile-value { color: var(--app-warning); }
.mtile--danger .mtile-value { color: var(--app-danger); }
.mtile--accent .mtile-value { color: var(--app-accent); }
.mtile-label {
  font-size: 11px;
  color: var(--app-text-muted);
  margin-top: 6px;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}
.mtile-trend {
  font-size: 18px;
  margin-left: 4px;
}
.mtile-trend--up { color: var(--app-success); }
.mtile-trend--down { color: var(--app-danger); }
</style>
