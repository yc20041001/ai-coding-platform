<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getProviderOptions, getModelConfigs, createModelConfig, updateModelConfig, deleteModelConfig,
  type ModelProviderOption, type ModelConfigItem, type ModelConfigRequest,
} from '@/modules/model/api'
import DynamicWorkspace from '@/shared/components/DynamicWorkspace.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import TechPanel from '@/shared/components/TechPanel.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import NeonDivider from '@/shared/components/NeonDivider.vue'
import GlowButton from '@/shared/components/GlowButton.vue'
import ModelConnectionTestDialog from '@/modules/model/components/ModelConnectionTestDialog.vue'
import ModelUsageCostPanel from '@/modules/model/components/ModelUsageCostPanel.vue'
import { formatDateTime } from '@/shared/utils/format'

const providers = ref<ModelProviderOption[]>([])
const configs = ref<ModelConfigItem[]>([])
const loadingProviders = ref(false)
const loadingConfigs = ref(false)

const editVisible = ref(false)
const editing = ref(false)
const editForm = ref<ModelConfigRequest & { id?: number }>({
  provider: '', modelName: '', modelType: 'CHAT', apiBase: '', apiKey: '', status: 'ENABLED',
})

const testVisible = ref(false)
const testProvider = ref('')

onMounted(() => {
  loadProviders()
  loadConfigs()
})

async function loadProviders() {
  loadingProviders.value = true
  try {
    const res = await getProviderOptions()
    providers.value = res.data.data
  } catch { /* handled */ } finally { loadingProviders.value = false }
}

async function loadConfigs() {
  loadingConfigs.value = true
  try {
    const res = await getModelConfigs()
    configs.value = res.data.data
  } catch { /* handled */ } finally { loadingConfigs.value = false }
}

function openCreate() {
  editForm.value = { provider: 'MOCK', modelName: '', modelType: 'CHAT', apiBase: '', apiKey: '', status: 'ENABLED' }
  editVisible.value = true
}

function openEdit(item: ModelConfigItem) {
  editForm.value = {
    id: item.id, provider: item.provider, modelName: item.modelName,
    modelType: item.modelType, apiBase: item.apiBase || '', apiKey: '',
    status: item.status,
    timeoutMs: item.timeoutMs ?? undefined,
    maxRetries: item.maxRetries ?? undefined,
    fallbackEnabled: item.fallbackEnabled ?? undefined,
    streamEnabled: item.streamEnabled ?? undefined,
  }
  editVisible.value = true
}

function onProviderChange(provider: string) {
  const p = providers.value.find(pr => pr.provider === provider)
  if (p && p.defaultBaseUrl && !editForm.value.apiBase) {
    editForm.value.apiBase = p.defaultBaseUrl
  }
}

async function handleSave() {
  editing.value = true
  try {
    if (editForm.value.id) {
      await updateModelConfig(editForm.value.id, editForm.value)
      ElMessage.success('Config updated')
    } else {
      await createModelConfig(editForm.value)
      ElMessage.success('Config created')
    }
    editVisible.value = false
    loadConfigs()
  } catch { /* handled */ } finally { editing.value = false }
}

async function handleDelete(item: ModelConfigItem) {
  try {
    await deleteModelConfig(item.id)
    ElMessage.success('Config deleted')
    loadConfigs()
  } catch { /* handled */ }
}

function openTest(provider: string) {
  testProvider.value = provider
  testVisible.value = true
}

function providerDisplayName(provider: string) {
  return providers.value.find(p => p.provider === provider)?.displayName || provider
}
</script>

