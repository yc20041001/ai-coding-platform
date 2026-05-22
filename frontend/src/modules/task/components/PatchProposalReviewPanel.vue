<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getPatchProposalReview,
  submitPatchProposalReviewDecision,
  type TaskArtifact,
  type PatchProposalReview,
} from '@/modules/task/api'
import MarkdownRenderer from '@/shared/components/MarkdownRenderer.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'

const props = defineProps<{ artifact: TaskArtifact }>()

const review = ref<PatchProposalReview | null>(null)
const loading = ref(false)
const submitting = ref(false)
const loaded = ref(false)

// Checklist state
const checklist = ref({
  matchesRequirement: false,
  noSensitiveData: false,
  noFileWritten: true,
  noGitOperation: true,
  readyForManualImplementation: false,
})
const safetyConfirmed = ref(false)
const comment = ref('')

// Parse files from artifact content
const parsedFiles = computed(() => {
  if (!props.artifact.content) return []
  const files: string[] = []
  const lines = props.artifact.content.split('\n')
  for (const line of lines) {
    const match = line.match(/^diff --git a\/(\S+) b\/(\S+)$/)
    if (match) {
      files.push(match[2] || match[1])
    }
  }
  return files
})

const hasDecision = computed(() => review.value?.decision != null)

const decisionLabel = computed(() => {
  const map: Record<string, string> = {
    ACCEPTED_AS_PLAN: '接受为计划',
    REJECTED: '已拒绝',
    NEEDS_CHANGES: '需要修改',
    MARKED_REVIEWED: '已审阅',
  }
  return review.value?.decision ? map[review.value.decision] || review.value.decision : ''
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await getPatchProposalReview(props.artifact.id)
    review.value = res.data.data
    loaded.value = true
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载审阅信息失败')
  } finally {
    loading.value = false
  }
})

