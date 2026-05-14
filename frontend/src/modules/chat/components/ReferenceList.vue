<script setup lang="ts">
import { ref } from 'vue'
import type { ChatReference } from '@/modules/chat/api'

const props = withDefaults(defineProps<{
  references: ChatReference[]
  compact?: boolean
}>(), {
  compact: false,
})

const expandedSnippets = ref<Set<string>>(new Set())

function toggleSnippet(id: string) {
  if (expandedSnippets.value.has(id)) {
    expandedSnippets.value.delete(id)
  } else {
    expandedSnippets.value.add(id)
  }
}

function formatScore(score: number | null): string {
  if (score == null) return '-'
  return (score * 100).toFixed(0) + '%'
}
</script>

<template>
  <div v-if="references && references.length > 0" class="ref-list" :class="{ 'ref-list--compact': compact }">
    <div class="ref-list__title">References ({{ references.length }})</div>
    <div v-for="ref in references" :key="ref.id" class="ref-item">
      <div class="ref-item__header">
        <el-tag size="small" type="info">{{ ref.referenceType }}</el-tag>
        <span v-if="ref.title" class="ref-item__title">{{ ref.title }}</span>
        <span v-if="ref.filePath" class="ref-item__path">{{ ref.filePath }}</span>
        <span v-if="ref.score != null" class="ref-item__score">
          <el-tag size="small" :type="ref.score > 0.7 ? 'success' : ref.score > 0.4 ? 'warning' : 'info'">
            {{ formatScore(ref.score) }}
          </el-tag>
        </span>
        <span v-if="ref.startLine != null" class="ref-item__lines">L{{ ref.startLine }}{{ ref.endLine ? `-L${ref.endLine}` : '' }}</span>
      </div>
      <div v-if="ref.url" class="ref-item__url">
        <a :href="ref.url" target="_blank" rel="noopener">{{ ref.url }}</a>
      </div>
      <div v-if="ref.snippet" class="ref-item__snippet">
        <div class="snippet-content" :class="{ expanded: expandedSnippets.has(ref.id) }">
          <pre><code>{{ ref.snippet }}</code></pre>
        </div>
        <el-button
          v-if="ref.snippet.length > 200"
          size="small" text type="primary"
          @click="toggleSnippet(ref.id)"
        >
          {{ expandedSnippets.has(ref.id) ? '收起' : '展开' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ref-list {
  margin-top: 8px;
  padding: 8px 10px;
  background: #fafbfc;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  max-width: 100%;
  overflow: hidden;
}
.ref-list--compact {
  padding: 4px 8px;
  font-size: 12px;
}
.ref-list__title {
  font-size: 11px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.ref-item {
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}
.ref-item:last-child {
  border-bottom: none;
}
.ref-item__header {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
  font-size: 12px;
}
.ref-item__title {
  font-weight: 500;
  color: #303133;
}
.ref-item__path {
  color: #606266;
  font-family: monospace;
  font-size: 11px;
}
.ref-item__score {
  margin-left: auto;
}
.ref-item__lines {
  font-size: 11px;
  color: #c0c4cc;
  font-family: monospace;
}
.ref-item__url {
  margin-top: 2px;
  font-size: 11px;
}
.ref-item__url a {
  color: var(--el-color-primary);
  text-decoration: none;
}
.ref-item__snippet {
  margin-top: 4px;
}
.snippet-content {
  max-height: 60px;
  overflow: hidden;
  position: relative;
}
.snippet-content.expanded {
  max-height: none;
}
.snippet-content pre {
  margin: 0;
  padding: 4px 8px;
  background: #f0f2f5;
  border-radius: 4px;
  font-size: 11px;
  overflow-x: auto;
}
.snippet-content code {
  font-family: 'SF Mono', Monaco, Menlo, Consolas, monospace;
  font-size: 11px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
