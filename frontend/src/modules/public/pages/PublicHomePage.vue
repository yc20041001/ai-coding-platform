<script setup lang="ts">
import PublicHero from '../components/PublicHero.vue'
import FeatureShowcase from '../components/FeatureShowcase.vue'
import ArchitecturePreview from '../components/ArchitecturePreview.vue'
import TrialEntryPanel from '../components/TrialEntryPanel.vue'
import PublicFaq from '../components/PublicFaq.vue'
</script>

<template>
  <div class="public-home">
    <PublicHero />
    <FeatureShowcase />
    <ArchitecturePreview />
    <TrialEntryPanel />
    <PublicFaq />
    <footer class="public-footer">
      <div class="public-footer-inner">
        <span>AI Coding Platform — Internal Alpha v1.0</span>
        <nav class="public-footer-links">
          <a href="https://github.com/yc20041001/ai-coding-platform" target="_blank">GitHub</a>
          <a href="https://github.com/yc20041001/ai-coding-platform/blob/main/docs/roadmap.md" target="_blank">Roadmap</a>
          <a href="https://github.com/yc20041001/ai-coding-platform/blob/main/CHANGELOG.md" target="_blank">Changelog</a>
          <a href="https://github.com/yc20041001/ai-coding-platform/issues/new?template=user_trial_feedback.yml" target="_blank">Feedback</a>
        </nav>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.public-home {
  min-height: 100vh;
  background: var(--app-bg);
}

.public-footer {
  border-top: 1px solid var(--app-border);
  padding: 24px 28px;
}

.public-footer-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--app-text-muted);
}

.public-footer-links {
  display: flex;
  gap: 20px;
}

.public-footer-links a {
  color: var(--app-text-muted);
  text-decoration: none;
  transition: color 0.15s;
}

.public-footer-links a:hover {
  color: var(--app-text-soft);
}

@media (max-width: 640px) {
  .public-footer-inner {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
  .public-footer-links {
    flex-wrap: wrap;
    justify-content: center;
    gap: 14px;
  }
}
</style>
