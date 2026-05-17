<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import type { DocumentChunk } from '@/modules/knowledge/api'

const MarkdownRenderer = defineAsyncComponent(() => import('@/shared/components/MarkdownRenderer.vue'))

defineProps<{
  chunks: DocumentChunk[]
  visible: boolean
  documentTitle?: string
  loading?: boolean
}>()

const emit = defineEmits<{
  close: []
}>()
</script>

<template>
  <el-drawer
    :model-value="visible"
    :title="documentTitle ? `切片：${documentTitle}` : '切片'"
    size="600px"
    @close="emit('close')"
  >
    <div v-loading="loading">
      <div v-for="chunk in chunks" :key="chunk.id" class="chunk-card">
        <div class="chunk-header">
          <span class="chunk-index">#{{ chunk.chunkIndex }}</span>
          <el-tag size="small" type="info">{{ chunk.tokenCount }} tokens</el-tag>
        </div>
        <div class="chunk-body">
          <MarkdownRenderer v-if="chunk.content" :content="chunk.content" />
          <pre v-else class="chunk-plain">{{ chunk.content }}</pre>
        </div>
      </div>
      <EmptyState v-if="!loading && chunks.length === 0" description="暂无切片数据" />
    </div>
  </el-drawer>
</template>

<style scoped>
.chunk-card {
  margin-bottom: 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}
.chunk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  font-size: 12px;
}
.chunk-index {
  font-weight: 600;
  color: #303133;
}
.chunk-body {
  padding: 12px;
  font-size: 13px;
  max-height: 300px;
  overflow-y: auto;
}
.chunk-plain {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'SF Mono', Monaco, Menlo, Consolas, monospace;
  font-size: 12px;
  margin: 0;
}
</style>
