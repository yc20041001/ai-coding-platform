<script setup lang="ts">
import { ref, watch } from 'vue'
import { getAgentVersions, type AgentVersion } from '@/modules/agent/api'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  agentId: string | null
  agentName: string
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const versions = ref<AgentVersion[]>([])
const loading = ref(false)
const error = ref(false)
const selectedVersion = ref<AgentVersion | null>(null)

watch(() => props.visible, async (val) => {
  if (val && props.agentId) {
    loading.value = true
    error.value = false
    selectedVersion.value = null
    try {
      const res = await getAgentVersions(props.agentId)
      versions.value = res.data.data
      if (versions.value.length > 0) {
        selectedVersion.value = versions.value[0]
      }
    } catch {
      versions.value = []
      error.value = true
    } finally {
      loading.value = false
    }
  } else if (!val) {
    versions.value = []
    selectedVersion.value = null
    error.value = false
  }
})

function selectVersion(v: AgentVersion) {
  selectedVersion.value = v
}

function statusTone(status: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'primary' | 'muted'> = {
    PUBLISHED: 'success', DRAFT: 'warning', ARCHIVED: 'muted',
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
    :title="`智能体版本 - ${props.agentName}`"
    size="65%"
    data-testid="agent-version-drawer"
    @close="emit('close')"
  >
    <div v-loading="loading">
      <template v-if="!loading && !error && versions.length > 0">
        <div class="av-layout">
          <!-- Version List -->
          <div class="av-list" data-testid="agent-version-list">
            <div class="av-list-header">版本列表 ({{ versions.length }})</div>
            <div
              v-for="v in versions"
              :key="v.id"
              class="av-version-item"
              :class="{ 'av-selected': selectedVersion?.id === v.id }"
              data-testid="agent-version-item"
              @click="selectVersion(v)"
            >
              <div class="av-version-top">
                <span class="av-version-no">{{ v.versionNo }}</span>
                <StatusPulse :status="v.status" :tone="statusTone(v.status)" />
              </div>
              <div class="av-version-meta">
                <span v-if="v.publishTime">发布: {{ formatDateTime(v.publishTime) }}</span>
                <span>创建: {{ formatDateTime(v.createTime) }}</span>
              </div>
            </div>
          </div>

          <!-- Version Detail -->
          <div class="av-detail" data-testid="agent-version-detail">
            <template v-if="selectedVersion">
              <div class="av-detail-header">
                <span class="av-detail-version">{{ selectedVersion.versionNo }}</span>
                <StatusPulse :status="selectedVersion.status" :tone="statusTone(selectedVersion.status)" />
              </div>

              <div class="detail-section">
                <h4>System Prompt</h4>
                <div v-if="selectedVersion.systemPrompt" class="detail-content-box">
                  <pre class="detail-pre">{{ selectedVersion.systemPrompt }}</pre>
                </div>
                <EmptyState v-else description="暂无 System Prompt" />
              </div>

              <div class="detail-section">
                <h4>Tool Policy</h4>
                <div v-if="selectedVersion.toolPolicy" class="detail-content-box">
                  <pre class="detail-pre">{{ prettyJson(selectedVersion.toolPolicy) }}</pre>
                </div>
                <EmptyState v-else description="暂无 Tool Policy" />
              </div>

              <div class="detail-section">
                <h4>Execution Policy</h4>
                <div v-if="selectedVersion.executionPolicy" class="detail-content-box">
                  <pre class="detail-pre">{{ prettyJson(selectedVersion.executionPolicy) }}</pre>
                </div>
                <EmptyState v-else description="暂无 Execution Policy" />
              </div>
            </template>
            <EmptyState v-else description="请选择一个版本查看详情" />
          </div>
        </div>
      </template>

      <EmptyState v-if="!loading && !error && versions.length === 0" description="该智能体暂无版本" />
      <ErrorState v-else-if="!loading && error" title="加载失败" message="无法加载智能体版本" />
    </div>
  </el-drawer>
</template>

<style scoped>
.av-layout {
  display: flex;
  gap: 0;
  height: 100%;
}

.av-list {
  width: 260px;
  min-width: 260px;
  border-right: 1px solid var(--app-border);
  overflow-y: auto;
  padding-right: 0;
}

.av-list-header {
  font-size: 11px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 0 0 12px 0;
}

.av-version-item {
  padding: 12px 14px;
  margin-right: 12px;
  border-radius: var(--app-radius);
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 4px;
  border: 1px solid transparent;
}

.av-version-item:hover {
  background: var(--app-panel);
}

.av-selected {
  background: var(--app-primary-soft);
  border-color: rgba(56, 189, 248, 0.25);
}

.av-version-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.av-version-no {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
  font-family: monospace;
}

.av-version-meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
  font-size: 10px;
  color: var(--app-text-muted);
}

.av-detail {
  flex: 1;
  overflow-y: auto;
  padding-left: 20px;
}

.av-detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--app-border);
}

.av-detail-version {
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text);
  font-family: monospace;
}

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
