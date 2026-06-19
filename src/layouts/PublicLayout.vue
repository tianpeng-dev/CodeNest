<script setup lang="ts">
import { computed } from 'vue';
import { RouterView, useRoute } from 'vue-router';

const route = useRoute();
const activePath = computed(() => route.path);
</script>

<template>
  <el-container class="public-layout">
    <el-header class="public-layout__header">
      <div class="public-layout__bar page-shell">
        <RouterLink class="public-layout__brand" to="/">CodeNest</RouterLink>
        <el-menu
          class="public-layout__menu"
          mode="horizontal"
          :default-active="activePath"
          router
          :ellipsis="false"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/search">搜索</el-menu-item>
          <el-menu-item index="/notifications">通知</el-menu-item>
          <el-menu-item index="/messages">私信</el-menu-item>
          <el-menu-item index="/creator/overview">创作中心</el-menu-item>
          <el-menu-item index="/admin">管理后台</el-menu-item>
        </el-menu>
      </div>
    </el-header>
    <el-main class="public-layout__main page-shell">
      <RouterView />
    </el-main>
  </el-container>
</template>

<style scoped>
.public-layout {
  min-height: 100vh;
  background: #f5f7fb;
}

.public-layout__header {
  height: 64px;
  padding: 0;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}

.public-layout__bar {
  display: flex;
  align-items: center;
  gap: 28px;
  height: 64px;
}

.public-layout__brand {
  flex: 0 0 auto;
  color: #172033;
  font-size: 22px;
  font-weight: 800;
}

.public-layout__menu {
  flex: 1;
  min-width: 0;
  border-bottom: 0;
}

.public-layout__main {
  min-height: calc(100vh - 64px);
  padding: 28px 0 40px;
}

@media (max-width: 760px) {
  .public-layout__bar {
    gap: 16px;
    overflow-x: auto;
  }

  .public-layout__brand {
    font-size: 18px;
  }

  .public-layout__menu {
    min-width: 520px;
  }
}
</style>
