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
    ElMessage.success('邀请已发送')
    inviteVisible.value = false
    inviteForm.value = { email: '', role: 'DEVELOPER' }
    load(1)
  } catch {
    ElMessage.error('邀请成员失败')
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
    ElMessage.success('角色已更新')
    roleVisible.value = false
    load()
  } catch {
    ElMessage.error('更新角色失败')
  } finally {
    updatingRole.value = false
  }
}

async function handleRemove(userId: string) {
  try {
    await removeMember(projectId, userId)
    ElMessage.success('成员已移除')
    load()
  } catch {
    ElMessage.error('移除成员失败')
  }
}

onMounted(() => load(1))
</script>

<template>
  <div class="page-container">
    <PageHeader title="成员" description="项目访问控制">
      <template #actions>
        <GlowButton size="small" accent="primary" @click="inviteVisible = true">邀请成员</GlowButton>
      </template>
    </PageHeader>

    <el-table :data="members" v-loading="loading" style="width:100%">
      <el-table-column prop="username" label="用户" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="row.role === 'OWNER' ? 'success' : row.role === 'ADMIN' ? 'warning' : 'info'">
            {{ row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="加入时间" width="150" class-name="nowrap-column">
        <template #default="{ row }">{{ formatDateTime(row.joinedTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openRoleDialog(row)">角色</el-button>
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

    <EmptyState v-if="!loading && members.length === 0" title="暂无成员" description="邀请团队成员协作此项目。" />

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
    <el-dialog v-model="inviteVisible" title="邀请成员" width="450px">
      <el-form label-position="top">
        <el-form-item label="邮箱" required>
          <el-input v-model="inviteForm.email" placeholder="user@example.com" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="inviteForm.role" style="width:100%">
            <el-option label="负责人" value="OWNER" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="开发者" value="DEVELOPER" />
            <el-option label="查看者" value="VIEWER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteVisible = false">取消</el-button>
        <el-button type="primary" :loading="inviting" @click="handleInvite">邀请</el-button>
      </template>
    </el-dialog>

    <!-- Role Dialog -->
    <el-dialog v-model="roleVisible" title="更新角色" width="400px">
      <el-form label-position="top">
        <el-form-item label="角色">
          <el-select v-model="roleForm.role" style="width:100%">
            <el-option label="负责人" value="OWNER" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="开发者" value="DEVELOPER" />
            <el-option label="查看者" value="VIEWER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="updatingRole" @click="handleUpdateRole">保存</el-button>
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
