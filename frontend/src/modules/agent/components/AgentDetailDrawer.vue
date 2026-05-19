<script setup lang="ts">
import { ref, watch } from 'vue'
import { getAgentDetail, type AgentDetail } from '@/modules/agent/api'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'

const props = defineProps<{
  agentId: string | null
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const detail = ref<AgentDetail | null>(null)
const loading = ref(false)
const error = ref(false)

watch(() => props.visible, async (val) => {
  if (val && props.agentId) {
    loading.value = true
    error.value = false
    try {
      const res = await getAgentDetail(props.agentId)
      detail.value = res.data.data
    } catch {
      detail.value = null
      error.value = true
    } finally {
      loading.value = false
    }
  } else if (!val) {
    detail.value = null
    error.value = false
  }
})

function statusTone(status: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'primary' | 'muted'> = {
    ACTIVE: 'success', ENABLED: 'success', DISABLED: 'muted', ERROR: 'danger', PUBLISHED: 'success', DRAFT: 'warning',
  }
  return map[status] || 'muted'
}

function prettyJson(raw: string | null): string {
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}
</script>

<template>
  <el-drawer
    :model-value="visible"
    :title="detail ? `${detail.name} (${detail.code})` : '智能体详情'"
    size="55%"
    @close="emit('close')"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <!-- Overview -->
        <div class="detail-section">
          <h4>概览</h4>
          <div class="detail-grid">
            <div class="detail-field"><span class="detail-label">ID</span><code>{{ detail.id }}</code></div>
            <div class="detail-field"><span class="detail-label">名称</span><span>{{ detail.name }}</span></div>
            <div class="detail-field"><span class="detail-label">编码</span><code>{{ detail.code }}</code></div>
            <div class="detail-field"><span class="detail-label">类型</span><span>{{ detail.type }}</span></div>
            <div class="detail-field"><span class="detail-label">状态</span><StatusPulse :status="detail.status" :tone="statusTone(detail.status)" /></div>
            <div class="detail-field"><span class="detail-label">描述</span><span>{{ detail.description || '-' }}</span></div>
          </div>
        </div>

        <!-- Latest Version -->
        <div class="detail-section">
          <h4>最新版本</h4>
          <div v-if="detail.latestVersion" class="detail-grid">
            <div class="detail-field"><span class="detail-label">版本 ID</span><code>{{ detail.latestVersion.id }}</code></div>
            <div class="detail-field"><span class="detail-label">版本号</span><span>{{ detail.latestVersion.versionNo }}</span></div>
            <div class="detail-field"><span class="detail-label">版本状态</span><StatusPulse :status="detail.latestVersion.status" :tone="statusTone(detail.latestVersion.status)" /></div>
            <div class="detail-field"><span class="detail-label">模型配置 ID</span><code v-if="detail.latestVersion.modelConfigId">{{ detail.latestVersion.modelConfigId }}</code><span v-else>-</span></div>
          </div>
          <EmptyState v-else description="暂无已发布版本" />
        </div>

        <!-- System Prompt -->
        <div class="detail-section">
          <h4>System Prompt</h4>
          <div v-if="detail.systemPrompt" class="detail-content-box">
            <pre class="detail-pre">{{ detail.systemPrompt }}</pre>
          </div>
          <EmptyState v-else description="暂无 System Prompt" />
        </div>

        <!-- Tool Policy -->
        <div class="detail-section">
          <h4>Tool Policy</h4>
          <div v-if="detail.toolPolicy" class="detail-content-box">
            <pre class="detail-pre">{{ prettyJson(detail.toolPolicy) }}</pre>
          </div>
          <EmptyState v-else description="暂无 Tool Policy" />
        </div>

        <!-- Execution Policy -->
        <div class="detail-section">
          <h4>Execution Policy</h4>
          <div v-if="detail.executionPolicy" class="detail-content-box">
            <pre class="detail-pre">{{ prettyJson(detail.executionPolicy) }}</pre>
          </div>
          <EmptyState v-else description="暂无 Execution Policy" />
        </div>
      </template>

      <ErrorState v-else-if="!loading && error" title="加载失败" message="无法加载智能体详情" />
    </div>
  </el-drawer>
</template>

<style scoped>
.detail-section {
  margin-bottom: 24px;
}
.detail-section h4 {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--app-border);
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.detail-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.detail-label {
  font-size: 11px;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.detail-field code {
  font-size: 12px;
  background: var(--app-panel);
  color: var(--app-text-soft);
  padding: 2px 8px;
  border-radius: var(--app-radius-sm);
  word-break: break-all;
}
.detail-field span {
  font-size: 13px;
  color: var(--app-text-soft);
}
.detail-content-box {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 14px 18px;
  max-height: 320px;
  overflow-y: auto;
}
.detail-pre {
  margin: 0;
  font-size: 12px;
  line-height: 1.55;
  color: var(--app-text-soft);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
}
</style>
