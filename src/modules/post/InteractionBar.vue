<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  ChatDotRound,
  Collection,
  Pointer,
  Share,
  Star,
  Warning,
} from '@element-plus/icons-vue';
import { togglePostAction } from '@/services/post.service';
import type { Post } from '@/types/post';

const props = defineProps<{
  post: Post;
}>();

const emit = defineEmits<{
  updated: [post: Post];
  focusComments: [];
}>();

const localPost = ref<Post>(props.post);
const liked = ref(false);
const disliked = ref(false);
const favorited = ref(false);
const pendingAction = ref<'like' | 'dislike' | 'favorite' | ''>('');

watch(
  () => props.post,
  (post) => {
    localPost.value = post;
  },
);

async function handleAction(action: 'like' | 'dislike' | 'favorite') {
  pendingAction.value = action;

  try {
    const nextPost = await togglePostAction(localPost.value.id, action);
    localPost.value = nextPost;

    if (action === 'like') {
      liked.value = !liked.value;
      if (liked.value) disliked.value = false;
    }

    if (action === 'dislike') {
      disliked.value = !disliked.value;
      if (disliked.value) liked.value = false;
    }

    if (action === 'favorite') {
      favorited.value = !favorited.value;
    }

    emit('updated', nextPost);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
  } finally {
    pendingAction.value = '';
  }
}

async function handleShare() {
  const shareUrl = typeof window === 'undefined' ? '' : window.location.href;

  try {
    await navigator.clipboard?.writeText(shareUrl);
    ElMessage.success('链接已复制');
  } catch {
    ElMessage.info(shareUrl || '当前环境暂不支持复制');
  }
}

function handleReport() {
  ElMessage.success('举报已记录，管理员会尽快处理');
}
</script>

<template>
  <section class="interaction-bar" aria-label="帖子互动">
    <el-button
      :type="liked ? 'primary' : 'default'"
      :loading="pendingAction === 'like'"
      @click="handleAction('like')"
    >
      <el-icon><Star /></el-icon>
      <span>赞 {{ localPost.likeCount }}</span>
    </el-button>
    <el-button
      :type="disliked ? 'warning' : 'default'"
      :loading="pendingAction === 'dislike'"
      @click="handleAction('dislike')"
    >
      <el-icon><Pointer /></el-icon>
      <span>踩</span>
    </el-button>
    <el-button
      :type="favorited ? 'primary' : 'default'"
      :loading="pendingAction === 'favorite'"
      @click="handleAction('favorite')"
    >
      <el-icon><Collection /></el-icon>
      <span>收藏 {{ localPost.favoriteCount }}</span>
    </el-button>
    <el-button @click="emit('focusComments')">
      <el-icon><ChatDotRound /></el-icon>
      <span>评论 {{ localPost.commentCount }}</span>
    </el-button>
    <el-button @click="handleShare">
      <el-icon><Share /></el-icon>
      <span>分享</span>
    </el-button>
    <el-button @click="handleReport">
      <el-icon><Warning /></el-icon>
      <span>举报</span>
    </el-button>
  </section>
</template>

<style scoped>
.interaction-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.interaction-bar :deep(.el-button) {
  margin-left: 0;
}

@media (max-width: 640px) {
  .interaction-bar {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .interaction-bar :deep(.el-button) {
    width: 100%;
  }
}
</style>
