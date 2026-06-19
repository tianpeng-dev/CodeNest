<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { ChatDotRound, Message, Star, UserFilled } from '@element-plus/icons-vue';
import { toggleFollow } from '@/services/user.service';
import type { Comment } from '@/types/comment';
import type { Post } from '@/types/post';
import type { User } from '@/types/user';

const props = defineProps<{
  author: User;
  hotPosts: Post[];
  latestComments: Comment[];
}>();

const localAuthor = ref<User>(props.author);
const followed = ref(false);
const followPending = ref(false);

watch(
  () => props.author,
  (author) => {
    localAuthor.value = author;
    followed.value = false;
  },
);

async function handleFollow() {
  followPending.value = true;

  try {
    localAuthor.value = await toggleFollow(localAuthor.value.id);
    followed.value = !followed.value;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '关注失败');
  } finally {
    followPending.value = false;
  }
}

function formatCount(value: number) {
  if (value >= 10000) {
    return `${(value / 10000).toFixed(1)}w`;
  }

  return String(value);
}
</script>

<template>
  <aside class="author-sidebar" aria-label="作者信息">
    <section class="author-card">
      <RouterLink class="author-card__identity" :to="`/u/${localAuthor.id}`">
        <el-avatar :size="54" :src="localAuthor.avatarUrl">
          {{ localAuthor.displayName.slice(0, 1) }}
        </el-avatar>
        <div>
          <strong>{{ localAuthor.displayName }}</strong>
          <span>@{{ localAuthor.username }}</span>
        </div>
      </RouterLink>
      <p>{{ localAuthor.bio }}</p>

      <dl class="author-card__stats">
        <div>
          <dt>文章</dt>
          <dd>{{ formatCount(localAuthor.postCount) }}</dd>
        </div>
        <div>
          <dt>获赞</dt>
          <dd>{{ formatCount(localAuthor.likeCount) }}</dd>
        </div>
        <div>
          <dt>粉丝</dt>
          <dd>{{ formatCount(localAuthor.followerCount) }}</dd>
        </div>
      </dl>

      <div class="author-card__actions">
        <el-button
          type="primary"
          :loading="followPending"
          @click="handleFollow"
        >
          <el-icon><UserFilled /></el-icon>
          <span>{{ followed ? '已关注' : '关注作者' }}</span>
        </el-button>
        <RouterLink :to="{ path: '/messages', query: { to: localAuthor.id } }">
          <el-button>
            <el-icon><Message /></el-icon>
            <span>私信</span>
          </el-button>
        </RouterLink>
      </div>
    </section>

    <section class="side-panel">
      <h2>
        <el-icon><Star /></el-icon>
        <span>热门文章</span>
      </h2>
      <div v-if="hotPosts.length > 0" class="side-list">
        <RouterLink
          v-for="post in hotPosts"
          :key="post.id"
          :to="`/post/${post.id}`"
        >
          <strong>{{ post.title }}</strong>
          <span>{{ formatCount(post.likeCount + post.favoriteCount) }} 热度</span>
        </RouterLink>
      </div>
      <p v-else class="side-empty">暂无热门文章</p>
    </section>

    <section class="side-panel">
      <h2>
        <el-icon><ChatDotRound /></el-icon>
        <span>最新评论</span>
      </h2>
      <div v-if="latestComments.length > 0" class="comment-snippets">
        <article v-for="comment in latestComments" :key="comment.id">
          <strong>{{ comment.author.displayName }}</strong>
          <p>{{ comment.content }}</p>
        </article>
      </div>
      <p v-else class="side-empty">暂无评论动态</p>
    </section>
  </aside>
</template>

<style scoped>
.author-sidebar {
  display: grid;
  gap: 14px;
}

.author-card,
.side-panel {
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.author-card__identity {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.author-card__identity strong,
.author-card__identity span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-card__identity strong {
  color: #172033;
  font-size: 17px;
}

.author-card__identity span,
.author-card p,
.side-empty,
.side-list span {
  color: #667085;
}

.author-card p {
  margin: 12px 0 0;
  line-height: 1.7;
}

.author-card__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 14px 0;
}

.author-card__stats div {
  padding: 9px;
  text-align: center;
  background: #f8fafc;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
}

.author-card__stats dt {
  color: #667085;
  font-size: 12px;
}

.author-card__stats dd {
  margin: 4px 0 0;
  color: #172033;
  font-weight: 800;
}

.author-card__actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.author-card__actions :deep(.el-button) {
  width: 100%;
}

.side-panel h2 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 12px;
  color: #172033;
  font-size: 16px;
}

.side-list,
.comment-snippets {
  display: grid;
  gap: 10px;
}

.side-list a {
  display: grid;
  gap: 5px;
  padding-bottom: 10px;
  border-bottom: 1px solid #edf1f7;
}

.side-list a:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.side-list strong {
  color: #172033;
  line-height: 1.5;
}

.side-list a:hover strong {
  color: #1f4f8f;
}

.comment-snippets article {
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
}

.comment-snippets strong {
  color: #344054;
  font-size: 13px;
}

.comment-snippets p,
.side-empty {
  margin: 6px 0 0;
  line-height: 1.6;
}
</style>
