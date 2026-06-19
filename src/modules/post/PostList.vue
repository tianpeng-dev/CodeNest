<script setup lang="ts">
import EmptyState from '@/components/EmptyState.vue';
import type { Post } from '@/types/post';
import PostCard from './PostCard.vue';

defineProps<{
  posts: Post[];
}>();
</script>

<template>
  <div class="post-list">
    <PostCard v-for="post in posts" :key="post.id" :post="post" />
    <EmptyState
      v-if="posts.length === 0"
      title="暂无推荐内容"
      description="当前筛选下还没有可展示的帖子，换个分类看看。"
    />
  </div>
</template>

<style scoped>
.post-list {
  min-width: 0;
}
</style>
