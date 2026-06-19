<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { Calendar, Collection, View } from '@element-plus/icons-vue';
import EmptyState from '@/components/EmptyState.vue';
import CommentList from '@/modules/comment/CommentList.vue';
import AuthorSidebar from '@/modules/post/AuthorSidebar.vue';
import InteractionBar from '@/modules/post/InteractionBar.vue';
import { getComments } from '@/services/comment.service';
import { getPostById, getPosts } from '@/services/post.service';
import type { Comment } from '@/types/comment';
import type { Post } from '@/types/post';
import { renderMarkdown } from '@/utils/markdown';

const route = useRoute();
const post = ref<Post | null>(null);
const comments = ref<Comment[]>([]);
const hotPosts = ref<Post[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const requestSequence = ref(0);
const commentsSection = ref<InstanceType<typeof CommentList> | null>(null);

const renderedContent = computed(() => {
  return post.value ? renderMarkdown(post.value.content) : '';
});

const formatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

function formatTime(value: string | null) {
  return formatter.format(new Date(value ?? Date.now()));
}

function formatCount(value: number) {
  if (value >= 10000) {
    return `${(value / 10000).toFixed(1)}w`;
  }

  return String(value);
}

async function loadPost(id: string) {
  const requestId = requestSequence.value + 1;
  requestSequence.value = requestId;
  loading.value = true;
  errorMessage.value = '';
  post.value = null;
  comments.value = [];
  hotPosts.value = [];

  try {
    const [loadedPost, loadedComments] = await Promise.all([
      getPostById(id),
      getComments(id),
    ]);

    if (requestId !== requestSequence.value) return;

    post.value = loadedPost;
    comments.value = loadedComments;

    const authorPosts = await getPosts({
      authorId: loadedPost.author.id,
      status: 'published',
      sortBy: 'popular',
      page: 1,
      pageSize: 5,
    });

    if (requestId === requestSequence.value) {
      hotPosts.value = authorPosts.items
        .filter((item) => item.id !== loadedPost.id)
        .slice(0, 3);
    }
  } catch (error) {
    if (requestId !== requestSequence.value) return;

    errorMessage.value =
      error instanceof Error ? error.message : '帖子详情加载失败';
  } finally {
    if (requestId === requestSequence.value) {
      loading.value = false;
    }
  }
}

function handlePostUpdated(nextPost: Post) {
  post.value = nextPost;
}

function handleCommentCreated(comment: Comment) {
  comments.value = [comment, ...comments.value];
  if (post.value) {
    post.value = {
      ...post.value,
      commentCount: post.value.commentCount + 1,
    };
  }
}

async function focusComments() {
  await nextTick();
  const element = commentsSection.value?.$el as HTMLElement | undefined;
  element?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

watch(
  () => route.params.id,
  (id) => {
    if (typeof id === 'string' && id) {
      loadPost(id);
    }
  },
  { immediate: true },
);
</script>

<template>
  <div class="post-detail-page">
    <el-skeleton v-if="loading" :rows="10" animated />

    <EmptyState
      v-else-if="errorMessage"
      title="帖子加载失败"
      :description="errorMessage"
    >
      <template #action>
        <el-button type="primary" @click="loadPost(String(route.params.id))">
          重新加载
        </el-button>
      </template>
    </EmptyState>

    <EmptyState
      v-else-if="!post"
      title="没有找到帖子"
      description="帖子可能已删除或暂时无法访问。"
    />

    <template v-else>
      <main class="post-detail-page__main">
        <article class="post-article">
          <header class="post-article__header">
            <RouterLink class="post-article__category" :to="`/category/${post.category.slug}`">
              {{ post.category.name }}
            </RouterLink>
            <h1>{{ post.title }}</h1>
            <p>{{ post.summary }}</p>
            <div class="post-article__meta">
              <RouterLink :to="`/u/${post.author.id}`">
                {{ post.author.displayName }}
              </RouterLink>
              <span>
                <el-icon><Calendar /></el-icon>
                {{ formatTime(post.publishedAt ?? post.createdAt) }}
              </span>
              <span>
                <el-icon><View /></el-icon>
                {{ formatCount(post.viewCount) }} 浏览
              </span>
              <span>
                <el-icon><Collection /></el-icon>
                {{ formatCount(post.favoriteCount) }} 收藏
              </span>
            </div>
            <div class="post-article__tags" aria-label="标签">
              <el-tag
                v-for="tag in post.tags"
                :key="tag"
                size="small"
                effect="plain"
              >
                {{ tag }}
              </el-tag>
            </div>
          </header>

          <div class="markdown-body" v-html="renderedContent" />
        </article>

        <InteractionBar
          :post="post"
          @updated="handlePostUpdated"
          @focus-comments="focusComments"
        />

        <CommentList
          ref="commentsSection"
          :post-id="post.id"
          :comments="comments"
          @created="handleCommentCreated"
        />
      </main>

      <AuthorSidebar
        class="post-detail-page__side"
        :author="post.author"
        :hot-posts="hotPosts"
        :latest-comments="comments.slice(0, 4)"
      />
    </template>
  </div>
</template>

<style scoped>
.post-detail-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.post-detail-page__main {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.post-article {
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.post-article__header {
  padding: 24px 26px 18px;
  border-bottom: 1px solid #edf1f7;
}

.post-article__category {
  display: inline-flex;
  color: #1f4f8f;
  font-size: 13px;
  font-weight: 800;
}

.post-article h1 {
  margin: 10px 0 0;
  color: #172033;
  font-size: 32px;
  line-height: 1.25;
}

.post-article__header p {
  margin: 12px 0 0;
  color: #475467;
  font-size: 16px;
  line-height: 1.8;
}

.post-article__meta,
.post-article__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.post-article__meta {
  color: #667085;
  font-size: 13px;
}

.post-article__meta a {
  color: #344054;
  font-weight: 800;
}

.post-article__meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.markdown-body {
  padding: 24px 26px 32px;
  color: #344054;
  font-size: 16px;
  line-height: 1.85;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 1.4em 0 0.6em;
  color: #172033;
  line-height: 1.35;
}

.markdown-body :deep(h1:first-child),
.markdown-body :deep(h2:first-child),
.markdown-body :deep(h3:first-child) {
  margin-top: 0;
}

.markdown-body :deep(p) {
  margin: 0 0 1em;
}

.markdown-body :deep(a) {
  color: #1f4f8f;
  font-weight: 700;
}

.markdown-body :deep(code) {
  padding: 2px 5px;
  background: #eef3f8;
  border-radius: 5px;
}

.markdown-body :deep(pre) {
  overflow-x: auto;
  padding: 14px;
  background: #101828;
  border-radius: 8px;
}

.markdown-body :deep(pre code) {
  padding: 0;
  color: #f8fafc;
  background: transparent;
}

@media (max-width: 900px) {
  .post-detail-page {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .post-article__header,
  .markdown-body {
    padding-right: 16px;
    padding-left: 16px;
  }

  .post-article h1 {
    font-size: 25px;
  }
}
</style>
