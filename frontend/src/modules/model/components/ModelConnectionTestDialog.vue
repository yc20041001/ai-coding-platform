<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { testConnection, type ConnectionTestResponse } from '@/modules/model/api'

const props = defineProps<{
  visible: boolean
  provider: string
}>()

const emit = defineEmits<{
  close: []
}>()

const form = ref({
  baseUrl: '',
  modelName: '',
  apiKey: '',
})
const testing = ref(false)
const result = ref<ConnectionTestResponse | null>(null)

watch(() => props.visible, (v) => {
  if (v) {
    form.value = { baseUrl: '', modelName: '', apiKey: '' }
    result.value = null
  }
})

async function handleTest() {
  testing.value = true
  result.value = null
  try {
    const res = await testConnection({
      provider: props.provider,
      baseUrl: form.value.baseUrl || undefined,
      modelName: form.value.modelName || undefined,
      apiKey: form.value.apiKey || undefined,
    })
    result.value = res.data.data
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }, message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '测试失败')
  } finally {
    testing.value = false
  }
}

function providerLabel(p: string) {
  const map: Record<string, string> = {
    MOCK: 'Mock Provider',
    OPENAI_COMPATIBLE: 'OpenAI Compatible',
    CLAUDE: 'Anthropic Claude',
    DEEPSEEK: 'DeepSeek',
    QWEN: 'Qwen (Tongyi)',
    GEMINI: 'Google Gemini',
  }
  return map[p] || p
}
</script>

<template>
  <el-dialog :model-value="visible" :title="`Test Connection - ${providerLabel(provider)}`" width="520px" @close="emit('close')">
    <el-form label-position="top">
      <el-form-item label="Base URL (optional, override default)">
        <el-input v-model="form.baseUrl" placeholder="Leave empty to use system default" />
      </el-form-item>
      <el-form-item label="Model Name (optional)">
        <el-input v-model="form.modelName" placeholder="Leave empty for default model" />
      </el-form-item>
      <el-form-item label="API Key (optional, uses env if empty)">
        <el-input v-model="form.apiKey" type="password" show-password placeholder="Leave empty to use env-configured key" />
      </el-form-item>
    </el-form>

    <div v-if="result" class="test-result" :class="{ 'test-success': result.success, 'test-fail': !result.success }">
      <div class="test-result-header">
        <span :class="result.success ? 'result-icon-ok' : 'result-icon-fail'">{{ result.success ? '✓' : '✗' }}</span>
        <span class="test-result-msg">{{ result.message }}</span>
      </div>
      <div class="test-result-details">
        <div><span class="detail-label">Latency</span><span>{{ result.latencyMs }}ms</span></div>
        <div v-if="result.modelName"><span class="detail-label">Model</span><span>{{ result.modelName }}</span></div>
        <div v-if="result.maskedApiKey"><span class="detail-label">API Key</span><code>{{ result.maskedApiKey }}</code></div>
        <div v-if="result.errorCode"><span class="detail-label">Error Code</span><code>{{ result.errorCode }}</code></div>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('close')">关闭</el-button>
      <el-button type="primary" :loading="testing" @click="handleTest">测试连接</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.test-result {
  margin-top: 16px;
  padding: 14px;
  border-radius: var(--app-radius);
  border: 1px solid var(--app-border);
}
.test-success { border-color: rgba(34, 197, 94, 0.3); background: rgba(34, 197, 94, 0.05); }
.test-fail { border-color: rgba(239, 68, 68, 0.3); background: rgba(239, 68, 68, 0.05); }
.test-result-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; font-size: 14px; }
.result-icon-ok { color: var(--app-success); font-size: 18px; font-weight: 700; }
.result-icon-fail { color: var(--app-danger); font-size: 18px; font-weight: 700; }
.test-result-msg { color: var(--app-text-soft); }
.test-result-details { display: flex; gap: 20px; flex-wrap: wrap; font-size: 12px; color: var(--app-text-muted); }
.detail-label { color: var(--app-text-muted); margin-right: 6px; }
.test-result-details code { font-size: 11px; background: rgba(148,163,184,0.1); padding: 1px 6px; border-radius: 3px; }
</style>
