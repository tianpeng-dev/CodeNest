<script setup lang="ts">
import { computed, onMounted, ref, type Component } from 'vue';
import {
  Calendar,
  ChatLineRound,
  Collection,
  EditPen,
  House,
  Link,
  Reading,
  Star,
  TrendCharts,
} from '@element-plus/icons-vue';
import EmptyState from '@/components/EmptyState.vue';
import StatCard from '@/components/StatCard.vue';
import PostList from '@/modules/post/PostList.vue';
import UserSummaryCard from '@/modules/user/UserSummaryCard.vue';
import { getPosts } from '@/services/post.service';
import type { Post } from '@/types/post';
import type { User } from '@/types/user';

interface CategoryFilter {
  id: string;
  label: string;
  description: string;
}

interface CommunityAction {
  title: string;
  description: string;
  icon: Component;
  to: string;
}

interface EventItem {
  date: string;
  label: string;
  title: string;
  description: string;
}

const categoryFilters: CategoryFilter[] = [
  { id: 'all', label: '全部', description: '社区最新发布' },
  { id: 'cat-frontend', label: '前端工程', description: 'Vue、构建与体验' },
  { id: 'cat-backend', label: '后端架构', description: 'API、缓存与服务' },
  { id: 'cat-growth', label: '社区运营', description: '增长、治理与数据' },
];

const leftMenu = [
  { label: '首页索引', to: '/', icon: House },
  { label: '技术专题', to: '/category/frontend', icon: Reading },
  { label: '热门讨论', to: '/search', icon: ChatLineRound },
  { label: '创作中心', to: '/creator/overview', icon: EditPen },
];

const friendLinks = [
  { label: 'Vue 中文社区', href: 'https://cn.vuejs.org/' },
  { label: 'Element Plus', href: 'https://element-plus.org/' },
  { label: 'MDN Web Docs', href: 'https://developer.mozilla.org/' },
];

const communityActions: CommunityAction[] = [
  {
    title: '本周技术问答',
    description: '把卡住的问题整理成帖，优先获得维护者回复。',
    icon: ChatLineRound,
    to: '/search',
  },
  {
    title: '作者成长计划',
    description: '连续发布技术记录，解锁专栏与数据分析能力。',
    icon: EditPen,
    to: '/creator/overview',
  },
  {
    title: '收藏夹精选',
    description: '从高收藏内容里筛出可复用的工程实践。',
    icon: Collection,
    to: '/search',
  },
];

const eventCalendar: EventItem[] = [
  {
    date: '06/21',
    label: '直播',
    title: 'Vue 组件边界复盘',
    description: '拆解组合式函数、Pinia 与页面状态的职责。',
  },
  {
    date: '06/24',
    label: '共创',
    title: '缓存与接口稳定性专题',
    description: '征集后端架构方向的实战复盘。',
  },
  {
    date: '06/28',
    label: '活动',
    title: '冷启动内容诊断日',
    description: '围绕阅读、评论、收藏指标做社区诊断。',
  },
];

const activeCategory = ref('all');
const page = ref(1);
const pageSize = 5;
const total = ref(0);
const posts = ref<Post[]>([]);
const loading = ref(false);
const errorMessage = ref('');

const activeCategoryInfo = computed(() => {
  return (
    categoryFilters.find((category) => category.id === activeCategory.value) ??
    categoryFilters[0]
  );
});

const headlinePost = computed(() => posts.value[0] ?? null);
const carouselPosts = computed(() => posts.value.slice(0, 3));

const recommendedUsers = computed<User[]>(() => {
  const usersById = new Map<string, User>();

  for (const post of posts.value) {
    usersById.set(post.author.id, post.author);
  }

  return Array.from(usersById.values()).slice(0, 2);
});

const totalViews = computed(() => {
  return posts.value.reduce((sum, post) => sum + post.viewCount, 0);
});

