<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElInput, ElSelect, ElOption } from 'element-plus'
import {
  buildCodeIndex,
  getCodeIndexSummary,
  searchCodeIndex,
  type CodeIndexSummaryResponse,
  type CodeSearchResponse,
  type CodeSearchResultResponse,
} from '@/modules/code-index/api'
import type { ApiError } from '@/shared/api/client'
import EmptyState from '@/shared/components/EmptyState.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import { formatDateTime } from '@/shared/utils/format'

const route = useRoute()
const projectId = route.params.projectId as string

const summary = ref<CodeIndexSummaryResponse | null>(null)
const loadingSummary = ref(false)
const building = ref(false)

const searchKeyword = ref('')
const searchType = ref('ALL')
const searchLanguage = ref('ALL')
const searchPathPrefix = ref('')
const searchLimit = ref(10)
const searchResults = ref<CodeSearchResultResponse[]>([])
const totalCount = ref(0)
const searching = ref(false)
const searched = ref(false)

async function loadSummary() {
  loadingSummary.value = true
  try {
    const res = await getCodeIndexSummary(projectId)
    summary.value = res.data.data
  } catch (e) {
    const err = e as ApiError
    if (err.code === 'NOT_FOUND') {
      summary.value = null
    } else {
      ElMessage.error('加载代码索引摘要失败')
    }
  } finally {
    loadingSummary.value = false
  }
}

async function handleBuild() {
  building.value = true
  try {
    const res = await buildCodeIndex(projectId, { branch: 'main', maxFiles: 100 })
    summary.value = res.data.data
    ElMessage.success('代码索引构建完成')
  } catch {
    ElMessage.error('代码索引构建失败')
  } finally {
    building.value = false
  }
}

async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  searching.value = true
  searched.value = false
  try {
    const res = await searchCodeIndex(projectId, {
      keyword: searchKeyword.value.trim(),
      searchType: searchType.value === 'ALL' ? undefined : searchType.value,
      language: searchLanguage.value === 'ALL' ? undefined : searchLanguage.value,
      pathPrefix: searchPathPrefix.value || undefined,
      limit: searchLimit.value,
    })
    searchResults.value = res.data.data.results || []
    totalCount.value = res.data.data.totalCount || 0
    searched.value = true
  } catch {
    ElMessage.error('代码搜索失败')
    searchResults.value = []
    totalCount.value = 0
  } finally {
    searching.value = false
  }
}
</script>

