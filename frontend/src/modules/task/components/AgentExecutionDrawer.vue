<script setup lang="ts">
import { ref, watch, defineAsyncComponent } from 'vue'
import { getAgentExecution, getExecutionModelLogs, type AgentExecution, type ModelRequestLog } from '@/modules/task/api'
import ReferenceList from '@/modules/chat/components/ReferenceList.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import { formatDateTime, formatNumber } from '@/shared/utils/format'

const MarkdownRenderer = defineAsyncComponent(() => import('@/shared/components/MarkdownRenderer.vue'))

const props = defineProps<{
  executionId: string | null
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const execution = ref<AgentExecution | null>(null)
const modelLogs = ref<ModelRequestLog[]>([])
const loading = ref(false)
const loadingLogs = ref(false)

watch(() => props.visible, async (val) => {
  if (val && props.executionId) {
    loading.value = true
    loadingLogs.value = true
    try {
      const [execRes, logRes] = await Promise.all([
        getAgentExecution(props.executionId),
        getExecutionModelLogs(props.executionId),
      ])
      execution.value = execRes.data.data
      modelLogs.value = logRes.data.data || []
    } catch {
      execution.value = null
    } finally {
      loading.value = false
      loadingLogs.value = false
    }
  }
})
</script>

<template>
  <el-drawer
    :model-value="visible"
    title="Agent Execution Detail"
    size="65%"
    @close="emit('close')"
  >
    <div v-loading="loading">
      <template v-if="execution">
        <div class="exec-section">
          <h4>Overview</h4>
          <div class="exec-grid">
            <div class="exec-field"><span class="exec-label">Execution ID</span><code>{{ execution.id }}</code></div>
            <div class="exec-field"><span class="exec-label">Agent</span><span>{{ execution.agentName }}</span></div>
            <div class="exec-field"><span class="exec-label">Type</span><span>{{ execution.executionType }}</span></div>
            <div class="exec-field"><span class="exec-label">Status</span><StatusPulse :status="execution.status" :tone="execution.status === 'COMPLETED' ? 'success' : execution.status === 'RUNNING' ? 'warning' : 'muted'" /></div>
            <div class="exec-field"><span class="exec-label">RAG Used</span><el-tag size="small" :type="execution.ragUsed ? 'success' : 'info'">{{ execution.ragUsed ? 'Yes' : 'No' }}</el-tag></div>
            <div class="exec-field"><span class="exec-label">Token Usage</span><span>{{ formatNumber(execution.tokenUsage) }}</span></div>
            <div class="exec-field"><span class="exec-label">Started</span><span>{{ formatDateTime(execution.startedAt) }}</span></div>
            <div class="exec-field"><span class="exec-label">Finished</span><span>{{ execution.finishedAt ? formatDateTime(execution.finishedAt) : '-' }}</span></div>
            <div class="exec-field"><span class="exec-label">Created</span><span>{{ formatDateTime(execution.createTime) }}</span></div>
          </div>
          <div v-if="execution.errorMessage" class="exec-error">
            <el-alert :title="execution.errorMessage" type="error" :closable="false" />
          </div>
        </div>

        <div class="exec-section">
          <h4>Input Prompt</h4>
          <div class="exec-content-box">
            <MarkdownRenderer v-if="execution.inputPrompt" :content="execution.inputPrompt" />
            <span v-else class="exec-empty">(empty)</span>
          </div>
        </div>

        <div class="exec-section">
          <h4>Output Content</h4>
          <div class="exec-content-box">
            <MarkdownRenderer v-if="execution.outputContent" :content="execution.outputContent" />
            <span v-else class="exec-empty">(empty)</span>
          </div>
        </div>

        <div class="exec-section">
          <h4>Model Logs</h4>
          <div v-if="modelLogs.length > 0">
            <div v-for="log in modelLogs" :key="log.id" class="model-log-card">
              <div class="ml-header">
                <el-tag size="small">{{ log.provider }}</el-tag>
                <span class="ml-model">{{ log.modelName }}</span>
                <el-tag size="small" type="info">{{ log.requestType }}</el-tag>
                <el-tag size="small" :type="log.success ? 'success' : 'danger'">{{ log.success ? 'OK' : 'FAIL' }}</el-tag>
              </div>
              <div class="ml-stats">
                <span>Prompt: {{ formatNumber(log.promptTokens) }}</span>
                <span>Completion: {{ formatNumber(log.completionTokens) }}</span>
                <span>Total: {{ formatNumber(log.totalTokens) }}</span>
                <span>Latency: {{ log.latencyMs }}ms</span>
              </div>
              <div v-if="log.errorMessage" class="ml-error">{{ log.errorMessage }}</div>
              <div class="ml-time">{{ formatDateTime(log.createTime) }}</div>
            </div>
          </div>
          <EmptyState v-else-if="!loadingLogs" description="No model logs" />
        </div>
      </template>
      <ErrorState v-else-if="!loading" title="Load Failed" message="Cannot load execution details" />
    </div>
  </el-drawer>
</template>

<style scoped>
.exec-section {
  margin-bottom: 24px;
}
.exec-section h4 {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--app-border);
}
.exec-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.exec-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.exec-label {
  font-size: 11px;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.exec-field code {
  font-size: 12px;
  background: var(--app-panel);
  color: var(--app-text-soft);
  padding: 2px 8px;
  border-radius: var(--app-radius-sm);
  word-break: break-all;
}
.exec-field span {
  font-size: 13px;
  color: var(--app-text-soft);
}
.exec-error {
  margin-top: 12px;
}
.exec-content-box {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 14px 18px;
  max-height: 300px;
  overflow-y: auto;
}
.exec-empty {
  color: var(--app-text-muted);
  font-style: italic;
  font-size: 13px;
}
.model-log-card {
  padding: 10px 12px;
  margin-bottom: 8px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-bg-soft);
}
.ml-header {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
}
.ml-model {
  font-weight: 500;
  font-size: 13px;
  color: var(--app-text);
}
.ml-stats {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--app-text-muted);
  margin-bottom: 4px;
}
.ml-error {
  font-size: 12px;
  color: var(--app-danger);
  margin-bottom: 2px;
}
.ml-time {
  font-size: 11px;
  color: var(--app-text-muted);
}
</style>
