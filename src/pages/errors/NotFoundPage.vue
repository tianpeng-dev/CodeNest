<script setup lang="ts">
import { useRouter } from 'vue-router';
import EmptyState from '@/components/EmptyState.vue';

const router = useRouter();
</script>

<template>
  <main class="error-page">
    <EmptyState title="页面不存在" description="你访问的页面可能已经移动、删除或暂时不可用。">
      <template #action>
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </template>
    </EmptyState>
  </main>
</template>

<style scoped>
.error-page {
  display: grid;
  min-height: min(520px, calc(100vh - 160px));
  place-items: center;
}
</style>
