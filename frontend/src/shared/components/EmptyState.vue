<script setup lang="ts">
defineProps<{
  description?: string
  title?: string
}>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-state__mark">
      <span class="empty-state__diamond" />
    </div>
    <div class="empty-state__title">{{ title || '暂无数据' }}</div>
    <div class="empty-state__desc">{{ description || '暂无记录' }}</div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 180px;
  padding: 36px 18px;
  color: var(--app-text-muted);
  text-align: center;
}
.empty-state__mark {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background:
    radial-gradient(circle at 50% 50%, rgba(56, 189, 248, 0.16), transparent 62%),
    rgba(15, 23, 42, 0.72);
  border: 1px solid rgba(56, 189, 248, 0.24);
  box-shadow: 0 0 22px rgba(56, 189, 248, 0.12);
}
.empty-state__diamond {
  width: 18px;
  height: 18px;
  transform: rotate(45deg);
  border: 2px solid rgba(56, 189, 248, 0.68);
  box-shadow: inset 0 0 10px rgba(56, 189, 248, 0.18);
}
.empty-state__title {
  margin-top: 6px;
  color: var(--app-text-soft);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.empty-state__desc {
  max-width: 360px;
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.6;
}
</style>
