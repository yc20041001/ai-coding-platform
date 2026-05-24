<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElInput, ElTag, ElButton } from 'element-plus'
import {
  searchSimilarIncidents,
  type SimilarIncidentItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  incidentId: string
}>()

const loading = ref(false)
const error = ref(false)
const searchQuery = ref('')
const results = ref<SimilarIncidentItem[]>([])

const sortedResults = computed(() => {
  return [...results.value].sort((a, b) => b.score - a.score)
})

function severityTag(severity: string) {
  const map: Record<string, 'danger' | 'warning' | 'info'> = {
    CRITICAL: 'danger',
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'info',
    INFO: 'info',
  }
  return map[severity] || 'info'
}

function statusTag(status: string) {
  const map: Record<string, 'danger' | 'warning' | 'success' | 'info'> = {
    OPEN: 'danger',
    ACKNOWLEDGED: 'warning',
    RESOLVED: 'success',
    WONT_FIX: 'info',
    FALSE_POSITIVE: 'info',
  }
  return map[status] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = {
    OPEN: '待处理',
    ACKNOWLEDGED: '已确认',
    RESOLVED: '已解决',
    WONT_FIX: '不修复',
    FALSE_POSITIVE: '误报',
  }
  return map[status] || status
}

async function loadSimilar() {
  loading.value = true
  error.value = false
  try {
    const params: { query?: string; limit?: number } = { limit: 10 }
    if (searchQuery.value.trim()) {
      params.query = searchQuery.value.trim()
    }
    const res = await searchSimilarIncidents(props.incidentId, params)
    results.value = res.data.data || []
  } catch {
    error.value = true
    results.value = []
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  await loadSimilar()
}

onMounted(() => {
  loadSimilar()
})
</script>

<template>
  <TechPanel
    v-loading="loading"
    title="相似事件"
    data-testid="similar-incident-list"
  >
    <ErrorState
      v-if="error"
      title="加载失败"
      message="无法加载相似事件数据"
      retry-text="重试"
      @retry="loadSimilar"
    />

    <template v-if="!error">
      <!-- Search bar -->
      <div class="sil-search-bar" data-testid="sil-search-bar">
        <ElInput
          v-model="searchQuery"
          placeholder="搜索相似事件..."
          size="small"
          clearable
          data-testid="sil-search-input"
          @keyup.enter="handleSearch"
        />
        <ElButton
          size="small"
          type="primary"
          data-testid="sil-search-btn"
          @click="handleSearch"
        >
          搜索
        </ElButton>
      </div>

      <!-- Results list -->
      <div
        v-if="sortedResults.length > 0"
        class="sil-list"
        data-testid="sil-list"
      >
        <div
          v-for="item in sortedResults"
          :key="item.incidentId"
          class="sil-card"
          data-testid="sil-card"
        >
          <div class="sil-card__header">
            <span class="sil-card__title">{{ item.title }}</span>
            <ElTag
              :type="severityTag(item.severity)"
              size="small"
              effect="dark"
              class="sil-card__severity"
            >
              {{ item.severity }}
            </ElTag>
          </div>
          <div class="sil-card__meta">
            <div class="sil-card__meta-item">
              <span class="sil-card__meta-label">状态</span>
              <ElTag :type="statusTag(item.status)" size="small">
                {{ statusText(item.status) }}
              </ElTag>
            </div>
            <div class="sil-card__meta-item">
              <span class="sil-card__meta-label">相似度</span>
              <span class="sil-card__score">{{ (item.score * 100).toFixed(1) }}%</span>
            </div>
            <div class="sil-card__meta-item">
              <span class="sil-card__meta-label">匹配字段</span>
              <span class="sil-card__field">{{ item.matchedField }}</span>
            </div>
            <div v-if="item.createTime" class="sil-card__meta-item">
              <span class="sil-card__meta-label">创建时间</span>
              <span class="sil-card__time">{{ formatDateTime(item.createTime) }}</span>
            </div>
          </div>
          <div v-if="item.snippet" class="sil-card__snippet">
            {{ item.snippet }}
          </div>
        </div>
      </div>

      <EmptyState
        v-if="!loading && sortedResults.length === 0"
        title="无相似事件"
        description="未找到相似的事件记录"
      />
    </template>
  </TechPanel>
</template>

<style scoped>
.sil-search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.sil-search-bar .el-input {
  flex: 1;
}

.sil-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sil-card {
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  padding: 14px 16px;
  transition: border-color 0.2s, background 0.2s;
}

.sil-card:hover {
  border-color: rgba(56, 189, 248, 0.25);
  background: rgba(15, 23, 42, 0.7);
}

.sil-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.sil-card__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-bright);
  line-height: 1.4;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sil-card__severity {
  flex-shrink: 0;
}

.sil-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}

.sil-card__meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.sil-card__meta-label {
  font-size: 10px;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-right: 2px;
}

.sil-card__score {
  font-size: 12px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.sil-card__field {
  font-size: 12px;
  color: var(--app-text-soft);
}

.sil-card__time {
  font-size: 12px;
  color: var(--app-text-muted);
}

.sil-card__snippet {
  font-size: 11px;
  color: var(--app-text-muted);
  line-height: 1.5;
  padding: 8px 10px;
  background: rgba(0, 0, 0, 0.25);
  border-radius: 4px;
  border: 1px solid rgba(56, 189, 248, 0.06);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}
</style>
