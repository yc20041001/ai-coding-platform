<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  getAuditTimeline,
  generateAuditReport,
  type ReleaseAuditTimeline,
  type ReleaseAuditReport,
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

const timeline = ref<ReleaseAuditTimeline | null>(null)
const loading = ref(false)
const error = ref(false)
const reportData = ref<ReleaseAuditReport | null>(null)
const reportDialogVisible = ref(false)

function loadTimeline() {
  if (!props.planId) return
  loading.value = true
  error.value = false
  getAuditTimeline(props.planId)
    .then(res => { timeline.value = res.data.data })
    .catch(() => { error.value = true })
    .finally(() => { loading.value = false })
}

async function handleExportReport() {
  if (!props.planId) return
  try {
    const res = await generateAuditReport(props.planId)
    reportData.value = res.data.data
    reportDialogVisible.value = true
  } catch {
    ElMessage.error('生成审计报告失败')
  }
}

function eventTypeTag(type: string) {
  const map: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    PLAN_CREATED: 'success',
    PLAN_STATUS_CHANGED: 'warning',
    STEP_STATUS_CHANGED: 'warning',
    VERIFICATION_RECORDED: 'success',
    ROLLBACK_DRILL_UPDATED: 'info',
    DECISION_LINKED: 'success',
    INCIDENT_LINKED: 'danger',
    POSTMORTEM_UPDATED: 'success',
    REPORT_EXPORTED: 'info',
  }
  return map[type] || 'info'
}

function eventTypeLabel(type: string): string {
  const map: Record<string, string> = {
    PLAN_CREATED: '计划创建',
    PLAN_STATUS_CHANGED: '状态变更',
    STEP_STATUS_CHANGED: '步骤变更',
    VERIFICATION_RECORDED: '验证记录',
    ROLLBACK_DRILL_UPDATED: '回滚演练',
    DECISION_LINKED: '决策关联',
    INCIDENT_LINKED: '事件关联',
    POSTMORTEM_UPDATED: '发布复盘',
    REPORT_EXPORTED: '报告导出',
  }
  return map[type] || type
}

watch(() => props.planId, () => { if (props.planId) loadTimeline() }, { immediate: true })
</script>

<template>
  <div>
    <div style="margin-bottom:12px;display:flex;align-items:center;justify-content:space-between">
      <span style="font-weight:700;font-size:13px;color:var(--app-text-soft);text-transform:uppercase;letter-spacing:0.05em">发布审计时间线</span>
      <ElButton v-if="props.planId" size="small" @click="handleExportReport">导出审计报告</ElButton>
    </div>

    <ErrorState v-if="error" title="加载失败" message="无法获取审计数据" retry-text="重试" @retry="loadTimeline" />

    <div v-if="!props.planId" style="font-size:12px;color:var(--app-text-muted);padding:8px 0">请先选择一个 Rollout Plan</div>

    <div v-if="timeline && props.planId">
      <div style="display:flex;gap:12px;margin-bottom:12px;flex-wrap:wrap">
        <div style="font-size:11px;color:var(--app-text-muted)">事件总数: <strong>{{ timeline.totalEvents }}</strong></div>
        <div v-if="timeline.latestEventTime" style="font-size:11px;color:var(--app-text-muted)">最新事件: <strong>{{ formatDateTime(timeline.latestEventTime) }}</strong></div>
      </div>

      <div v-if="timeline.eventCountsByType" style="display:flex;gap:6px;margin-bottom:12px;flex-wrap:wrap">
        <ElTag v-for="(count, type) in timeline.eventCountsByType" :key="type" :type="eventTypeTag(type)" size="small" effect="dark">
          {{ eventTypeLabel(type) }}: {{ count }}
        </ElTag>
      </div>

      <NeonDivider tone="muted" style="margin:12px 0" />

      <div v-loading="loading">
        <div v-for="event in timeline.events" :key="event.id" style="display:flex;gap:8px;padding:6px 0;border-bottom:1px solid rgba(56,189,248,0.05)">
          <div style="min-width:140px;font-size:11px;color:var(--app-text-muted);font-family:'SF Mono',monospace">{{ formatDateTime(event.eventTime) }}</div>
          <div style="min-width:100px">
            <ElTag :type="eventTypeTag(event.eventType)" size="small" effect="dark">{{ eventTypeLabel(event.eventType) }}</ElTag>
          </div>
          <div style="flex:1;font-size:12px;color:var(--app-text-bright)">{{ event.summary }}</div>
          <div v-if="event.actorName" style="min-width:80px;font-size:11px;color:var(--app-text-muted);text-align:right">{{ event.actorName }}</div>
        </div>
      </div>
    </div>

    <EmptyState v-if="!loading && timeline && timeline.totalEvents === 0 && props.planId" description="暂无审计事件" />

    <ElDialog v-model="reportDialogVisible" title="发布审计报告" width="70%" top="5vh">
      <div v-if="reportData" style="white-space:pre-wrap;font-family:'SF Mono','Cascadia Code',monospace;font-size:12px;line-height:1.6;background:rgba(15,23,42,0.3);padding:16px;border-radius:8px;max-height:60vh;overflow-y:auto">
        {{ reportData.reportMarkdown }}
      </div>
    </ElDialog>
  </div>
</template>
