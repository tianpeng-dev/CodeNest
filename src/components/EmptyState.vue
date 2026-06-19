<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
}>();
</script>

<template>
  <section class="empty-state" aria-live="polite">
    <div class="empty-state__mark">CN</div>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <div v-if="$slots.action" class="empty-state__action">
      <slot name="action" />
    </div>
  </section>
</template>

<style scoped>
.empty-state {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 36px 20px;
  color: #667085;
  text-align: center;
  background: #ffffff;
  border: 1px dashed #cfd6e4;
  border-radius: 8px;
}

.empty-state__mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  color: #1f4f8f;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
  background: #eef5ff;
  border: 1px solid #c9ddff;
  border-radius: 8px;
}

.empty-state h2 {
  margin: 0;
  color: #172033;
  font-size: 18px;
}

.empty-state p {
  max-width: 420px;
  margin: 0;
  line-height: 1.7;
}

.empty-state__action {
  margin-top: 4px;
}
</style>
