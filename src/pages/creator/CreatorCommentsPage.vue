<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ChatLineRound } from '@element-plus/icons-vue';
import EmptyState from '@/components/EmptyState.vue';
import { getCreatorComments, type CreatorComment } from '@/services/creator.service';
import { deleteComment } from '@/services/comment.service';

const loading = ref(false);
const comments = ref<CreatorComment[]>([]);

const uniqueArticleCount = computed(() => {
  return new Set(comments.value.map((comment) => comment.postId)).size;
});

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

async function loadComments() {
  loading.value = true;

  try {
    comments.value = await getCreatorComments();
  } finally {
    loading.value = false;
  }
}

async function removeComment(comment: CreatorComment) {
  await deleteComment(comment.id);
  await loadComments();
}

onMounted(loadComments);
</script>

<template>
  <section class="creator-comments" v-loading="loading">
    <header class="page-header">
      <div>
        <p>Conversation</p>
        <h1>评论管理</h1>
      </div>
      <div class="comments-meter">
        <el-icon><ChatLineRound /></el-icon>
        <span>{{ comments.length }} 条评论来自 {{ uniqueArticleCount }} 篇文章</span>
      </div>
    </header>

    <section class="surface-panel">
      <el-table v-if="comments.length" :data="comments" row-key="id">
        <el-table-column label="评论内容" min-width="280">
          <template #default="{ row }">
            <div class="comment-content">
              <strong>{{ row.author.displayName }}</strong>
              <span>{{ row.content }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属文章" min-width="220">
          <template #default="{ row }">
            <RouterLink :to="`/post/${row.post.id}`">{{ row.post.title }}</RouterLink>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="110">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="danger" @click="removeComment(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState
        v-else
        title="暂无评论"
        description="读者互动会按文章聚合到这里。"
      />
    </section>
  </section>
</template>

<style scoped>
.page-header,
.comments-meter {
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

.comments-meter {
  gap: 8px;
  padding: 10px 12px;
  color: #3d6f8f;
  background: #eef7fb;
  border: 1px solid #c7e4f1;
  border-radius: 8px;
  font-weight: 700;
}

.surface-panel {
  padding: 14px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.comment-content strong,
.comment-content span {
  display: block;
}

.comment-content strong {
  color: #172033;
}

.comment-content span {
  margin-top: 4px;
  color: #667085;
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
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