const totalComments = computed(() => {
  return posts.value.reduce((sum, post) => sum + post.commentCount, 0);
});

function formatCount(value: number) {
  if (value >= 10000) {
    return `${(value / 10000).toFixed(1)}w`;
  }

  return String(value);
}

async function loadPosts() {
  loading.value = true;
  errorMessage.value = '';

  try {
    const result = await getPosts({
      page: page.value,
      pageSize,
      status: 'published',
      sortBy: 'latest',
      categoryId:
        activeCategory.value === 'all' ? undefined : activeCategory.value,
    });

    posts.value = result.items;
    total.value = result.total;
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '推荐内容加载失败';
    posts.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleCategoryChange() {
  page.value = 1;
  loadPosts();
}

function handlePageChange(nextPage: number) {
  page.value = nextPage;
  loadPosts();
}

onMounted(() => {
  loadPosts();
});
</script>

<template>
  <div class="home-page">
    <aside class="home-page__left" aria-label="站点导航">
      <section class="side-section">
        <h2>CodeNest</h2>
        <nav class="home-menu">
          <RouterLink v-for="item in leftMenu" :key="item.label" :to="item.to">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
      </section>

      <section class="side-section">
        <h2>友情链接</h2>
        <div class="friend-links">
          <a
            v-for="item in friendLinks"
            :key="item.href"
            :href="item.href"
            target="_blank"
            rel="noreferrer"
          >
            <el-icon><Link /></el-icon>
            <span>{{ item.label }}</span>
          </a>
        </div>
      </section>

      <p class="filing-text">
        CodeNest 技术社区<br />
        京ICP备 20260619 号
      </p>
    </aside>

    <main class="home-page__main">
      <section class="portal-surface category-strip" aria-label="分类筛选">
        <div>
          <span class="section-kicker">频道</span>
          <h1>{{ activeCategoryInfo.label }}</h1>
          <p>{{ activeCategoryInfo.description }}</p>
        </div>
        <el-radio-group
          v-model="activeCategory"
          class="category-strip__filters"
          @change="handleCategoryChange"
        >
          <el-radio-button
            v-for="category in categoryFilters"
            :key="category.id"
            :label="category.id"
          >
            {{ category.label }}
          </el-radio-button>
        </el-radio-group>
      </section>

      <section class="top-grid">
        <article class="portal-surface headline-block">
          <span class="section-kicker">头条</span>
          <template v-if="headlinePost">
            <RouterLink class="headline-block__title" :to="`/post/${headlinePost.id}`">
              {{ headlinePost.title }}
            </RouterLink>
            <p>{{ headlinePost.summary }}</p>
            <div class="headline-block__meta">
              <span>{{ headlinePost.author.displayName }}</span>
              <span>{{ formatCount(headlinePost.viewCount) }} 浏览</span>
              <span>{{ headlinePost.commentCount }} 评论</span>
            </div>
          </template>
          <EmptyState
            v-else-if="!loading"
            title="暂无头条"
            description="当前分类还没有已发布内容。"
          />
        </article>

        <section class="portal-surface carousel-block" aria-label="推荐轮播">
          <el-carousel
            v-if="carouselPosts.length > 0"
            height="210px"
            trigger="click"
            indicator-position="outside"
          >
            <el-carousel-item
              v-for="post in carouselPosts"
              :key="post.id"
            >
              <RouterLink class="carousel-slide" :to="`/post/${post.id}`">
                <img :src="post.coverUrl" :alt="post.title" />
                <span>{{ post.category.name }}</span>
                <strong>{{ post.title }}</strong>
              </RouterLink>
            </el-carousel-item>
          </el-carousel>
          <EmptyState
            v-else-if="!loading"
            title="暂无轮播内容"
            description="发布后的高质量帖子会出现在这里。"
          />
        </section>
      </section>

      <section class="portal-surface feed-section">
        <header class="section-header">
          <div>
            <span class="section-kicker">推荐阅读</span>
            <h2>技术文章</h2>
          </div>
          <RouterLink class="section-header__link" to="/search">查看更多</RouterLink>
        </header>

        <el-alert
          v-if="errorMessage"
          class="feed-section__alert"
          type="error"
          :title="errorMessage"
          show-icon
          :closable="false"
        />

        <el-skeleton v-if="loading" :rows="8" animated />
        <PostList v-else :posts="posts" />

        <footer class="feed-section__pagination">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="page"
            :page-size="pageSize"
            :total="total"
            @current-change="handlePageChange"
          />
        </footer>
      </section>
    </main>

    <aside class="home-page__right" aria-label="社区推荐">
      <section class="portal-surface stats-grid">
        <StatCard label="推荐文章" :value="total" :delta="12" />
        <StatCard label="本页浏览" :value="formatCount(totalViews)" :delta="8" />
        <StatCard label="讨论数" :value="totalComments" :delta="-2" />
      </section>

      <section class="portal-surface community-block">
        <header class="section-header">
          <div>
            <span class="section-kicker">社区</span>
            <h2>推荐行动</h2>
          </div>
          <el-icon><TrendCharts /></el-icon>
        </header>
        <div class="community-actions">
          <RouterLink
            v-for="item in communityActions"
            :key="item.title"
            :to="item.to"
            class="community-action"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.description }}</small>
            </span>
          </RouterLink>
        </div>
      </section>

      <section class="portal-surface authors-block">
        <header class="section-header">
          <div>
            <span class="section-kicker">作者</span>
            <h2>活跃成员</h2>
          </div>
          <el-icon><Star /></el-icon>
        </header>
        <div class="authors-block__list">
          <UserSummaryCard
            v-for="user in recommendedUsers"
            :key="user.id"
            :user="user"
          />
        </div>
      </section>

      <section class="portal-surface event-block">
        <header class="section-header">
          <div>
            <span class="section-kicker">日程</span>
            <h2>活动日历</h2>
          </div>
          <el-icon><Calendar /></el-icon>
        </header>
        <ol class="event-list">
          <li v-for="event in eventCalendar" :key="event.date + event.title">
            <time>{{ event.date }}</time>
            <span>{{ event.label }}</span>
            <strong>{{ event.title }}</strong>
            <p>{{ event.description }}</p>
          </li>
        </ol>
      </section>
    </aside>
  </div>
