<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  listMembers, inviteMember, updateMemberRole, removeMember,
  type ProjectMember,
} from '@/modules/member/api'
import PageHeader from '@/shared/components/PageHeader.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import ConfirmButton from '@/shared/components/ConfirmButton.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
import { usePagination } from '@/shared/composables/usePagination'
import { formatDateTime } from '@/shared/utils/format'

const route = useRoute()
const projectId = route.params.projectId as string

const { loading, records: members, pagination, load } = usePagination<ProjectMember>(
  (page, pageSize) => listMembers(projectId, page, pageSize),
)

const inviteVisible = ref(false)
const inviteForm = ref({ email: '', role: 'DEVELOPER' })
const inviting = ref(false)

const roleVisible = ref(false)
const roleForm = ref({ userId: '', role: '' })
const updatingRole = ref(false)

async function handleInvite() {
  inviting.value = true
  try {
    await inviteMember(projectId, inviteForm.value)
    ElMessage.success('Invitation sent')
    inviteVisible.value = false
    inviteForm.value = { email: '', role: 'DEVELOPER' }
    load(1)
  } catch {
    ElMessage.error('Failed to invite member')
  } finally {
    inviting.value = false
  }
}

function openRoleDialog(member: ProjectMember) {
  roleForm.value = { userId: member.userId, role: member.role }
  roleVisible.value = true
}

async function handleUpdateRole() {
  updatingRole.value = true
  try {
    await updateMemberRole(projectId, roleForm.value.userId, { role: roleForm.value.role })
    ElMessage.success('Role updated')
    roleVisible.value = false
    load()
  } catch {
    ElMessage.error('Failed to update role')
  } finally {
    updatingRole.value = false
  }
}

async function handleRemove(userId: string) {
  try {
    await removeMember(projectId, userId)
    ElMessage.success('Member removed')
    load()
  } catch {
    ElMessage.error('Failed to remove member')
  }
}

onMounted(() => load(1))
</script>

<template>
  <div class="page-container">
    <PageHeader title="Members" description="Project access control">
      <template #actions>
        <GlowButton size="small" accent="primary" @click="inviteVisible = true">Invite Member</GlowButton>
      </template>
    </PageHeader>

    <el-table :data="members" v-loading="loading" style="width:100%">
      <el-table-column prop="username" label="User" min-width="120" />
      <el-table-column prop="email" label="Email" min-width="180" />
      <el-table-column label="Role" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="row.role === 'OWNER' ? 'success' : row.role === 'ADMIN' ? 'warning' : 'info'">
            {{ row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Status" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Joined" width="150" class-name="nowrap-column">
        <template #default="{ row }">{{ formatDateTime(row.joinedTime) }}</template>
      </el-table-column>
      <el-table-column label="Actions" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openRoleDialog(row)">Role</el-button>
          <ConfirmButton
            :message="`Remove member ${row.username}?`"
            button-text="Remove"
            size="small"
            type="danger"
            link
            @confirm="handleRemove(row.userId)"
          />
        </template>
      </el-table-column>
    </el-table>

    <EmptyState v-if="!loading && members.length === 0" title="No Members" description="Invite teammates to collaborate on this project." />

    <el-pagination
      v-if="members.length > 0"
      v-model:current-page="pagination.page"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      layout="total, prev, pager, next" size="small"
      style="margin-top:16px;justify-content:flex-end"
      @current-change="(p: number) => load(p)"
    />

    <!-- Invite Dialog -->
    <el-dialog v-model="inviteVisible" title="Invite Member" width="450px">
      <el-form label-position="top">
        <el-form-item label="Email" required>
          <el-input v-model="inviteForm.email" placeholder="user@example.com" />
        </el-form-item>
        <el-form-item label="Role">
          <el-select v-model="inviteForm.role" style="width:100%">
            <el-option label="Owner" value="OWNER" />
            <el-option label="Admin" value="ADMIN" />
            <el-option label="Developer" value="DEVELOPER" />
            <el-option label="Viewer" value="VIEWER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="inviting" @click="handleInvite">Invite</el-button>
      </template>
    </el-dialog>

    <!-- Role Dialog -->
    <el-dialog v-model="roleVisible" title="Update Role" width="400px">
      <el-form label-position="top">
        <el-form-item label="Role">
          <el-select v-model="roleForm.role" style="width:100%">
            <el-option label="Owner" value="OWNER" />
            <el-option label="Admin" value="ADMIN" />
            <el-option label="Developer" value="DEVELOPER" />
            <el-option label="Viewer" value="VIEWER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="updatingRole" @click="handleUpdateRole">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
:deep(.nowrap-column .cell) {
  white-space: nowrap;
}

:deep(.el-tag) {
  color: var(--app-primary);
  background: rgba(56, 189, 248, 0.09);
  border-color: rgba(56, 189, 248, 0.22);
}

:deep(.el-tag--success) {
  color: var(--app-success);
  background: rgba(34, 197, 94, 0.09);
  border-color: rgba(34, 197, 94, 0.22);
}
</style>