<template>
  <div class="code-index-page">
    <!-- Summary Section -->
    <div class="ci-section" data-testid="code-index-summary-section">
      <h3 class="ci-section-title">代码索引摘要</h3>
      <div v-if="loadingSummary" class="ci-loading">加载中...</div>
      <div v-else-if="summary" class="ci-summary-grid">
        <div class="ci-summary-card" data-testid="code-index-file-count">
          <div class="ci-summary-value">{{ summary.fileCount }}</div>
          <div class="ci-summary-label">文件</div>
        </div>
        <div class="ci-summary-card" data-testid="code-index-symbol-count">
          <div class="ci-summary-value">{{ summary.symbolCount }}</div>
          <div class="ci-summary-label">符号</div>
        </div>
        <div class="ci-summary-card" data-testid="code-index-chunk-count">
          <div class="ci-summary-value">{{ summary.chunkCount }}</div>
          <div class="ci-summary-label">切片</div>
        </div>
        <div class="ci-summary-card" data-testid="code-index-indexed-at">
          <div class="ci-summary-value-small">{{ summary.indexedAt ? formatDateTime(summary.indexedAt) : '未索引' }}</div>
          <div class="ci-summary-label">索引时间</div>
        </div>
        <div class="ci-summary-card" data-testid="code-index-mock-badge">
          <div class="ci-summary-value-mock">{{ summary.mock ? '模拟' : '真实' }}</div>
          <div class="ci-summary-label">模式</div>
        </div>
      </div>
      <EmptyState v-else title="未构建" description="代码索引尚未构建，请先构建索引" />
    </div>

    <NeonDivider tone="primary" style="margin:20px 0" />

    <!-- Build Button -->
    <div class="ci-section" data-testid="code-index-build-section">
      <GlowButton
        :loading="building"
        accent="primary"
        data-testid="code-index-build-btn"
        @click="handleBuild"
      >
        构建代码索引
      </GlowButton>
      <span class="ci-hint">构建索引将扫描工作区代码，生成文件、符号和切片索引</span>
    </div>

    <NeonDivider tone="primary" style="margin:20px 0" />

    <!-- Search Section -->
    <div class="ci-section" data-testid="code-index-search-section">
      <h3 class="ci-section-title">代码搜索</h3>
      <div class="ci-search-form">
        <div class="ci-search-row">
          <div class="ci-search-field ci-search-field--grow">
            <label class="ci-search-label">关键词</label>
            <ElInput
              v-model="searchKeyword"
              placeholder="输入搜索关键词"
              clearable
              data-testid="code-index-search-keyword"
            />
          </div>
          <div class="ci-search-field">
            <label class="ci-search-label">搜索类型</label>
            <ElSelect
              v-model="searchType"
              data-testid="code-index-search-type"
            >
              <ElOption label="全部" value="ALL" />
              <ElOption label="文件" value="FILE" />
              <ElOption label="符号" value="SYMBOL" />
              <ElOption label="片段" value="CHUNK" />
            </ElSelect>
          </div>
          <div class="ci-search-field">
            <label class="ci-search-label">语言</label>
            <ElSelect
              v-model="searchLanguage"
              data-testid="code-index-search-language"
            >
              <ElOption label="全部" value="ALL" />
              <ElOption label="Java" value="java" />
              <ElOption label="TypeScript" value="ts" />
              <ElOption label="JavaScript" value="js" />
              <ElOption label="Vue" value="vue" />
              <ElOption label="SQL" value="sql" />
              <ElOption label="Markdown" value="md" />
            </ElSelect>
          </div>
          <div class="ci-search-field">
            <label class="ci-search-label">路径</label>
            <ElInput
              v-model="searchPathPrefix"
              placeholder="路径前缀"
              clearable
              data-testid="code-index-search-path"
            />
          </div>
          <div class="ci-search-field" style="max-width:100px">
            <label class="ci-search-label">数量</label>
            <ElInput
              v-model.number="searchLimit"
              type="number"
              :min="1"
              :max="50"
              data-testid="code-index-search-limit"
            />
          </div>
          <div class="ci-search-field ci-search-field--action">
            <GlowButton
              :loading="searching"
              accent="primary"
              size="small"
              data-testid="code-index-search-btn"
              @click="handleSearch"
            >
              搜索
            </GlowButton>
          </div>
        </div>
      </div>
    </div>

    <!-- Results Section -->
    <div
      v-if="searched || searchResults.length > 0"
      class="ci-section"
      data-testid="code-index-results-section"
    >
      <h3 class="ci-section-title">
        搜索结果
        <span v-if="searched" class="ci-result-count">(共 {{ totalCount }} 条)</span>
      </h3>
      <div v-if="searchResults.length === 0" class="ci-no-results">
        未找到匹配结果
      </div>
      <div v-else class="ci-result-list">
        <div
          v-for="(result, index) in searchResults"
          :key="index"
          class="ci-result-item"
          :data-testid="`code-index-result-${index}`"
        >
          <div class="ci-result-header">
            <span class="ci-result-type" :class="`ci-result-type--${result.resultType.toLowerCase()}`">
              {{ result.resultType }}
            </span>
            <span class="ci-result-file">{{ result.filePath }}</span>
            <span v-if="result.startLine > 0" class="ci-result-lines">
              L{{ result.startLine }}-{{ result.endLine }}
            </span>
          </div>
          <div v-if="result.symbolName" class="ci-result-symbol">
            符号: {{ result.symbolName }} ({{ result.symbolType }})
          </div>
          <div v-if="result.snippet" class="ci-result-snippet">
            <pre><code>{{ result.snippet }}</code></pre>
          </div>
        </div>
      </div>
    </div>

    <!-- Safety Note -->
    <NeonDivider tone="muted" style="margin:20px 0" />
    <div class="ci-safety" data-testid="readonly-adapter-safety-note">
      <p><strong>安全提示：</strong>代码索引为只读操作。系统从工作区读取文件内容并构建索引。</p>
      <p>机密信息（如 API Key、Token、密码等）会在索引前自动脱敏。敏感路径（如 .env, .git, node_modules）会被跳过。</p>
      <p>系统不会写入文件系统，不会执行 Git 写操作。</p>
    </div>
  </div>
