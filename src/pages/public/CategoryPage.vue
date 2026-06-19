<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { Reading } from '@element-plus/icons-vue';
import EmptyState from '@/components/EmptyState.vue';
import PostList from '@/modules/post/PostList.vue';
import { getPosts } from '@/services/post.service';
import type { Category, Post } from '@/types/post';

const route = useRoute();
const slug = computed(() => String(route.params.slug ?? ''));
const posts = ref<Post[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 6;
const loading = ref(false);
const errorMessage = ref('');
const requestSequence = ref(0);

const category = computed<Category | null>(() => {
  return posts.value[0]?.category ?? null;
});

async function loadPosts() {
  const requestId = requestSequence.value + 1;
  requestSequence.value = requestId;
  loading.value = true;
  errorMessage.value = '';

  try {
    const result = await getPosts({
      categorySlug: slug.value,
      status: 'published',
      sortBy: 'latest',
      page: page.value,
      pageSize,
    });

    if (requestId !== requestSequence.value) return;

    posts.value = result.items;
    total.value = result.total;
  } catch (error) {
    if (requestId !== requestSequence.value) return;

    errorMessage.value =
      error instanceof Error ? error.message : '分类内容加载失败';
    posts.value = [];
    total.value = 0;
  } finally {
    if (requestId === requestSequence.value) {
      loading.value = false;
    }
  }
}

function handlePageChange(nextPage: number) {
  page.value = nextPage;
  loadPosts();
}

watch(
  slug,
  () => {
    page.value = 1;
    loadPosts();
  },
  { immediate: true },
);
</script>

<template>
  <div class="category-page">
    <section class="category-heading">
      <div class="category-heading__icon">
        <el-icon><Reading /></el-icon>
      </div>
      <div>
        <span class="section-kicker">分类</span>
        <h1>{{ category?.name ?? slug }}</h1>
        <p>{{ category?.description ?? '查看该分类下的已发布内容。' }}</p>
      </div>
      <strong>{{ total }} 篇</strong>
    </section>

    <section class="category-content">
      <el-skeleton v-if="loading" :rows="8" animated />

      <EmptyState
        v-else-if="errorMessage"
        title="分类加载失败"
        :description="errorMessage"
      >
        <template #action>
          <el-button type="primary" @click="loadPosts">重新加载</el-button>
        </template>
      </EmptyState>

      <PostList v-else-if="posts.length > 0" :posts="posts" />

      <EmptyState
        v-else
        title="这个分类还没有内容"
        description="稍后再来，或回到首页浏览其他技术专题。"
      />

      <el-pagination
        v-if="total > pageSize"
        class="category-content__pagination"
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
.category-page {
  display: grid;
  gap: 18px;
}

.category-heading,
.category-content {
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.category-heading {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 20px;
}

.category-heading__icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  color: #1f4f8f;
  background: #eef5ff;
  border: 1px solid #c9ddff;
  border-radius: 8px;
}

.section-kicker {
  color: #1f4f8f;
  font-size: 13px;
  font-weight: 800;
}

.category-heading h1 {
  margin: 4px 0 0;
  color: #172033;
  font-size: 28px;
}

.category-heading p {
  margin: 8px 0 0;
  color: #667085;
}

.category-heading strong {
  color: #172033;
  font-size: 22px;
}

.category-content {
  padding: 20px;
}

.category-content__pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 640px) {
  .category-heading {
    grid-template-columns: 44px minmax(0, 1fr);
  }

  .category-heading__icon {
    width: 44px;
    height: 44px;
  }

  .category-heading strong {
    grid-column: 2;
    font-size: 18px;
  }
}
</style>