<template>
  <div class="page-container">
    <DynamicWorkspace
      title="Model Gateway"
      subtitle="Model Configuration & Connection Testing"
      eyebrow="AI Infrastructure"
      :status="`${configs.length} configs`"
    >
      <template #actions>
        <StatusPulse status="Online" tone="success" />
        <GlowButton accent="primary" size="small" @click="openCreate">+ New Config</GlowButton>
      </template>

    <NeonDivider tone="primary" style="margin-bottom:16px" />

    <!-- Provider Cards -->
    <TechPanel title="Available Providers" glow style="margin-bottom:20px">
      <div v-loading="loadingProviders" class="provider-grid">
        <div v-for="p in providers" :key="p.provider" class="provider-card">
          <div class="provider-card-header">
            <span class="provider-icon">{{ p.provider === 'MOCK' ? '◆' : p.provider === 'CLAUDE' ? '◎' : '◇' }}</span>
            <div>
              <div class="provider-name">{{ p.displayName }}</div>
              <div class="provider-code">{{ p.provider }}</div>
            </div>
          </div>
          <div class="provider-details">
            <div class="provider-detail">
              <span :class="p.supportsStream ? 'cap-yes' : 'cap-no'">{{ p.supportsStream ? '✓' : '✗' }}</span>
              <span>Stream</span>
            </div>
            <div class="provider-detail">
              <span :class="p.requiresApiKey ? 'cap-warn' : 'cap-ok'">{{ p.requiresApiKey ? '🔑' : '—' }}</span>
              <span>{{ p.requiresApiKey ? 'API Key' : 'No Key' }}</span>
            </div>
          </div>
          <div v-if="p.knownModels && p.knownModels.length > 0" class="provider-models">
            <el-tag v-for="m in p.knownModels.slice(0, 3)" :key="m" size="small" type="info">{{ m }}</el-tag>
            <el-tag v-if="p.knownModels.length > 3" size="small" type="info">+{{ p.knownModels.length - 3 }}</el-tag>
          </div>
          <GlowButton size="small" accent="primary" class="provider-test-btn" @click="openTest(p.provider)">Test Connection</GlowButton>
        </div>
      </div>
    </TechPanel>

    <!-- Model Configs Table -->
    <TechPanel title="Model Configurations">
      <el-table :data="configs" v-loading="loadingConfigs" size="small" style="width:100%">
        <el-table-column label="Provider" width="160">
          <template #default="{ row }">
            <span style="font-weight:600">{{ providerDisplayName(row.provider) }}</span>
            <div style="font-size:11px;color:var(--app-text-muted)">{{ row.provider }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="Model" min-width="160" />
        <el-table-column prop="modelType" label="Type" width="90" />
        <el-table-column label="Status" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="API Key" width="140">
          <template #default="{ row }">
            <code style="font-size:11px;color:var(--app-text-muted)">{{ row.maskedApiKey || '<not set>' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="Stream" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="row.streamEnabled ? 'success' : 'info'">{{ row.streamEnabled ? 'On' : 'Off' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Fallback" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.fallbackEnabled !== false ? 'success' : 'info'">{{ row.fallbackEnabled !== false ? 'On' : 'Off' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Updated" width="150">
          <template #default="{ row }">{{ formatDateTime(row.updateTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openEdit(row)">Edit</el-button>
            <el-button size="small" text type="primary" @click="openTest(row.provider)">Test</el-button>
            <el-button size="small" text type="danger" @click="handleDelete(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-if="!loadingConfigs && configs.length === 0" description="No model configs yet. Click + New Config to create" />
    </TechPanel>

    <!-- Usage Cost Panel -->
    <ModelUsageCostPanel style="margin-top:20px" />

    </DynamicWorkspace>

    <!-- Edit Dialog -->
    <el-dialog v-model="editVisible" :title="editForm.id ? 'Edit Model Config' : 'New Model Config'" width="550px">
      <el-form label-position="top">
        <el-form-item label="Provider" required>
          <el-select v-model="editForm.provider" style="width:100%" @change="onProviderChange" :disabled="!!editForm.id">
            <el-option v-for="p in providers" :key="p.provider" :label="`${p.displayName} (${p.provider})`" :value="p.provider" />
          </el-select>
        </el-form-item>
        <el-form-item label="Model Name" required>
          <el-input v-model="editForm.modelName" placeholder="e.g. gpt-4.1-mini" />
        </el-form-item>
        <el-form-item label="API Base URL">
          <el-input v-model="editForm.apiBase" placeholder="https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="editForm.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Status">
              <el-select v-model="editForm.status" style="width:100%">
                <el-option label="Enabled" value="ENABLED" />
                <el-option label="Disabled" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Timeout (ms)">
              <el-input-number v-model="editForm.timeoutMs" :min="5000" :max="300000" :step="5000" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="Max Retries">
              <el-input-number v-model="editForm.maxRetries" :min="0" :max="10" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Stream">
              <el-switch v-model="editForm.streamEnabled" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Fallback">
              <el-switch v-model="editForm.fallbackEnabled" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="editing" @click="handleSave">Save</el-button>
      </template>
    </el-dialog>

    <!-- Connection Test Dialog -->
    <ModelConnectionTestDialog
      :visible="testVisible"
      :provider="testProvider"
      @close="testVisible = false"
    />
  </div>
</template>

<style scoped>
.provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}
.provider-card {
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  background: var(--app-bg-soft);
  transition: all 0.15s;
}
.provider-card:hover {
  border-color: var(--app-border-strong);
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.06);
}
.provider-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.provider-icon {
  font-size: 20px;
  color: var(--app-primary);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-primary-soft);
  border-radius: 8px;
}
.provider-name { font-size: 14px; font-weight: 600; color: var(--app-text); }
.provider-code { font-size: 11px; color: var(--app-text-muted); font-family: monospace; }
.provider-details {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--app-text-muted);
  margin-bottom: 8px;
}
.provider-detail { display: flex; align-items: center; gap: 4px; }
.cap-yes { color: var(--app-success); font-weight: 700; }
.cap-no { color: var(--app-text-muted); }
.cap-warn { font-size: 11px; }
.cap-ok { color: var(--app-text-muted); }
.provider-models { display: flex; gap: 4px; flex-wrap: wrap; }
.provider-models :deep(.el-tag) {
  color: var(--app-primary);
  background: rgba(56, 189, 248, 0.09);
  border-color: rgba(56, 189, 248, 0.22);
}
.provider-test-btn {
  width: 100%;
  margin-top: 10px;
}
</style>
