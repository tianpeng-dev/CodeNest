<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { Delete, DocumentAdd, View } from '@element-plus/icons-vue';
import EmptyState from '@/components/EmptyState.vue';
import { getCreatorPosts } from '@/services/creator.service';
import { deletePost } from '@/services/post.service';
import type { Post } from '@/types/post';

const loading = ref(false);
const posts = ref<Post[]>([]);

const publishedCount = computed(() => {
  return posts.value.filter((post) => post.status === 'published').length;
});

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
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value));
}

async function loadPosts() {
  loading.value = true;

  try {
    const result = await getCreatorPosts({ page: 1, pageSize: 50 });
    posts.value = result.items;
  } finally {
    loading.value = false;
  }
}

async function removePost(post: Post) {
  await deletePost(post.id);
  await loadPosts();
}

onMounted(loadPosts);
</script>

<template>
  <section class="creator-posts" v-loading="loading">
    <header class="page-header">
      <div>
        <p>Content</p>
        <h1>内容管理</h1>
      </div>
      <RouterLink to="/creator/editor">
        <el-button type="primary" :icon="DocumentAdd">新建文章</el-button>
      </RouterLink>
    </header>

    <section class="posts-summary">
      <div>
        <span>全部文章</span>
        <strong>{{ posts.length }}</strong>
      </div>
      <div>
        <span>已发布</span>
        <strong>{{ publishedCount }}</strong>
      </div>
      <div>
        <span>草稿</span>
        <strong>{{ posts.length - publishedCount }}</strong>
      </div>
    </section>

    <section class="surface-panel">
      <el-table v-if="posts.length" :data="posts" row-key="id">
        <el-table-column label="文章" min-width="280">
          <template #default="{ row }">
            <div class="post-cell">
              <img :src="row.coverUrl" alt="" />
              <div>
                <strong>{{ row.title }}</strong>
                <span>{{ row.summary }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" prop="category.name" width="120" />
        <el-table-column label="浏览" prop="viewCount" width="100" />
        <el-table-column label="评论" prop="commentCount" width="100" />
        <el-table-column label="更新" width="130">
          <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="160">
          <template #default="{ row }">
            <div class="table-actions">
              <RouterLink v-if="row.status !== 'deleted'" :to="`/post/${row.id}`">
                <el-button text :icon="View">查看</el-button>
              </RouterLink>
              <el-button
                v-if="row.status !== 'deleted'"
                text
                type="danger"
                :icon="Delete"
                @click="removePost(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState
        v-else
        title="暂无文章"
        description="保存草稿或发布文章后，会在这里统一管理。"
      />
    </section>
  </section>
</template>

<style scoped>
.page-header,
.posts-summary,
.post-cell {
  display: flex;
  align-items: center;
}

.page-header {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header p {
  margin: 0 0 6px;
  color: #3d6f8f;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.page-header h1 {
  margin: 0;
  color: #172033;
  font-size: 28px;
}

.posts-summary {
  gap: 14px;
  margin-bottom: 16px;
}

.posts-summary div {
  min-width: 140px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.posts-summary span {
  display: block;
  color: #667085;
  font-size: 12px;
}

.posts-summary strong {
  display: block;
  margin-top: 6px;
  color: #172033;
  font-size: 22px;
}

.surface-panel {
  padding: 14px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.post-cell {
  gap: 12px;
  min-width: 0;
}

.post-cell img {
  width: 64px;
  height: 46px;
  object-fit: cover;
  border-radius: 6px;
}

.post-cell strong,
.post-cell span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-cell strong {
  color: #172033;
}

.post-cell span {
  max-width: 440px;
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
}

@media (max-width: 700px) {
  .page-header,
  .posts-summary {
    align-items: flex-start;
    flex-direction: column;
  }

  .posts-summary div {
    width: 100%;
  }
}
</style>
