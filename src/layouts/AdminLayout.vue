<script setup lang="ts">
import { computed } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth.store';

const route = useRoute();
const authStore = useAuthStore();
const activePath = computed(() => route.path);
const displayName = computed(() => authStore.currentUser?.displayName ?? '管理员');
</script>

<template>
  <el-container class="admin-layout">
    <el-aside class="admin-layout__aside" width="240px">
      <RouterLink class="admin-layout__brand" to="/">CodeNest Admin</RouterLink>
      <el-menu :default-active="activePath" router>
        <el-menu-item index="/admin">仪表盘</el-menu-item>
        <el-menu-item index="/admin/users">用户</el-menu-item>
        <el-menu-item index="/admin/posts">帖子</el-menu-item>
        <el-menu-item index="/admin/categories">分类</el-menu-item>
        <el-menu-item index="/admin/moderators">版主</el-menu-item>
        <el-menu-item index="/admin/sensitive-words">敏感词</el-menu-item>
        <el-menu-item index="/admin/analytics">分析</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-layout__header">
        <strong>管理后台</strong>
        <span class="admin-layout__status">{{ displayName }}</span>
      </el-header>
      <el-main class="admin-layout__main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f3f5f9;
}

.admin-layout__aside {
  min-height: 100vh;
  background: #ffffff;
  border-right: 1px solid #dcdfe6;
}

.admin-layout__brand {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 24px;
  color: #172033;
  font-size: 19px;
  font-weight: 800;
}

.admin-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 28px;
  background: #ffffff;
  border-bottom: 1px solid #dcdfe6;
}

.admin-layout__status {
  color: #667085;
  font-size: 14px;
}

.admin-layout__main {
  min-height: calc(100vh - 64px);
  padding: 28px;
}

@media (max-width: 900px) {
  .admin-layout {
    display: block;
  }

  .admin-layout__aside {
    width: 100% !important;
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid #dcdfe6;
  }

  .admin-layout__main {
    padding: 20px;
  }
}
</style>
