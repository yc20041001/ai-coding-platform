<script setup lang="ts">
defineProps<{
  title?: string
  message?: string
  retryText?: string
}>()

const emit = defineEmits<{
  retry: []
}>()
</script>

<template>
  <div class="error-state">
    <el-icon :size="40" color="#f56c6c"><svg viewBox="0 0 1024 1024" width="1em" height="1em" fill="currentColor"><path d="M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896zm0 64a384 384 0 1 0 0 768 384 384 0 0 0 0-768zm-42.667 256h85.334v256h-85.334V384zm0 298.667h85.334V768h-85.334v-85.333z"/></svg></el-icon>
    <div v-if="title" class="error-title">{{ title }}</div>
    <div v-if="message" class="error-message">{{ message }}</div>
    <el-button v-if="retryText" type="primary" size="small" @click="emit('retry')">{{ retryText }}</el-button>
  </div>
</template>

<style scoped>
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  gap: 12px;
}
.error-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.error-message {
  font-size: 13px;
  color: #909399;
  max-width: 400px;
  text-align: center;
}
</style>
