<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import EmptyState from '@/components/EmptyState.vue';
import { createComment } from '@/services/comment.service';
import { useAuthStore } from '@/stores/auth.store';
import type { Comment } from '@/types/comment';

const props = defineProps<{
  postId: string;
  comments: Comment[];
}>();

const emit = defineEmits<{
  created: [comment: Comment];
}>();

const authStore = useAuthStore();
const localComments = ref<Comment[]>([...props.comments]);
const content = ref('');
const submitting = ref(false);

const formatter = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

watch(
  () => props.comments,
  (comments) => {
    localComments.value = [...comments];
  },
);

async function submitComment() {
  const trimmed = content.value.trim();
  if (!trimmed) {
    ElMessage.warning('先写点评论内容');
    return;
  }

  submitting.value = true;

  try {
    const comment = await createComment(props.postId, trimmed);
    localComments.value = [comment, ...localComments.value];
    content.value = '';
    emit('created', comment);
    ElMessage.success('评论已发布');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评论发布失败');
  } finally {
    submitting.value = false;
  }
}

function formatTime(value: string) {
  return formatter.format(new Date(value));
}
</script>

<template>
  <section class="comment-list" aria-label="评论">
    <header class="comment-list__header">
      <h2>评论</h2>
      <span>{{ localComments.length }} 条</span>
    </header>

    <form
      v-if="authStore.isLoggedIn"
      class="comment-list__form"
      @submit.prevent="submitComment"
    >
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        maxlength="300"
        show-word-limit
        placeholder="写下你的想法"
      />
      <div class="comment-list__form-actions">
        <el-button
          type="primary"
          native-type="submit"
          :loading="submitting"
        >
          发布评论
        </el-button>
      </div>
    </form>
    <div v-else class="comment-list__login">
      <span>登录后参与讨论</span>
      <RouterLink to="/login">
        <el-button type="primary" plain>登录</el-button>
      </RouterLink>
    </div>

    <div v-if="localComments.length > 0" class="comment-list__items">
      <article
        v-for="comment in localComments"
        :key="comment.id"
        class="comment-item"
      >
        <el-avatar :size="38" :src="comment.author.avatarUrl">
          {{ comment.author.displayName.slice(0, 1) }}
        </el-avatar>
        <div class="comment-item__body">
          <header>
            <RouterLink :to="`/u/${comment.author.id}`">
              {{ comment.author.displayName }}
            </RouterLink>
            <time :datetime="comment.createdAt">{{ formatTime(comment.createdAt) }}</time>
          </header>
          <p>{{ comment.content }}</p>
        </div>
      </article>
    </div>
    <EmptyState
      v-else
      title="暂无评论"
      description="还没有人开场，欢迎留下第一条讨论。"
    />
  </section>
</template>

<style scoped>
.comment-list {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.comment-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.comment-list__header h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
}

.comment-list__header span,
.comment-list__login {
  color: #667085;
}

.comment-list__form {
  display: grid;
  gap: 10px;
}

.comment-list__form-actions {
  display: flex;
  justify-content: flex-end;
}

.comment-list__login {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
}

.comment-list__items {
  display: grid;
  gap: 14px;
}

.comment-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #edf1f7;
}

.comment-item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.comment-item__body {
  min-width: 0;
}

.comment-item__body header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.comment-item__body a {
  color: #172033;
  font-weight: 800;
}

.comment-item__body time {
  color: #98a2b3;
  font-size: 12px;
}

.comment-item__body p {
  margin: 7px 0 0;
  color: #344054;
  line-height: 1.7;
}

@media (max-width: 560px) {
  .comment-list {
    padding: 14px;
  }

  .comment-list__login {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
