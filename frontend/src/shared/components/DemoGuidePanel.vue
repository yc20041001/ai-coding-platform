<script setup lang="ts">
defineProps<{
  hasDemoData?: boolean
  demoProjectId?: string
}>()

defineEmits<{
  seedData: []
}>()
</script>

<template>
  <div class="guide-panel">
    <div class="guide-header">
      <span class="guide-icon">◈</span>
      <span class="guide-title">Demo Experience Path</span>
      <slot name="badge" />
    </div>

    <div v-if="!hasDemoData" class="guide-empty">
      <p>No demo data found. Initialize demo data to explore the platform.</p>
      <button class="guide-action" @click="$emit('seedData')">
        Initialize Demo Data
      </button>
      <p class="guide-hint">
        Or run: <code>bash scripts/demo-seed-data.sh</code>
      </p>
    </div>

    <div v-else class="guide-steps">
      <div class="guide-step">
        <span class="guide-step-num">1</span>
        <span class="guide-step-label">Open Demo Project</span>
        <span class="guide-step-arrow">→</span>
      </div>
      <div class="guide-step">
        <span class="guide-step-num">2</span>
        <span class="guide-step-label">Explore Knowledge Base</span>
        <span class="guide-step-arrow">→</span>
      </div>
      <div class="guide-step">
        <span class="guide-step-num">3</span>
        <span class="guide-step-label">Ask Chat with RAG</span>
        <span class="guide-step-arrow">→</span>
      </div>
      <div class="guide-step">
        <span class="guide-step-num">4</span>
        <span class="guide-step-label">Execute Agent Task</span>
        <span class="guide-step-arrow">→</span>
      </div>
      <div class="guide-step">
        <span class="guide-step-num">5</span>
        <span class="guide-step-label">Review Usage & Audit</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.guide-panel {
  border: 1px solid var(--app-border, rgba(255, 255, 255, 0.06));
  border-radius: 8px;
  padding: 16px 20px;
  margin: 16px 0;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.04) 0%, transparent 50%);
}

.guide-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.guide-icon {
  color: var(--app-cyan, #409EFF);
  font-size: 14px;
}

.guide-title {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--app-text-muted);
}

.guide-empty {
  color: var(--app-text-dim);
  font-size: 13px;
  line-height: 1.6;
}

.guide-empty p {
  margin: 0 0 10px 0;
}

.guide-action {
  background: var(--app-cyan, #409EFF);
  color: #fff;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

.guide-action:hover {
  opacity: 0.85;
}

.guide-hint {
  margin-top: 8px;
  font-size: 11px;
  color: var(--app-text-muted);
}

.guide-hint code {
  background: rgba(255, 255, 255, 0.06);
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
}

.guide-steps {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.guide-step {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.guide-step-num {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--app-cyan, #409EFF);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.guide-step-label {
  font-size: 11px;
  color: var(--app-text-dim);
  white-space: nowrap;
}

.guide-step-arrow {
  color: var(--app-text-muted);
  font-size: 10px;
  margin-left: 2px;
}
</style>
