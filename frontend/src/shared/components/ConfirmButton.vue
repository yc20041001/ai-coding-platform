<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(defineProps<{
  message: string
  type?: string
  buttonText?: string
  size?: string
  link?: boolean
}>(), {
  type: 'danger',
  buttonText: 'Delete',
  size: 'small',
  link: false,
})

const emit = defineEmits<{
  confirm: []
}>()

const visible = ref(false)

function handleConfirm() {
  visible.value = false
  emit('confirm')
}
</script>

<template>
  <el-popconfirm
    v-model:visible="visible"
    :title="message"
    confirm-button-text="Confirm"
    cancel-button-text="Cancel"
    @confirm="handleConfirm"
  >
    <template #reference>
      <el-button :type="type" :size="size" :link="link">{{ buttonText }}</el-button>
    </template>
  </el-popconfirm>
</template>
