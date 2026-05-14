<script setup lang="ts">
defineProps<{
  description?: string
}>()
</script>

<template>
  <div class="empty-state">
    <el-empty :description="description || '暂无数据'" />
  </div>
</template>

<style scoped>
.empty-state {
  padding: 48px 0;
}
</style>
