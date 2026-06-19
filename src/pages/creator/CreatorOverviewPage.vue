<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { DocumentAdd, EditPen, View } from '@element-plus/icons-vue';
import StatCard from '@/components/StatCard.vue';
import EmptyState from '@/components/EmptyState.vue';
import { getCreatorAnalytics, getCreatorPosts, type CreatorAnalytics } from '@/services/creator.service';
import type { Post } from '@/types/post';

const loading = ref(false);
const analytics = ref<CreatorAnalytics | null>(null);
const recentPosts = ref<Post[]>([]);

const statCards = computed(() => [
  {
    label: '全部文章',
    value: analytics.value?.postCount ?? 0,
    delta: '+12%',
  },
  {
    label: '已发布',
    value: analytics.value?.publishedCount ?? 0,
    delta: '+8%',
  },
  {
    label: '总浏览',
    value: analytics.value?.totalViews ?? 0,
    delta: '+18%',
  },
  {
    label: '收藏',
    value: analytics.value?.totalFavorites ?? 0,
    delta: '+6%',
  },
]);

function statusType(status: Post['status']) {
  if (status === 'published') return 'success';
  if (status === 'hidden') return 'warning';
  if (status === 'deleted') return 'danger';
  return 'info';
}

function statusText(status: Post['status']) {
  return {
    draft: '草稿',
    published: '已发布',
    hidden: '审核中',
    deleted: '已删除',
  }[status];
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

onMounted(async () => {
  loading.value = true;

  try {
    const [analyticsResult, postsResult] = await Promise.all([
      getCreatorAnalytics(),
      getCreatorPosts({ page: 1, pageSize: 5 }),
    ]);
    analytics.value = analyticsResult;
    recentPosts.value = postsResult.items;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <section class="creator-page" v-loading="loading">
    <header class="creator-page__header">
      <div>
        <p class="creator-page__eyebrow">Workspace</p>
        <h1>创作中心</h1>
      </div>
      <div class="creator-page__actions">
        <RouterLink to="/creator/editor">
          <el-button type="primary" :icon="DocumentAdd">写文章</el-button>
        </RouterLink>
      </div>
    </header>

    <div class="creator-overview__stats">
      <StatCard
        v-for="stat in statCards"
        :key="stat.label"
        :label="stat.label"
        :value="stat.value"
        :delta="stat.delta"
      />
    </div>

    <section class="surface-panel">
      <div class="surface-panel__header">
        <div>
          <h2>最近内容</h2>
          <p>跟进草稿、审核和已发布文章。</p>
        </div>
        <RouterLink to="/creator/posts">查看全部</RouterLink>
      </div>

      <div v-if="recentPosts.length" class="creator-overview__posts">
        <article v-for="post in recentPosts" :key="post.id" class="creator-overview__post">
          <img :src="post.coverUrl" alt="" />
          <div>
            <div class="creator-overview__post-title">
              <h3>{{ post.title }}</h3>
              <el-tag size="small" :type="statusType(post.status)">
                {{ statusText(post.status) }}
              </el-tag>
            </div>
            <p>{{ post.summary }}</p>
            <div class="creator-overview__meta">
              <span><el-icon><View /></el-icon>{{ post.viewCount }}</span>
              <span><el-icon><EditPen /></el-icon>{{ formatDate(post.updatedAt) }}</span>
            </div>
          </div>
        </article>
      </div>

      <EmptyState
        v-else
        title="还没有内容"
        description="从第一篇文章开始搭建你的创作者主页。"
      />
    </section>
  </section>
</template>

<style scoped>
.creator-page {
  min-width: 0;
}

.creator-page__header,
.surface-panel__header,
.creator-overview__post-title,
.creator-overview__meta {
  display: flex;
  align-items: center;
}

.creator-page__header {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.creator-page__eyebrow {
  margin: 0 0 6px;
  color: #3d6f8f;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.creator-page h1,
.surface-panel h2,
.creator-overview__post h3 {
  margin: 0;
  color: #172033;
}

.creator-page h1 {
  font-size: 28px;
}

.creator-overview__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.surface-panel {
  padding: 18px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.surface-panel__header {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.surface-panel__header p {
  margin: 6px 0 0;
  color: #667085;
}

.creator-overview__posts {
  display: grid;
  gap: 12px;
}

.creator-overview__post {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 14px;
  padding: 12px;
  border: 1px solid #edf1f7;
  border-radius: 8px;
}

.creator-overview__post img {
  width: 112px;
  height: 78px;
  object-fit: cover;
  border-radius: 6px;
}

.creator-overview__post-title {
  justify-content: space-between;
  gap: 12px;
}

.creator-overview__post h3 {
  overflow: hidden;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.creator-overview__post p {
  display: -webkit-box;
  margin: 8px 0;
  overflow: hidden;
  color: #667085;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.creator-overview__meta {
  gap: 14px;
  color: #98a2b3;
  font-size: 13px;
}

.creator-overview__meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 980px) {
  .creator-overview__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .creator-page__header,
  .surface-panel__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .creator-overview__stats {
    grid-template-columns: 1fr;
  }

  .creator-overview__post {
    grid-template-columns: 1fr;
  }

  .creator-overview__post img {
    width: 100%;
    height: 150px;
  }
}
</style>
