<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  listKnowledgeBases, createKnowledgeBase, uploadDocument,
  listDocuments, listChunks, searchRag,
  type KnowledgeBaseItem, type KnowledgeDocument, type DocumentChunk, type RagReference,
} from '@/modules/knowledge/api'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
import MarkdownRenderer from '@/shared/components/MarkdownRenderer.vue'
import ChunkPreviewDrawer from '@/modules/knowledge/components/ChunkPreviewDrawer.vue'
import { formatDateTime, formatNumber } from '@/shared/utils/format'

const route = useRoute()
const projectId = route.params.projectId as string

const kbs = ref<KnowledgeBaseItem[]>([])
const loadingKbs = ref(false)

const selectedKbId = ref<string | null>(null)
const documents = ref<KnowledgeDocument[]>([])
const loadingDocs = ref(false)

const createKbVisible = ref(false)
const createKbForm = ref({ name: '', description: '' })
const creatingKb = ref(false)

const uploadVisible = ref(false)
const uploadForm = ref({ title: '', documentType: 'MARKDOWN', content: '' })
const uploading = ref(false)

const chunkVisible = ref(false)
const chunks = ref<DocumentChunk[]>([])
const loadingChunks = ref(false)
const chunkDocTitle = ref('')

const searchQuery = ref('')
const searching = ref(false)
const searchResults = ref<RagReference[]>([])
const searchError = ref(false)

onMounted(() => loadKbs())

async function loadKbs() {
  loadingKbs.value = true
  try {
    const res = await listKnowledgeBases(projectId, 1, 20)
    kbs.value = res.data.data.records
  } catch { /* handled */ } finally { loadingKbs.value = false }
}

async function selectKb(kbId: string) {
  selectedKbId.value = kbId
  loadingDocs.value = true
  try {
    const res = await listDocuments(kbId, 1, 20)
    documents.value = res.data.data.records
  } catch { /* handled */ } finally { loadingDocs.value = false }
}

async function handleCreateKb() {
  creatingKb.value = true
  try {
    await createKnowledgeBase(projectId, createKbForm.value.name, createKbForm.value.description)
    ElMessage.success('知识库已创建')
    createKbVisible.value = false
    createKbForm.value = { name: '', description: '' }
    loadKbs()
  } catch { /* handled */ } finally { creatingKb.value = false }
}

async function handleUpload() {
  if (!selectedKbId.value) return
  uploading.value = true
  try {
    await uploadDocument(projectId, {
      knowledgeBaseId: selectedKbId.value,
      title: uploadForm.value.title,
      documentType: uploadForm.value.documentType,
      content: uploadForm.value.content,
    })
    ElMessage.success('文档已上传')
    uploadVisible.value = false
    uploadForm.value = { title: '', documentType: 'MARKDOWN', content: '' }
    selectKb(selectedKbId.value)
  } catch { /* handled */ } finally { uploading.value = false }
}

async function handleViewChunks(docId: string, docTitle: string) {
  loadingChunks.value = true
  chunkVisible.value = true
  chunkDocTitle.value = docTitle
  try {
    const res = await listChunks(docId)
    chunks.value = res.data.data
  } catch { /* handled */ } finally { loadingChunks.value = false }
}

async function handleSearch() {
  if (!searchQuery.value.trim()) return
  searching.value = true
  searchError.value = false
  try {
    const res = await searchRag(projectId, searchQuery.value)
    searchResults.value = res.data.data.references || []
  } catch {
    searchError.value = true
    searchResults.value = []
  } finally { searching.value = false }
}
</script>

