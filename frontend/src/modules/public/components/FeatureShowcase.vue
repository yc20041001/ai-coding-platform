<script setup lang="ts">
interface Feature {
  title: string
  summary: string
  status: 'ready' | 'mock' | 'planned'
  icon: string
}

const features: Feature[] = [
  {
    title: '项目工作台',
    summary: '多标签项目控制台，包含概览、任务、对话、知识库、仓库和成员。每个项目都是一个独立的 AI 协作单元。',
    status: 'ready',
    icon: 'P',
  },
  {
    title: '知识库与 RAG',
    summary: '上传文档后自动切片、向量化和检索。RAG 上下文会自动注入 Chat 和任务执行，并保留相关性评分。',
    status: 'ready',
    icon: 'K',
  },
  {
    title: 'SSE 流式对话',
    summary: '实时 SSE 流式对话，并高亮 RAG 引用。每条回复都会展示使用了哪些知识片段及相关性评分。',
    status: 'ready',
    icon: 'C',
  },
  {
    title: '智能体任务执行',
    summary: '创建并执行 AI 智能体任务。完整状态机覆盖待处理、运行中、已完成，并保留日志、产物和模型调用轨迹。',
    status: 'ready',
    icon: 'T',
  },
  {
    title: '模型网关',
    summary: '统一的大模型接入层，支持 OpenAI、Claude、DeepSeek、Qwen、Gemini 和 Mock。包含连接测试、降级策略、成本估算和 Prompt 安全过滤。',
    status: 'ready',
    icon: 'M',
  },
  {
    title: 'GitHub PR 评审',
    summary: '只读 GitHub OAuth 集成。可浏览仓库并用 AI 辅助分析 Pull Request，不会自动评论、推送或合并。',
    status: 'ready',
    icon: 'G',
  },
  {
    title: '可观测性与审计',
    summary: '系统级指标面板、项目级模型用量与成本汇总、可筛选审计日志，以及模型请求追踪。',
    status: 'ready',
    icon: 'O',
  },
]

function statusLabel(s: Feature['status']): string {
  if (s === 'ready') return '已就绪'
  if (s === 'mock') return '模拟'
  return '规划中'
}
</script>

<template>
  <section class="fs">
    <div class="fs-header">
      <span class="fs-eyebrow">核心能力</span>
      <h2 class="fs-title">平台包含什么</h2>
      <p class="fs-sub">七个模块串起完整的 AI 编程协作闭环。</p>
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
