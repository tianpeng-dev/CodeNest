<script setup lang="ts">
import ForbiddenState from '@/components/ForbiddenState.vue';
</script>

<template>
  <main class="error-page">
    <ForbiddenState />
  </main>
</template>

<style scoped>
.error-page {
  display: grid;
  min-height: min(520px, calc(100vh - 160px));
  place-items: center;
}
</style>
