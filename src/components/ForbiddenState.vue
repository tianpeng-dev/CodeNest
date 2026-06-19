<script setup lang="ts">
import { useRouter } from 'vue-router';
import EmptyState from './EmptyState.vue';

const router = useRouter();

function backHome() {
  router.push('/');
}
</script>

<template>
  <EmptyState title="无权访问" description="当前账号没有访问这个页面的权限。">
    <template #action>
      <el-button type="primary" @click="backHome">返回首页</el-button>
    </template>
  </EmptyState>
</template>
