<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePagination } from '@/shared/composables/usePagination'
import { listProjects, createProject } from '@/modules/project/api'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusTag from '@/shared/components/StatusTag.vue'
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
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
    ElMessage.success('Project created')
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
    <DynamicWorkspace
      title="Projects"
      subtitle="Project Workspace Hub"
      eyebrow="Workspace"
      :status="`${pagination.total} active`"
    >
      <template #actions>
        <GlowButton accent="primary" @click="createVisible = true" data-testid="btn-create-project">
          + New Project
        </GlowButton>
      </template>

      <NeonDivider tone="primary" />

      <div data-testid="project-table-area">
        <el-table
          v-if="records.length > 0"
          :data="records"
          v-loading="loading"
          @row-click="(row: ProjectItem) => goDetail(row.id)"
          style="cursor:pointer;width:100%;margin-top:8px"
          data-testid="project-table"
        >
        <el-table-column prop="name" label="Name" min-width="180">
          <template #default="{ row }">
            <div class="proj-name">
              <span class="proj-name-icon">◇</span>
              <span class="proj-name-text">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="Description" min-width="240">
          <template #default="{ row }">{{ truncate(row.description, 80) }}</template>
        </el-table-column>
        <el-table-column label="Status" width="120">
          <template #default="{ row }">
            <StatusPulse
              :status="row.status"
              :tone="row.status === 'ACTIVE' ? 'success' : 'muted'"
            />
          </template>
        </el-table-column>
        <el-table-column label="Created" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <EmptyState v-else-if="!loading" description="No projects yet" />
      </div>

      <el-pagination
        v-if="pagination.total > 0"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end"
        @change="load()"
      />

      <el-dialog v-model="createVisible" title="Create Project" width="480px">
        <div data-testid="dialog-create-project">
          <el-form label-position="top">
          <el-form-item label="Name" required>
            <el-input v-model="createForm.name" placeholder="Project name" data-testid="input-project-name" />
          </el-form-item>
          <el-form-item label="Description">
            <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="Project description" data-testid="input-project-description" />
          </el-form-item>
          <el-form-item label="Tech Stack (comma-separated)">
            <el-input v-model="createForm.techStackStr" placeholder="Java, Spring Boot, Vue 3" data-testid="input-project-techstack" />
          </el-form-item>
        </el-form>
        </div>
        <template #footer>
          <el-button @click="createVisible = false" data-testid="btn-cancel-project">Cancel</el-button>
          <el-button type="primary" :loading="creating" @click="handleCreate" data-testid="btn-submit-project">Create</el-button>
        </template>
      </el-dialog>
    </DynamicWorkspace>
  </div>
</template>

<style scoped>
.proj-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.proj-name-icon {
  color: var(--app-primary);
  font-size: 12px;
}

.proj-name-text {
  font-weight: 600;
  color: var(--app-text);
}
</style>