async function handleSubmitDecision(decision: string) {
  if (!safetyConfirmed.value) {
    ElMessage.warning('请先确认安全提示')
    return
  }
  if (!checklist.value.noFileWritten) {
    ElMessage.warning('请确认未写入文件')
    return
  }
  if (!checklist.value.noGitOperation) {
    ElMessage.warning('请确认未执行 Git 操作')
    return
  }

  submitting.value = true
  try {
    const res = await submitPatchProposalReviewDecision(props.artifact.id, {
      decision,
      comment: comment.value || undefined,
      safetyConfirmed: true,
      checklist: checklist.value as unknown as Record<string, unknown>,
    })
    review.value = res.data.data
    ElMessage.success('审阅决策已提交')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '提交审阅决策失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="ppr-panel" v-loading="loading" data-testid="patch-review-panel">
    <!-- Safety Banner -->
    <div class="ppr-safety-banner" data-testid="patch-review-safety-banner">
      <span class="ppr-safety-icon">⚠</span>
      <span>安全提示：该补丁提案仅用于审阅。系统未写入文件，未执行 git apply，未提交代码。</span>
    </div>

    <!-- Review Status -->
    <div v-if="review && review.status === 'REVIEWED'" class="ppr-status-bar" data-testid="patch-review-status">
      <el-tag type="success" effect="dark" data-testid="patch-review-decision-tag">
        已审阅: {{ decisionLabel }}
      </el-tag>
      <span v-if="review.reviewComment" class="ppr-status-comment">意见: {{ review.reviewComment }}</span>
    </div>
    <div v-else-if="review && review.status === 'PENDING'" class="ppr-status-bar">
      <el-tag type="warning" effect="dark">待审阅</el-tag>
      <span class="ppr-status-hint">请在下方完成审阅并提交决策。</span>
    </div>

    <!-- File List -->
    <div class="ppr-section" data-testid="patch-review-file-list">
      <div class="ppr-section-title">涉及文件</div>
      <div v-if="parsedFiles.length > 0" class="ppr-file-items">
        <div v-for="file in parsedFiles" :key="file" class="ppr-file-item" data-testid="patch-review-file-item">
          <span class="ppr-file-icon">◇</span>
          <span class="ppr-file-path">{{ file }}</span>
        </div>
      </div>
      <div v-else class="ppr-no-files">未解析到文件列表</div>
    </div>

    <NeonDivider tone="primary" style="margin:12px 0" />

    <!-- Diff Content -->
    <div class="ppr-section" data-testid="patch-review-diff">
      <div class="ppr-section-title">补丁内容</div>
      <div class="ppr-diff-content">
        <MarkdownRenderer :content="artifact.content || ''" />
      </div>
    </div>

    <!-- Checklist -->
    <div class="ppr-section" data-testid="patch-review-checklist">
      <div class="ppr-section-title">审阅检查清单</div>
      <div class="ppr-checklist">
        <label class="ppr-check-item" data-testid="patch-review-check-matches">
          <el-checkbox v-model="checklist.matchesRequirement" />
          <span>是否符合任务需求</span>
        </label>
        <label class="ppr-check-item" data-testid="patch-review-check-sensitive">
          <el-checkbox v-model="checklist.noSensitiveData" />
          <span>是否无敏感信息</span>
        </label>
        <label class="ppr-check-item ppr-check-readonly" data-testid="patch-review-check-filewrite">
          <el-checkbox v-model="checklist.noFileWritten" :disabled="true" />
          <span>是否确认未写入文件</span>
          <span class="ppr-check-note">(系统保证未写入)</span>
        </label>
        <label class="ppr-check-item ppr-check-readonly" data-testid="patch-review-check-gitop">
          <el-checkbox v-model="checklist.noGitOperation" :disabled="true" />
          <span>是否确认未执行 Git 操作</span>
          <span class="ppr-check-note">(系统保证未执行)</span>
        </label>
        <label class="ppr-check-item" data-testid="patch-review-check-ready">
          <el-checkbox v-model="checklist.readyForManualImplementation" />
          <span>是否可作为后续手工实现计划</span>
        </label>
      </div>
    </div>

    <!-- Safety Confirmed -->
    <div class="ppr-safety-confirm" data-testid="patch-review-safety-confirmed">
      <label>
        <el-checkbox v-model="safetyConfirmed" />
        <span class="ppr-confirm-text">我确认：该补丁提案仅作为审阅参考，未被实际应用。</span>
      </label>
    </div>

    <!-- Comment -->
    <div class="ppr-comment-section">
      <el-input
        v-model="comment"
        type="textarea"
        :rows="2"
        placeholder="审阅意见（可选）"
        data-testid="patch-review-comment"
      />
    </div>

    <!-- Decision Buttons -->
    <div v-if="!review || review.status === 'PENDING'" class="ppr-actions">
      <el-button
        type="success"
        :loading="submitting"
        :disabled="!safetyConfirmed"
        data-testid="btn-accept-patch-plan"
        @click="handleSubmitDecision('ACCEPTED_AS_PLAN')"
      >接受为计划</el-button>
      <el-button
        type="warning"
        :loading="submitting"
        :disabled="!safetyConfirmed"
        data-testid="btn-needs-patch-changes"
        @click="handleSubmitDecision('NEEDS_CHANGES')"
      >需要修改</el-button>
      <el-button
        type="danger"
        :loading="submitting"
        :disabled="!safetyConfirmed"
        data-testid="btn-reject-patch-proposal"
        @click="handleSubmitDecision('REJECTED')"
      >拒绝</el-button>
      <el-button
        type="info"
        :loading="submitting"
        :disabled="!safetyConfirmed"
        data-testid="btn-mark-patch-reviewed"
        @click="handleSubmitDecision('MARKED_REVIEWED')"
      >标记已审阅</el-button>
    </div>
    <div v-else class="ppr-actions">
      <el-tag type="info" effect="plain">审阅已完成，不可重复提交。</el-tag>
    </div>
  </div>
</template>

<style scoped>
.ppr-panel {
  padding: 4px 0;
}
.ppr-safety-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(230,162,60,0.1);
  border: 1px solid rgba(230,162,60,0.3);
  border-radius: var(--app-radius);
  font-size: 12px;
  color: var(--el-color-warning);
  margin-bottom: 16px;
}
.ppr-safety-icon {
  font-size: 16px;
  flex-shrink: 0;
}
.ppr-status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 12px;
}
.ppr-status-comment {
  color: var(--app-text-soft);
}
.ppr-status-hint {
  color: var(--app-text-muted);
}
.ppr-section {
  margin-bottom: 16px;
}
.ppr-section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 10px;
}
.ppr-file-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ppr-file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: rgba(148,163,184,0.04);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  font-size: 12px;
  font-family: monospace;
  color: var(--app-text-soft);
}
.ppr-file-icon {
  color: var(--app-primary);
  font-size: 10px;
}
.ppr-file-path {
  word-break: break-all;
}
.ppr-no-files {
  font-size: 12px;
  color: var(--app-text-muted);
  padding: 8px 0;
}
.ppr-diff-content {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  padding: 16px;
  max-height: 400px;
  overflow-y: auto;
}
.ppr-checklist {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ppr-check-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--app-text-soft);
  cursor: pointer;
}
.ppr-check-readonly {
  color: var(--app-text-muted);
}
.ppr-check-note {
  font-size: 11px;
  color: var(--el-color-info);
}
.ppr-safety-confirm {
  margin: 16px 0;
  padding: 10px 14px;
  background: rgba(230,162,60,0.06);
  border: 1px solid rgba(230,162,60,0.2);
  border-radius: var(--app-radius);
}
.ppr-confirm-text {
  font-size: 12px;
  color: var(--el-color-warning);
  margin-left: 6px;
}
.ppr-comment-section {
  margin-bottom: 16px;
}
.ppr-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
