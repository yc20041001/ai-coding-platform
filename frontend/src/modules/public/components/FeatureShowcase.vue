<script setup lang="ts">
interface Feature {
  title: string
  summary: string
  status: 'ready' | 'mock' | 'planned'
  icon: string
}

const features: Feature[] = [
  {
    title: 'Project Workspace',
    summary: 'Multi-tab project console with Overview, Tasks, Chat, Knowledge, Repository, and Members. Each project is a self-contained AI collaboration unit.',
    status: 'ready',
    icon: 'P',
  },
  {
    title: 'Knowledge Base & RAG',
    summary: 'Upload documents, auto-chunk, embed, and search. RAG context is automatically injected into Chat prompts and Task executions with relevance scoring.',
    status: 'ready',
    icon: 'K',
  },
  {
    title: 'Chat with SSE Streaming',
    summary: 'Real-time SSE streaming chat with RAG reference highlighting. Each response shows which knowledge chunks were used, with relevance scores.',
    status: 'ready',
    icon: 'C',
  },
  {
    title: 'Agent Task Execution',
    summary: 'Create and execute AI agent tasks (FEATURE, BUGFIX, REVIEW, REFACTOR). Full state machine: PENDING → RUNNING → COMPLETED, with logs, artifacts, and model call traces.',
    status: 'ready',
    icon: 'T',
  },
  {
    title: 'Model Gateway',
    summary: 'Unified LLM access layer supporting OpenAI, Claude, DeepSeek, Qwen, Gemini + Mock. Connection testing, fallback strategy, cost estimation, and prompt safety filtering.',
    status: 'ready',
    icon: 'M',
  },
  {
    title: 'GitHub PR Review',
    summary: 'Read-only GitHub OAuth integration. Browse repositories and review pull requests with AI-assisted analysis. No automatic comments or pushes.',
    status: 'ready',
    icon: 'G',
  },
  {
    title: 'Observability & Audit',
    summary: 'System-wide metrics dashboard, per-project model usage and cost summaries, full audit log with action filtering, and model request traceability.',
    status: 'ready',
    icon: 'O',
  },
]

function statusLabel(s: Feature['status']): string {
  if (s === 'ready') return 'Ready'
  if (s === 'mock') return 'Mock'
  return 'Planned'
}
</script>

<template>
  <section class="fs">
    <div class="fs-header">
      <span class="fs-eyebrow">Capabilities</span>
      <h2 class="fs-title">What's Inside</h2>
      <p class="fs-sub">Seven integrated modules forming a complete AI coding collaboration loop.</p>
    </div>
    <div class="fs-grid">
      <article
        v-for="f in features"
        :key="f.title"
        class="fs-card"
      >
        <div class="fs-card-top">
          <span class="fs-card-icon">{{ f.icon }}</span>
          <span
            class="fs-card-status"
            :class="`fs-card-status--${f.status}`"
          >
            <span class="fs-card-status-dot" />
            {{ statusLabel(f.status) }}
          </span>
        </div>
        <h3 class="fs-card-title">{{ f.title }}</h3>
        <p class="fs-card-summary">{{ f.summary }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.fs {
  max-width: 1100px;
  margin: 0 auto;
  padding: 80px 28px;
}

.fs-header {
  text-align: center;
  margin-bottom: 48px;
}

.fs-eyebrow {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--app-primary);
}

.fs-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--app-text);
  margin: 8px 0;
}

.fs-sub {
  font-size: 14px;
  color: var(--app-text-muted);
  margin: 0;
}

.fs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.fs-card {
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  padding: 22px;
  transition: border-color 0.2s var(--app-ease-out), background 0.2s var(--app-ease-out);
}

.fs-card:hover {
  background: var(--app-surface-raised);
  border-color: var(--app-border-strong);
}

.fs-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.fs-card-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 700;
  font-family: monospace;
  background: rgba(56, 189, 248, 0.1);
  color: var(--app-primary);
  border: 1px solid rgba(56, 189, 248, 0.18);
}

.fs-card-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  font-family: monospace;
  padding: 2px 8px;
  border-radius: 3px;
}

.fs-card-status--ready {
  background: rgba(34, 197, 94, 0.1);
  color: var(--app-success);
  border: 1px solid rgba(34, 197, 94, 0.18);
}

.fs-card-status--mock {
  background: rgba(245, 158, 11, 0.1);
  color: var(--app-warning);
  border: 1px solid rgba(245, 158, 11, 0.18);
}

.fs-card-status--planned {
  background: rgba(124, 140, 165, 0.1);
  color: var(--app-text-muted);
  border: 1px solid rgba(124, 140, 165, 0.18);
}

.fs-card-status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.fs-card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text);
  margin: 0 0 8px;
}

.fs-card-summary {
  font-size: 13px;
  color: var(--app-text-muted);
  line-height: 1.6;
  margin: 0;
}

@media (max-width: 640px) {
  .fs-grid {
    grid-template-columns: 1fr;
  }
}
</style>
