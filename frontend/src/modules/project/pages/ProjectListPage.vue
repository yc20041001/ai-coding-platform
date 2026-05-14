<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePagination } from '@/shared/composables/usePagination'
import { listProjects, createProject } from '@/modules/project/api'
import PageHeader from '@/shared/components/PageHeader.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusTag from '@/shared/components/StatusTag.vue'
import RuntimeBadge from '@/shared/components/RuntimeBadge.vue'
import { formatDateTime, truncate } from '@/shared/utils/format'
import type { ProjectItem } from '@/modules/project/api'

const router = useRouter()
const { loading, records, pagination, load } = usePagination<ProjectItem>(listProjects)

const createVisible = ref(false)
const creating = ref(false)
const createForm = ref({ name: '', description: '', techStackStr: '' })

async function handleCreate() {
  creating.value = true
  try {
    const techStack = createForm.value.techStackStr
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
    await createProject({
      name: createForm.value.name,
      description: createForm.value.description,
      techStack,
    })
    ElMessage.success('项目创建成功')
    createVisible.value = false
    createForm.value = { name: '', description: '', techStackStr: '' }
    load(1)
  } catch {
    // error handled by interceptor
  } finally {
    creating.value = false
  }
}

function goDetail(id: string) {
  router.push(`/projects/${id}`)
}

load(1)
</script>

<template>
  <div class="page-container">
    <PageHeader title="Projects" description="项目管理">
      <template #actions>
        <RuntimeBadge v-if="records.length > 0" status="online" :label="`${pagination.total} projects`" style="margin-right:8px" />
        <el-button type="primary" @click="createVisible = true" data-testid="btn-create-project">新建项目</el-button>
      </template>
    </PageHeader>

    <el-table
      v-if="records.length > 0"
      :data="records"
      v-loading="loading"
      @row-click="(row: ProjectItem) => goDetail(row.id)"
      style="cursor:pointer;width:100%"
    >
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="description" label="描述" min-width="200">
        <template #default="{ row }">{{ truncate(row.description, 60) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <EmptyState v-else-if="!loading" description="暂无项目" />

    <el-pagination
      v-if="pagination.total > 0"
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      layout="total, prev, pager, next"
      style="margin-top:16px;justify-content:flex-end"
      @change="load()"
    />

    <el-dialog v-model="createVisible" title="新建项目" width="480px">
      <el-form label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" placeholder="项目名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="项目描述" />
        </el-form-item>
        <el-form-item label="技术栈（逗号分隔）">
          <el-input v-model="createForm.techStackStr" placeholder="Java, Spring Boot, Vue 3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>
