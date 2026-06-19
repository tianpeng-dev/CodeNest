<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import StatCard from '@/components/StatCard.vue';
import StatsChart from '@/modules/analytics/StatsChart.vue';
import {
  getAdminCategories,
  getAdminMetrics,
  getAdminPosts,
  getAdminUsers,
  getSensitiveWords,
} from '@/services/admin.service';
import type { AdminMetric, SensitiveWord } from '@/types/admin';
import type { PiePoint, TrendPoint } from '@/types/analytics';
import type { Post } from '@/types/post';
import type { User } from '@/types/user';

const metrics = ref<AdminMetric[]>([]);
const posts = ref<Post[]>([]);
const users = ref<User[]>([]);
const sensitiveWords = ref<SensitiveWord[]>([]);
const contentPie = ref<PiePoint[]>([]);
const loading = ref(false);

const trend = computed<TrendPoint[]>(() => {
  const buckets = new Map<string, number>();

  posts.value.forEach((post) => {
    const date = post.createdAt.slice(0, 10);
    buckets.set(date, (buckets.get(date) ?? 0) + post.viewCount);
  });

  return [...buckets.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .slice(-7)
    .map(([date, value]) => ({ date, value }));
});

const recentActivity = computed(() => {
  const postItems = posts.value.slice(0, 4).map((post) => ({
    id: post.id,
    title: post.title,
    meta: `${post.author.displayName} · ${post.category.name}`,
    status: post.status === 'published' ? '已发布' : post.status === 'hidden' ? '已隐藏' : '草稿',
  }));

  const sensitiveItems = sensitiveWords.value.slice(0, 2).map((word) => ({
    id: word.id,
    title: `敏感词命中：${word.word}`,
    meta: `${word.hitCount} 次命中`,
    status: word.level === 'high' ? '高风险' : '待观察',
  }));

  return [...postItems, ...sensitiveItems].slice(0, 5);
});

const activeAdminCount = computed(() => {
  return users.value.filter((user) => user.role === 'admin' && user.status === 'active').length;
});

onMounted(async () => {
  loading.value = true;
  try {
    const [metricResult, postResult, userResult, words, categories] = await Promise.all([
      getAdminMetrics(),
      getAdminPosts({ page: 1, pageSize: 50 }),
      getAdminUsers(),
      getSensitiveWords(),
      getAdminCategories(),
    ]);

    metrics.value = metricResult;
    posts.value = postResult.items;
    users.value = userResult;
    sensitiveWords.value = words;
    contentPie.value = categories.map((category) => ({
      name: category.name,
      value: category.postCount,
    }));
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <section class="admin-dashboard" v-loading="loading">
    <header class="admin-dashboard__header">
      <div>
        <h1>管理后台</h1>
        <p>平台核心指标、内容走势和近期治理动态。</p>
      </div>
      <span>{{ activeAdminCount }} 名在线管理角色</span>
    </header>

    <div class="admin-dashboard__metrics">
      <StatCard
        v-for="metric in metrics"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :delta="metric.trend"
      />
    </div>

    <div class="admin-dashboard__charts">
      <section class="admin-panel">
        <header>
          <h2>内容浏览趋势</h2>
        </header>
        <StatsChart type="line" title="浏览量" :data="trend" :height="300" />
      </section>

      <section class="admin-panel">
        <header>
          <h2>内容分类占比</h2>
        </header>
        <StatsChart type="pie" title="分类占比" :data="contentPie" :height="300" />
      </section>
    </div>

    <section class="admin-panel">
      <header>
        <h2>近期平台活动</h2>
      </header>
      <ul class="admin-dashboard__activity">
        <li v-for="item in recentActivity" :key="item.id">
          <div>
            <strong>{{ item.title }}</strong>
            <span>{{ item.meta }}</span>
          </div>
          <el-tag size="small" type="info">{{ item.status }}</el-tag>
        </li>
      </ul>
    </section>
  </section>
</template>

<style scoped>
.admin-dashboard {
  display: grid;
  gap: 18px;
}

.admin-dashboard__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.admin-dashboard__header h1 {
  margin: 0;
  color: #172033;
  font-size: 24px;
  line-height: 1.2;
}

.admin-dashboard__header p {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
}

.admin-dashboard__header span {
  color: #475467;
  font-size: 13px;
  font-weight: 700;
}

.admin-dashboard__metrics {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-dashboard__charts {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
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

.admin-dashboard__activity {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.admin-dashboard__activity li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 54px;
  border-top: 1px solid #edf1f7;
}

.admin-dashboard__activity li:first-child {
  border-top: 0;
}

.admin-dashboard__activity strong,
.admin-dashboard__activity span {
  display: block;
}

.admin-dashboard__activity strong {
  color: #172033;
  font-size: 14px;
}

.admin-dashboard__activity span {
  margin-top: 3px;
  color: #667085;
  font-size: 12px;
}

@media (max-width: 1080px) {
  .admin-dashboard__metrics,
  .admin-dashboard__charts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .admin-dashboard__header,
  .admin-dashboard__activity li {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-dashboard__metrics,
  .admin-dashboard__charts {
    grid-template-columns: 1fr;
  }
}
</style>
