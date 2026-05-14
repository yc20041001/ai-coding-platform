<script setup lang="ts">
import { computed } from 'vue'
import { statusTagType } from '@/shared/types/common'
import type { StatusType } from '@/shared/types/common'

const props = defineProps<{
  status: StatusType
}>()

const tagType = computed(() => statusTagType(props.status) || 'info')
</script>

<template>
  <el-tag :type="tagType" size="small" effect="plain">
    {{ status }}
  </el-tag>
</template>