<template>
  <div class="kb-layout">
    <aside class="kb-sidebar">
      <div class="kb-sidebar-header">
        <div class="kb-sidebar-title-row">
          <span class="kb-sidebar-title">知识库</span>
          <StatusPulse status="Active" tone="success" />
        </div>
        <p class="kb-sidebar-sub">项目记忆系统</p>
        <GlowButton accent="primary" size="small" style="width:100%;margin-top:10px" @click="createKbVisible = true">+ New KB</GlowButton>
      </div>
      <div v-loading="loadingKbs" class="kb-list">
        <div
          v-for="kb in kbs" :key="kb.id"
          class="kb-item" :class="{ active: kb.id === selectedKbId }"
          @click="selectKb(kb.id)"
        >
          <div class="kb-item-bar" />
          <div class="kb-name">{{ kb.name }}</div>
          <div class="kb-meta">{{ kb.documentCount }} docs / {{ formatNumber(kb.chunkCount) }} chunks</div>
        </div>
        <EmptyState v-if="!loadingKbs && kbs.length === 0" description="暂无知识库" />
      </div>
    </aside>

    <section class="kb-main">
      <div v-if="!selectedKbId" class="kb-empty">
        <span class="kb-empty-icon">◈</span>
        <span>选择一个知识库查看文档</span>
      </div>
      <template v-else>
        <div class="kb-toolbar">
          <span class="kb-section-title">文档</span>
          <GlowButton accent="success" size="small" @click="uploadVisible = true">+ Upload</GlowButton>
        </div>

        <el-table :data="documents" v-loading="loadingDocs" size="small" style="width:100%">
          <el-table-column prop="title" label="标题" min-width="180">
            <template #default="{ row }">
              <span style="font-weight:600;color:var(--app-text)">{{ row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="documentType" label="类型" width="100" />
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <StatusPulse
                :status="row.status"
                :tone="row.status === 'COMPLETED' ? 'success' : row.status === 'PENDING' ? 'warning' : 'muted'"
              />
            </template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="切片" width="90" />
          <el-table-column label="创建时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="handleViewChunks(row.id, row.title)">切片</el-button>
            </template>
          </el-table-column>
        </el-table>

        <NeonDivider tone="primary" style="margin-top:24px" />

        <div class="kb-search">
          <span class="kb-search-label">RAG 检索</span>
          <div class="kb-search-row">
            <el-input v-model="searchQuery" placeholder="搜索知识内容..." size="small" style="width:320px" @keyup.enter="handleSearch" />
            <el-button size="small" type="primary" :loading="searching" @click="handleSearch">搜索</el-button>
          </div>
        </div>
        <div v-if="searchResults.length > 0" class="search-results">
          <div v-for="(ref, idx) in searchResults" :key="idx" class="search-result-item">
            <div class="sr-header">
              <el-tag size="small">{{ ref.referenceType }}</el-tag>
              <span class="sr-title">{{ ref.title || 'Untitled' }}</span>
              <span v-if="ref.filePath" class="sr-path">{{ ref.filePath }}</span>
              <el-tag size="small" :type="ref.score > 0.7 ? 'success' : ref.score > 0.4 ? 'warning' : 'info'">
                {{ (ref.score * 100).toFixed(0) }}%
              </el-tag>
            </div>
            <div class="sr-score-bar">
              <div class="sr-score-fill" :style="{ width: (ref.score * 100) + '%' }" />
            </div>
            <div class="sr-snippet" v-if="ref.snippet">
              <MarkdownRenderer :content="ref.snippet.substring(0, 300)" />
            </div>
          </div>
        </div>
        <ErrorState v-else-if="searchError" title="搜索失败" message="RAG 检索出现错误" />
        <EmptyState v-else-if="!searching && searchQuery && searchResults.length === 0 && !searchError" description="暂无匹配结果" />
      </template>
    </section>

    <!-- Create KB Dialog -->
    <el-dialog v-model="createKbVisible" title="创建知识库" width="450px">
      <el-form label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="createKbForm.name" placeholder="知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createKbForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createKbVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingKb" @click="handleCreateKb">创建</el-button>
      </template>
    </el-dialog>

    <!-- Upload Doc Dialog -->
    <el-dialog v-model="uploadVisible" title="上传文档" width="550px">
      <el-form label-position="top">
        <el-form-item label="标题" required>
          <el-input v-model="uploadForm.title" placeholder="文档标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="uploadForm.documentType" style="width:100%">
            <el-option label="Markdown" value="MARKDOWN" />
            <el-option label="文本" value="TEXT" />
            <el-option label="代码" value="CODE" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="uploadForm.content" type="textarea" :rows="8" placeholder="文档内容..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <ChunkPreviewDrawer
      :visible="chunkVisible"
      :chunks="chunks"
      :document-title="chunkDocTitle"
      :loading="loadingChunks"
      @close="chunkVisible = false"
    />
  </div>
</template>

<style scoped>
.kb-layout {
  display: flex;
  gap: 0;
  height: calc(100vh - 48px - 88px);
  overflow: hidden;
}

.kb-sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid var(--app-border);
  background: var(--app-surface);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  display: flex;
  flex-direction: column;
}
.kb-sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--app-border);
}
.kb-sidebar-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.kb-sidebar-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--app-text);
  text-transform: uppercase;
  letter-spacing: 0.8px;
}
.kb-sidebar-sub {
  font-size: 11px;
  color: var(--app-text-muted);
  margin-top: 4px;
}
.kb-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.kb-main {
  flex: 1;
  background: var(--app-bg-soft);
  padding: 20px;
  overflow-y: auto;
}
.kb-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
  color: var(--app-text-muted);
  font-size: 14px;
}
.kb-empty-icon {
  font-size: 48px;
  opacity: 0.2;
}

.kb-item {
  padding: 10px 12px;
  border-radius: var(--app-radius-sm);
  cursor: pointer;
  margin-bottom: 2px;
  border: 1px solid transparent;
  transition: all 0.2s var(--app-ease-out);
  position: relative;
  overflow: hidden;
}
.kb-item-bar {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background: var(--app-primary);
  border-radius: 0 2px 2px 0;
  transition: height 0.2s var(--app-ease-out);
}
.kb-item:hover { background: var(--app-panel-hover); }
.kb-item.active {
  background: var(--app-primary-soft);
  border-color: rgba(56, 189, 248, 0.2);
}
.kb-item.active .kb-item-bar { height: 60%; }
.kb-name { font-size: 13px; font-weight: 500; color: var(--app-text-soft); }
.kb-meta { font-size: 11px; color: var(--app-text-muted); margin-top: 2px; }

.kb-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.kb-section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text);
}

.kb-search {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.kb-search-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
}
.kb-search-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-results { margin-top: 14px; }
.search-result-item {
  padding: 14px;
  margin-bottom: 8px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-panel);
  transition: border-color 0.2s;
}
.search-result-item:hover {
  border-color: var(--app-border-strong);
}
.sr-header { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; flex-wrap: wrap; }
.sr-title { font-weight: 600; color: var(--app-text-soft); }
.sr-path { font-size: 11px; color: var(--app-text-muted); font-family: monospace; }
.sr-score-bar {
  height: 2px;
  background: var(--app-panel-hover);
  border-radius: 1px;
  margin-bottom: 8px;
}
.sr-score-fill {
  height: 100%;
  background: var(--app-primary);
  border-radius: 1px;
  transition: width 0.5s var(--app-ease-out);
}
.sr-snippet { font-size: 13px; color: var(--app-text-soft); line-height: 1.6; }
</style>