</template>

<style scoped>
.code-index-page {
  padding: 8px 0;
}

.ci-section {
  margin-bottom: 16px;
}

.ci-section-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--app-text-soft);
  margin: 0 0 12px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.ci-loading {
  color: var(--app-text-muted);
  font-size: 13px;
  padding: 24px 0;
}

.ci-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 12px;
}

.ci-summary-card {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(56, 189, 248, 0.15);
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}

.ci-summary-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--app-text-bright);
  line-height: 1.2;
}

.ci-summary-value-small {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-soft);
  line-height: 1.2;
}

.ci-summary-value-mock {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-warning);
  line-height: 1.2;
}

.ci-summary-label {
  font-size: 11px;
  color: var(--app-text-muted);
  margin-top: 4px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.ci-hint {
  margin-left: 12px;
  font-size: 12px;
  color: var(--app-text-muted);
}

.ci-search-form {
  max-width: 900px;
}

.ci-search-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.ci-search-field {
  min-width: 120px;
}

.ci-search-field--grow {
  flex: 1;
  min-width: 180px;
}

.ci-search-field--action {
  display: flex;
  align-items: flex-end;
  min-width: auto;
}

.ci-search-label {
  display: block;
  font-size: 11px;
  color: var(--app-text-muted);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.ci-result-count {
  font-weight: 400;
  font-size: 12px;
  color: var(--app-text-muted);
  margin-left: 6px;
}

.ci-no-results {
  color: var(--app-text-muted);
  font-size: 13px;
  padding: 24px 0;
}

.ci-result-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ci-result-item {
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(56, 189, 248, 0.1);
  border-radius: 6px;
  padding: 12px;
}

.ci-result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.ci-result-type {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 3px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.ci-result-type--file {
  background: rgba(56, 189, 248, 0.15);
  color: #38bdf8;
}

.ci-result-type--symbol {
  background: rgba(251, 191, 36, 0.15);
  color: #fbbf24;
}

.ci-result-type--chunk {
  background: rgba(52, 211, 153, 0.15);
  color: #34d399;
}

.ci-result-file {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-soft);
  font-family: 'SF Mono', 'Cascadia Code', monospace;
}

.ci-result-lines {
  font-size: 11px;
  color: var(--app-text-muted);
  font-family: 'SF Mono', 'Cascadia Code', monospace;
}

.ci-result-symbol {
  font-size: 12px;
  color: var(--app-text-muted);
  margin-top: 4px;
}

.ci-result-snippet {
  margin-top: 6px;
  max-height: 120px;
  overflow: auto;
  border-radius: 4px;
}

.ci-result-snippet pre {
  margin: 0;
  padding: 8px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
}

.ci-result-snippet code {
  color: var(--app-text-soft);
  font-family: 'SF Mono', 'Cascadia Code', monospace;
  white-space: pre-wrap;
  word-break: break-all;
}

.ci-safety {
  padding: 12px 0;
  font-size: 12px;
  color: var(--app-text-muted);
  line-height: 1.7;
}

.ci-safety p {
  margin: 0;
}
</style>
