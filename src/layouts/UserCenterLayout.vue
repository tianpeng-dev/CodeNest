<script setup lang="ts">
import { computed } from 'vue';
import { RouterView, useRoute } from 'vue-router';

const route = useRoute();
const activePath = computed(() => route.path);
</script>

<template>
  <el-container class="creator-layout">
    <el-aside class="creator-layout__aside" width="220px">
      <RouterLink class="creator-layout__brand" to="/">CodeNest</RouterLink>
      <el-menu :default-active="activePath" router>
        <el-menu-item index="/creator/overview">总览</el-menu-item>
        <el-menu-item index="/creator/editor">编辑器</el-menu-item>
        <el-menu-item index="/creator/posts">内容</el-menu-item>
        <el-menu-item index="/creator/comments">评论</el-menu-item>
        <el-menu-item index="/creator/columns">专栏</el-menu-item>
        <el-menu-item index="/creator/analytics">数据</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="creator-layout__header">
        <span>创作中心</span>
      </el-header>
      <el-main class="creator-layout__main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.creator-layout {
  min-height: 100vh;
  background: #f5f7fb;
}

.creator-layout__aside {
  min-height: 100vh;
  background: #ffffff;
  border-right: 1px solid #e4e7ed;
}

.creator-layout__brand {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 24px;
  color: #172033;
  font-size: 20px;
  font-weight: 800;
}

.creator-layout__header {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 28px;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  color: #172033;
  font-weight: 700;
}

.creator-layout__main {
  min-height: calc(100vh - 64px);
  padding: 28px;
}

@media (max-width: 760px) {
  .creator-layout {
    display: block;
  }

  .creator-layout__aside {
    width: 100% !important;
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid #e4e7ed;
  }

  .creator-layout__main {
    padding: 20px;
  }
}
</style>
