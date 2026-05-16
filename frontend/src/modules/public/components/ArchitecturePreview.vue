<script setup lang="ts">
interface Layer {
  label: string
  items: string[]
}

const layers: Layer[] = [
  { label: 'Frontend Console', items: ['Vue 3 SPA', 'Dark Tech UI', 'SSE Streaming'] },
  { label: 'Spring Boot API', items: ['REST Controllers', 'JWT Auth', 'RBAC Permissions'] },
  { label: 'Core Modules', items: ['Project', 'Task', 'Chat', 'RAG', 'Agent', 'Repo'] },
  { label: 'Model Gateway', items: ['Multi-Provider', 'Fallback', 'Cost Tracking', 'Safety'] },
  { label: 'Integrations', items: ['GitHub OAuth', 'PR Review'] },
  { label: 'Infrastructure', items: ['MySQL 8', 'Redis 7', 'RabbitMQ', 'Docker'] },
]

const deps: [number, number][] = [
  [0, 1], [1, 2], [2, 3], [3, 4],
  [2, 5], [1, 5],
]
</script>

<template>
  <section class="ap">
    <div class="ap-header">
      <span class="ap-eyebrow">Architecture</span>
      <h2 class="ap-title">System Overview</h2>
      <p class="ap-sub">Layered architecture with clear separation of concerns.</p>
    </div>
    <div class="ap-stack">
      <div
        v-for="(layer, i) in layers"
        :key="layer.label"
        class="ap-layer"
      >
        <div class="ap-layer-label">
          <span class="ap-layer-num">{{ i + 1 }}</span>
          {{ layer.label }}
        </div>
        <div class="ap-layer-items">
          <span
            v-for="item in layer.items"
            :key="item"
            class="ap-layer-chip"
          >
            {{ item }}
          </span>
        </div>
      </div>
      <div class="ap-observability">
        <span class="ap-obs-dot" />
        Observability & Audit — spans all layers
      </div>
    </div>
  </section>
</template>

<style scoped>
.ap {
  max-width: 800px;
  margin: 0 auto;
  padding: 80px 28px;
}

.ap-header {
  text-align: center;
  margin-bottom: 40px;
}

.ap-eyebrow {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--app-primary);
}

.ap-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--app-text);
  margin: 8px 0;
}

.ap-sub {
  font-size: 14px;
  color: var(--app-text-muted);
  margin: 0;
}

.ap-stack {
  display: flex;
  flex-direction: column;
  gap: 0;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  overflow: hidden;
}

.ap-layer {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 20px;
  background: var(--app-surface);
  border-bottom: 1px solid var(--app-border);
}

.ap-layer:last-of-type {
  border-bottom: none;
}

.ap-layer-label {
  flex-shrink: 0;
  width: 180px;
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-soft);
  display: flex;
  align-items: center;
  gap: 10px;
}

.ap-layer-num {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 700;
  font-family: monospace;
  background: rgba(56, 189, 248, 0.12);
  color: var(--app-primary);
  border: 1px solid rgba(56, 189, 248, 0.2);
}

.ap-layer-items {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ap-layer-chip {
  font-size: 11px;
  font-weight: 500;
  font-family: monospace;
  padding: 3px 10px;
  border-radius: 4px;
  background: rgba(148, 163, 184, 0.06);
  color: var(--app-text-muted);
  border: 1px solid rgba(148, 163, 184, 0.1);
}

.ap-observability {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 20px;
  background: rgba(34, 197, 94, 0.04);
  border: 1px solid rgba(34, 197, 94, 0.12);
  border-top: none;
  font-size: 12px;
  font-weight: 500;
  color: var(--app-success);
  font-family: monospace;
}

.ap-obs-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--app-success);
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.4);
}

@media (max-width: 640px) {
  .ap-layer {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .ap-layer-label {
    width: 100%;
  }
}
</style>
