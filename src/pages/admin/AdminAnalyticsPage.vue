<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import StatCard from '@/components/StatCard.vue';
import AdminTablePage, {
  type AdminRowAction,
  type AdminTableColumn,
} from '@/modules/admin/AdminTablePage.vue';
import StatsChart from '@/modules/analytics/StatsChart.vue';
import {
  getAdminCategories,
  getAdminMetrics,
  getAdminPosts,
} from '@/services/admin.service';
import type { AdminMetric } from '@/types/admin';
import type { PiePoint, TrendPoint } from '@/types/analytics';
import type { Post } from '@/types/post';

type TopPostRow = Post & {
  reviewState: 'new' | 'reviewed';
};

const metrics = ref<AdminMetric[]>([]);
const posts = ref<TopPostRow[]>([]);
const categoryPie = ref<PiePoint[]>([]);
const loading = ref(false);

const engagementTrend = computed<TrendPoint[]>(() => {
  return [...posts.value]
    .sort((left, right) => left.createdAt.localeCompare(right.createdAt))
    .slice(-7)
    .map((post) => ({
      date: post.createdAt.slice(0, 10),
      value: post.likeCount + post.favoriteCount + post.commentCount,
    }));
});

const topPosts = computed(() => {
  return [...posts.value]
    .sort((left, right) => right.viewCount - left.viewCount)
    .slice(0, 8);
});

const totalViews = computed(() => posts.value.reduce((sum, post) => sum + post.viewCount, 0));
const totalEngagement = computed(() =>
  posts.value.reduce(
    (sum, post) => sum + post.likeCount + post.favoriteCount + post.commentCount,
    0,
  ),
);

const columns: AdminTableColumn<TopPostRow>[] = [
  { key: 'title', label: '热门帖子', minWidth: 240 },
  { key: 'author', label: '作者', minWidth: 120, formatter: (row) => row.author.displayName },
  { key: 'category', label: '分类', minWidth: 110, formatter: (row) => row.category.name },
  { key: 'viewCount', label: '浏览', width: 90, align: 'right' },
  { key: 'likeCount', label: '点赞', width: 90, align: 'right' },
  { key: 'commentCount', label: '评论', width: 90, align: 'right' },
  {
    key: 'reviewState',
    label: '状态',
    width: 100,
    tag: (row) => ({
      label: row.reviewState === 'reviewed' ? '已复盘' : '待复盘',
      type: row.reviewState === 'reviewed' ? 'success' : 'info',
    }),
  },
];

const actions: AdminRowAction<TopPostRow>[] = [
  {
    label: '标记复盘',
    type: 'primary',
    confirmText: (row) => `确认标记《${row.title}》为已复盘？`,
    successText: '热门帖子已标记',
    disabled: (row) => row.reviewState === 'reviewed',
    onConfirm: (row) => {
      row.reviewState = 'reviewed';
    },
  },
];

onMounted(async () => {
  loading.value = true;
  try {
    const [metricResult, postResult, categories] = await Promise.all([
      getAdminMetrics(),
      getAdminPosts({ page: 1, pageSize: 50 }),
      getAdminCategories(),
    ]);

    metrics.value = metricResult;
    posts.value = postResult.items.map((post) => ({
      ...post,
      reviewState: 'new',
    }));
    categoryPie.value = categories.map((category) => ({
      name: category.name,
      value: category.postCount,
    }));
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <section class="admin-analytics" v-loading="loading">
    <header class="admin-analytics__header">
      <div>
        <h1>运营分析</h1>
        <p>平台内容表现、互动走势和热门帖子复盘入口。</p>
      </div>
    </header>

    <div class="admin-analytics__metrics">
      <StatCard
        v-for="metric in metrics"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :delta="metric.trend"
      />
      <StatCard label="总浏览" :value="totalViews" :delta="9" />
      <StatCard label="总互动" :value="totalEngagement" :delta="6" />
    </div>

    <div class="admin-analytics__charts">
      <section class="admin-panel">
        <header>
          <h2>互动趋势</h2>
        </header>
        <StatsChart type="line" title="互动数" :data="engagementTrend" :height="300" />
      </section>

      <section class="admin-panel">
        <header>
          <h2>分类内容量</h2>
        </header>
        <StatsChart type="pie" title="帖子数" :data="categoryPie" :height="300" />
      </section>
    </div>

    <AdminTablePage
      title="热门帖子"
      description="按浏览量排序的内容表现列表，复盘标记仅保存在当前页面。"
      :rows="topPosts"
      :columns="columns"
      :actions="actions"
      :page-size="8"
    />
  </section>
</template>

<style scoped>
.admin-analytics {
  display: grid;
  gap: 18px;
}

.admin-analytics__header h1 {
  margin: 0;
  color: #172033;
  font-size: 24px;
  line-height: 1.2;
}

.admin-analytics__header p {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
}

.admin-analytics__metrics {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.admin-analytics__charts {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
}

.admin-panel {
  min-width: 0;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.admin-panel header {
  margin-bottom: 10px;
}

.admin-panel h2 {
  margin: 0;
  color: #172033;
  font-size: 16px;
}

@media (max-width: 1180px) {
  .admin-analytics__metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .admin-analytics__charts {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .admin-analytics__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
