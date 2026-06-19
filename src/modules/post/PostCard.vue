<script setup lang="ts">
import {
  ChatDotRound,
  Collection,
  Star,
  View,
} from '@element-plus/icons-vue';
import type { Post } from '@/types/post';

const props = defineProps<{
  post: Post;
}>();

const formatter = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

function formatPublishTime(post: Post) {
  return formatter.format(new Date(post.publishedAt ?? post.createdAt));
}

function formatCount(value: number) {
  if (value >= 10000) {
    return `${(value / 10000).toFixed(1)}w`;
  }

  return String(value);
}
</script>

<template>
  <article class="post-card">
    <div class="post-card__body">
      <RouterLink class="post-card__title" :to="`/post/${props.post.id}`">
        {{ props.post.title }}
      </RouterLink>
      <p class="post-card__summary">{{ props.post.summary }}</p>
      <div class="post-card__tags" aria-label="文章标签">
        <el-tag
          v-for="tag in props.post.tags"
          :key="tag"
          size="small"
          effect="plain"
          round
        >
          {{ tag }}
        </el-tag>
      </div>
      <footer class="post-card__meta">
        <RouterLink class="post-card__author" :to="`/u/${props.post.author.id}`">
          <el-avatar :size="24" :src="props.post.author.avatarUrl">
            {{ props.post.author.displayName.slice(0, 1) }}
          </el-avatar>
          <span>{{ props.post.author.displayName }}</span>
        </RouterLink>
        <span>{{ props.post.category.name }}</span>
        <time :datetime="props.post.publishedAt ?? props.post.createdAt">
          {{ formatPublishTime(props.post) }}
        </time>
      </footer>
    </div>

    <dl class="post-card__stats" aria-label="互动数据">
      <div>
        <dt><el-icon><View /></el-icon><span>浏览</span></dt>
        <dd>{{ formatCount(props.post.viewCount) }}</dd>
      </div>
      <div>
        <dt><el-icon><Star /></el-icon><span>点赞</span></dt>
        <dd>{{ formatCount(props.post.likeCount) }}</dd>
      </div>
      <div>
        <dt><el-icon><Collection /></el-icon><span>收藏</span></dt>
        <dd>{{ formatCount(props.post.favoriteCount) }}</dd>
      </div>
      <div>
        <dt><el-icon><ChatDotRound /></el-icon><span>评论</span></dt>
        <dd>{{ formatCount(props.post.commentCount) }}</dd>
      </div>
    </dl>
  </article>
</template>

<style scoped>
.post-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 168px;
  gap: 18px;
  padding: 18px 0;
  border-bottom: 1px solid #edf1f7;
}

.post-card:first-child {
  padding-top: 0;
}

.post-card:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.post-card__body {
  min-width: 0;
}

.post-card__title {
  display: inline;
  color: #172033;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.4;
}

.post-card__title:hover {
  color: #1f4f8f;
}

.post-card__summary {
  display: -webkit-box;
  margin: 8px 0 0;
  overflow: hidden;
  color: #475467;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.post-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.post-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
  color: #667085;
  font-size: 13px;
}

.post-card__author {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #344054;
  font-weight: 700;
}

.post-card__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  align-self: center;
  margin: 0;
}

.post-card__stats div {
  min-width: 0;
  padding: 9px;
  background: #f8fafc;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
}

.post-card__stats dt {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #667085;
  font-size: 12px;
}

.post-card__stats dd {
  margin: 5px 0 0;
  color: #172033;
  font-size: 15px;
  font-weight: 800;
}

@media (max-width: 720px) {
  .post-card {
    grid-template-columns: 1fr;
  }

  .post-card__stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .post-card__stats dt span {
    display: none;
  }
}
</style>
