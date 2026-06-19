<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import StatCard from '@/components/StatCard.vue';
import StatsChart from '@/modules/analytics/StatsChart.vue';
import {
  getCreatorAnalytics,
  getCreatorPosts,
  type CreatorAnalytics,
} from '@/services/creator.service';
import type { Post } from '@/types/post';

const loading = ref(false);
const analytics = ref<CreatorAnalytics | null>(null);
const posts = ref<Post[]>([]);

const statCards = computed(() => [
  {
    label: '总浏览',
    value: analytics.value?.totalViews ?? 0,
    delta: '+18%',
  },
  {
    label: '总点赞',
    value: analytics.value?.totalLikes ?? 0,
    delta: '+11%',
  },
  {
    label: '总收藏',
    value: analytics.value?.totalFavorites ?? 0,
    delta: '+6%',
  },
  {
    label: '草稿',
    value: analytics.value?.draftCount ?? 0,
    delta: '待发布',
  },
]);

const tableData = computed(() => {
  return posts.value.map((post) => ({
    ...post,
    engagement: post.likeCount + post.favoriteCount + post.commentCount,
  }));
});

function formatDate(value: string | null) {
  if (!value) return '-';

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value));
}

onMounted(async () => {
  loading.value = true;

  try {
    const [analyticsResult, postsResult] = await Promise.all([
      getCreatorAnalytics(),
      getCreatorPosts({ page: 1, pageSize: 50 }),
    ]);
    analytics.value = analyticsResult;
    posts.value = postsResult.items;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <section class="creator-analytics" v-loading="loading">
    <header class="page-header">
      <div>
        <p>Analytics</p>
        <h1>创作数据</h1>
      </div>
    </header>

    <div class="analytics-stats">
      <StatCard
        v-for="stat in statCards"
        :key="stat.label"
        :label="stat.label"
        :value="stat.value"
        :delta="stat.delta"
      />
    </div>

    <div class="analytics-grid">
      <section class="surface-panel surface-panel--wide">
        <div class="surface-panel__header">
          <h2>近 7 日浏览趋势</h2>
          <span>按日聚合</span>
        </div>
        <StatsChart type="line" title="浏览量" :data="analytics?.trend ?? []" :height="320" />
      </section>

      <section class="surface-panel">
        <div class="surface-panel__header">
          <h2>分类占比</h2>
          <span>内容分布</span>
        </div>
        <StatsChart type="pie" title="分类占比" :data="analytics?.pie ?? []" :height="320" />
      </section>
    </div>

    <section class="surface-panel">
      <div class="surface-panel__header">
        <h2>文章数据</h2>
        <span>{{ tableData.length }} 篇</span>
      </div>
      <el-table :data="tableData" row-key="id">
        <el-table-column label="文章" min-width="260">
          <template #default="{ row }">
            <div class="article-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.category.name }} · {{ formatDate(row.publishedAt) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'">
              {{ row.status === 'published' ? '已发布' : '未发布' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="浏览" prop="viewCount" sortable width="110" />
        <el-table-column label="点赞" prop="likeCount" sortable width="110" />
        <el-table-column label="收藏" prop="favoriteCount" sortable width="110" />
        <el-table-column label="评论" prop="commentCount" sortable width="110" />
        <el-table-column label="互动" prop="engagement" sortable width="110" />
      </el-table>
    </section>
  </section>
</template>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-header p {
  margin: 0 0 6px;
  color: #3d6f8f;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.page-header h1,
.surface-panel h2 {
  margin: 0;
  color: #172033;
}

.page-header h1 {
  font-size: 28px;
}

.analytics-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.analytics-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 16px;
  margin-bottom: 16px;
}

.surface-panel {
  min-width: 0;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.surface-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.surface-panel__header span {
  color: #98a2b3;
  font-size: 13px;
  font-weight: 700;
}

.article-cell strong,
.article-cell span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-cell strong {
  color: #172033;
}

.article-cell span {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

@media (max-width: 1080px) {
  .analytics-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .analytics-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .analytics-stats {
    grid-template-columns: 1fr;
  }

  .surface-panel__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
