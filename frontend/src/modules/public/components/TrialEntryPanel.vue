<script setup lang="ts">
</script>

<template>
  <section class="te">
    <div class="te-header">
      <span class="te-eyebrow">立即试用</span>
      <h2 class="te-title">开始试用</h2>
      <p class="te-sub">3 分钟内启动完整演示环境，无需 API Key，也不依赖云服务。</p>
    </div>

    <div class="te-grid">
      <div class="te-card">
        <h3 class="te-card-title">
          <span class="te-step">1</span>
          启动后端
        </h3>
        <pre class="te-code">cd backend
source ../.env
mvn spring-boot:run</pre>
        <p class="te-card-note">需要 Java 17 与 Maven。MySQL、Redis、RabbitMQ 可通过 Docker Compose 启动。</p>
      </div>

      <div class="te-card">
        <h3 class="te-card-title">
          <span class="te-step">2</span>
          启动前端
        </h3>
        <pre class="te-code">cd frontend
npm install
npm run dev -- --host 0.0.0.0</pre>
        <p class="te-card-note">访问地址为 http://localhost:5173，需要 Node.js 18+。</p>
      </div>

      <div class="te-card">
        <h3 class="te-card-title">
          <span class="te-step">3</span>
          Initialize Demo Data
        </h3>
        <pre class="te-code">bash scripts/demo-seed-data.sh</pre>
        <p class="te-card-note">创建演示项目、包含 3 个文档的知识库、对话会话和 2 个已执行任务。脚本幂等，可安全重复运行。</p>
      </div>

      <div class="te-card">
        <h3 class="te-card-title">
          <span class="te-step">4</span>
          登录并体验
        </h3>
        <div class="te-cred">
          <div class="te-cred-row">
            <span class="te-cred-key">邮箱</span>
            <code class="te-cred-val">admin@example.com</code>
          </div>
          <div class="te-cred-row">
            <span class="te-cred-key">密码</span>
            <code class="te-cred-val">Admin@123456</code>
          </div>
        </div>
        <p class="te-card-note">查看 <a href="https://github.com/yc20041001/ai-coding-platform/blob/main/docs/demo-walkthrough.md" target="_blank">演示指南</a> 获取完整体验路线。</p>
      </div>
    </div>

    <div class="te-disclaimers">
      <div class="te-disclaimer">
        <span class="te-disclaimer-icon">&#9888;</span>
        <strong>默认使用模拟供应商</strong> — Chat 与任务回复均为模拟生成。
        No real LLM API calls are made unless you configure API keys.
      </div>
      <div class="te-disclaimer">
        <span class="te-disclaimer-icon">&#8505;</span>
        <strong>GitHub OAuth 可选</strong> — 仓库浏览与 PR 评审
        work only after configuring GitHub OAuth credentials. Not required for core features.
      </div>
      <div class="te-disclaimer">
        <span class="te-disclaimer-icon">&#128273;</span>
        <strong>真实模型需要 API Key</strong> — 如需使用 OpenAI、Claude、DeepSeek、Qwen，
        or Gemini, set the corresponding <code>*_ENABLED=true</code> and <code>*_API_KEY</code> in your <code>.env</code>.
      </div>
    </div>

    <div class="te-footer">
      <p>
        详细启动说明请查看
        <a href="https://github.com/yc20041001/ai-coding-platform/blob/main/docs/trial-entry-guide.md" target="_blank">试用入口指南</a>
        and
        <a href="https://github.com/yc20041001/ai-coding-platform/blob/main/README.md" target="_blank">README</a>。
        发现问题？
        <a href="https://github.com/yc20041001/ai-coding-platform/issues/new?template=user_trial_feedback.yml" target="_blank">提交反馈</a>.
      </p>
    </div>
  </section>
</template>

<style scoped>
.te {
  max-width: 1100px;
  margin: 0 auto;
  padding: 80px 28px;
}

.te-header {
  text-align: center;
  margin-bottom: 40px;
}

.te-eyebrow {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--app-primary);
}

.te-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--app-text);
  margin: 8px 0;
}

.te-sub {
  font-size: 14px;
  color: var(--app-text-muted);
  margin: 0 auto;
  max-width: 520px;
}

.te-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}

.te-card {
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  padding: 22px;
}

.te-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--app-text);
  margin: 0 0 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.te-step {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  font-family: monospace;
  background: var(--app-primary-soft);
  color: var(--app-primary);
  border: 1px solid rgba(56, 189, 248, 0.2);
  flex-shrink: 0;
}

.te-code {
  background: rgba(7, 11, 24, 0.6);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  padding: 12px 14px;
  font-size: 12px;
  font-family: 'Fira Code', 'Cascadia Code', 'JetBrains Mono', monospace;
  color: var(--app-text-soft);
  line-height: 1.6;
  margin: 0 0 10px;
  overflow-x: auto;
  white-space: pre;
}

.te-card-note {
  font-size: 12px;
  color: var(--app-text-muted);
  line-height: 1.6;
  margin: 0;
}

.te-card-note a {
  color: var(--app-primary);
}

.te-cred {
  background: rgba(7, 11, 24, 0.6);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  padding: 10px 14px;
  margin-bottom: 10px;
}

.te-cred-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  font-family: monospace;
  margin-bottom: 6px;
}

.te-cred-row:last-child {
  margin-bottom: 0;
}

.te-cred-key {
  color: var(--app-text-muted);
  width: 70px;
  flex-shrink: 0;
}

.te-cred-val {
  color: var(--app-primary);
  background: transparent;
  border: none;
  font-size: 12px;
  padding: 0;
}

.te-disclaimers {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 780px;
  margin: 0 auto;
}

.te-disclaimer {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  border-radius: var(--app-radius-sm);
  background: rgba(245, 158, 11, 0.05);
  border: 1px solid rgba(245, 158, 11, 0.1);
  font-size: 12px;
  color: var(--app-text-muted);
  line-height: 1.6;
}

.te-disclaimer strong {
  color: var(--app-warning);
}

.te-disclaimer-icon {
  flex-shrink: 0;
  font-size: 14px;
}

.te-disclaimer code {
  background: rgba(245, 158, 11, 0.08);
  color: var(--app-warning);
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 11px;
  font-family: monospace;
}

.te-footer {
  text-align: center;
  margin-top: 32px;
  font-size: 13px;
  color: var(--app-text-muted);
}

.te-footer a {
  color: var(--app-primary);
}

@media (max-width: 640px) {
  .te-grid {
    grid-template-columns: 1fr;
  }
}
</style>
