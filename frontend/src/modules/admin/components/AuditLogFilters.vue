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
      <el-form-item label="User ID">
        <el-input v-model="filters.userId" placeholder="User ID" clearable style="width:120px" />
      </el-form-item>
      <el-form-item label="Action">
        <el-select v-model="filters.actionType" placeholder="Action type" clearable style="width:150px">
          <el-option v-for="a in actionTypes" :key="a" :label="a" :value="a" />
        </el-select>
      </el-form-item>
      <el-form-item label="Resource">
        <el-select v-model="filters.resourceType" placeholder="Resource type" clearable style="width:130px">
          <el-option v-for="r in resourceTypes" :key="r" :label="r" :value="r" />
        </el-select>
      </el-form-item>
      <el-form-item label="Resource ID">
        <el-input v-model="filters.resourceId" placeholder="Resource ID" clearable style="width:120px" />
      </el-form-item>
      <el-form-item label="Start Time">
        <el-date-picker v-model="filters.startTime" type="datetime" placeholder="Start time" style="width:170px" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" />
      </el-form-item>
      <el-form-item label="End Time">
        <el-date-picker v-model="filters.endTime" type="datetime" placeholder="End time" style="width:170px" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">Search</el-button>
        <el-button @click="handleReset">Reset</el-button>
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
