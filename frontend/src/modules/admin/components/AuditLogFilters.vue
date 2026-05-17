<script setup lang="ts">
import { ref } from 'vue'

export interface AuditLogFilterValues {
  userId?: string
  actionType?: string
  resourceType?: string
  resourceId?: string
  startTime?: string
  endTime?: string
}

const emit = defineEmits<{
  search: [values: AuditLogFilterValues]
  reset: []
}>()

const filters = ref<AuditLogFilterValues>({})

const actionTypes = [
  'AUTH_LOGIN', 'AUTH_LOGOUT', 'PROJECT_CREATE', 'PROJECT_UPDATE', 'PROJECT_DELETE',
  'TASK_CREATE', 'TASK_EXECUTE', 'CHAT_SEND', 'RAG_DOCUMENT_UPLOAD', 'RAG_SEARCH',
  'MODEL_CALL', 'MEMBER_INVITE', 'MEMBER_REMOVE',
]

const resourceTypes = ['PROJECT', 'TASK', 'CHAT_SESSION', 'KNOWLEDGE_BASE', 'AGENT', 'REPOSITORY']

function handleSearch() {
  emit('search', { ...filters.value })
}

function handleReset() {
  filters.value = {}
  emit('reset')
}
</script>

<template>
  <div class="audit-filters">
    <el-form :model="filters" inline size="small">
      <el-form-item label="用户 ID">
        <el-input v-model="filters.userId" placeholder="用户 ID" clearable style="width:120px" />
      </el-form-item>
      <el-form-item label="操作">
        <el-select v-model="filters.actionType" placeholder="操作类型" clearable style="width:150px">
          <el-option v-for="a in actionTypes" :key="a" :label="a" :value="a" />
        </el-select>
      </el-form-item>
      <el-form-item label="资源">
        <el-select v-model="filters.resourceType" placeholder="资源类型" clearable style="width:130px">
          <el-option v-for="r in resourceTypes" :key="r" :label="r" :value="r" />
        </el-select>
      </el-form-item>
      <el-form-item label="资源 ID">
        <el-input v-model="filters.resourceId" placeholder="资源 ID" clearable style="width:120px" />
      </el-form-item>
      <el-form-item label="开始时间">
        <el-date-picker v-model="filters.startTime" type="datetime" placeholder="开始时间" style="width:170px" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-date-picker v-model="filters.endTime" type="datetime" placeholder="结束时间" style="width:170px" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.audit-filters {
  padding: 12px 16px;
  background: var(--app-bg-soft);
  border-radius: var(--app-radius);
  border: 1px solid var(--app-border);
  margin-bottom: 12px;
}
.audit-filters :deep(.el-form-item) {
  margin-bottom: 4px;
}
.audit-filters :deep(.el-form-item__label) {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
</style>
