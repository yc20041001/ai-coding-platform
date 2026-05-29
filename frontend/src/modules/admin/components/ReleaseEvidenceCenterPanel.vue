<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  getEvidenceBundle,
  generateEvidenceBundle,
  updateEvidenceBundleStatus,
  generateExecutiveReport,
  type ReleaseEvidenceBundleItem,
  type ReleaseExecutiveReportItem,
} from '@/modules/admin/api'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ErrorState from '@/shared/components/ErrorState.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import { ElTag, ElButton, ElDialog, ElMessage } from 'element-plus'
import { formatDateTime } from '@/shared/utils/format'

const props = defineProps<{
  projectId?: string | null
  planId?: string | null
}>()

const bundle = ref<ReleaseEvidenceBundleItem | null>(null)
const loading = ref(false)
const error = ref(false)
const reportData = ref<ReleaseExecutiveReportItem | null>(null)
const reportDialogVisible = ref(false)
const markdownDialogVisible = ref(false)

function loadBundle() {
  if (!props.planId) return
  loading.value = true
  error.value = false
  getEvidenceBundle(props.planId)
    .then(res => { bundle.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleGenerate() {
  if (!props.planId || !props.projectId) return
  try {
    const res = await generateEvidenceBundle(props.planId, { projectId: props.projectId })
    bundle.value = res.data.data
    ElMessage.success('证据包生成成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '生成失败')
  }
}

async function handlePublish() {
  if (!props.planId) return
  try {
    const res = await updateEvidenceBundleStatus(props.planId, 'PUBLISHED')
    bundle.value = res.data.data
    ElMessage.success('证据包已发布')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '发布失败')
  }
}

async function handleArchive() {
  if (!props.planId) return
  try {
    const res = await updateEvidenceBundleStatus(props.planId, 'ARCHIVED')
    bundle.value = res.data.data
    ElMessage.success('证据包已归档')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '归档失败')
  }
}

function bundleStatusTag(status: string) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    DRAFT: 'info',
    GENERATED: 'success',
    PUBLISHED: 'success',
    ARCHIVED: 'warning',
  }
  return map[status] || 'info'
}

function bundleStatusLabel(status: string): string {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    GENERATED: '已生成',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档',
  }
  return map[status] || status
}

async function handleExportReport() {
  if (!props.planId) return
  try {
    const res = await generateExecutiveReport(props.planId)
    reportData.value = res.data.data
    reportDialogVisible.value = true
  } catch {
    ElMessage.error('生成执行报告失败')
  }
}

watch(() => props.planId, () => { if (props.planId) loadBundle() }, { immediate: true })
</script>

<template>
  <div>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">发布证据中心</span>
      <div style="display:flex;gap:4px">
        <ElButton v-if="props.planId" size="small" type="primary" @click="handleGenerate">生成证据包</ElButton>
        <ElButton v-if="props.planId" size="small" @click="handleExportReport">导出执行报告</ElButton>
      </div>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取证据包" retry-text="重试" @retry="loadBundle" />

    <div v-if="!props.planId" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">请先选择一个 Rollout Plan</div>

    <div v-if="bundle && props.planId" v-loading="loading">
      <div style="display:flex;gap:8px;margin-bottom:10px;flex-wrap:wrap">
        <ElTag :type="bundleStatusTag(bundle.bundleStatus)" size="small" effect="dark">{{ bundleStatusLabel(bundle.bundleStatus) }}</ElTag>
        <ElTag v-if="bundle.generatedAt" size="small" effect="plain">生成于 {{ formatDateTime(bundle.generatedAt) }}</ElTag>
      </div>

      <div v-if="bundle.bundleStatus === 'GENERATED'" style="display:flex;gap:4px;margin-bottom:10px">
        <ElButton size="small" @click="handlePublish">发布</ElButton>
        <ElButton size="small" @click="handleArchive">归档</ElButton>
      </div>
      <div v-if="bundle.bundleStatus === 'PUBLISHED'" style="display:flex;gap:4px;margin-bottom:10px">
        <ElButton size="small" @click="handleArchive">归档</ElButton>
      </div>

      <NeonDivider tone="muted" style="margin:12px 0" />

      <div v-if="bundle.summaryMarkdown" style="white-space:pre-wrap;font-family:'SF Mono','Cascadia Code',monospace;font-size:11px;line-height:1.6;background:rgba(15,23,42,0.3);padding:12px;border-radius:6px;max-height:300px;overflow-y:auto">
        {{ bundle.summaryMarkdown }}
      </div>

      <div v-if="bundle.evidenceJson" style="margin-top:8px;font-size:11px;color:var(--app-text-muted)">
        <ElButton size="small" link @click="markdownDialogVisible = true">查看原始证据 JSON</ElButton>
      </div>
    </div>

    <EmptyState v-if="!loading && !bundle && props.planId && !error" description="暂无证据包，请先生成" />

    <ElDialog v-model="reportDialogVisible" title="执行摘要报告" width="70%" top="5vh">
      <div v-if="reportData" style="white-space:pre-wrap;font-family:'SF Mono','Cascadia Code',monospace;font-size:12px;line-height:1.6;background:rgba(15,23,42,0.3);padding:16px;border-radius:8px;max-height:60vh;overflow-y:auto">
        {{ reportData.reportMarkdown }}
      </div>
    </ElDialog>

    <ElDialog v-model="markdownDialogVisible" title="原始证据 JSON" width="60%">
      <div v-if="bundle?.evidenceJson" style="white-space:pre-wrap;font-family:'SF Mono',monospace;font-size:11px;background:rgba(15,23,42,0.3);padding:12px;border-radius:6px;max-height:50vh;overflow-y:auto">
        {{ bundle.evidenceJson }}
      </div>
    </ElDialog>
  </div>
</template>