</template>

<style scoped>
.home-page {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) 312px;
  align-items: start;
  gap: 16px;
}

.home-page__left,
.home-page__right {
  display: grid;
  gap: 14px;
}

.home-page__main {
  display: grid;
  min-width: 0;
  gap: 14px;
}

.portal-surface,
.side-section {
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.portal-surface {
  padding: 16px;
}

.side-section {
  padding: 14px;
}

.side-section h2,
.section-header h2,
.category-strip h1 {
  margin: 0;
  color: #172033;
}

.side-section h2 {
  font-size: 14px;
}

.section-kicker {
  display: inline-flex;
  align-items: center;
  color: #1f4f8f;
  font-size: 12px;
  font-weight: 800;
}

.home-menu,
.friend-links {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.home-menu a,
.friend-links a {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 8px;
  color: #344054;
  font-size: 13px;
  border-radius: 8px;
}

.home-menu a:hover,
.friend-links a:hover {
  color: #1f4f8f;
  background: #eef5ff;
}

.home-menu span,
.friend-links span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filing-text {
  margin: 0;
  padding: 12px 6px;
  color: #667085;
  font-size: 12px;
  line-height: 1.7;
}

.category-strip {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
}

.category-strip h1 {
  margin-top: 2px;
  font-size: 24px;
}

.category-strip p {
  margin: 4px 0 0;
  color: #667085;
}

.category-strip__filters {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(320px, 1.1fr);
  gap: 14px;
}

.headline-block {
  display: flex;
  min-height: 252px;
  flex-direction: column;
}

.headline-block__title {
  margin-top: 10px;
  color: #172033;
  font-size: 28px;
  font-weight: 900;
  line-height: 1.25;
}

.headline-block__title:hover {
  color: #1f4f8f;
}

.headline-block p {
  margin: 12px 0 0;
  color: #475467;
  line-height: 1.8;
}

.headline-block__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: auto;
  padding-top: 18px;
  color: #667085;
  font-size: 13px;
}

.carousel-block {
  min-width: 0;
  padding-bottom: 8px;
}

.carousel-slide {
  position: relative;
  display: grid;
  align-content: end;
  height: 100%;
  min-height: 210px;
  overflow: hidden;
  padding: 20px;
  color: #ffffff;
  background: #172033;
  border-radius: 8px;
}

.carousel-slide::after {
  position: absolute;
  inset: 0;
  content: "";
  background: linear-gradient(180deg, rgb(23 32 51 / 10%), rgb(23 32 51 / 82%));
}

.carousel-slide img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-slide span,
.carousel-slide strong {
  position: relative;
  z-index: 1;
}

.carousel-slide span {
  width: fit-content;
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 800;
  background: rgb(31 79 143 / 86%);
  border-radius: 999px;
}

.carousel-slide strong {
  display: block;
  margin-top: 10px;
  font-size: 24px;
  line-height: 1.3;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-header h2 {
  margin-top: 2px;
  font-size: 18px;
}

.section-header__link {
  color: #1f4f8f;
  font-size: 13px;
  font-weight: 800;
}

.feed-section {
  min-width: 0;
}

.feed-section__alert {
  margin-bottom: 14px;
}

.feed-section__pagination {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  padding: 0;
  background: transparent;
  border: 0;
}

.community-actions {
  display: grid;
  gap: 10px;
}

.community-action {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
}

.community-action > .el-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  color: #1f4f8f;
  background: #eef5ff;
  border-radius: 8px;
}

.community-action strong,
.community-action small {
  display: block;
}

.community-action strong {
  color: #172033;
  font-size: 14px;
}

.community-action small {
  margin-top: 4px;
  color: #667085;
  line-height: 1.5;
}

.authors-block__list {
  display: grid;
  gap: 10px;
}

.event-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.event-list li {
  display: grid;
  grid-template-columns: 52px 44px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf1f7;
}

.event-list li:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.event-list time {
  color: #1f4f8f;
  font-weight: 900;
}

.event-list li > span {
  justify-self: start;
  padding: 2px 6px;
  color: #815b00;
  font-size: 12px;
  font-weight: 800;
  background: #fff5d6;
  border-radius: 999px;
}

.event-list strong {
  min-width: 0;
  color: #172033;
  font-size: 14px;
}

.event-list p {
  grid-column: 3;
  margin: -4px 0 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .home-page {
    grid-template-columns: 168px minmax(0, 1fr);
  }

  .home-page__right {
    grid-column: 1 / -1;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stats-grid,
  .event-block {
    grid-column: 1 / -1;
  }

  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .home-page,
  .home-page__right,
  .top-grid {
    grid-template-columns: 1fr;
  }

  .home-page__left {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filing-text {
    grid-column: 1 / -1;
  }

  .category-strip {
    grid-template-columns: 1fr;
  }

  .category-strip__filters {
    justify-content: flex-start;
  }

  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .portal-surface,
  .side-section {
    padding: 12px;
  }

  .home-page__left,
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .headline-block {
    min-height: 0;
  }

  .headline-block__title {
    font-size: 22px;
  }

  .carousel-slide strong {
    font-size: 20px;
  }

  .event-list li {
    grid-template-columns: 52px minmax(0, 1fr);
  }

  .event-list li > span {
    grid-column: 2;
    grid-row: 1;
    margin-left: 58px;
  }

  .event-list strong,
  .event-list p {
    grid-column: 2;
  }
}
</style>
