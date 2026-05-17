<script setup lang="ts">
import { ref } from 'vue'

interface FaqItem {
  q: string
  a: string
}

const faqs: FaqItem[] = [
  {
    q: 'Is this a production-ready product?',
    a: 'The platform is currently in Internal Alpha (v1.0). All core modules are functional and tested, but it has not been battle-tested under production load. It is suitable for demos, trials, and internal team use. Production deployment guides, monitoring, and security runbooks are available.',
  },
  {
    q: 'Does it call real AI models by default?',
    a: 'No. The platform defaults to a built-in Mock Provider that returns simulated responses. This means you can explore every feature — Chat, Task execution, RAG — without any API keys or external costs. Real model calls only happen after you explicitly configure a provider.',
  },
  {
    q: 'Is GitHub OAuth required?',
    a: 'No. GitHub OAuth is entirely optional. The Repository and PR Review features are available but will show a "Not Configured" state until you set up GitHub OAuth credentials. All other features (Projects, Tasks, Chat, RAG, 模型网关) work without it.',
  },
  {
    q: 'Does any data leave my machine?',
    a: 'When using the Mock Provider: no. All data stays on your local machine or within your Docker containers. When you configure a real model provider (OpenAI, Claude, etc.), your prompts and RAG context are sent to that provider\'s API — governed by their respective data policies.',
  },
  {
    q: 'How do I start the demo?',
    a: 'See the Trial Entry section above. In short: start backend (mvn spring-boot:run), start frontend (npm run dev), run demo-seed-data.sh, and login with admin@example.com / Admin@123456. Full guide in the Trial Entry Guide.',
  },
  {
    q: 'How do I submit feedback?',
    a: 'File a GitHub Issue using one of our templates: Bug Report, Feature Request, or Trial Feedback. We triage all feedback using a published 8-step process and taxonomy. Links: Feedback Taxonomy, Triage Guide, and Roadmap.',
  },
  {
    q: 'How do I connect a real AI model?',
    a: 'Edit your .env file and set *_ENABLED=true plus *_API_KEY for each provider you want to use (OpenAI、Claude、DeepSeek、Qwen， Gemini). Then configure models in the 模型网关 page of the console. See the README for a complete list of environment variables.',
  },
  {
    q: 'What are the current limitations?',
    a: 'Key limitations: (1) Single-admin user — no multi-user registration yet (planned v1.2). (2) Mock responses only — real model quality depends on your API keys. (3) No real-time collaboration — each user works independently. (4) Webhook/CI integration is planned but not yet implemented. (5) No mobile optimization.',
  },
]

const openIndex = ref<number | null>(0)

function toggle(idx: number) {
  openIndex.value = openIndex.value === idx ? null : idx
}
</script>

<template>
  <section class="faq">
    <div class="faq-header">
      <span class="faq-eyebrow">常见问题</span>
      <h2 class="faq-title">常见问题</h2>
    </div>
    <div class="faq-list">
      <div
        v-for="(item, idx) in faqs"
        :key="idx"
        class="faq-item"
        :class="{ 'faq-item--open': openIndex === idx }"
      >
        <button
          class="faq-q"
          @click="toggle(idx)"
        >
          <span class="faq-q-text">{{ item.q }}</span>
          <span class="faq-q-icon">{{ openIndex === idx ? '–' : '+' }}</span>
        </button>
        <div
          v-show="openIndex === idx"
          class="faq-a"
        >
          <p>{{ item.a }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.faq {
  max-width: 780px;
  margin: 0 auto;
  padding: 80px 28px 120px;
}

.faq-header {
  text-align: center;
  margin-bottom: 36px;
}

.faq-eyebrow {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--app-primary);
}

.faq-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--app-text);
  margin: 8px 0;
}

.faq-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.faq-item {
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  overflow: hidden;
  transition: border-color 0.2s;
}

.faq-item--open {
  border-color: var(--app-border-strong);
}

.faq-q {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  background: none;
  border: none;
  color: var(--app-text-soft);
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: color 0.15s;
  font-family: inherit;
}

.faq-q:hover {
  color: var(--app-text);
}

.faq-q-text {
  flex: 1;
}

.faq-q-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 400;
  color: var(--app-text-muted);
  border-radius: 4px;
  background: rgba(148, 163, 184, 0.06);
  font-family: monospace;
}

.faq-a {
  padding: 0 20px 18px;
  font-size: 13px;
  color: var(--app-text-muted);
  line-height: 1.7;
}

.faq-a p {
  margin: 0;
}

@media (max-width: 640px) {
  .faq-q {
    font-size: 13px;
    padding: 14px 16px;
  }
  .faq-a {
    padding: 0 16px 16px;
    font-size: 12px;
  }
}
</style>
