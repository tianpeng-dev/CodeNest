<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Search } from '@element-plus/icons-vue';
import EmptyState from '@/components/EmptyState.vue';
import PostList from '@/modules/post/PostList.vue';
import { getPosts } from '@/services/post.service';
import type { Post } from '@/types/post';

const route = useRoute();
const router = useRouter();
const keyword = computed(() => String(route.query.keyword ?? '').trim());
const searchText = ref(keyword.value);
const posts = ref<Post[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 6;
const loading = ref(false);
const errorMessage = ref('');
const requestSequence = ref(0);

async function loadPosts() {
  const requestId = requestSequence.value + 1;
  requestSequence.value = requestId;
  loading.value = true;
  errorMessage.value = '';

  try {
    const result = await getPosts({
      keyword: keyword.value || undefined,
      status: 'published',
      sortBy: keyword.value ? 'popular' : 'latest',
      page: page.value,
      pageSize,
    });

    if (requestId !== requestSequence.value) return;

    posts.value = result.items;
    total.value = result.total;
  } catch (error) {
    if (requestId !== requestSequence.value) return;

    errorMessage.value =
      error instanceof Error ? error.message : '搜索结果加载失败';
    posts.value = [];
    total.value = 0;
  } finally {
    if (requestId === requestSequence.value) {
      loading.value = false;
    }
  }
}

function submitSearch() {
  page.value = 1;
  router.push({
    path: '/search',
    query: searchText.value.trim()
      ? { keyword: searchText.value.trim() }
      : undefined,
  });
}

function handlePageChange(nextPage: number) {
  page.value = nextPage;
  loadPosts();
}

watch(
  keyword,
  (nextKeyword) => {
    searchText.value = nextKeyword;
    page.value = 1;
    loadPosts();
  },
  { immediate: true },
);
</script>

<template>
  <div class="search-page">
    <section class="search-panel">
      <div>
        <span class="section-kicker">搜索</span>
        <h1>{{ keyword ? `“${keyword}”的结果` : '全站内容检索' }}</h1>
        <p>按标题、摘要和正文匹配已发布内容。</p>
      </div>
      <form class="search-panel__form" @submit.prevent="submitSearch">
        <el-input
          v-model="searchText"
          placeholder="搜索 Vue、缓存、社区运营"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" native-type="submit">搜索</el-button>
      </form>
    </section>

    <section class="search-results">
      <header class="result-header">
        <h2>内容列表</h2>
        <span>{{ total }} 条结果</span>
      </header>

      <el-skeleton v-if="loading" :rows="8" animated />

      <EmptyState
        v-else-if="errorMessage"
        title="搜索失败"
        :description="errorMessage"
      >
        <template #action>
          <el-button type="primary" @click="loadPosts">重新加载</el-button>
        </template>
      </EmptyState>

      <PostList v-else-if="posts.length > 0" :posts="posts" />

      <EmptyState
        v-else
        title="没有匹配内容"
        description="换一个关键词，或清空搜索查看最新发布。"
      />

      <el-pagination
        v-if="total > pageSize"
        class="search-results__pagination"
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="total"
        :current-page="page"
        @current-change="handlePageChange"
      />
    </section>
  </div>
</template>

<style scoped>
.search-page {
  display: grid;
  gap: 18px;
}

.search-panel,
.search-results {
  padding: 20px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.search-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 440px);
  gap: 20px;
  align-items: end;
}

.section-kicker {
  color: #1f4f8f;
  font-size: 13px;
  font-weight: 800;
}

.search-panel h1,
.result-header h2 {
  margin: 6px 0 0;
  color: #172033;
}

.search-panel h1 {
  font-size: 28px;
}

.search-panel p {
  margin: 8px 0 0;
  color: #667085;
}

.search-panel__form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.result-header h2 {
  font-size: 20px;
}

.result-header span {
  color: #667085;
}

.search-results__pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 760px) {
  .search-panel {
    grid-template-columns: 1fr;
  }

  .search-panel__form {
    grid-template-columns: 1fr;
  }
}
</style>
