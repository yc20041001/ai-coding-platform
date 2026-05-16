<script setup lang="ts">
defineProps<{
  primaryCtaLabel?: string
  primaryCtaHref?: string
  secondaryCtaLabel?: string
  secondaryCtaHref?: string
}>()
</script>

<template>
  <section class="ph">
    <div class="ph-bg" />
    <div class="ph-grid" />
    <div class="ph-content">
      <div class="ph-badge">
        <span class="ph-badge-dot" />
        Internal Alpha — Mock Provider by Default
      </div>
      <h1 class="ph-title">
        AI Coding Platform
      </h1>
      <p class="ph-tagline">
        AI-native workspace for projects, agents, knowledge, and code review.
      </p>
      <p class="ph-desc">
        A unified collaboration console that connects project context, knowledge base RAG,
        agent task execution, and GitHub PR review — with full observability and audit trail.
        Start exploring with built-in Mock provider, or bring your own API keys for real models.
      </p>
      <div class="ph-actions">
        <a :href="primaryCtaHref || '/login'" class="ph-cta-primary">
          Open Console
          <span class="ph-cta-arrow">&rarr;</span>
        </a>
        <a :href="secondaryCtaHref || 'https://github.com/yc20041001/ai-coding-platform'" target="_blank" class="ph-cta-secondary">
          View GitHub
        </a>
        <a href="https://github.com/yc20041001/ai-coding-platform/blob/main/docs/demo-walkthrough.md" target="_blank" class="ph-cta-ghost">
          Read Walkthrough
        </a>
        <a href="https://github.com/yc20041001/ai-coding-platform/blob/main/docs/roadmap.md" target="_blank" class="ph-cta-ghost">
          View Roadmap
        </a>
      </div>
      <p class="ph-note">
        No API key required to explore. Default: Mock Provider.
        GitHub OAuth is optional. Real model access requires your own API keys.
      </p>
    </div>
  </section>
</template>

<style scoped>
.ph {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 28px 100px;
  overflow: hidden;
}

.ph-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 40% 20%, rgba(56, 189, 248, 0.06) 0%, transparent 60%),
    radial-gradient(ellipse 60% 60% at 70% 50%, rgba(139, 92, 246, 0.05) 0%, transparent 55%),
    radial-gradient(ellipse 50% 50% at 50% 85%, rgba(34, 197, 94, 0.03) 0%, transparent 50%);
  pointer-events: none;
}

.ph-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.025) 1px, transparent 1px);
  background-size: 64px 64px;
  pointer-events: none;
}

.ph-content {
  position: relative;
  z-index: 1;
  max-width: 780px;
  text-align: center;
}

.ph-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-family: monospace;
  background: rgba(245, 158, 11, 0.1);
  color: var(--app-warning);
  border: 1px solid rgba(245, 158, 11, 0.2);
  margin-bottom: 28px;
}

.ph-badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--app-warning);
  box-shadow: 0 0 6px rgba(245, 158, 11, 0.5);
  animation: statusPulse 2s ease-in-out infinite;
}

@keyframes statusPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.ph-title {
  font-size: 56px;
  font-weight: 800;
  color: var(--app-text);
  margin: 0 0 12px;
  line-height: 1.1;
  letter-spacing: -1px;
  background: linear-gradient(135deg, #e5edf8 0%, #38bdf8 50%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.ph-tagline {
  font-size: 20px;
  color: var(--app-text-soft);
  margin: 0 0 16px;
  font-weight: 500;
  line-height: 1.5;
}

.ph-desc {
  font-size: 14px;
  color: var(--app-text-muted);
  line-height: 1.7;
  margin: 0 auto 36px;
  max-width: 600px;
}

.ph-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.ph-cta-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border-radius: var(--app-radius);
  font-size: 14px;
  font-weight: 600;
  color: #070b18;
  background: var(--app-primary);
  border: none;
  text-decoration: none;
  transition: all 0.2s var(--app-ease-out);
  box-shadow: 0 0 24px rgba(56, 189, 248, 0.25);
}

.ph-cta-primary:hover {
  background: #4cc5f9;
  box-shadow: 0 0 32px rgba(56, 189, 248, 0.4);
  transform: translateY(-1px);
}

.ph-cta-arrow {
  font-size: 16px;
  transition: transform 0.2s;
}

.ph-cta-primary:hover .ph-cta-arrow {
  transform: translateX(3px);
}

.ph-cta-secondary {
  display: inline-flex;
  align-items: center;
  padding: 12px 24px;
  border-radius: var(--app-radius);
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text);
  background: var(--app-surface);
  border: 1px solid var(--app-border-strong);
  text-decoration: none;
  transition: all 0.2s var(--app-ease-out);
}

.ph-cta-secondary:hover {
  background: var(--app-surface-raised);
  border-color: var(--app-primary);
}

.ph-cta-ghost {
  display: inline-flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: var(--app-radius);
  font-size: 13px;
  font-weight: 500;
  color: var(--app-text-muted);
  text-decoration: none;
  transition: all 0.2s var(--app-ease-out);
}

.ph-cta-ghost:hover {
  color: var(--app-text-soft);
  background: var(--app-panel-hover);
}

.ph-note {
  font-size: 12px;
  color: var(--app-text-muted);
  opacity: 0.7;
  line-height: 1.6;
  max-width: 500px;
  margin: 0 auto;
}

@media (max-width: 640px) {
  .ph-title {
    font-size: 34px;
  }
  .ph-tagline {
    font-size: 16px;
  }
  .ph-actions {
    flex-direction: column;
    gap: 8px;
  }
  .ph-cta-primary,
  .ph-cta-secondary,
  .ph-cta-ghost {
    width: 100%;
    justify-content: center;
  }
}
</style>
