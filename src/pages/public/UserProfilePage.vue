<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Collection, Message, Star, UserFilled } from '@element-plus/icons-vue';
import EmptyState from '@/components/EmptyState.vue';
import PostList from '@/modules/post/PostList.vue';
import { getPosts } from '@/services/post.service';
import { getUserProfile, toggleFollow } from '@/services/user.service';
import type { Post } from '@/types/post';
import type { User } from '@/types/user';

const route = useRoute();
const userId = computed(() => String(route.params.id ?? ''));
const user = ref<User | null>(null);
const posts = ref<Post[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 6;
const loading = ref(false);
const followPending = ref(false);
const followed = ref(false);
const errorMessage = ref('');
const requestSequence = ref(0);

async function loadProfile() {
  const requestId = requestSequence.value + 1;
  requestSequence.value = requestId;
  loading.value = true;
  errorMessage.value = '';
  user.value = null;
  posts.value = [];
  total.value = 0;
  followed.value = false;

  try {
    const profile = await getUserProfile(userId.value);
    const result = await getPosts({
      authorId: profile.id,
      status: 'published',
      sortBy: 'latest',
      page: page.value,
      pageSize,
    });

    if (requestId !== requestSequence.value) return;

    user.value = profile;
    posts.value = result.items;
    total.value = result.total;
  } catch (error) {
    if (requestId !== requestSequence.value) return;

    errorMessage.value =
      error instanceof Error ? error.message : '用户主页加载失败';
  } finally {
    if (requestId === requestSequence.value) {
      loading.value = false;
    }
  }
}

async function handleFollow() {
  if (!user.value) return;
  followPending.value = true;

  try {
    user.value = await toggleFollow(user.value.id);
    followed.value = !followed.value;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '关注失败');
  } finally {
    followPending.value = false;
  }
}

function handlePageChange(nextPage: number) {
  page.value = nextPage;
  loadProfile();
}

watch(
  userId,
  () => {
    page.value = 1;
    loadProfile();
  },
  { immediate: true },
);
</script>

<template>
  <div class="user-profile-page">
    <el-skeleton v-if="loading" :rows="10" animated />

    <EmptyState
      v-else-if="errorMessage"
      title="用户主页加载失败"
      :description="errorMessage"
    >
      <template #action>
        <el-button type="primary" @click="loadProfile">重新加载</el-button>
      </template>
    </EmptyState>

    <EmptyState
      v-else-if="!user"
      title="没有找到用户"
      description="该用户不存在或暂时无法访问。"
    />

    <template v-else>
      <section class="profile-card">
        <el-avatar :size="72" :src="user.avatarUrl">
          {{ user.displayName.slice(0, 1) }}
        </el-avatar>
        <div class="profile-card__content">
          <span class="section-kicker">用户主页</span>
          <h1>{{ user.displayName }}</h1>
          <p>@{{ user.username }} · {{ user.bio }}</p>
          <div class="profile-card__actions">
            <el-button
              type="primary"
              :loading="followPending"
              @click="handleFollow"
            >
              <el-icon><UserFilled /></el-icon>
              <span>{{ followed ? '已关注' : '关注' }}</span>
            </el-button>
            <RouterLink :to="{ path: '/messages', query: { to: user.id } }">
              <el-button>
                <el-icon><Message /></el-icon>
                <span>私信</span>
              </el-button>
            </RouterLink>
          </div>
        </div>
        <dl class="profile-card__stats">
          <div>
            <dt>文章</dt>
            <dd>{{ user.postCount }}</dd>
          </div>
          <div>
            <dt>获赞</dt>
            <dd>{{ user.likeCount }}</dd>
          </div>
          <div>
            <dt>收藏</dt>
            <dd><el-icon><Collection /></el-icon>{{ user.favoriteCount }}</dd>
          </div>
          <div>
            <dt>粉丝</dt>
            <dd><el-icon><Star /></el-icon>{{ user.followerCount }}</dd>
          </div>
        </dl>
      </section>

      <section class="profile-posts">
        <header class="profile-posts__header">
          <h2>发布内容</h2>
          <span>{{ total }} 篇已发布</span>
        </header>

        <PostList v-if="posts.length > 0" :posts="posts" />
        <EmptyState
          v-else
          title="还没有公开内容"
          description="作者发布后的文章会出现在这里。"
        />

        <el-pagination
          v-if="total > pageSize"
          class="profile-posts__pagination"
          background
          layout="prev, pager, next"
          :page-size="pageSize"
          :total="total"
          :current-page="page"
          @current-change="handlePageChange"
        />
      </section>
    </template>
  </div>
</template>

<style scoped>
.user-profile-page {
  display: grid;
  gap: 18px;
}

.profile-card,
.profile-posts {
  background: #ffffff;
  border: 1px solid #dce3ee;
  border-radius: 8px;
}

.profile-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) 360px;
  gap: 18px;
  align-items: center;
  padding: 22px;
}

.section-kicker {
  color: #1f4f8f;
  font-size: 13px;
  font-weight: 800;
}

.profile-card h1 {
  margin: 4px 0 0;
  color: #172033;
  font-size: 30px;
}

.profile-card p {
  margin: 8px 0 0;
  color: #667085;
  line-height: 1.7;
}

.profile-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.profile-card__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.profile-card__stats div {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e4eaf3;
  border-radius: 8px;
}

.profile-card__stats dt {
  color: #667085;
  font-size: 12px;
}

.profile-card__stats dd {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 6px 0 0;
  color: #172033;
  font-size: 18px;
  font-weight: 800;
}

.profile-posts {
  padding: 20px;
}

.profile-posts__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.profile-posts__header h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
}

.profile-posts__header span {
  color: #667085;
}

.profile-posts__pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 980px) {
  .profile-card {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .profile-card__stats {
    grid-column: 1 / -1;
  }
}

@media (max-width: 560px) {
  .profile-card {
    grid-template-columns: 1fr;
  }
}
</style>
